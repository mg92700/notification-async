package org.notification.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EventAlertListener {

    private static final Logger logger = LoggerFactory.getLogger(EventAlertListener.class);
    public static final String QUEUE_NAME = "alerte-email";


    @RabbitListener(queues = QUEUE_NAME)
    public void listen(String message) {
        logger.info("Received message: {}", message);

    }
}