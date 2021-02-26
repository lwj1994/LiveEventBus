
[![](https://jitpack.io/v/lwj1994/LiveEventBus.svg)](https://jitpack.io/#lwj1994/LiveEventBus)

## How to

To get a Git project into your build:
### Step 1. Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:
```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

### Step 2. Add the dependency
```
	dependencies {
	        implementation 'com.github.lwj1994:LiveEventBus:0.2.0'
	}
```


## 前言
本库是使用 LiveData 实现的 EventBus，`LiveEventBus` 就是一种优雅的实现方式，没有修改任何源码，也没有任何反射。
`LiveEventBus` 的优点：
* __类型安全__
* __没有反射，不修改源码，无需担心 LiveData 版本问题__
* __基于 LiveData 与 Lifecycle 紧密联系，可以自由选择是否关联 Lifecycle 的周期__
* __可选是否支持粘性事件__
* __只有 2 个类十分小巧，你可以直接 copy 去使用__
## 使用方法


LiveEventBus 将事件分为这 4 种，每种有对应的使用方法，不要混淆他们。
* 关联生命周期的普通事件
* 关联生命周期的粘性事件
* 不关联生命周期 (但是会 __自动销毁__ ) 的普通事件
* 不关联生命周期 (但是会 __自动销毁__ ) 的粘性事件
### 关联生命周期的普通事件
这类事件和 LiveData 一样，完全跟随生命周期，在 lifecycle 不活跃的时候不会接收到消息。

* __所以一旦 Activity 进入 onStop 就收不到消息，只有等 Activity 恢复到 onStart 才能继续收到消息。__
* __这类事件不支持粘性，只有先 observe 才能收到后续的消息__
```
//接收事件：
LiveEventBus.instance.on(Event::class.java).observe(lifecycleOwner) {
    //
}
// 发送事件：
LiveEventBus.instance.send(Event()）
```
### 关联生命周期的粘性事件
和上述事件【关联生命周期的普通事件】一样，但是多了粘性的特性：

* __支持先 sendSticky 发送事件，然后再  observeSticky__
```
// 发送粘性事件：
LiveEventBus.instance.sendSticky(Event()）

//接收粘性事件：
LiveEventBus.instance.on(Event::class.java).observeSticky(lifecycleOwner) {
    //
}
```
### 不关联生命周期 (但是会自动销毁) 的普通事件
和 【关联生命周期的普通事件】类似，不同的是：
* __当 Activity/Fragment 不活跃的时候（onStop）同样能收到事件进行回调__
* __在 Activity/Fragment onDestroy 的时候自动注销事件，所以不必担心泄露问题__

```
//接收事件：
LiveEventBus.instance.on(ForeverEvent::class.java).observeForever(this) {
    //
}
// 发送事件
LiveEventBus.instance.send(ForeverEvent())
```

### 不关联生命周期 (但是会自动销毁) 的粘性事件
和【不关联生命周期 (但是会自动销毁) 的普通事件】类似，但是多了粘性的特性：
* __支持先 sendSticky 发送事件，然后再  observeSticky__
```
// 发送粘性事件
LiveEventBus.instance.sendSticky(ForeverEvent())
// 接收粘性事件
LiveEventBus.instance.on(ForeverEvent::class.java).observeForeverSticky(this) {
    //
}
```

## 原理
LiveEventBus 主要内部实现依赖于 EventLiveData， EventLiveData 继承自 LiveData，可以单独拿出来使用。

避免在 onActive 时响应回调的原理, 重写了 observe，给 `LifecycleOwner` 配上一个 key 参数。
* 判断当前的 key 是否已经注册过来实现是否是粘性
* 为每一个 key 单独保存 value，消费完一个事件立马置为 UNSET 状态，判断当前的 key 是否是 UNSET 状态觉得是否响应回调
```
fun observe(
        owner: LifecycleOwner,
        key: String = getKey(owner),
        observer: Observer<in T>
    )
```

