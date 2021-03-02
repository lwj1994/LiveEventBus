package com.lwjlol.liveeventbus.demo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

        findViewById<View>(R.id.send).setOnClickListener {
            LiveEventBus.instance.sendSticky(FirstEvent("event from MainActivity"))
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
