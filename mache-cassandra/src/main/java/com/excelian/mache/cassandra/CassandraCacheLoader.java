package com.excelian.mache.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.DriverException;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.TokenAwarePolicy;
import com.excelian.mache.core.AbstractCacheLoader;
import com.excelian.mache.core.SchemaOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cassandra.core.cql.CqlIdentifier;
import org.springframework.data.cassandra.convert.MappingCassandraConverter;
import org.springframework.data.cassandra.core.CassandraAdminTemplate;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.mapping.Table;

import java.util.HashMap;

/**
 * CacheLoader to bind Cassandra API onto the GuavaCache
 *
 * @implNote : Replication class and factor need to be configurable.
 */
public class CassandraCacheLoader<K, V> extends AbstractCacheLoader<K, V, Session> {

    private static final Logger LOG = LoggerFactory.getLogger(CassandraCacheLoader.class);

    private final Cluster cluster;
    private final CassandraConfig config;
    private Session session;
    private SchemaOptions schemaOption;
    private final String keySpace;

    private boolean isTableCreated = false;

    private final Class<V> clazz;

    public CassandraCacheLoader(Class<V> clazz, Cluster cluster, SchemaOptions schemaOption, String keySpace, CassandraConfig config) {
        this.cluster = cluster;
        this.config = config;
        this.cluster.getConfiguration().getQueryOptions().setConsistencyLevel(config.getConsistencyLevel());
        this.schemaOption = schemaOption;
        this.keySpace = keySpace.replace("-", "_").replace(" ", "_").replace(":", "_");
        this.clazz = clazz;
    }

    @Override
    public void create() {
        if (schemaOption.shouldCreateSchema() && session == null) {
            synchronized (this) {
                if (session == null) {
                    try {
                        session = cluster.connect();
                        if (schemaOption.shouldCreateSchema()) {
                            createKeySpace();
                        }
                        createTable();
                    } catch (DriverException e) {
                        LOG.error("Failed to create keyspace : {}.\\n{}", keySpace, e);
                    }
                }
            }
        } else {
            session = cluster.connect(keySpace);
        }
    }

    private void createKeySpace() {
        session.execute(String.format("CREATE KEYSPACE IF NOT EXISTS %s  WITH REPLICATION = {'class':'%s', 'replication_factor':%d}; ", keySpace, config.getReplicationClass(), config.getReplicationFactor()));
        session.execute(String.format("USE %s ", keySpace));
        LOG.info("Created keyspace if missing {}", keySpace);
    }

    private void createTable() {
        if (!isTableCreated) {
            isTableCreated = true;
            CassandraAdminTemplate adminTemplate = new CassandraAdminTemplate(session, new MappingCassandraConverter());
            adminTemplate.createTable(true, new CqlIdentifier(getTableName()), clazz, new HashMap<>());
        }
    }

    public void put(K key, V value) {
        ops().insert(value);
    }

    public void remove(K key) {
        ops().deleteById(clazz, key);
    }

    @Override
    public V load(K key) throws Exception {
        V value = ops().selectOneById(clazz, key);
        LOG.trace("Loaded value from DB : {}", key);
        return value;
    }

    @Override
    public void close() {
        if (session != null && !session.isClosed()) {
            if (schemaOption.shouldDropSchema()) {
                try {
                    session.execute(String.format("DROP KEYSPACE %s; ", keySpace));
                    LOG.info("Dropped keyspace {}", keySpace);
                } catch (DriverException e) {
                    LOG.error("Failed to drop keyspace : {}. err={}", keySpace, e);
                }
            }
            session.close();
        }
    }

    @Override
    public String getName() {
        return clazz.getSimpleName();
    }

    @Override
    public Session getDriverSession() {
        if (session == null)
            throw new IllegalStateException("Session has not been created - read/write to cache first");
        return session;
    }

    private CassandraOperations ops() {
        return new CassandraTemplate(session);
    }

    public static Cluster connect(String contactPoint, String clusterName, int port, CassandraConfig config) {
        Cluster cluster = Cluster.builder()
            .addContactPoint(contactPoint)
            .withPort(port)
            .withClusterName(clusterName)
            .withReconnectionPolicy(config.getReconnectionPolicy())
            .withLoadBalancingPolicy(new TokenAwarePolicy(new DCAwareRoundRobinPolicy())) // go to the node with data
            .build();
        Metadata metadataToWarmConnection = cluster.getMetadata();
        return cluster;
    }

    private String getTableName() {
        final Table annotation = getAnnotationOrThrow();
        final String value = annotation.value();
        if (value.length() == 0) {
            return clazz.getSimpleName();
        }
        return value;
    }

    private Table getAnnotationOrThrow() {
        final Table annotation = clazz.getAnnotation(Table.class);
        if (annotation == null) {
            throw new RuntimeException("Stored class [" + clazz.getSimpleName() + "] must specify Table annotation.");
        }
        return annotation;
    }
}