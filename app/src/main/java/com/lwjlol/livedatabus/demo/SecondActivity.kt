package com.lwjlol.livedatabus.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.lwjlol.livedatabus.LiveDataBus

class SecondActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    findViewById<TextView>(R.id.textview).text = "SecondActivity"
    LiveDataBus.instance.on(SecondEvent::class.java).observe(this){
      findViewById<TextView>(R.id.textview).text = it.name
    }
    LiveDataBus.instance.on(SecondEvent::class.java).observeSticky(this){
      findViewById<TextView>(R.id.textview).text = it.name
    }
  }

  override fun onDestroy() {
    LiveDataBus.instance.post(FirstEvent("222"))
    LiveDataBus.instance.postSticky(FirstEvent("222--postSticky"))
    super.onDestroy()
  }
}
