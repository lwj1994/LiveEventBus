package com.lwjlol.liveeventbus.javaversion;

import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.collection.LruCache;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

@SuppressWarnings("rawtypes")
public class LiveEventBus {
    private static final int DEFAULT_MAX_EVENT = 20;
    private final LruCache<Class, EventLiveData> eventMap = new LruCache<>(DEFAULT_MAX_EVENT);
    private final ArrayMap<Class, EventLiveData> stickyEventMap = new ArrayMap<>(DEFAULT_MAX_EVENT);

    private LiveEventBus() {

    }

    public static LiveEventBus getInstance() {
        return Loader.instance;
    }

    @NonNull
    private static EventLiveData getLiveData(Class clazz, boolean sticky, LruCache<Class, EventLiveData> eventMap, ArrayMap<Class, EventLiveData> stickyEventMap) {
        EventLiveData eventLiveData;
        if (sticky) {
            eventLiveData = stickyEventMap.get(clazz);
        } else {
            eventLiveData = eventMap.get(clazz);
        }
        if (eventLiveData == null) {
            eventLiveData = new EventLiveData(sticky);
            if (sticky) {
                stickyEventMap.put(clazz, eventLiveData);
            } else {
                eventMap.put(clazz, eventLiveData);
            }
        }
        return eventLiveData;
    }

    /**
     * @param clazz 为了类型安全, 指定事件 type class
     */
    public <T> Bus<T> on(Class<T> clazz) {
        return new Bus<>(clazz, eventMap, stickyEventMap);
    }

    public final void clear() {
        this.stickyEventMap.clear();
        this.eventMap.evictAll();
    }

    public void send(@NonNull Object event) {
        sendInternal(event, false);
    }

    public void sendSticky(@NonNull Object event) {
        sendInternal(event, true);
    }

    @SuppressWarnings("unchecked")
    private void sendInternal(@NonNull Object event, boolean sticky) {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            synchronized (this) {
                getLiveData(event.getClass(), sticky, eventMap, stickyEventMap).postValue(event);
            }
        } else {
            getLiveData(event.getClass(), sticky, eventMap, stickyEventMap).setValue(event);
        }
    }

    private static class Loader {
        static LiveEventBus instance = new LiveEventBus();
    }

    private static final class Bus<T> {
        private final Class<T> clazz;
        private final LruCache<Class, EventLiveData> liveDataMap;
        private final ArrayMap<Class, EventLiveData> stickyEventMap;

        public Bus(Class<T> clazz, LruCache<Class, EventLiveData> liveDataMap, ArrayMap<Class, EventLiveData> stickyEventMap) {
            this.clazz = clazz;
            this.liveDataMap = liveDataMap;
            this.stickyEventMap = stickyEventMap;
        }

        private void observe(@NonNull LifecycleOwner owner, @Nullable String key, boolean sticky, @NonNull Observer<? super T> observer, boolean isForever) {
            //noinspection unchecked
            EventLiveData<T> liveData = getLiveData(this.clazz, sticky, this.liveDataMap, this.stickyEventMap);
            if (key == null) {
                key = liveData.getKey(owner);
            }
            if (isForever) {
                liveData.observeForever(owner, key, observer);
            } else {
                liveData.observe(owner, key, observer);
            }
        }


        public final void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
            this.observe(owner, null, false, observer, false);
        }

        public final void observe(@NonNull LifecycleOwner owner, @NonNull String key, @NonNull Observer<? super T> observer) {
            this.observe(owner, key, false, observer, false);
        }

        public final void observeSticky(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
            this.observe(owner, null, true, observer, false);
        }

        public final void observeSticky(@NonNull LifecycleOwner owner, @NonNull String key, @NonNull Observer<? super T> observer) {
            this.observe(owner, key, true, observer, false);
        }

    }
}
