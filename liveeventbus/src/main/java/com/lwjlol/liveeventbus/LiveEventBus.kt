package com.lwjlol.liveeventbus

import android.os.Looper
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
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
                getLiveData(event::class.java, sticky, eventMap, stickyEventMap).postValue(event)
            }
        } else {
            getLiveData(event::class.java, sticky, eventMap, stickyEventMap).setValue(event)
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


    @RestrictTo(RestrictTo.Scope.LIBRARY)
    class PrimitiveBus(
        val eventKey: String = "",
        private val liveDataMap: LruCache<Class<*>, EventLiveData<*>>,
        private val stickyEventMap: ArrayMap<Class<*>, EventLiveData<*>>
    ) {

        inline fun observeInt(
            owner: LifecycleOwner,
            ownerKey: String? = null,
            sticky: Boolean = false,
            forever: Boolean = false,
            crossinline block: (Int) -> Unit
        ) {
            this.observe(owner, ownerKey, sticky = sticky, forever = forever) {
                if (it.eventKey == eventKey) {
                    block(it.intValue)
                }
            }
        }

        inline fun observeLong(
            owner: LifecycleOwner,
            ownerKey: String? = null,
            sticky: Boolean = false,
            forever: Boolean = false,
            crossinline block: (Long) -> Unit
        ) {
            this.observe(owner, ownerKey, sticky = sticky, forever = forever) {
                if (it.eventKey == eventKey) {
                    block(it.longValue)
                }
            }
        }

        inline fun observeDouble(
            owner: LifecycleOwner,
            ownerKey: String? = null,
            sticky: Boolean = false,
            forever: Boolean = false,
            crossinline block: (Double) -> Unit
        ) {
            this.observe(owner, ownerKey, sticky = sticky, forever = forever) {
                if (it.eventKey == eventKey) {
                    block(it.doubleValue)
                }
            }
        }

        inline fun observeFloat(
            owner: LifecycleOwner,
            ownerKey: String? = null,
            sticky: Boolean = false,
            forever: Boolean = false,
            crossinline block: (Float) -> Unit
        ) {
            this.observe(owner, ownerKey, sticky = sticky, forever = forever) {
                if (it.eventKey == eventKey) {
                    block(it.floatValue)
                }
            }
        }

        inline fun observeBoolean(
            owner: LifecycleOwner,
            ownerKey: String? = null,
            sticky: Boolean = false,
            forever: Boolean = false,
            crossinline block: (Boolean) -> Unit
        ) {
            this.observe(owner, ownerKey, sticky = sticky, forever = forever) {
                if (it.eventKey == eventKey) {
                    block(it.booleanValue)
                }
            }
        }

        inline fun observeChar(
            owner: LifecycleOwner,
            ownerKey: String? = null,
            sticky: Boolean = false,
            forever: Boolean = false,
            crossinline block: (Char) -> Unit
        ) {
            this.observe(owner, ownerKey, sticky = sticky, forever = forever) {
                if (it.eventKey == eventKey) {
                    block(it.charValue)
                }
            }
        }

        inline fun observeString(
            owner: LifecycleOwner,
            ownerKey: String? = null,
            sticky: Boolean = false,
            forever: Boolean = false,
            crossinline block: (String) -> Unit
        ) {
            this.observe(owner, ownerKey, sticky = sticky, forever = forever) {
                if (it.eventKey == eventKey) {
                    block(it.stringValue)
                }
            }
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun observe(
            owner: LifecycleOwner,
            key: String?,
            sticky: Boolean,
            forever: Boolean,
            observer: Observer<in PrimitiveEvent>,
        ) {
            @Suppress("UNCHECKED_CAST") val liveData = getLiveData(
                PrimitiveEvent::class.java,
                sticky,
                liveDataMap,
                stickyEventMap
            ) as EventLiveData<PrimitiveEvent>
            val k = key ?: liveData.getKey(owner)
            if (forever) {
                liveData.observeForever(owner, k, observer)
            } else {
                liveData.observe(owner, k, observer)
            }
        }
    }

    /**
     * 基本数据类型事件
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    data class PrimitiveEvent(
        val eventKey: String = "",
        val isSticky: Boolean = false,
        val booleanValue: Boolean = false,
        val stringValue: String = "",
        val intValue: Int = 0,
        val longValue: Long = 0L,
        val doubleValue: Double = 0.0,
        val floatValue: Float = 0F,
        val charValue: Char = ' '
    )
}
