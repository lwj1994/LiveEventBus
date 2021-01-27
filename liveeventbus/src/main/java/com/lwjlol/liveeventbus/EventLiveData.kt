package com.lwjlol.liveeventbus

import androidx.annotation.MainThread
import androidx.collection.ArrayMap
import androidx.lifecycle.*

/**
 * @param sticky indicate that event is a sticky event
 */
class EventLiveData<T>(val sticky: Boolean = false) : MutableLiveData<T>() {
    private val tempValueMap = ArrayMap<String, Any>(2)
    private val foreverObserverMap = ArrayMap<String, Observer<T>>(2)
    private var isObservedMap = ArrayMap<String, Boolean>(2)

    override fun setValue(value: T?) {
        for (item in tempValueMap) {
            if (value == null) {
                item.setValue(UNSET)
            } else {
                // 跳过非粘性还没注册的组件
                if (isObservedMap[item.key] != true && !sticky) {
                    continue
                }
                item.setValue(value)
            }
        }
        super.setValue(value)
    }

    override fun observe(
        owner: LifecycleOwner,
        observer: Observer<in T>
    ) {
        observe(owner, "${owner::class.qualifiedName}-$owner", observer)
    }

    @MainThread
    override fun observeForever(observer: Observer<in T>) {
        observeForever(null, observer.toString(), observer)
    }

    override fun removeObserver(observer: Observer<in T>) {
        super.removeObserver(observer)
        onClear(observer.toString())
    }

    @Deprecated("use observe(owner,observer)")
    @MainThread
    fun observe(
        owner: LifecycleOwner,
        key: String = "${owner::class.qualifiedName}-$owner",
        observer: Observer<in T>
    ) {
        owner.lifecycle.addObserver(OnDestroyLifecycleObserver(this, key))
        isObservedMap[key] = true
        if (!sticky) {
            tempValueMap[key] = UNSET
        } else {
            if (tempValueMap[key] == null) {
                if (value != null) {
                    tempValueMap[key] = value
                } else {
                    tempValueMap[key] = UNSET
                }
            }
        }
        super.observe(owner, Observer {
            val value = tempValueMap[key]
            if (value == UNSET || value == null || it == null) return@Observer
            @Suppress("UNCHECKED_CAST")
            observer.onChanged(value as T)
            tempValueMap[key] = UNSET
        })
    }

    @Deprecated("use observeForever(owner,observer)")
    @MainThread
    fun observeForever(
        owner: LifecycleOwner?,
        key: String = if (owner == null) "" else "${owner::class.qualifiedName}-$owner",
        observer: Observer<in T>
    ) {
        owner?.lifecycle?.addObserver(OnDestroyLifecycleObserver(this, key))
        isObservedMap[key] = true
        if (!sticky) {
            tempValueMap[key] = UNSET
        } else {
            if (tempValueMap[key] == null) {
                if (value != null) {
                    tempValueMap[key] = value
                } else {
                    tempValueMap[key] = UNSET
                }
            }
        }
        val foreverObserver = Observer<T> {
            val value = tempValueMap[key]
            if (value == UNSET || value == null || it == null) return@Observer
            @Suppress("UNCHECKED_CAST")
            observer.onChanged(value as T)
            tempValueMap[key] = UNSET
        }
        foreverObserverMap[key] = foreverObserver
        super.observeForever(foreverObserver)
    }

    @MainThread
    fun observeForever(
        owner: LifecycleOwner,
        observer: Observer<in T>
    ) {
        observe(owner, "${owner::class.qualifiedName}-$owner", observer)
    }

    fun onClear(key: String) {
        foreverObserverMap[key]?.let {
            removeObserver(it)
        }
        tempValueMap[key] = null
        foreverObserverMap[key] = null
        isObservedMap[key] = false

        val address = super.toString()
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
                sb.append(key).append(":").append(tempValueMap[key])
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
                sb.append(key).append(":").append(foreverObserverMap[key])
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
                sb.append(key).append(":").append(isObservedMap[key])
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

    companion object {
        private val UNSET = Any()
        private const val TAG = "EventLiveData"
    }
}
