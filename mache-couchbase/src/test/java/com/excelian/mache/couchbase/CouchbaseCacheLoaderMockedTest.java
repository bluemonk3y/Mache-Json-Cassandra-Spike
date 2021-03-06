package com.excelian.mache.couchbase;

import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.cluster.BucketSettings;
import com.couchbase.client.java.cluster.ClusterManager;
import com.couchbase.client.java.cluster.DefaultBucketSettings;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.excelian.mache.core.SchemaOptions;
import com.excelian.mache.couchbase.CouchbaseCacheLoader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;

import static org.mockito.Mockito.*;

@PrepareForTest(CouchbaseCluster.class)
@PowerMockIgnore("javax.management.*")
@RunWith(PowerMockRunner.class)
public class CouchbaseCacheLoaderMockedTest {

    CouchbaseCacheLoader loader;
    CouchbaseCluster mockedCluster;
    ClusterManager mockedManager;

    @Before
    public void mockCouchbaseCluster() {
        PowerMockito.mockStatic(CouchbaseCluster.class);
        mockedCluster = mock(CouchbaseCluster.class);
        when(CouchbaseCluster.create(any(CouchbaseEnvironment.class),
                anyListOf(String.class))).thenReturn(mockedCluster);
        mockedManager = mock(ClusterManager.class);
        when(mockedCluster.clusterManager(anyString(), anyString())).thenReturn(mockedManager);
    }

    @Test
    public void shouldCreateConnection() throws Exception {
        givenCacheLoaderWith(SchemaOptions.USE_EXISTING_SCHEMA);
        loader.create();
        thenClusterManagerAndBucketCreated(mockedCluster);
    }

    @Test
    public void shouldCreateBucket() throws Exception {
        givenCacheLoaderWith(SchemaOptions.CREATE_SCHEMA_IF_NEEDED);

        when(mockedManager.hasBucket(anyString())).thenReturn(false);
        when(mockedCluster.clusterManager(anyString(), anyString())).thenReturn(mockedManager);

        loader.create();

        thenBucketCreated();
        thenClusterManagerAndBucketCreated(mockedCluster);
    }

    @Test
    public void shouldDropAndCreateBucket() throws Exception {
        givenCacheLoaderWith(SchemaOptions.CREATE_AND_DROP_SCHEMA);

        when(mockedManager.hasBucket(anyString())).thenAnswer(getAlternatingBooleanAnswer());
        when(mockedCluster.clusterManager(anyString(), anyString())).thenReturn(mockedManager);

        loader.create();

        thenBucketDropped();
        thenBucketCreated();
        thenClusterManagerAndBucketCreated(mockedCluster);
    }

    @Test
    public void shouldCloseCluster() throws Exception {
        givenCacheLoaderWith(SchemaOptions.USE_EXISTING_SCHEMA);
        loader.create();
        loader.close();
        thenClusterClosed();
    }

    @Test
    public void shouldCloseClusterAndDropSchema() throws Exception {
        givenCacheLoaderWith(SchemaOptions.CREATE_AND_DROP_SCHEMA);
        loader.create();
        when(mockedManager.hasBucket(anyString())).thenReturn(true);
        loader.close();
        thenClusterClosed();
        thenBucketDropped();
    }

    private void thenClusterClosed() {
        verify(mockedCluster).disconnect();
    }

    private void thenBucketCreated() {
        verify(mockedManager).insertBucket(any(BucketSettings.class));
    }

    private void thenBucketDropped() {
        verify(mockedManager).removeBucket(eq("test"));
    }

    private void thenClusterManagerAndBucketCreated(CouchbaseCluster mockedCluster) {
        verify(mockedCluster).clusterManager("Admin", "Pass");
        verify(mockedCluster).openBucket("test");
    }

    private void givenCacheLoaderWith(SchemaOptions schemaOptions) {
        BucketSettings bucket = DefaultBucketSettings.builder().name("test").build();
        loader = new CouchbaseCacheLoader<>(String.class, Object.class, bucket, DefaultCouchbaseEnvironment.create(),
                Collections.singletonList("localhost"), "Admin", "Pass", schemaOptions);
    }


    private Answer<Boolean> getAlternatingBooleanAnswer() {
        return new Answer<Boolean>() {
            boolean returnValue = false;

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                returnValue = !returnValue;

                return returnValue;
            }
        };
    }

}
