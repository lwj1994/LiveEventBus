[![](https://jitpack.io/v/lwj1994/LiveEventBus.svg)](https://jitpack.io/#lwj1994/LiveEventBus)

## 前言
本库是使用 LiveData 实现的 EventBus，`LiveEventBus` 就是一种优雅的实现方式，没有修改任何源码，也没有任何反射。
`LiveEventBus` 的优点：
* __类型安全__
* __没有反射，不修改源码，无需担心 LiveData 版本问题__
* __基于 LiveData 与 Lifecycle 紧密联系，可以自由选择是否关联 Lifecycle 的周期__
* __可选是否支持粘性事件__
* __只有 2 个类十分小巧，你可以直接 copy 去使用__
* __支持直接发射基本数据类型__

> __java 版本__:  https://github.com/lwj1994/LiveEventBus/blob/main/app/src/main/java/com/lwjlol/liveeventbus/javaversion/LiveEventBus.java

## 使用方法
```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
    implementation 'com.github.lwj1994:LiveEventBus:${latestVersion}'
}
```



```kotlin
//注册接收事件：
LiveEventBus.instance.on(Event::class.java).observe(lifecycleOwner) {
    //
}


// 发送事件, sticky:是否粘性，默认 true
// forever: 是否一直观察事件，默认 false
LiveEventBus.instance.send(Event(), sticty = true, forever = false）

```

### 支持直接发射基本数据类型

```kotlin
LiveEventBus.instance.send("key1",1)
LiveEventBus.instance.send("key2",false)
LiveEventBus.instance.send("key3",1L)
LiveEventBus.instance.send("key4",1.0)
LiveEventBus.instance.send("key5",1F)
LiveEventBus.instance.send("key6",'A')
LiveEventBus.instance.send("key7","text")


LiveEventBus.instance.on("key1").observeInt(this){

}

LiveEventBus.instance.on("key2").observeBoolean(this){

}
LiveEventBus.instance.on("key3").observeLong(this){

}
LiveEventBus.instance.on("key4").observeDouble(this){

}
LiveEventBus.instance.on("key5").observeFloat(this){

}
LiveEventBus.instance.on("key6").observeChar(this){

}
LiveEventBus.instance.on("key7").observeString(this){

}
```

## 自定义 ownerKey
你可以根据需求，传入自定义的 ownerKey，该库会根据 ownerKey 来决定是否接收事件：一个事件只能被一个 ownerKey 消费一次。
默认 `ownerKey = lifecycleOwner::class.qualifiedName`
```kotlin
//接收事件：
LiveEventBus.instance.on(Event::class.java).observe(lifecycleOwner, ownerKey) {
    //
}
```



## 原理
`LiveEventBus` 主要内部实现依赖于 `EventLiveData`， `EventLiveData` 继承自 LiveData，可以单独拿出来使用。

避免在 onActive 时响应回调的原理很简单，重写了 observe，给 `LifecycleOwner` 配上一个 ownerKey 参数。
* 判断当前的 ownerKey 是否已经注册过来实现是否是粘性
* 为每一个 ownerKey 单独保存 value，消费完一个事件立马置为 UNSET 状态，判断当前的 ownerKey 是否是 UNSET 状态决定是否响应回调
```kotlin
fun observe(
        owner: LifecycleOwner,
        ownerKey: String = getKey(owner),
        observer: Observer<in T>
    )
```

ownerKey 的默认实现是当前 `LifecycleOwner` 的全路径类名，可以根据实际需求传入 ownerKey：
```kotlin
fun getKey(owner: LifecycleOwner) = "${owner::class.qualifiedName}"
```
