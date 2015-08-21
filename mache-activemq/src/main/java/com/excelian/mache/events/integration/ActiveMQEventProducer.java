package com.excelian.mache.events.integration;

import com.excelian.mache.events.BaseCoordinationEntryEventProducer;
import com.excelian.mache.observable.coordination.CoordinationEntryEvent;

import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;


public class ActiveMQEventProducer extends BaseCoordinationEntryEventProducer {
    private static final Logger LOG = LoggerFactory.getLogger(ActiveMQEventProducer.class);
    private final ActiveMqConfig config;
    Session session;
    MessageProducer producer;

    protected ActiveMQEventProducer(Connection connection, String topicName, ActiveMqConfig config) throws JMSException {
        super(topicName);
        this.config = config;

        session = connection.createSession(false, config.getAutoAcknowledge());
        Destination destination = session.createTopic(getTopicName());

        producer = session.createProducer(destination);
        producer.setDeliveryMode(config.getDeliveryMode());

        producer.setTimeToLive(config.getTimeToLiveInMillis());
    }

    @Override
    public void send(final CoordinationEntryEvent<?> event) {

        Gson gson = new Gson();
        TextMessage message;
        try {
            String payload = gson.toJson(event);
            message = session.createTextMessage(payload);
            producer.send(message);
            LOG.debug("SEND: {}", payload);
        } catch (JMSException e) {
            LOG.error("Error sending message: {}", e);
            throw new RuntimeException("Error while sending event", e);
        }
    }

    @Override
    public void close() {
        if (producer != null) {
            try {
                producer.close();
                session.close();
            } catch (JMSException e) {
                // ignored
            }
            producer = null;
        }
    }
}