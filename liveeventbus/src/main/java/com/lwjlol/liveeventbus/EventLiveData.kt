package com.lwjlol.liveeventbus

import androidx.annotation.MainThread
import androidx.collection.ArrayMap
import androidx.lifecycle.*

/**
 * @param sticky indicate that event is a sticky event
 */
class EventLiveData<T>(val sticky: Boolean = false) : MutableLiveData<T>() {
    private val tempValueMap = ArrayMap<String, Any?>(2)
    private val foreverObserverMap = ArrayMap<String, Observer<T>>(2)
    private var isObservedMap = ArrayMap<String, Boolean>(2)

    @MainThread
    fun call() {
        value = null
    }

    override fun setValue(value: T?) {
        for (item in tempValueMap) {
            // 跳过非粘性还没注册的组件
            if (isObservedMap[item.key] != true && !sticky) {
                continue
            }
            item.setValue(value ?: NULL)
        }
        super.setValue(value)
    }

    override fun observe(
        owner: LifecycleOwner,
        observer: Observer<in T>
    ) {
        observe(owner, getKey(owner), observer)
    }

    inline fun observeCall(owner: LifecycleOwner, crossinline onCall: (() -> Unit)) {
        observe(owner, Observer {
            onCall()
        })
    }

    inline fun observeCallForever(owner: LifecycleOwner, crossinline onCall: (() -> Unit)) {
        observeForever(owner, {
            onCall()
        })
    }

    inline fun observeNonNull(owner: LifecycleOwner, crossinline block: ((T) -> Unit)) {
        observe(owner, Observer {
            block(it ?: return@Observer)
        })
    }

    inline fun observeForeverNonNull(owner: LifecycleOwner, crossinline block: ((T) -> Unit)) {
        observe(owner, Observer {
            block(it ?: return@Observer)
        })
    }

    inline fun observe(owner: LifecycleOwner, crossinline block: ((T?) -> Unit)) {
        observe(owner, Observer {
            block(it)
        })
    }

    inline fun observeForever(owner: LifecycleOwner, crossinline block: ((T?) -> Unit)) {
        observeForever(owner, Observer {
            block(it)
        })
    }


    @MainThread
    override fun observeForever(observer: Observer<in T>) {
        observeForever(null, observer.toString(), observer)
    }

    override fun removeObserver(observer: Observer<in T>) {
        super.removeObserver(observer)
        reset(observer.toString())
    }

    @Deprecated("use observe(owner,observer)")
    @MainThread
    fun observe(
        owner: LifecycleOwner,
        key: String = getKey(owner),
        observer: Observer<in T>
    ) {
        onObserve(owner, key)
        super.observe(owner, getObserverWrapper(key, observer))
    }

    @Deprecated("use observeForever(owner,observer)")
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

    private fun onObserve(
        owner: LifecycleOwner?,
        key: String
    ) {
        owner?.lifecycle?.addObserver(OnDestroyLifecycleObserver(this, key))
        isObservedMap[key] = true
        if (tempValueMap[key] == null) {
            tempValueMap[key] = if (!sticky || value == null) UNSET else value
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

    private fun getKey(owner: LifecycleOwner) = "${owner::class.qualifiedName}-$owner"

    fun onClear(key: String) {
        foreverObserverMap[key]?.let {
            removeObserver(it)
        }
        tempValueMap[key] = null
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
