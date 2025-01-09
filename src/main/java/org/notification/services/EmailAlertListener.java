package org.notification.services;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
@Component
public class EmailAlertListener {

    public static final String QUEUE_NAME = "alerte-email";

    @RabbitListener(queues = QUEUE_NAME)
    public void listen(String message) {
        System.out.println("Received message: " + message);
        // Traitement du message
    }
}