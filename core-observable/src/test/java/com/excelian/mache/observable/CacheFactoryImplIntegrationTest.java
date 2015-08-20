package com.excelian.mache.observable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.jms.JMSException;

import com.excelian.mache.core.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.excelian.mache.events.MQConfiguration;
import com.excelian.mache.events.MQFactory;
import com.excelian.mache.events.integration.ActiveMQFactory;
import com.excelian.mache.observable.utils.UUIDUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CacheFactoryImplIntegrationTest {
	private static final String LOCAL_MQ = "vm://localhost";

	MacheLoader<String, TestEntity, String> cacheLoader;
	MQConfiguration mqConfiguration = () -> "testTopic";

	MQFactory mqFactory1;
	CacheFactory cacheFactory1;

	MQFactory mqFactory2;
	CacheFactory cacheFactory2;

	Mache<String, TestEntity> unspiedCache1;
	Mache<String, TestEntity> spiedCache1;

	TestEntity testValue = new TestEntity("testValue");
	TestEntity testValue2 = new TestEntity("testValue2");

	@Mock
	MacheFactory spiedMacheFactory;
	
	MacheFactory macheFactory;

	private final UUIDUtils uuidUtils = new UUIDUtils();

	@Before
	public void beforeTest() throws JMSException {
		MockitoAnnotations.initMocks(this);
		
		cacheLoader = new InMemoryCacheLoader<>("loaderForTestEntity");

		macheFactory = new MacheFactory();

		mqFactory1 = new ActiveMQFactory(LOCAL_MQ);
		cacheFactory1 = new CacheFactoryImpl(mqFactory1, mqConfiguration, spiedMacheFactory, uuidUtils);

		unspiedCache1 = macheFactory.create(cacheLoader);
		spiedCache1 = spy(unspiedCache1);
		when(spiedMacheFactory.create(cacheLoader)).thenReturn(spiedCache1);

		mqFactory2 = new ActiveMQFactory(LOCAL_MQ);
		cacheFactory2 = new CacheFactoryImpl(mqFactory2, mqConfiguration, macheFactory, uuidUtils);
	}

	@After
	public void TearDown() throws IOException {
		mqFactory1.close();
		mqFactory2.close();
	}

	@Test
	public void shouldProperlySetupCachesUsingSameCacheLoader() throws ExecutionException, InterruptedException {
		Mache<String, TestEntity> cache1 = cacheFactory1.createCache(cacheLoader);
		cache1.put(testValue.pkey, testValue);

		Mache<String, TestEntity> cache2 = cacheFactory2.createCache(cacheLoader);

		assertEquals(testValue, cache2.get(testValue.pkey));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shouldProperlyInvalidateFromAnotherCacheWhenItemPut() throws ExecutionException, InterruptedException {
		Mache<String, TestEntity> cache1 = cacheFactory1.createCache(cacheLoader);
		Mache<String, TestEntity> cache2 = cacheFactory2.createCache(cacheLoader);

		reset(spiedCache1);

		Thread.sleep(500); //some time for all listeners to connect
		cache2.put(testValue2.pkey, testValue2);

		Thread.sleep(2000);//give time for the message to propagate and invalidate to be called

		verify(spiedCache1).invalidate(testValue2.pkey);
	}

	@Test
	public void shouldProperlyPropagateValues() throws ExecutionException, InterruptedException, JMSException {
		MacheLoader<String, TestEntity2, String> cacheLoader = new InMemoryCacheLoader<>("loaderForTestEntity2");
		MQFactory mqFactory1 = new ActiveMQFactory(LOCAL_MQ);
		CacheFactory cacheFactory1 = new CacheFactoryImpl(mqFactory1, mqConfiguration, new MacheFactory(), new UUIDUtils());
		MQFactory mqFactory2 = new ActiveMQFactory(LOCAL_MQ);
		CacheFactory cacheFactory2 = new CacheFactoryImpl(mqFactory2, mqConfiguration, new MacheFactory(), new UUIDUtils());

		Mache<String, TestEntity2> cache1 = cacheFactory1.createCache(cacheLoader);
		Mache<String, TestEntity2> cache2 = cacheFactory2.createCache(cacheLoader);

		final String key1 = "X1";
		final String val1 = "someValue1";

		cache1.put(key1, new TestEntity2(key1, val1));
		assertEquals(val1, cache2.get(key1).otherValue);

		final String val2 = "someValue2";
		cache1.put(key1, new TestEntity2(key1, val2));
		Thread.sleep(500);
		assertEquals(val2, cache2.get(key1).otherValue);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shouldNotInvalidateFromAnotherCacheWhenItemFetched() throws ExecutionException, InterruptedException {
		Mache<String, TestEntity> cache1 = cacheFactory1.createCache(cacheLoader);
		Mache<String, TestEntity> cache2 = cacheFactory2.createCache(cacheLoader);

		/* insert data into loader and ensure it is within cache */
		cache1.put(testValue2.pkey, testValue2);
		assertNotNull(cache1.get(testValue2.pkey));
		Thread.sleep(1000);//give time for the message to propagate and invalidate to be called from put

		/* reset mocks */
		reset(spiedCache1);
		/* pull it into 2nd cache (this should NOT affect any other cache*/
		assertNotNull(cache2.get(testValue2.pkey));
		Thread.sleep(1000);//give time for any messages to propagate and invalidate to 'potentially' called

		verify(spiedCache1, never()).invalidate(testValue2.pkey);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shouldNotInvalidateSameCacheOnPut() throws ExecutionException, InterruptedException {
		Mache<String, TestEntity> cache1 = cacheFactory1.createCache(cacheLoader);

		reset(spiedCache1);
		cache1.put(testValue2.pkey, testValue2);

		Thread.sleep(1000);//give time for the message to propagate and invalidate to be called

		verify(spiedCache1, times(0)).invalidate(testValue2.pkey);
	}
}