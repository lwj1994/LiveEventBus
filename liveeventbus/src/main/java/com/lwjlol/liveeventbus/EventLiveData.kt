package com.lwjlol.liveeventbus

import androidx.annotation.MainThread
import androidx.collection.ArrayMap
import androidx.lifecycle.*

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
    observe(owner, owner::class.qualifiedName ?: "", observer)
  }

  @MainThread
  fun observe(
    owner: LifecycleOwner,
    key: String = owner::class.qualifiedName ?: "",
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

  @Deprecated("use observeForever(owner: LifecycleOwner, key: String, observer: Observer)",
    ReplaceWith("", "")
  )
  override fun observeForever(observer: Observer<in T>) {
    super.observeForever(observer)
//    throw IllegalAccessException("use observeForever(owner: LifecycleOwner, key: String, observer: Observer)")
  }

  @MainThread
  fun observeForever(
    owner: LifecycleOwner,
    key: String = owner::class.qualifiedName ?: "",
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

  companion object {
    private val UNSET = Any()
    private const val TAG = "EventLiveData"
  }
}
