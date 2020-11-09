package com.lwjlol.liveeventbus

import androidx.collection.LruCache
import androidx.lifecycle.*

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

    private fun ifProcessorMapGetNull(clazz: Class<*>, sticky: Boolean): EventLiveData<Any> {
        val liveData = EventLiveData<Any>(sticky)
        liveDataMap.put(clazz, liveData)
        return liveData
    }


    private fun postInternal(event: Any, sticky: Boolean) {
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

    fun postSticky(event: Any) = postInternal(event, true)

    fun post(event: Any) = postInternal(event, false)


    private object Singleton {
        val instance = LiveEventBus()
    }

    class Bus<T>(
        private val clazz: Class<T>,
        private val liveDataMap: LruCache<Class<*>, EventLiveData<*>>
    ) {
        private fun <T> observe(owner: LifecycleOwner, sticky: Boolean, observer: Observer<T>) {
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
            val key = owner::class.simpleName ?: ""
            owner.lifecycle.addObserver(OnDestroyLifecycleObserver(liveData, key))
            liveData.observeForever(key, observer)
        }

        fun observe(owner: LifecycleOwner, observer: Observer<T>) = observe(owner, false, observer)

        fun observeForever(owner: LifecycleOwner, observer: Observer<T>) =
            observeForever(owner, false, observer)

        fun observeForeverSticky(owner: LifecycleOwner, observer: Observer<T>) =
            observeForever(owner, true, observer)

        /**
         * 支持粘性事件
         */
        fun observeSticky(owner: LifecycleOwner, observer: Observer<T>) =
            observe(owner, true, observer)
    }


    class OnDestroyLifecycleObserver<T>(private val livaData: EventLiveData<T>, val key: String) :
        LifecycleObserver {

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            livaData.removeForeverObserver(key)
        }
    }
}
