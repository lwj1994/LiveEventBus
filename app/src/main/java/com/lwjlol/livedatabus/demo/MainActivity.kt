package com.lwjlol.livedatabus.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.lwjlol.livedatabus.LiveDataBus

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    LiveDataBus.instance.on(String::class.java).observe(this){

    }

  }
}
