package com.excelian.mache.events.integration;

import com.excelian.mache.events.BaseCoordinationEntryEventConsumer;
import com.excelian.mache.events.BaseCoordinationEntryEventProducer;
import com.excelian.mache.events.MQConfiguration;
import com.excelian.mache.events.MQFactory;
import com.excelian.mache.observable.coordination.CoordinationEntryEvent;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import javax.jms.JMSException;

public class RabbitMQFactory<K> implements MQFactory<K> {

    private final Channel channel;
    private final Connection connection;
    private final RabbitMqConfig rabbitMqConfig;

    public RabbitMQFactory(ConnectionFactory factory, RabbitMqConfig rabbitMqConfig) throws JMSException, IOException {
        this.rabbitMqConfig = rabbitMqConfig;
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.exchangeDeclare(rabbitMqConfig.getExchangeName(), "direct", true);
    }

    @Override
    public BaseCoordinationEntryEventProducer<K> getProducer(MQConfiguration config) {
        return new RabbitMQEventProducer<>(channel, config.getTopicName(), rabbitMqConfig);
    }

    @Override
    public BaseCoordinationEntryEventConsumer<K> getConsumer(MQConfiguration config) throws IOException, JMSException {
        return new RabbitMQEventConsumer<>(channel, config.getTopicName(), rabbitMqConfig);
    }

    @Override
    public void close() throws IOException {
        channel.close();
        connection.close();
    }
}
