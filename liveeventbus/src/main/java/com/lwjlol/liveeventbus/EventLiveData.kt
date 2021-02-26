package com.lwjlol.liveeventbus

import android.os.Looper
import androidx.annotation.MainThread
import androidx.collection.ArrayMap
import androidx.lifecycle.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * @param sticky indicate that event is a sticky event
 *
 */
class EventLiveData<T>(val sticky: Boolean = true) : MutableLiveData<T>() {
    private val tempValueMap = ArrayMap<String, Any?>(2)
    private val foreverObserverMap = ArrayMap<String, Observer<T>>(2)
    private val callMap = ArrayMap<String, Int>(2)
    private var isObservedMap = ArrayMap<String, Boolean>(2)
    private val callCount = AtomicInteger(0)

    @Volatile
    private var isCall: Boolean? = null

    fun call() {
        callCount.incrementAndGet()
        isCall = true
        if (Looper.myLooper() == Looper.getMainLooper()) {
            setValueForAll(value)
            super.setValue(null)
        } else {
            setValueForAll(value)
            super.postValue(null)
        }
    }

    override fun setValue(value: T?) {
        isCall = false
        setValueForAll(value)
        super.setValue(value)
    }

    private fun setValueForAll(value: T?) {
        for (item in tempValueMap) {
            // 跳过非粘性还没注册的组件
            if (isObservedMap[item.key] != true && !sticky) {
                continue
            }
            item.setValue(value ?: NULL)
        }
    }

    /**
     * @param owner
     * @param key an event only can be consumed once by same key
     * @param observer
     */
    @MainThread
    fun observe(
        owner: LifecycleOwner,
        key: String = getKey(owner),
        observer: Observer<in T>
    ) {
        onObserve(owner, key)
        super.observe(owner, getObserverWrapper(key, observer))
    }

    override fun observe(
        owner: LifecycleOwner,
        observer: Observer<in T>
    ) {
        observe(owner, getKey(owner), observer)
    }

    inline fun observe(
        owner: LifecycleOwner,
        key: String? = null,
        crossinline block: ((T?) -> Unit)
    ) {
        observe(owner, key ?: getKey(owner), Observer {
            block(it)
        })
    }

    inline fun observeNonNull(
        owner: LifecycleOwner,
        key: String? = null,
        crossinline block: ((T) -> Unit)
    ) {
        observe(owner, key ?: getKey(owner), Observer {
            block(it ?: return@Observer)
        })
    }

    inline fun observeCall(
        owner: LifecycleOwner,
        key: String? = null,
        crossinline onCall: (() -> Unit)
    ) {
        observe(owner, key ?: getKey(owner), {
            onCall()
        })
    }

    inline fun observeCallForever(
        owner: LifecycleOwner,
        key: String? = null,
        crossinline onCall: (() -> Unit)
    ) {
        observeForever(owner, key ?: getKey(owner), {
            onCall()
        })
    }

    @MainThread
    fun observeForever(
        owner: LifecycleOwner?,
        key: String = if (owner == null) "" else getKey(owner),
        observer: Observer<in T>
    ) {
        onObserve(owner, key)
        super.observeForever(getObserverWrapper(key, observer).also {
            foreverObserverMap[key] = it
        })
    }


    @MainThread
    override fun observeForever(observer: Observer<in T>) {
        observeForever(null, observer.toString(), observer)
    }

    inline fun observeForever(
        owner: LifecycleOwner,
        key: String? = null,
        crossinline block: ((T?) -> Unit)
    ) {
        observeForever(owner, key ?: getKey(owner), Observer {
            block(it)
        })
    }

    inline fun observeForeverNonNull(
        owner: LifecycleOwner,
        key: String? = null,
        crossinline block: ((T) -> Unit)
    ) {
        observeForever(owner, key ?: getKey(owner), Observer {
            block(it ?: return@Observer)
        })
    }

    override fun removeObserver(observer: Observer<in T>) {
        super.removeObserver(observer)
        reset(observer.toString())
    }

    private fun onObserve(
        owner: LifecycleOwner?,
        key: String
    ) {
        owner?.lifecycle?.addObserver(OnDestroyLifecycleObserver(this, key))
        isObservedMap[key] = true
        if (tempValueMap[key] == null) {
            tempValueMap[key] = if (!sticky || value == null) {
                UNSET
            } else {
                value
            }
        }

        // 最后一次是 setValue
        if (isCall == false && sticky) {
            if (value == null) {
                tempValueMap[key] = NULL
            }
        }

        // 最后一次是 call 事件
        if (isCall == true && callCount.get() > 0 && sticky) {
            if (callMap.getOrDefault(key, 0) < callCount.get()) {
                tempValueMap[key] = NULL
            }
        }
    }

    private fun getObserverWrapper(
        key: String,
        observer: Observer<in T>
    ): Observer<T> {
        return Observer<T> {
            if (tempValueMap[key] === UNSET) return@Observer
            @Suppress("UNCHECKED_CAST")
            observer.onChanged(value as T)
            // 消费完 call 事件就同步进度
            if (isCall == true && tempValueMap[key] === NULL) {
                callMap[key] = callCount.get()
            }
            tempValueMap[key] = UNSET
        }
    }

    @MainThread
    fun observeForever(
        owner: LifecycleOwner,
        observer: Observer<in T>
    ) {
        observeForever(owner, getKey(owner), observer)
    }

    fun getKey(owner: LifecycleOwner) = "${owner::class.qualifiedName}"

    fun onClear(key: String) {
        foreverObserverMap[key]?.let {
            removeObserver(it)
        }
        foreverObserverMap[key] = null
        isObservedMap[key] = false
    }

    private class OnDestroyLifecycleObserver<T>(
        private val livaData: EventLiveData<T>,
        private val key: String
    ) : LifecycleObserver {

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            livaData.onClear(key)
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()

        sb.append("\ntempValueMap = ")
        if (tempValueMap.isNotEmpty()) {
            tempValueMap.keys.forEachIndexed { index, key ->
                if (index == 0) {
                    sb.append("{")
                }
                sb.append(key)
                    .append(":")
                    .append(tempValueMap[key])
                if (index == tempValueMap.keys.size - 1) {
                    sb.append("}")
                } else {
                    sb.append(", ")
                }
            }
        } else {
            sb.append("{}")
        }


        sb.append("\nforeverObserverMap = ")
        if (foreverObserverMap.keys.isEmpty()) {
            sb.append("{}")
        } else {
            foreverObserverMap.keys.forEachIndexed { index, key ->
                if (index == 0) {
                    sb.append("{")
                }
                sb.append(key)
                    .append(":")
                    .append(foreverObserverMap[key])
                if (index == foreverObserverMap.keys.size - 1) {
                    sb.append("}")
                } else {
                    sb.append(", ")
                }
            }
        }

        sb.append("\nisObservedMap = ")
        if (isObservedMap.isEmpty) {
            sb.append("{}")
        } else {
            isObservedMap.keys.forEachIndexed { index, key ->
                if (index == 0) {
                    sb.append("{")
                }
                sb.append(key)
                    .append(":")
                    .append(isObservedMap[key])
                if (index == isObservedMap.keys.size - 1) {
                    sb.append("}")
                } else {
                    sb.append(", ")
                }
            }
        }

        val address = super.toString()
        return "$address:\n$sb"
    }

    private fun reset(key: String) {
        tempValueMap[key] = null
        foreverObserverMap[key] = null
        isObservedMap[key] = false
    }

    companion object {
        private val UNSET = Any()
        private val NULL = Any()
        private const val TAG = "EventLiveData"
    }
}
