package com.excelian.mache.events;

import com.excelian.mache.observable.EventType;
import com.excelian.mache.observable.coordination.CoordinationEntryEvent;
import com.excelian.mache.observable.coordination.CoordinationEventListener;
import com.excelian.mache.observable.coordination.RemoteCacheEntryCreatedListener;
import com.excelian.mache.observable.coordination.RemoteCacheEntryInvalidateListener;
import com.excelian.mache.observable.coordination.RemoteCacheEntryRemovedListener;
import com.excelian.mache.observable.coordination.RemoteCacheEntryUpdatedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.jms.JMSException;

public abstract class BaseCoordinationEntryEventConsumer implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(BaseCoordinationEntryEventConsumer.class);

    public abstract void beginSubscriptionThread() throws InterruptedException, JMSException, IOException;

    public abstract void close();

    protected ConcurrentHashMap<EventType, ArrayList<CoordinationEventListener>> eventMap;
    private String topicName;

    protected BaseCoordinationEntryEventConsumer(String topicName) {
        //TODO lists need to be threadsafe ?
        eventMap = new ConcurrentHashMap<>();
        eventMap.putIfAbsent(EventType.CREATED, new ArrayList<>());
        eventMap.putIfAbsent(EventType.REMOVED, new ArrayList<>());
        eventMap.putIfAbsent(EventType.UPDATED, new ArrayList<>());
        eventMap.putIfAbsent(EventType.INVALIDATE, new ArrayList<>());

        this.topicName = topicName;
    }

    public String getTopicName() {
        return topicName;
    }

    public void registerEventListener(CoordinationEventListener listener) {
        if (listener instanceof RemoteCacheEntryCreatedListener) {
            eventMap.get(EventType.CREATED).add(listener);
        }

        if (listener instanceof RemoteCacheEntryUpdatedListener) {
            eventMap.get(EventType.UPDATED).add(listener);
        }

        if (listener instanceof RemoteCacheEntryRemovedListener) {
            eventMap.get(EventType.REMOVED).add(listener);
        }

        if (listener instanceof RemoteCacheEntryInvalidateListener) {
            eventMap.get(EventType.INVALIDATE).add(listener);
        }
    }

    protected CoordinationEntryEvent<?> routeEventToListeners(
        ConcurrentHashMap<EventType, ArrayList<CoordinationEventListener>> eventMap, CoordinationEntryEvent<?> event) {
        EventType eventType = event.getEventType();

        List<CoordinationEntryEvent<?>> events = new ArrayList<CoordinationEntryEvent<?>>();
        events.add(event);

        for (CoordinationEventListener listener : eventMap.get(eventType)) {
            if (eventType == EventType.CREATED) {
                ((RemoteCacheEntryCreatedListener) listener).onCreated(events);
            } else if (eventType == EventType.REMOVED) {
                ((RemoteCacheEntryRemovedListener) listener).onRemoved(events);
            } else if (eventType == EventType.UPDATED) {
                ((RemoteCacheEntryUpdatedListener) listener).onUpdated(events);
            } else if (eventType == EventType.INVALIDATE) {
                ((RemoteCacheEntryInvalidateListener) listener).onInvalidate(events);
            } else {
                LOG.error("Error. Unsupported coordination event type received - {}", eventType);
            }
        }
        return event;
    }
}