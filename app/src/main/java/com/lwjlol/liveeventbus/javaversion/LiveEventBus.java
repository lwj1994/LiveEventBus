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

    public BasicBus on(String key) {
        return new BasicBus(key, eventMap, stickyEventMap);
    }

    public final void clear() {
        this.stickyEventMap.clear();
        this.eventMap.evictAll();
    }

    public void send(@NonNull Object event) {
        sendInternal(event, false);
    }

    public void send(String key, String s, boolean sticky) {
        BasicEvent event = new BasicEvent();
        event.setS(s);
        sendInternal(event, sticky);
    }

    public void send(String key, String s) {
        send(key, s, false);
    }

    public void send(String key, long s) {
        send(key, s, false);
    }

    public void send(String key, int s) {
        send(key, s, false);
    }

    public void send(String key, double s) {
        send(key, s, false);
    }

    public void send(String key, char s) {
        send(key, s, false);
    }

    public void send(String key, boolean s) {
        send(key, s, false);
    }

    public void send(String key, float s) {
        send(key, s, false);
    }

    public void send(String key, int s, boolean sticky) {
        BasicEvent event = new BasicEvent();
        event.setNumberInt(s);
        sendInternal(event, sticky);
    }

    public void send(String key, long s, boolean sticky) {
        BasicEvent event = new BasicEvent();
        event.setNumberLong(s);
        sendInternal(event, sticky);
    }

    public void send(String key, double s, boolean sticky) {
        BasicEvent event = new BasicEvent();
        event.setNumberDouble(s);
        sendInternal(event, sticky);
    }

    public void send(String key, float s, boolean sticky) {
        BasicEvent event = new BasicEvent();
        event.setNumberFloat(s);
        sendInternal(event, sticky);
    }

    public void send(String key, char s, boolean sticky) {
        BasicEvent event = new BasicEvent();
        event.setC(s);
        sendInternal(event, sticky);
    }

    public void send(String key, boolean s, boolean sticky) {
        BasicEvent event = new BasicEvent();
        event.setB(s);
        sendInternal(event, sticky);
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

    public interface CallbackString {
        void onChanged(String v);
    }

    public interface CallbackInt {
        void onChanged(int v);
    }

    public interface CallbackLong {
        void onChanged(long v);
    }

    public interface CallbackFloat {
        void onChanged(float v);
    }

    public interface CallbackDouble {
        void onChanged(double v);
    }

    public interface CallbackChar {
        void onChanged(char v);
    }

    public interface CallbackBoolean {
        void onChanged(boolean v);
    }

    public static final class BasicBus {
        @NonNull
        private final String key;
        @NonNull
        private final LruCache<Class, EventLiveData> liveDataMap;
        @NonNull
        private final ArrayMap<Class, EventLiveData> stickyEventMap;

        public BasicBus(@NonNull String key, @NonNull LruCache<Class, EventLiveData> liveDataMap, @NonNull ArrayMap<Class, EventLiveData> stickyEventMap) {
            this.key = key;
            this.liveDataMap = liveDataMap;
            this.stickyEventMap = stickyEventMap;
        }

        private void observe(@NonNull LifecycleOwner owner, @Nullable String key, boolean sticky, @NonNull Observer<? super BasicEvent> observer, boolean isForever) {
            //noinspection unchecked
            EventLiveData<BasicEvent> liveData = getLiveData(BasicBus.class, sticky, this.liveDataMap, this.stickyEventMap);
            if (key == null) {
                key = liveData.getKey(owner);
            }
            if (isForever) {
                liveData.observeForever(owner, key, observer);
            } else {
                liveData.observe(owner, key, observer);
            }
        }

        /**
         * @param owner
         * @param ownerKey 标识 owner 的 key，避免数据倒灌，相同 ownerKey 的 owner 只能同时消费一次事件
         * @param observer
         */
        public final void observe(@NonNull LifecycleOwner owner, @Nullable String ownerKey, boolean sticky, @NonNull CallbackString observer) {
            this.observe(owner, ownerKey, sticky, new Observer<BasicEvent>() {
                @Override
                public void onChanged(BasicEvent event) {
                    if (key.equals(event.getKey()))
                        observer.onChanged(event.s);
                }
            }, false);
        }

        public final void observe(@NonNull LifecycleOwner owner, @NonNull CallbackString observer) {
            observe(owner, null, false, observer);
        }

        public final void observe(@NonNull LifecycleOwner owner, @NonNull CallbackInt observer) {
            observe(owner, null, false, observer);
        }

        public final void observe(@NonNull LifecycleOwner owner, @NonNull CallbackLong observer) {
            observe(owner, null, false, observer);
        }

        public final void observe(@NonNull LifecycleOwner owner, @NonNull CallbackDouble observer) {
            observe(owner, null, false, observer);
        }

        public final void observe(@NonNull LifecycleOwner owner, @NonNull CallbackChar observer) {
            observe(owner, null, false, observer);
        }

        public final void observe(@NonNull LifecycleOwner owner, @NonNull CallbackFloat observer) {
            observe(owner, null, false, observer);
        }

        public final void observe(@NonNull LifecycleOwner owner, @Nullable String ownerKey, boolean sticky, @NonNull CallbackInt observer) {
            this.observe(owner, ownerKey, sticky, new Observer<BasicEvent>() {
                @Override
                public void onChanged(BasicEvent event) {
                    observer.onChanged(event.numberInt);
                }
            }, false);
        }

        public final void observe(@NonNull LifecycleOwner owner, @Nullable String ownerKey, boolean sticky, @NonNull CallbackFloat observer) {
            this.observe(owner, ownerKey, sticky, new Observer<BasicEvent>() {
                @Override
                public void onChanged(BasicEvent event) {
                    observer.onChanged(event.numberFloat);
                }
            }, false);
        }

        public final void observe(@NonNull LifecycleOwner owner, @Nullable String ownerKey, boolean sticky, @NonNull CallbackDouble observer) {
            this.observe(owner, ownerKey, sticky, new Observer<BasicEvent>() {
                @Override
                public void onChanged(BasicEvent event) {
                    observer.onChanged(event.numberDouble);
                }
            }, false);
        }

        public final void observe(@NonNull LifecycleOwner owner, @Nullable String ownerKey, boolean sticky, @NonNull CallbackLong observer) {
            this.observe(owner, ownerKey, sticky, new Observer<BasicEvent>() {
                @Override
                public void onChanged(BasicEvent event) {
                    observer.onChanged(event.numberLong);
                }
            }, false);
        }

        public final void observe(@NonNull LifecycleOwner owner, @Nullable String ownerKey, boolean sticky, @NonNull CallbackChar observer) {
            this.observe(owner, ownerKey, sticky, new Observer<BasicEvent>() {
                @Override
                public void onChanged(BasicEvent event) {
                    observer.onChanged(event.c);
                }
            }, false);
        }

        public final void observe(@NonNull LifecycleOwner owner, @Nullable String ownerKey, boolean sticky, @NonNull CallbackBoolean observer) {
            this.observe(owner, ownerKey, sticky, new Observer<BasicEvent>() {
                @Override
                public void onChanged(BasicEvent event) {
                    observer.onChanged(event.b);
                }
            }, false);
        }


        interface Callback {
            void onChange(Object value);
        }


    }

    public static final class Bus<T> {
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

        /**
         * @param owner
         * @param ownerKey 标识 owner 的 key，避免数据倒灌，相同 ownerKey 的 owner 只能同时消费一次事件
         * @param observer
         */
        public final void observe(@NonNull LifecycleOwner owner, @NonNull String ownerKey, @NonNull Observer<? super T> observer) {
            this.observe(owner, ownerKey, false, observer, false);
        }

        public final void observeSticky(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
            this.observe(owner, null, true, observer, false);
        }


        public final void observeSticky(@NonNull LifecycleOwner owner, @NonNull String ownerKey, @NonNull Observer<? super T> observer) {
            this.observe(owner, ownerKey, true, observer, false);
        }
    }

    /**
     * 基本数据类型的载体
     */
    static class BasicEvent {
        private String key;
        private int numberInt;
        private long numberLong;
        private double numberDouble;
        private float numberFloat;
        private char c;
        private boolean b;
        private String s;

        public BasicEvent() {
            this.numberInt = 0;
            this.numberLong = 0L;
            this.numberDouble = 0;
            this.c = ' ';
            this.b = false;
            this.s = null;
            this.numberFloat = 0F;
            this.key = null;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public float getNumberFloat() {
            return numberFloat;
        }

        public void setNumberFloat(float numberFloat) {
            this.numberFloat = numberFloat;
        }

        public int getNumberInt() {
            return numberInt;
        }

        public void setNumberInt(int numberInt) {
            this.numberInt = numberInt;
        }

        public long getNumberLong() {
            return numberLong;
        }

        public void setNumberLong(long numberLong) {
            this.numberLong = numberLong;
        }

        public double getNumberDouble() {
            return numberDouble;
        }

        public void setNumberDouble(double numberDouble) {
            this.numberDouble = numberDouble;
        }

        public char getC() {
            return c;
        }

        public void setC(char c) {
            this.c = c;
        }

        public boolean isB() {
            return b;
        }

        public void setB(boolean b) {
            this.b = b;
        }

        public String getS() {
            return s;
        }

        public void setS(String s) {
            this.s = s;
        }

    }
}
