package com.lwjlol.liveeventbus.demo

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lwjlol.liveeventbus.LiveEventBus

class SecondActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.textview).text = "SecondActivity"
        findViewById<TextView>(R.id.textview).setOnClickListener {
            LiveEventBus.instance.sendSticky(
                SecondEvent(
                    findViewById<TextView>(
                        R.id.textview
                    ).text.toString() + "2"
                )
            )
            LiveEventBus.instance.send(FirstEvent("222"))
            LiveEventBus.instance.send(ForeverEvent("3333333333"))
        }
        LiveEventBus.instance.on(SecondEvent::class.java)
            .observeSticky(this) {
                findViewById<TextView>(R.id.textview).text = it.name

            }

    }

    override fun onDestroy() {
        super.onDestroy()
    }
}