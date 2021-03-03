package com.lwjlol.liveeventbus

import android.os.Looper
import android.util.Log
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

    /**
     * @param eventKey 基本数据类型的事件 key
     */
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
    fun send(eventKey: String, value: String, sticky: Boolean = true) {
        send(PrimitiveStringEvent(eventKey = eventKey, stringValue = value), sticky)
    }

    @MainThread
    fun send(eventKey: String, value: Int, sticky: Boolean = true) {
        send(PrimitiveIntEvent(eventKey = eventKey, intValue = value), sticky)
    }

    @MainThread
    fun send(eventKey: String, value: Long, sticky: Boolean = true) {
        send(PrimitiveLongEvent(eventKey = eventKey, longValue = value), sticky)
    }

    @MainThread
    fun send(eventKey: String, value: Double, sticky: Boolean = true) {
        send(PrimitiveDoubleEvent(eventKey = eventKey, doubleValue = value), sticky)
    }

    @MainThread
    fun send(eventKey: String, value: Float, sticky: Boolean = true) {
        send(PrimitiveFloatEvent(eventKey = eventKey, floatValue = value), sticky)
    }

    @MainThread
    fun send(eventKey: String, value: Char, sticky: Boolean = true) {
        send(PrimitiveCharEvent(eventKey = eventKey, charValue = value), sticky)
    }

    @MainThread
    fun send(eventKey: String, value: Boolean, sticky: Boolean = true) {
        send(PrimitiveBooleanEvent(eventKey = eventKey, booleanValue = value), sticky)
    }

    /**
     * @param sticky 默认 true，发送粘性事件
     */
    @MainThread
    fun send(
        event: Any,
        sticky: Boolean = true
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

    private object Singleton {
        val instance = LiveEventBus()
    }

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
            val k = ownerKey ?: EventLiveData.getKey(owner)
            if (forever) {
                liveData.observeForever(owner, k, observer)
                stickyLiveData.observeForever(owner, k, observer)
            } else {
                liveData.observe(owner, k, observer)
                stickyLiveData.observe(owner, k, observer)
            }
        }
    }


    class PrimitiveBus(
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        val eventKey: String,
        private val liveDataMap: LruCache<Class<*>, EventLiveData<*>>,
        private val stickyEventMap: ArrayMap<Class<*>, EventLiveData<*>>
    ) {

        inline fun observeInt(
            owner: LifecycleOwner,
            ownerKey: String? = null,
            forever: Boolean = false,
            crossinline block: (Int) -> Unit
        ) {
            this.observeInt(owner, ownerKey, eventKey, forever = forever) {
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
            this.observeLong(owner, ownerKey, eventKey, forever = forever) {
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
            this.observeDouble(owner, ownerKey, eventKey, forever = forever) {
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
            this.observeFloat(owner, ownerKey, eventKey, forever = forever) {
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
            this.observeBoolean(owner, ownerKey, eventKey, forever = forever) {
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
            this.observeChar(owner, ownerKey, eventKey, forever = forever) {
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
            this.observeString(owner, ownerKey, eventKey, forever = forever) {
                if (it.eventKey == eventKey) {
                    block(it.stringValue)
                }
            }
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Suppress("UNCHECKED_CAST")
        fun observeDouble(
            owner: LifecycleOwner,
            ownerKey: String?,
            eventKey: String,
            forever: Boolean,
            observer: Observer<in PrimitiveDoubleEvent>,
        ) {
            val (liveData, stickyLiveData) =
                getLiveData(
                    PrimitiveDoubleEvent::class.java,
                    liveDataMap,
                    stickyEventMap
                ) as Pair<EventLiveData<PrimitiveDoubleEvent>, EventLiveData<PrimitiveDoubleEvent>>
            val k = "${(ownerKey ?: EventLiveData.getKey(owner))}_${eventKey}"
            if (forever) {
                liveData.observeForever(owner, k, observer)
                stickyLiveData.observeForever(owner, k, observer)
            } else {
                liveData.observe(owner, k, observer)
                stickyLiveData.observe(owner, k, observer)
            }
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Suppress("UNCHECKED_CAST")
        fun observeFloat(
            owner: LifecycleOwner,
            ownerKey: String?,
            eventKey: String,
            forever: Boolean,
            observer: Observer<in PrimitiveFloatEvent>,
        ) {
            val (liveData, stickyLiveData) =
                getLiveData(
                    PrimitiveFloatEvent::class.java,
                    liveDataMap,
                    stickyEventMap
                ) as Pair<EventLiveData<PrimitiveFloatEvent>, EventLiveData<PrimitiveFloatEvent>>
            val k = "${(ownerKey ?: EventLiveData.getKey(owner))}_${eventKey}"
            if (forever) {
                liveData.observeForever(owner, k, observer)
                stickyLiveData.observeForever(owner, k, observer)
            } else {
                liveData.observe(owner, k, observer)
                stickyLiveData.observe(owner, k, observer)
            }
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Suppress("UNCHECKED_CAST")
        fun observeLong(
            owner: LifecycleOwner,
            ownerKey: String?,
            eventKey: String,
            forever: Boolean,
            observer: Observer<in PrimitiveLongEvent>,
        ) {
            val (liveData, stickyLiveData) =
                getLiveData(
                    PrimitiveLongEvent::class.java,
                    liveDataMap,
                    stickyEventMap
                ) as Pair<EventLiveData<PrimitiveLongEvent>, EventLiveData<PrimitiveLongEvent>>
            val k = "${(ownerKey ?: EventLiveData.getKey(owner))}_${eventKey}"
            if (forever) {
                liveData.observeForever(owner, k, observer)
                stickyLiveData.observeForever(owner, k, observer)
            } else {
                liveData.observe(owner, k, observer)
                stickyLiveData.observe(owner, k, observer)
            }
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Suppress("UNCHECKED_CAST")
        fun observeInt(
            owner: LifecycleOwner,
            ownerKey: String?,
            eventKey: String,
            forever: Boolean,
            observer: Observer<in PrimitiveIntEvent>,
        ) {
            val (liveData, stickyLiveData) =
                getLiveData(
                    PrimitiveIntEvent::class.java,
                    liveDataMap,
                    stickyEventMap
                ) as Pair<EventLiveData<PrimitiveIntEvent>, EventLiveData<PrimitiveIntEvent>>
            val k = "${(ownerKey ?: EventLiveData.getKey(owner))}_${eventKey}"
            if (forever) {
                liveData.observeForever(owner, k, observer)
                stickyLiveData.observeForever(owner, k, observer)
            } else {
                liveData.observe(owner, k, observer)
                stickyLiveData.observe(owner, k, observer)
            }
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Suppress("UNCHECKED_CAST")
        fun observeBoolean(
            owner: LifecycleOwner,
            ownerKey: String?,
            eventKey: String,
            forever: Boolean,
            observer: Observer<in PrimitiveBooleanEvent>,
        ) {
            val (liveData, stickyLiveData) =
                getLiveData(
                    PrimitiveBooleanEvent::class.java,
                    liveDataMap,
                    stickyEventMap
                ) as Pair<EventLiveData<PrimitiveBooleanEvent>, EventLiveData<PrimitiveBooleanEvent>>
            val k = "${(ownerKey ?: EventLiveData.getKey(owner))}_${eventKey}"
            if (forever) {
                liveData.observeForever(owner, k, observer)
                stickyLiveData.observeForever(owner, k, observer)
            } else {
                liveData.observe(owner, k, observer)
                stickyLiveData.observe(owner, k, observer)
            }
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Suppress("UNCHECKED_CAST")
        fun observeChar(
            owner: LifecycleOwner,
            ownerKey: String?,
            eventKey: String,
            forever: Boolean,
            observer: Observer<in PrimitiveCharEvent>,
        ) {
            val (liveData, stickyLiveData) =
                getLiveData(
                    PrimitiveCharEvent::class.java,
                    liveDataMap,
                    stickyEventMap
                ) as Pair<EventLiveData<PrimitiveCharEvent>, EventLiveData<PrimitiveCharEvent>>
            val k = "${(ownerKey ?: EventLiveData.getKey(owner))}_${eventKey}"
            if (forever) {
                liveData.observeForever(owner, k, observer)
                stickyLiveData.observeForever(owner, k, observer)
            } else {
                liveData.observe(owner, k, observer)
                stickyLiveData.observe(owner, k, observer)
            }
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Suppress("UNCHECKED_CAST")
        fun observeString(
            owner: LifecycleOwner,
            ownerKey: String?,
            eventKey: String,
            forever: Boolean,
            observer: Observer<in PrimitiveStringEvent>,
        ) {
            val (liveData, stickyLiveData) =
                getLiveData(
                    PrimitiveStringEvent::class.java,
                    liveDataMap,
                    stickyEventMap
                ) as Pair<EventLiveData<PrimitiveStringEvent>, EventLiveData<PrimitiveStringEvent>>
            val k = "${(ownerKey ?: EventLiveData.getKey(owner))}_${eventKey}"
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
    data class PrimitiveStringEvent(
        val eventKey: String = "",
        val sticky: Boolean = false,
        val stringValue: String = ""
    )

    /**
     * 基本数据类型事件
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    data class PrimitiveIntEvent(
        val eventKey: String = "",
        val sticky: Boolean = false,
        val intValue: Int = 0
    )

    /**
     * 基本数据类型事件
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    data class PrimitiveLongEvent(
        val eventKey: String = "",
        val sticky: Boolean = false,
        val longValue: Long = 0L
    )


    /**
     * 基本数据类型事件
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    data class PrimitiveFloatEvent(
        val eventKey: String = "",
        val sticky: Boolean = false,
        val floatValue: Float = 0F
    )

    /**
     * 基本数据类型事件
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    data class PrimitiveDoubleEvent(
        val eventKey: String = "",
        val sticky: Boolean = false,
        val doubleValue: Double = 0.0
    )

    /**
     * 基本数据类型事件
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    data class PrimitiveBooleanEvent(
        val eventKey: String = "",
        val sticky: Boolean = false,
        val booleanValue: Boolean = false
    )


    /**
     * 基本数据类型事件
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    data class PrimitiveCharEvent(
        val eventKey: String = "",
        val sticky: Boolean = false,
        val charValue: Char = ' '
    )
}
