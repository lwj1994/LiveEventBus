
[ ![Download](https://api.bintray.com/packages/wenchieh/maven/LiveEventBus/images/download.svg) ](https://bintray.com/wenchieh/maven/LiveEventBus/_latestVersion)
```
implementation 'com.lwjlol:liveeventbus:0.0.1'
```

## 前言
本库是使用 LiveData 实现的 EventBus，目前网上很火的美团方案实现的 EventBus 对 LiveData 源码入侵太严重，我觉得太不优雅了。实现一个 EventBus 其实很简单，`LiveEventBus` 就是一种优雅的实现方式，没有修改任何源码，也没有任何反射。
`LiveEventBus` 的优点：
* __类型安全__
* __没有反射，不修改源码，无需担心 LiveData 版本问题__
* __可以设置 event 缓存的个数__
* __基于 LiveData 与 Lifecycle 紧密联系，可以自由选择是否关联 Lifecycle 的周期__
* __支持粘性事件__
* __只有 2 个类十分小巧，你可以直接 copy 去使用__
## 使用方法


LiveEventBus 将事件分为这 4 种，每种有对应的使用方法，不要混淆他们。
* 关联生命周期的普通事件
* 关联生命周期的粘性事件
* 不关联生命周期 (但是会 __自动销毁__ ) 的普通事件
* 不关联生命周期 (但是会 __自动销毁__ ) 的粘性事件
### 关联生命周期的普通事件
这类事件和 LiveData 一样，完全跟随生命周期，在 lifecycle 不活跃的时候不会接收到消息。

* __所以一旦 Activity 进入 onPause 就收不到消息，只有等 Activity 恢复到 onResume 才能继续收到消息。__
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
* __当 Activity/Fragment 不活跃的时候（onPause）同样能收到事件进行回调__
* __在 Activity/Fragment onDestory 的时候自动注销事件，所以不必担心泄露问题__

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
## 支持设置最大事件缓存
```
// 超过 200 条事件会自动清除掉最不经常使用的事件
LiveEventBus.setInitMaxEventSize(200)
```
