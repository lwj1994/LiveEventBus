package com.lwjlol.liveeventbus.demo

import androidx.lifecycle.ViewModel
import com.lwjlol.liveeventbus.EventLiveData

class AViewModel:ViewModel() {
    val liveData = EventLiveData<Int>(false)
}
