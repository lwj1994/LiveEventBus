package com.lwjlol.liveeventbus

import androidx.collection.ArrayMap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.OnLifecycleEvent

/**
 * @param sticky indicate that event is a sticky event
 */
class EventLiveData<T>(val sticky: Boolean = false) : MutableLiveData<T>() {
  private val tempValueMap = ArrayMap<String, Any>(16)
  private val foreverObserverMap = ArrayMap<String, Observer<T>>(16)
  private var isObservedMap = ArrayMap<String, Boolean>(16)

  override fun setValue(value: T) {
    for (item in tempValueMap) {
      // 跳过非粘性还没注册的组件
      if (isObservedMap[item.key] != true && !sticky) {
        continue
      }
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
    owner.lifecycle.addObserver(OnDestroyLifecycleObserver(this, key))
    isObservedMap[key] = true
    if (!sticky) {
      tempValueMap[key] = UNSET
    } else {
      if (tempValueMap[key] != UNSET) {
        tempValueMap[key] = value
      }
    }
    super.observe(owner, Observer {
      val value = tempValueMap[key]
      if (value == UNSET || it == null) return@Observer
      @Suppress("UNCHECKED_CAST")
      observer.onChanged(value as T)
      tempValueMap[key] = UNSET
    })
  }

  override fun observeForever(observer: Observer<in T>) {
    throw IllegalAccessException("")
  }

  fun observeForever(
    owner: LifecycleOwner,
    key: String = "",
    observer: Observer<in T>
  ) {
    owner.lifecycle.addObserver(OnDestroyLifecycleObserver(this, key))
    isObservedMap[key] = true
    if (!sticky) {
      tempValueMap[key] = UNSET
    } else {
      if (tempValueMap[key] != UNSET) {
        tempValueMap[key] = value
      }
    }
    val foreverObserver = Observer<T> {
      val value = tempValueMap[key]
      if (value == UNSET || it == null) return@Observer
      @Suppress("UNCHECKED_CAST")
      observer.onChanged(value as T)
      tempValueMap[key] = UNSET
    }
    foreverObserverMap[key] = foreverObserver
    super.observeForever(foreverObserver)
  }

  fun onClear(key: String) {
    foreverObserverMap[key]?.let {
      removeObserver(it)
    }
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

  companion object {
    private val UNSET = Any()
    private const val TAG = "EventLiveData"
  }
}
