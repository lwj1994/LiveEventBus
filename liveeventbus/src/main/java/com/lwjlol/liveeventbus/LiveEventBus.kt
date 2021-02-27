package com.lwjlol.liveeventbus

import android.os.Looper
import androidx.annotation.MainThread
import androidx.collection.ArrayMap
import androidx.collection.LruCache
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer

class LiveEventBus private constructor() {
    private val eventMap = LruCache<Class<*>, EventLiveData<*>>(DEFAULT_MAX_EVENT)
    private val stickyEventMap = ArrayMap<Class<*>, EventLiveData<*>>(DEFAULT_MAX_EVENT)


    companion object {
        private const val DEFAULT_MAX_EVENT = 20

        val instance = Singleton.instance
    }

    /**
     * @param clazz 为了类型安全, 指定事件 type class
     */
    fun <T> on(clazz: Class<T>): Bus<T> {
        return Bus(clazz, eventMap, stickyEventMap)
    }

    /**
     * 清空所有的事件缓存
     */
    fun clear() {
        stickyEventMap.clear()
        eventMap.evictAll()
    }

    private fun ifProcessorMapGetNull(
        clazz: Class<*>,
        sticky: Boolean
    ): EventLiveData<Any> {
        val liveData = EventLiveData<Any>(sticky)
        if (sticky) {
            stickyEventMap[clazz] = liveData
        } else {
            eventMap.put(clazz, liveData)
        }
        return liveData
    }

    private fun sendInternal(
        event: Any,
        sticky: Boolean
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            synchronized(this) {
                @Suppress("UNCHECKED_CAST") val liveData =
                    ((if (sticky) {
                        stickyEventMap[event::class.java]
                    } else {
                        eventMap.get(event::class.java)
                    } ?: ifProcessorMapGetNull(
                        event::class.java,
                        sticky
                    ))) as EventLiveData<Any>

                check(liveData.sticky == sticky) {
                    "liveData has different sticky state to ${event::class.simpleName}!"
                }
                liveData.postValue(event)
            }
        } else {
            @Suppress("UNCHECKED_CAST") val liveData =
                (if (sticky) {
                    stickyEventMap[event::class.java]
                } else {
                    eventMap.get(event::class.java)
                } ?: ifProcessorMapGetNull(
                    event::class.java,
                    sticky
                )) as EventLiveData<Any>

            check(liveData.sticky == sticky) {
                "liveData has different sticky state to ${event::class.simpleName}!"
            }
            liveData.setValue(event)
        }

    }

    @MainThread
    fun sendSticky(event: Any) = sendInternal(event, true)

    @MainThread
    fun send(event: Any) = sendInternal(event, false)

    private object Singleton {
        val instance = LiveEventBus()
    }

    class Bus<T>(
        private val clazz: Class<T>,
        private val liveDataMap: LruCache<Class<*>, EventLiveData<*>>,
        private val stickyEventMap: ArrayMap<Class<*>, EventLiveData<*>>
    ) {
        private fun <T> observe(
            owner: LifecycleOwner,
            key: String?,
            sticky: Boolean,
            observer: Observer<T>
        ) {
            @Suppress("UNCHECKED_CAST")
            val liveData = (if (sticky) {
                stickyEventMap[clazz]
            } else {
                liveDataMap.get(clazz)
            } ?: ifProcessorMapGetNull(sticky)) as EventLiveData<T>
            check(liveData.sticky == sticky) {
                "liveData has different sticky state to ${clazz.simpleName}!"
            }
            liveData.observe(owner, key ?: liveData.getKey(owner), observer)
        }

        private fun ifProcessorMapGetNull(sticky: Boolean): EventLiveData<T> {
            val liveData = EventLiveData<T>(sticky)
            if (sticky) {
                stickyEventMap[clazz] = liveData
            } else {
                liveDataMap.put(clazz, liveData)
            }
            return liveData
        }


        private fun <T> observeForever(
            key: String?,
            sticky: Boolean,
            observer: Observer<T>
        ) {
            @Suppress("UNCHECKED_CAST")
            val liveData =
                (if (sticky) {
                    stickyEventMap[clazz]
                } else {
                    liveDataMap.get(clazz)
                } ?: ifProcessorMapGetNull(sticky)) as EventLiveData<T>
            liveData.observeForever(key, observer)
        }

        fun observe(
            owner: LifecycleOwner,
            observer: Observer<T>
        ) = observe(owner, null, false, observer)

        fun observe(
            owner: LifecycleOwner,
            key: String,
            observer: Observer<T>
        ) = observe(owner, key, false, observer)

        fun observeForever(
            key: String? = null,
            observer: Observer<T>
        ) = observeForever(key, false, observer)

        fun observeForeverSticky(
            key: String? = null,
            observer: Observer<T>
        ) = observeForever(key, true, observer)

        /**
         * 支持粘性事件
         */
        fun observeSticky(
            owner: LifecycleOwner,
            observer: Observer<T>
        ) = observe(owner, null, true, observer)


        /**
         * 支持粘性事件
         */
        fun observeSticky(
            owner: LifecycleOwner,
            key: String,
            observer: Observer<T>
        ) = observe(owner, key, true, observer)


        fun removeObserver(owner: LifecycleOwner) {
            ((stickyEventMap[clazz] ?: liveDataMap[clazz]) ?: return).removeObservers(owner)
        }
    }
}
