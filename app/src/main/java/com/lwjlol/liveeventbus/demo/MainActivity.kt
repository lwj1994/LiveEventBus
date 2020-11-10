package com.lwjlol.liveeventbus.demo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lwjlol.liveeventbus.LiveEventBus

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    LiveEventBus.setInitMaxEventSize(200)

    LiveEventBus.instance.on(FirstEvent::class.java).observe(this) {
      findViewById<TextView>(R.id.textview).text = it.name
      Toast.makeText(this, "FirstEvent", Toast.LENGTH_LONG).show()
    }


    LiveEventBus.instance.on(ForeverEvent::class.java).observeForever(this) {
      Toast.makeText(this, "ForeverEvent", Toast.LENGTH_LONG).show()
    }

    LiveEventBus.instance.sendSticky(SecondEvent("SecondEvent postSticky"))

    findViewById<TextView>(R.id.textview).setOnClickListener {
      startActivity(Intent(this, SecondActivity::class.java))
    }

    LiveEventBus.instance.on(EventC::class.java).observeForever(this) {
      Log.d("EventC", "MainActivity$it")
    }
  }
}
