package com.lwjlol.liveeventbus

import android.os.Looper
import androidx.annotation.MainThread
import androidx.collection.LruCache
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer

class LiveEventBus private constructor() {
    private val liveDataMap = LruCache<Class<*>, EventLiveData<*>>(DEFAULT_MAX_EVENT)

    var maxEventSize: Int
        get() = liveDataMap.maxSize()
        set(maxSize) {
            liveDataMap.resize(maxSize)
        }

    companion object {
        @JvmStatic
        private var DEFAULT_MAX_EVENT = 20

        val instance = Singleton.instance

        @JvmStatic
        fun setInitMaxEventSize(maxSize: Int) {
            DEFAULT_MAX_EVENT = maxSize
        }
    }

    /**
     * @param clazz 为了类型安全, 指定事件 type class
     */
    fun <T> on(clazz: Class<T>): Bus<T> {
        return Bus(clazz, liveDataMap)
    }

    /**
     * 清空所有的事件缓存
     */
    fun clear() {
        liveDataMap.evictAll()
    }

    private fun ifProcessorMapGetNull(
        clazz: Class<*>,
        sticky: Boolean
    ): EventLiveData<Any> {
        val liveData = EventLiveData<Any>(sticky)
        liveDataMap.put(clazz, liveData)
        return liveData
    }

    private fun sendInternal(
        event: Any,
        sticky: Boolean
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            synchronized(this) {
                @Suppress("UNCHECKED_CAST") val liveData =
                    ((liveDataMap.get(event::class.java) ?: ifProcessorMapGetNull(
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
                ((liveDataMap.get(event::class.java) ?: ifProcessorMapGetNull(
                    event::class.java,
                    sticky
                ))) as EventLiveData<Any>

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
        private val liveDataMap: LruCache<Class<*>, EventLiveData<*>>
    ) {
        private fun <T> observe(
            owner: LifecycleOwner,
            sticky: Boolean,
            observer: Observer<T>
        ) {
            @Suppress("UNCHECKED_CAST")
            val liveData =
                (liveDataMap.get(clazz) ?: ifProcessorMapGetNull(
                    sticky
                )) as EventLiveData<T>
            check(liveData.sticky == sticky) {
                "liveData has different sticky state to ${clazz.simpleName}!"
            }
            liveData.observe(owner, observer)
        }

        private fun ifProcessorMapGetNull(sticky: Boolean): EventLiveData<T> {
            val liveData = EventLiveData<T>(sticky)
            liveDataMap.put(clazz, liveData)
            return liveData
        }

        private fun <T> observeForever(
            owner: LifecycleOwner,
            sticky: Boolean,
            observer: Observer<T>
        ) {
            @Suppress("UNCHECKED_CAST")
            val liveData =
                (liveDataMap.get(clazz) ?: ifProcessorMapGetNull(sticky)) as EventLiveData<T>
            liveData.observeForever(owner, observer)
        }

        fun observe(
            owner: LifecycleOwner,
            observer: Observer<T>
        ) = observe(owner, false, observer)

        fun observeForever(
            owner: LifecycleOwner,
            observer: Observer<T>
        ) = observeForever(owner, false, observer)

        fun observeForeverSticky(
            owner: LifecycleOwner,
            observer: Observer<T>
        ) =
            observeForever(owner, true, observer)

        /**
         * 支持粘性事件
         */
        fun observeSticky(
            owner: LifecycleOwner,
            observer: Observer<T>
        ) = observe(owner, true, observer)
    }
}
