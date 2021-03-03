package com.lwjlol.liveeventbus.demo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.lwjlol.liveeventbus.EventLiveData
import com.lwjlol.liveeventbus.LiveEventBus

class MainActivity : AppCompatActivity() {
    val viewModel by lazy {
        ViewModelUtil.getViewModel(this, AViewModel::class.java)
    }

    val liveData = EventLiveData<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
//        liveData.value = null
        liveData.observeNonNull(this) {
            findViewById<TextView>(R.id.text).text = "${it}"
        }

        LiveEventBus.instance.on(SecondEvent::class.java).observe(this,forever = true){
//            Toast.makeText(this,it.name+",state:${lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)}",Toast.LENGTH_LONG).show()
//            findViewById<TextView>(R.id.text).text = "${it.name}"
        }
        LiveEventBus.instance.on("key1").observeInt(this,forever = true){
            findViewById<TextView>(R.id.text).text = findViewById<TextView>(R.id.text).text.toString() + "$it"
        }

        LiveEventBus.instance.on("key2").observeInt(this,forever = false){
            findViewById<TextView>(R.id.text).text = findViewById<TextView>(R.id.text).text.toString() + "$it"
        }

        LiveEventBus.instance.on("key3").observeBoolean(this,forever = true){
            findViewById<TextView>(R.id.text).text = findViewById<TextView>(R.id.text).text.toString() + "\n$it"
        }
        findViewById<View>(R.id.send).setOnClickListener {
            LiveEventBus.instance.send(FirstEvent("sticky event from MainActivity"),sticky = true)

        }
        findViewById<View>(R.id.call).setOnClickListener {
            Thread {
                liveData.call()
                liveData.postValue(null)
                liveData.postValue("23123")
                liveData.postValue("2")
                liveData.postValue("3")
                liveData.call()

            }.start()


        }
        findViewById<View>(R.id.open).setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }
        LiveEventBus.instance.send("weqeqe", 12321313)
        LiveEventBus.instance.on("qwe").observeLong(this) {

        }

//            .observeForever(this) {
//            findViewById<TextView>(R.id.text).text = it.name
//        }

        com.lwjlol.liveeventbus.javaversion.LiveEventBus.getInstance()
            .send("qeqwe", "wqeqweqwe")
        com.lwjlol.liveeventbus.javaversion.LiveEventBus.getInstance().on("qeqwe")
            .observe(this, com.lwjlol.liveeventbus.javaversion.LiveEventBus.CallbackString {

            })


    }
}
