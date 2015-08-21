package com.excelian.mache.events.integration;

import com.excelian.mache.events.BaseCoordinationEntryEventConsumer;
import com.excelian.mache.events.BaseCoordinationEntryEventProducer;
import com.excelian.mache.events.MQConfiguration;
import com.excelian.mache.events.MQFactory;

import java.io.IOException;
import java.util.Properties;
import javax.jms.JMSException;

public class KafkaMQFactory implements MQFactory {
    private final String connectionString;

    public KafkaMQFactory(String connectionString) throws JMSException, IOException {
        this.connectionString = connectionString;
    }

    Properties createProducerConfig(String zooKeeper) {
        String ZK_PORT = "9092";
        String ZK_CONNECTION = zooKeeper + ":" + ZK_PORT;

        Properties producerProperies = new Properties();
        producerProperies.put("metadata.broker.list", ZK_CONNECTION);
        producerProperies.put("serializer.class", "kafka.serializer.StringEncoder");
        //props.put("partitioner.class", "com.test.groups.SimplePartitioner");//https://cwiki.apache.org/confluence/display/KAFKA/0.8.0+Producer+Example
        producerProperies.put("request.required.acks", "1");

        return producerProperies;
    }

    Properties CreateConsumerConfig(String zooKeeper) {
        String ZK_PORT = "2181";
        String ZK_CONNECTION = zooKeeper + ":" + ZK_PORT;

        Properties consumerProperties = new Properties();
        consumerProperties.put("metadata.broker.list", ZK_CONNECTION);
        consumerProperties.put("zookeeper.connect", ZK_CONNECTION);
        consumerProperties.put("zookeeper.session.timeout.ms", "8000");
        consumerProperties.put("zookeeper.sync.time.ms", "100");
        consumerProperties.put("auto.commit.interval.ms", "50");
        consumerProperties.put("auto.offset.reset", "largest");//seeks to end

        return consumerProperties;
    }

    @Override
    public BaseCoordinationEntryEventProducer getProducer(MQConfiguration config) {
        return new KafkaEventProducer(createProducerConfig(connectionString), config.getTopicName());
    }

    @Override
    public BaseCoordinationEntryEventConsumer getConsumer(MQConfiguration config) throws IOException, JMSException {
        return new KafkaEventConsumer(CreateConsumerConfig(connectionString), config.getTopicName());
    }

    @Override
    public void close() throws IOException {
    }
}