package com.lwjlol.liveeventbus

import androidx.collection.ArrayMap
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

/**
 * @param sticky indicate that event is a sticky event
 */
class EventLiveData<T>(val sticky: Boolean = false) : MutableLiveData<T>() {
    private val valueMap = ArrayMap<String, Any>(16)
    private val foreverObserverMap = ArrayMap<String, Observer<T>>(16)
    private var isObserved = false

    override fun setValue(value: T) {
        if (!isObserved && !sticky) {
            return
        }
        for (item in valueMap) {
            item.setValue(value)
        }
        super.setValue(value)
    }

    override fun observe(
        owner: LifecycleOwner,
        observer: Observer<in T>
    ) {
        observe(owner, owner::class.simpleName ?: "", observer)
    }

    fun observe(
        owner: LifecycleOwner,
        key: String = owner::class.simpleName ?: "",
        observer: Observer<in T>
    ) {
        isObserved = true
        if (!sticky) {
            valueMap[key] = UNSET
        } else {
            valueMap[key] = value
        }
        super.observe(owner, Observer {
            val value = valueMap[key]
            if (value == UNSET || it == null) return@Observer
            @Suppress("UNCHECKED_CAST")
            observer.onChanged(value as T)
            valueMap[key] = UNSET
        })
    }

    override fun observeForever(observer: Observer<in T>) {
        observeForever("", observer)
    }

    fun observeForever(
        key: String = "",
        observer: Observer<in T>
    ) {
        val foreverKey = "forever_$key"
        isObserved = true
        if (!sticky) {
            valueMap[foreverKey] = UNSET
        } else {
            valueMap[foreverKey] = value
        }

        val foreverObserver = Observer<T> {
            val value = valueMap[foreverKey]
            if (value == UNSET || it == null) return@Observer
            @Suppress("UNCHECKED_CAST")
            observer.onChanged(value as T)
            valueMap[foreverKey] = UNSET
        }
        foreverObserverMap[key] = foreverObserver
        super.observeForever(foreverObserver)
    }

    fun removeForeverObserver(key: String) {
        removeObserver(foreverObserverMap[key] ?: return)
    }

    companion object {
        private val UNSET = Any()
        private const val TAG = "EventLiveData"
    }
}
