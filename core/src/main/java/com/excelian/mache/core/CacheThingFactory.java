package com.excelian.mache.core;

public class CacheThingFactory {
    public <K, V, D> Mache<K, V> create(MacheLoader<K, V, D> cacheLoader, String... options) {
        return new MacheImpl<K, V>(cacheLoader, options);
    }
}
