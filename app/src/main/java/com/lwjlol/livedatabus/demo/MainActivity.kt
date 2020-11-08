package com.lwjlol.livedatabus.demo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.lwjlol.livedatabus.LiveDataBus

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    LiveDataBus.instance.on(FirstEvent::class.java).observe(this){
      findViewById<TextView>(R.id.textview).text = it.name
    }

    LiveDataBus.instance.postSticky(SecondEvent("SecondEvent postSticky"))
    LiveDataBus.instance.post(SecondEvent("SecondEvent"))

    findViewById<TextView>(R.id.textview).setOnClickListener{
      startActivity(Intent(this,SecondActivity::class.java))
    }
  }
}
