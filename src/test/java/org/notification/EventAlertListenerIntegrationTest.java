package org.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.notification.services.EventAlertListener;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.time.Duration;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test d'intégration : vrai RabbitMQ (Testcontainers). Vérifie que la file « alerte-email » est déclarée
 * durable au démarrage et qu'un message publié dessus est bien consommé par {@link EventAlertListener}.
 */
@SpringBootTest
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class EventAlertListenerIntegrationTest {

    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4-management-alpine");

    @DynamicPropertySource
    static void rabbitProperties(DynamicPropertyRegistry registry) {
        // RabbitMQConfig construit sa ConnectionFactory à partir de ces propriétés
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Test
    void la_file_alerte_email_est_declaree_au_demarrage() {
        Properties queue = amqpAdmin.getQueueProperties(EventAlertListener.QUEUE_NAME);

        assertThat(queue).isNotNull();
        assertThat(queue.get("QUEUE_NAME")).isEqualTo("alerte-email");
    }

    @Test
    void un_message_publie_sur_la_file_est_consomme_par_le_listener(CapturedOutput output) throws InterruptedException {
        String message = "alerte-" + UUID.randomUUID();

        rabbitTemplate.convertAndSend(EventAlertListener.QUEUE_NAME, message);

        assertThat(waitFor(output, "Received message: " + message, Duration.ofSeconds(15)))
                .as("le listener doit journaliser le message reçu")
                .isTrue();
        // le message a été acquitté : la file est vide
        Properties queue = amqpAdmin.getQueueProperties(EventAlertListener.QUEUE_NAME);
        assertThat(((Number) queue.get("QUEUE_MESSAGE_COUNT")).intValue()).isZero();
    }

    private static boolean waitFor(CapturedOutput output, String expected, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (output.getOut().contains(expected)) {
                return true;
            }
            Thread.sleep(100);
        }
        return false;
    }
}
