package com.excelian.mache.observable.coordination;

import javax.cache.event.CacheEntryListenerException;

public interface RemoteCacheEntryUpdatedListener<K> extends CoordinationEventListener<K> {
    void onUpdated(Iterable<CoordinationEntryEvent<K>> events)
            throws CacheEntryListenerException;
}
