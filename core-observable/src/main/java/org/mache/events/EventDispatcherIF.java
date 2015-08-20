package org.mache.events;

import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListener;
import java.util.List;

public interface EventDispatcherIF<K, V> {
    void registerEventListener(CacheEntryListener<K, V> listener);

    void addEvent(CacheEntryEvent<K, V> event);

    List<? extends CacheEntryEvent<K, V>> getQueuedEvents();

    void fire();

    void consume();
}
