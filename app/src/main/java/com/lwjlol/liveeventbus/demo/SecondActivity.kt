package com.lwjlol.liveeventbus.demo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lwjlol.liveeventbus.LiveEventBus

class SecondActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sec)
        findViewById<TextView>(R.id.text).text = R.id.view_container.toString()
        findViewById<TextView>(R.id.send).setOnClickListener {
            LiveEventBus.instance.send(SecondEvent("event from SecondActivity"))
            LiveEventBus.instance.send("key1", 1)
            LiveEventBus.instance.send("key1", 1)
            LiveEventBus.instance.send("key2", 2)
            LiveEventBus.instance.send("key2", 2)
//            LiveEventBus.instance.send("key3",false)
//            LiveEventBus.instance.send("key2","213123")
        }
        findViewById<TextView>(R.id.open).setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
            findViewById<View>(R.id.send).setOnClickListener {
                LiveEventBus.instance.send(
                    StickyEvent("sticky event from SecondActivity"),
                    sticky = true
                )

            }
            finish()
        }

        LiveEventBus.instance.on(StickyEvent::class.java).observe(this, "lwjlol") {
            findViewById<TextView>(R.id.text).text = it.name
        }


    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
