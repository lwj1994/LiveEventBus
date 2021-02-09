package com.lwjlol.liveeventbus.demo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lwjlol.liveeventbus.EventLiveData
import com.lwjlol.liveeventbus.LiveEventBus

class MainActivity : AppCompatActivity() {
    val liveData = EventLiveData<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        liveData.observeCall(this) {
            findViewById<TextView>(R.id.text).text = "call"
        }

        findViewById<View>(R.id.send).setOnClickListener {
            LiveEventBus.instance.sendSticky(FirstEvent("event from MainActivity"))
        }
        findViewById<View>(R.id.call).setOnClickListener {
            liveData.call()
        }
        findViewById<View>(R.id.open).setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }
        LiveEventBus.instance.on(SecondEvent::class.java).observeForever(this, {
            findViewById<TextView>(R.id.text).text = it.name
        })

    }
}
