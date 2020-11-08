package com.lwjlol.livedatabus

import androidx.collection.LruCache
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlin.math.max

class LiveDataBus private constructor() {
    private val liveDataMap = LruCache<Class<*>, MutableLiveData<*>>(DEFAULT_MAX_EVENT)

    var maxEventSize: Int
        get() = liveDataMap.maxSize()
        set(maxSize) {
            liveDataMap.resize(maxSize)
        }

    companion object {
        val instance = Singleton.instance

        @JvmStatic
        private var DEFAULT_MAX_EVENT = 20

        @JvmStatic
        fun setMaxSize(maxSize: Int) {
            this.DEFAULT_MAX_EVENT = maxSize
        }

    }

    /**
     * @param clazz 为了类型安全, 指定事件 type class
     */
    fun <T> on(clazz: Class<T>): Bus<T> {
        return Bus(clazz, liveDataMap)
    }

    private fun ifProcessorMapGetNull(clazz: Class<*>, sticky: Boolean): MutableLiveData<Any> {
        val liveData = if (sticky) {
            MutableLiveData<Any>()
        } else {
            EventLiveData<Any>()
        }
        liveDataMap.put(clazz, liveData)
        return liveData
    }


    private fun postInternal(event: Any, sticky: Boolean) {
        val liveData =
            (liveDataMap.get(event::class.java) ?: ifProcessorMapGetNull(event::class.java, sticky))
        liveData.value = event
    }

    fun postSticky(event: Any) = postInternal(event, true)

    fun post(event: Any) = postInternal(event, false)


    private object Singleton {
        val instance = LiveDataBus()
    }

    class Bus<T>(
        private val clazz: Class<T>,
        private val liveDataMap: LruCache<Class<*>, MutableLiveData<*>>
    ) {
        private fun <T> observe(owner: LifecycleOwner, sticky: Boolean, observer: Observer<T>) {
            @Suppress("UNCHECKED_CAST")
            val liveData =
                (liveDataMap.get(clazz) ?: ifProcessorMapGetNull(
                    sticky
                )) as MutableLiveData<T>
            liveData.observe(owner, observer)
        }

        private fun ifProcessorMapGetNull(sticky: Boolean): MutableLiveData<T> {
            val liveData = if (sticky) {
                MutableLiveData<T>()
            } else {
                EventLiveData<T>()
            }
            liveDataMap.put(clazz, liveData)
            return liveData
        }


        private fun <T> observeForever(sticky: Boolean, observer: Observer<T>) {
            @Suppress("UNCHECKED_CAST")
            val liveData =
                (liveDataMap.get(clazz) ?: ifProcessorMapGetNull(sticky)) as MutableLiveData<T>
            liveData.observeForever(observer)
        }

        private fun removeObserverInternal(observer: Observer<T>, sticky: Boolean) {
            @Suppress("UNCHECKED_CAST") val liveData =
                (liveDataMap.get(clazz) ?: ifProcessorMapGetNull(sticky)) as MutableLiveData<T>
            liveData.removeObserver(observer)
        }

        fun removeObserver(observer: Observer<T>) = removeObserverInternal(observer, false)

        fun removeObserverSticky(observer: Observer<T>) = removeObserverInternal(observer, true)

        fun observe(owner: LifecycleOwner, observer: Observer<T>) = observe(owner, false, observer)

        fun observeForever(observer: Observer<T>) = observeForever(false, observer)

        fun observeForeverSticky(observer: Observer<T>) = observeForever(true, observer)

        /**
         * 支持粘性事件
         */
        fun observeSticky(owner: LifecycleOwner, observer: Observer<T>) =
            observe(owner, true, observer)
    }


}
