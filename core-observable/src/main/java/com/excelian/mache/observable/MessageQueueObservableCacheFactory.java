package com.excelian.mache.observable;

import javax.cache.event.CacheEntryListenerException;

import com.excelian.mache.observable.coordination.CoordinationEntryEvent;
import com.excelian.mache.observable.coordination.RemoteCacheEntryListener;
import com.excelian.mache.core.MacheFactory;
import com.excelian.mache.core.Mache;
import com.excelian.mache.core.MacheLoader;
import com.excelian.mache.events.BaseCoordinationEntryEventConsumer;
import com.excelian.mache.events.MQConfiguration;
import com.excelian.mache.events.MQFactory;
import com.excelian.mache.observable.utils.UUIDUtils;

//TODO create artifact to put this class into - it probably will be final artifact depending on anything else
public class MessageQueueObservableCacheFactory implements ObservableCacheFactory {
	private final MQFactory communicationFactory;
	private final MQConfiguration configuration;
	private final MacheFactory macheFactory;
	private final UUIDUtils uuidUtils;

	public MessageQueueObservableCacheFactory(final MQFactory communicationFactory, final MQConfiguration configuration, final MacheFactory macheFactory, final UUIDUtils uuidUtils) {
		this.communicationFactory = communicationFactory;
		this.configuration = configuration;
		this.macheFactory = macheFactory;
		this.uuidUtils = uuidUtils;
	}

	@Override
	public <K, V, D> Mache<K, V> createCache(final MacheLoader<K, V, D> cacheLoader) {
		return createCache(macheFactory.create(cacheLoader));
	}


	// TODO introduce cacheLoaderFactory after moved to proper artifact
	@Override
	public <K, V> Mache<K, V> createCache(final Mache<K, V> underlyingCache) {
	  final ObservableMap<K, V> observable = new ObservableMap<K, V>(underlyingCache, uuidUtils);

		observable.registerListener(communicationFactory.getProducer(configuration));

		try {
			BaseCoordinationEntryEventConsumer consumer=communicationFactory.getConsumer(configuration);
			consumer.registerEventListener(
					new RemoteCacheEntryListener() {
						@Override
						public void onUpdated(Iterable<CoordinationEntryEvent<?>> events) throws CacheEntryListenerException {
							handle(events);
						}

						@Override
						public void onInvalidate(Iterable<CoordinationEntryEvent<?>> events) throws CacheEntryListenerException {
							handle(events);
						}

						@Override
						public void onCreated(Iterable<CoordinationEntryEvent<?>> events) throws CacheEntryListenerException {
							handle(events);
						}

						@Override
						public void onRemoved(Iterable<CoordinationEntryEvent<?>> events)
								throws CacheEntryListenerException {
							handle(events);
						}

						public void handle(Iterable<CoordinationEntryEvent<?>> events)
						{
							for (final CoordinationEntryEvent<?> e : events) {
								if (e.getEntityName().equals(underlyingCache.getName()) && !e.getCacheId().equals(underlyingCache.getId())) {
									@SuppressWarnings("unchecked")
									K key = (K) e.getKey();
									underlyingCache.invalidate(key);//if we called remove it would go to the DB too.
								}
							}
						}
					});
			consumer.beginSubscriptionThread();
		} catch (Exception  e ) {
			throw new RuntimeException("Error creating cache consumer from underlying cache " + underlyingCache , e);
		}
		return observable;
	}

}