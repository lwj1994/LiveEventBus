package com.lwjlol.liveeventbus.javaversion;

import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.OnLifecycleEvent;

import java.util.concurrent.atomic.AtomicInteger;


public class EventLiveData<T> extends MutableLiveData<T> {
    private static final Object UNSET = new Object();
    private static final Object NULL = new Object();
    private static final Object CALL = new Object();
    private static final String TAG = "EventLiveData";
    private final ArrayMap<String, Object> tempValueMap = new ArrayMap<>(2);
    private final ArrayMap<String, Observer<? super T>> foreverObserverMap = new ArrayMap<>(2);
    private final ArrayMap<String, Integer> callMap = new ArrayMap<>(2);
    private final ArrayMap<String, Boolean> isObservedMap = new ArrayMap<>();
    private final AtomicInteger callCount = new AtomicInteger(0);
    private final boolean sticky;
    @Nullable
    private Boolean lastIsCall;
    private volatile boolean invokePostValueFromCall;

    public EventLiveData(boolean sticky) {
        this.sticky = sticky;
    }


    @Nullable
    public final Boolean getLastIsCall() {
        return this.lastIsCall;
    }

    public final void call() {
        this.callCount.incrementAndGet();
        this.setCallForAll();
        this.lastIsCall = true;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            super.setValue(null);
        } else {
            this.invokePostValueFromCall = true;
            super.postValue(null);
        }

    }

    public void postValue(@Nullable T value) {
        this.invokePostValueFromCall = false;
        super.postValue(value);
    }

    public void setValue(@Nullable T value) {
        this.lastIsCall = this.invokePostValueFromCall;
        this.invokePostValueFromCall = false;
        this.setValueForAll(value);
        super.setValue(value);
    }

    private void setCallForAll() {
        for (int i = 0; i < tempValueMap.size(); i++) {
            String key = tempValueMap.keyAt(i);
            tempValueMap.put(key, CALL);
        }
    }

    private void setValueForAll(@Nullable T value) {
        for (int i = 0; i < tempValueMap.size(); i++) {
            String key = tempValueMap.keyAt(i);
            if (value == null) {
                tempValueMap.put(key, NULL);
            } else {
                tempValueMap.put(key, value);
            }
        }
    }

    @MainThread
    public final void observe(@NonNull LifecycleOwner owner, @NonNull String key, @NonNull Observer<? super T> observer) {
        this.onObserve(owner, key);
        super.observe(this.get(owner), getObserverWrapper(key, observer));
    }


    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        this.observe(owner, this.getKey(owner), observer);
    }


    public final void observeNonNull(@NonNull LifecycleOwner owner, @Nullable String key, @NonNull NonNullObserver<? super T> block) {
        String k = key;
        if (key == null) {
            k = this.getKey(owner);
        }

        this.observe(owner, k, ((Observer<? super T>) it -> {
            if (lastIsCall != null && lastIsCall) {
                throw new IllegalArgumentException("observeNonNull unSupport observe Call, use observe");
            } else {
                if (it != null) {
                    block.onChanged(it);
                }
            }
        }));
    }

    @MainThread
    public void observeForever(@NonNull Observer<? super T> observer) {
        this.observeForever((LifecycleOwner) null, observer.toString(), observer);
    }

    @MainThread
    public final void observeForever(@Nullable LifecycleOwner owner, @Nullable String key, @NonNull Observer<? super T> observer) {
        String k = key;
        if (key == null) {
            k = this.getKey(owner);
        }
        this.onObserve(owner, k);
        Observer<? super T> observerWrapper = this.getObserverWrapper(k, observer);
        foreverObserverMap.put(key, observer);
        super.observeForever(observerWrapper);
    }

    public final void observeForeverNonNull(@Nullable LifecycleOwner owner, @Nullable String key, @NonNull NonNullObserver<? super T> block) {
        this.observeForever(owner, key, ((Observer<? super T>) it -> {
            if (lastIsCall != null && lastIsCall) {
                throw new IllegalArgumentException("observeNonNull unSupport observe Call, use observe");
            } else {
                if (it != null) {
                    block.onChanged(it);
                }
            }
        }));
    }


    public final void removeObserver(@Nullable String key, @NonNull Observer<? super T> observer) {
        super.removeObserver(observer);
        if (key == null) {
            reset(observer.toString());
        } else {
            reset(key);
        }

    }


    public void removeObserver(@NonNull Observer<? super T> observer) {
        super.removeObserver(observer);
        this.reset(observer.toString());
    }

    @NonNull
    public final LifecycleOwner get(@NonNull LifecycleOwner owner) {
        return owner instanceof Fragment && ((Fragment) owner).getView() != null ? ((Fragment) owner).getViewLifecycleOwner() : owner;
    }

    private void onObserve(LifecycleOwner owner, String key) {
        if (owner != null) {
            LifecycleOwner lifecycleOwner = this.get(owner);
            lifecycleOwner.getLifecycle().addObserver((LifecycleObserver) (new EventLiveData.OnDestroyLifecycleObserver<T>(this, key)));
        }

        (this.isObservedMap).put(key, true);
        if (this.tempValueMap.get(key) == null) {
            (this.tempValueMap).put(key, this.sticky && this.getValue() != null ? this.getValue() : UNSET);
        }

        if (lastIsCall != null && !lastIsCall && this.sticky && this.getValue() == null) {
            (this.tempValueMap).put(key, NULL);
        }

        if (lastIsCall != null && lastIsCall && this.callCount.get() > 0 && this.sticky && this.callMap.getOrDefault(key, 0) < this.callCount.get()) {
            (this.tempValueMap).put(key, CALL);
        }

    }

    private Observer<? super T> getObserverWrapper(final String key, final Observer<? super T> observer) {
        return (it -> {
            if (tempValueMap.get(key) != UNSET) {
                observer.onChanged(getValue());
                if (tempValueMap.get(key) == CALL) {
                    callMap.put(key, callCount.get());
                }
                tempValueMap.put(key, UNSET);
            }
        });
    }

    @NonNull
    public final String getKey(@Nullable LifecycleOwner owner) {
        if (owner == null) {
            return String.valueOf(SystemClock.currentThreadTimeMillis());
        }
        String k = owner.getClass().getCanonicalName();
        if (k == null) {
            k = owner.getClass().getName();
        }
        return k;
    }

    public void onClear(String key) {
        Observer<? super T> observer = this.foreverObserverMap.get(key);
        if (observer != null) {
            this.removeObserver(observer);
        } else {
            this.reset(key);
        }

    }

    @NonNull
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\ntempValueMap = ");
        if (!tempValueMap.isEmpty()) {
            for (int i = 0; i < tempValueMap.size(); i++) {
                String key = tempValueMap.keyAt(i);
                Object value = tempValueMap.getOrDefault(key, "null");
                if (i == 0) {
                    sb.append("{");
                }
                sb.append(key).append(":").append(value);
                if (i == this.tempValueMap.size() - 1) {
                    sb.append("}");
                } else {
                    sb.append(", ");
                }
            }
        } else {
            sb.append("{}");
        }

        sb.append("\nforeverObserverMap = ");
        if (this.foreverObserverMap.keySet().isEmpty()) {
            sb.append("{}");
        } else {
            for (int i = 0; i < foreverObserverMap.size(); i++) {
                String key = foreverObserverMap.keyAt(i);
                Observer<? super T> value = foreverObserverMap.get(key);
                if (i == 0) {
                    sb.append("{");
                }
                sb.append(key).append(":");
                if (value == null) {
                    sb.append("null");
                } else {
                    sb.append(value.toString());
                }
                if (i == this.foreverObserverMap.size() - 1) {
                    sb.append("}");
                } else {
                    sb.append(", ");
                }
            }
        }

        sb.append("\nisObservedMap = ");
        if (this.isObservedMap.isEmpty()) {
            sb.append("{}");
        } else {
            for (int i = 0; i < isObservedMap.size(); i++) {
                String key = isObservedMap.keyAt(i);
                Boolean value = isObservedMap.get(key);
                if (i == 0) {
                    sb.append("{");
                }
                sb.append(key).append(":");
                if (value == null) {
                    sb.append("null");
                } else {
                    sb.append(value.toString());
                }
                if (i == this.isObservedMap.size() - 1) {
                    sb.append("}");
                } else {
                    sb.append(", ");
                }
            }
        }

        String address = super.toString();
        return address + ":\n" + sb;
    }

    private void reset(String key) {
        tempValueMap.put(key, null);
        isObservedMap.put(key, null);
        callMap.put(key, null);
        foreverObserverMap.put(key, null);
    }

    public final boolean getSticky() {
        return this.sticky;
    }

    interface NonNullObserver<T> {
        void onChanged(@NonNull T value);
    }

    static final class OnDestroyLifecycleObserver<T> implements LifecycleObserver {
        private final EventLiveData<T> livaData;
        private final String key;

        public OnDestroyLifecycleObserver(@NonNull EventLiveData<T> livaData, @NonNull String key) {
            super();
            this.livaData = livaData;
            this.key = key;
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        public final void onDestroy() {
            this.livaData.onClear(this.key);
        }
    }
}
