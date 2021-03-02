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

        @Suppress("UNCHECKED_CAST")
        private fun getLiveData(
            clazz: Class<*>,
            sticky: Boolean,
            eventMap: LruCache<Class<*>, EventLiveData<*>>,
            stickyEventMap: ArrayMap<Class<*>, EventLiveData<*>>
        ): EventLiveData<Any> {
            val liveData = (if (sticky) {
                stickyEventMap[clazz]
            } else {
                eventMap.get(clazz)
            } ?: ifProcessorMapGetNull(
                clazz,
                sticky,
                eventMap,
                stickyEventMap
            )) as EventLiveData<Any>
            check(liveData.sticky == sticky) {
                "liveData has different sticky state to ${clazz.name}!"
            }
            return liveData
        }

        private fun ifProcessorMapGetNull(
            clazz: Class<*>,
            sticky: Boolean,
            eventMap: LruCache<Class<*>, EventLiveData<*>>,
            stickyEventMap: ArrayMap<Class<*>, EventLiveData<*>>
        ): EventLiveData<Any> {
            val liveData = EventLiveData<Any>(sticky)
            if (sticky) {
                stickyEventMap[clazz] = liveData
            } else {
                eventMap.put(clazz, liveData)
            }
            return liveData
        }
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


    private fun sendInternal(
        event: Any,
        sticky: Boolean
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            synchronized(this) {
                getLiveData(event::class.java, sticky, eventMap, stickyEventMap).postValue(event)
            }
        } else {
            getLiveData(event::class.java, sticky, eventMap, stickyEventMap).setValue(event)
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
            ownerKey: String?,
            sticky: Boolean,
            observer: Observer<T>
        ) {
            @Suppress("UNCHECKED_CAST")
            val liveData =
                getLiveData(clazz, sticky, liveDataMap, stickyEventMap) as EventLiveData<T>
            liveData.observe(owner, ownerKey ?: liveData.getKey(owner), observer)
        }


        private fun <T> observeForever(
            owner: LifecycleOwner,
            ownerKey: String?,
            sticky: Boolean,
            observer: Observer<T>
        ) {
            @Suppress("UNCHECKED_CAST")
            val liveData =
                getLiveData(clazz, sticky, liveDataMap, stickyEventMap) as EventLiveData<T>
            liveData.observeForever(owner, ownerKey, observer)
        }

        fun observe(
            owner: LifecycleOwner,
            observer: Observer<T>
        ) = observe(owner, null, false, observer)

        fun observe(
            owner: LifecycleOwner,
            ownerKey: String,
            observer: Observer<T>
        ) = observe(owner, ownerKey, false, observer)

        fun observeForever(
            owner: LifecycleOwner,
            ownerKey: String? = null,
            observer: Observer<T>
        ) = observeForever(owner, ownerKey, false, observer)

        fun observeForeverSticky(
            owner: LifecycleOwner,
            ownerKey: String? = null,
            observer: Observer<T>
        ) = observeForever(owner, ownerKey, true, observer)

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
            ownerKey: String,
            observer: Observer<T>
        ) = observe(owner, ownerKey, true, observer)


        fun removeObserver(ownerKey: String? = null, observer: Observer<T>) {
            @Suppress("UNCHECKED_CAST")
            (((stickyEventMap[clazz] ?: liveDataMap[clazz])
                ?: return) as EventLiveData<T>).removeObserver(observer)
        }
    }
}
