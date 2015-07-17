package org.mache.examples.mongo;

import com.mongodb.ServerAddress;
import org.mache.*;
import org.mache.events.MQConfiguration;
import org.mache.events.MQFactory;
import org.mache.events.integration.RabbitMQFactory;

import javax.jms.JMSException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by jbowkett on 17/07/15.
 */
public class MongoExample {
  
  protected static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
  
  public ExCache<String, MongoAnnotatedMessage> exampleCache() throws Exception {
    final MongoDBCacheLoader<String, MongoAnnotatedMessage> cacheLoader = getCacheLoader();
    final CacheFactory cacheFactory = getCacheFactory();
    return cacheFactory.createCache(cacheLoader);
  }

  private MongoDBCacheLoader<String, MongoAnnotatedMessage> getCacheLoader() {
    final List<ServerAddress> serverAddresses = Arrays.asList(new ServerAddress("10.28.1.140", 27017));
    final String keySpace = "NoSQL_MacheClient_Test_" + DATE_FORMAT.format(new Date());
    return new MongoDBCacheLoader<>(MongoAnnotatedMessage.class, serverAddresses, SchemaOptions.CREATEANDDROPSCHEMA, keySpace);
  }


  private CacheFactory getCacheFactory() throws JMSException, IOException {
    System.out.println("Creating CacheFactory...");
    final MQConfiguration mqConfiguration = () -> "testTopic";
    final CacheThingFactory cacheThingFactory = new CacheThingFactory();
    final String LOCAL_MQ = "localhost";
    final MQFactory mqFactory = new RabbitMQFactory(LOCAL_MQ);
    final CacheFactoryImpl cacheFactory = new CacheFactoryImpl(mqFactory, mqConfiguration, cacheThingFactory);
    System.out.println("Cache Factory Created.");
    return cacheFactory;
  }
}
