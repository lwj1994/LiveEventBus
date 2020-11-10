package com.lwjlol.liveeventbus.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lwjlol.liveeventbus.LiveEventBus

class TestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.decorView.postDelayed(
            {
                LiveEventBus.instance.send(EventC("我发送了给了 2 个人"))

            }, 3000
        )
    }
}
