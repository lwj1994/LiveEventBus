package com.lwjlol.livedatabus

import androidx.collection.ArrayMap
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

class EventLiveData<T> : MutableLiveData<T>() {
    private val observerMap = ArrayMap<String, Any>()

    override fun setValue(value: T) {
        for (item in observerMap) {
            item.setValue(value)
        }
        super.setValue(value)
    }

    override fun observe(
        owner: LifecycleOwner,
        observer: Observer<in T>
    ) {

        observe(owner, observer = {
            observer.onChanged(it)
        })
    }

    fun observe(
        owner: LifecycleOwner,
        key: String = owner::class.simpleName ?: "",
        observer: (value: T) -> Unit
    ) {
        observerMap[key] = UNSET
        super.observe(owner, Observer {
            val value = observerMap[key]
            if (value == UNSET || it == null) return@Observer
            @Suppress("UNCHECKED_CAST")
            observer(value as T)
            observerMap[key] = UNSET
        })
    }

    companion object {
        private val UNSET = Any()
        private const val TAG = "FleetingLiveData"
    }
}
