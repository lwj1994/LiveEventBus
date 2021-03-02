package com.lwjlol.liveeventbus

import android.os.Looper
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.collection.ArrayMap
import androidx.collection.LruCache
import androidx.lifecycle.Lifecycle
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
            eventMap: LruCache<Class<*>, EventLiveData<*>>,
            stickyEventMap: ArrayMap<Class<*>, EventLiveData<*>>
        ): Pair<EventLiveData<Any>, EventLiveData<Any>> {
            val eventLiveData: EventLiveData<Any> = (eventMap[clazz] ?: createAndPutLiveData(
                clazz,
                false,
                eventMap,
                stickyEventMap
            )) as EventLiveData<Any>
            val stickyLiveData: EventLiveData<Any> = (stickyEventMap[clazz] ?: createAndPutLiveData(
                clazz,
                true,
                eventMap,
                stickyEventMap
            )) as EventLiveData<Any>
            return eventLiveData to stickyLiveData
        }

        private fun createAndPutLiveData(
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

    fun on(eventKey: String): PrimitiveBus {
        return PrimitiveBus(eventKey, eventMap, stickyEventMap)
    }

    /**
     * 清空所有的事件缓存
     */
    fun clear() {
        stickyEventMap.clear()
        eventMap.evictAll()
    }

    @MainThread
    fun send(eventKey: String, value: String, sticky: Boolean = false) {
        send(PrimitiveEvent(eventKey = eventKey, stringValue = value), sticky)
    }

    @MainThread
    fun send(eventKey: String, value: Int, sticky: Boolean = false) {
        send(PrimitiveEvent(eventKey = eventKey, intValue = value), sticky)
    }

    @MainThread
    fun send(eventKey: String, value: Long, sticky: Boolean = false) {
        send(PrimitiveEvent(eventKey = eventKey, longValue = value), sticky)
    }

    @MainThread
    fun send(eventKey: String, value: Double, sticky: Boolean = false) {
        send(PrimitiveEvent(eventKey = eventKey, doubleValue = value), sticky)
    }

    @MainThread
    fun send(eventKey: String, value: Float, sticky: Boolean = false) {
        send(PrimitiveEvent(eventKey = eventKey, floatValue = value), sticky)
    }

    @MainThread
    fun send(eventKey: String, value: Char, sticky: Boolean = false) {
        send(PrimitiveEvent(eventKey = eventKey, charValue = value), sticky)
    }

    @MainThread
    fun send(eventKey: String, value: Boolean, sticky: Boolean = false) {
        send(PrimitiveEvent(eventKey = eventKey, booleanValue = value), sticky)
    }

    @MainThread
    fun send(
        event: Any,
        sticky: Boolean = false
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            synchronized(this) {
                getSingleLiveData(event, sticky).postValue(event)
            }
        } else {
            getSingleLiveData(event, sticky).value = event
        }
    }


    private fun getSingleLiveData(
        event: Any,
        sticky: Boolean
    ): EventLiveData<Any> {
        val (liveData, stickyLiveData) = getLiveData(
            event::class.java,
            eventMap,
            stickyEventMap
        )
        return if (sticky) {
            stickyLiveData
        } else {
            liveData
        }
    }

    @MainThread
    fun sendSticky(event: Any) = send(event, true)

    @MainThread
    fun send(event: Any) = send(event, false)

    private object Singleton {
        val instance = LiveEventBus()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    class Bus<T>(
        private val clazz: Class<T>,
        private val liveDataMap: LruCache<Class<*>, EventLiveData<*>>,
        private val stickyEventMap: ArrayMap<Class<*>, EventLiveData<*>>
    ) {

        /**
         * @param owner
         * @param ownerKey [LifecycleOwner] 的 key 默认为 [EventLiveData.getKey]
         * @param forever 是否一直观察 true:调用[observe]之后就会收到回调，直到[owner]销毁，
         * false:仅在 [owner] 的生命周期 >= [Lifecycle.Event.ON_START] 时才会收到回调
         */
        @Suppress("UNCHECKED_CAST")
        fun observe(
            owner: LifecycleOwner,
            ownerKey: String? = null,
            forever: Boolean = false,
            observer: Observer<T>
        ) {
            val (liveData, stickyLiveData) = getLiveData(
                clazz,
                liveDataMap,
                stickyEventMap
            ) as Pair<EventLiveData<T>, EventLiveData<T>>
            val k = ownerKey ?: liveData.getKey(owner)
            if (forever) {
                liveData.observeForever(owner, k, observer)
                stickyLiveData.observeForever(owner, k, observer)
            } else {
                liveData.observe(owner, k, observer)
                stickyLiveData.observeForever(owner, k, observer)
            }
        }
    }


    @RestrictTo(RestrictTo.Scope.LIBRARY)
    class PrimitiveBus(
        val eventKey: String = "",
        private val liveDataMap: LruCache<Class<*>, EventLiveData<*>>,
        private val stickyEventMap: ArrayMap<Class<*>, EventLiveData<*>>
    ) {

        inline fun observeInt(
            owner: LifecycleOwner,
            ownerKey: String? = null,
            forever: Boolean = false,
            crossinline block: (Int) -> Unit
        ) {
            this.observe(owner, ownerKey, forever = forever) {
                if (it.eventKey == eventKey) {
                    block(it.intValue)
                }
            }
        }

        inline fun observeLong(
            owner: LifecycleOwner,
            ownerKey: String? = null,
            forever: Boolean = false,
            crossinline block: (Long) -> Unit
        ) {
            this.observe(owner, ownerKey, forever = forever) {
                if (it.eventKey == eventKey) {
                    block(it.longValue)
                }
            }
        }

        inline fun observeDouble(
            owner: LifecycleOwner,
            ownerKey: String? = null,
            forever: Boolean = false,
            crossinline block: (Double) -> Unit
        ) {
            this.observe(owner, ownerKey, forever = forever) {
                if (it.eventKey == eventKey) {
                    block(it.doubleValue)
                }
            }
        }

        inline fun observeFloat(
            owner: LifecycleOwner,
            ownerKey: String? = null,
            forever: Boolean = false,
            crossinline block: (Float) -> Unit
        ) {
            this.observe(owner, ownerKey, forever = forever) {
                if (it.eventKey == eventKey) {
                    block(it.floatValue)
                }
            }
        }

        inline fun observeBoolean(
            owner: LifecycleOwner,
            ownerKey: String? = null,
            forever: Boolean = false,
            crossinline block: (Boolean) -> Unit
        ) {
            this.observe(owner, ownerKey, forever = forever) {
                if (it.eventKey == eventKey) {
                    block(it.booleanValue)
                }
            }
        }

        inline fun observeChar(
            owner: LifecycleOwner,
            ownerKey: String? = null,
            forever: Boolean = false,
            crossinline block: (Char) -> Unit
        ) {
            this.observe(owner, ownerKey, forever = forever) {
                if (it.eventKey == eventKey) {
                    block(it.charValue)
                }
            }
        }

        inline fun observeString(
            owner: LifecycleOwner,
            ownerKey: String? = null,
            forever: Boolean = false,
            crossinline block: (String) -> Unit
        ) {
            this.observe(owner, ownerKey, forever = forever) {
                if (it.eventKey == eventKey) {
                    block(it.stringValue)
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun observe(
            owner: LifecycleOwner,
            key: String?,
            forever: Boolean,
            observer: Observer<in PrimitiveEvent>,
        ) {
            val (liveData, stickyLiveData) =
                getLiveData(
                    PrimitiveEvent::class.java,
                    liveDataMap,
                    stickyEventMap
                ) as Pair<EventLiveData<PrimitiveEvent>, EventLiveData<PrimitiveEvent>>
            val k = key ?: liveData.getKey(owner)
            if (forever) {
                liveData.observeForever(owner, k, observer)
                stickyLiveData.observeForever(owner, k, observer)
            } else {
                liveData.observe(owner, k, observer)
                stickyLiveData.observe(owner, k, observer)
            }
        }
    }

    /**
     * 基本数据类型事件
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    data class PrimitiveEvent(
        val eventKey: String = "",
        val sticky: Boolean = false,
        val booleanValue: Boolean = false,
        val stringValue: String = "",
        val intValue: Int = 0,
        val longValue: Long = 0L,
        val doubleValue: Double = 0.0,
        val floatValue: Float = 0F,
        val charValue: Char = ' '
    )
}
