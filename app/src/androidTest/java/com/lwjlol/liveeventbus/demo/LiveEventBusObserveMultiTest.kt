package com.lwjlol.liveeventbus.demo

import android.app.Activity
import android.os.Looper
import android.util.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lwjlol.liveeventbus.LiveEventBus
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 给多个 obsercer 发送消息
 */
@RunWith(AndroidJUnit4::class)
class LiveEventBusObserveMultiTest {
    private lateinit var handler: android.os.Handler

    companion object {
        private const val TAG = "LiveEventBusTest"
    }

    @get:Rule
    val rule: ActivityScenarioRule<MainActivity> = ActivityScenarioRule(MainActivity::class.java)

    @Before
    @Throws(Exception::class)
    fun setUp() {
        Looper.prepare()
        Looper.loop()
        handler = android.os.Handler(Looper.myLooper()!!)

        Log.d(TAG, "onActivity -- ${Thread.currentThread().name}")
//        rule.scenario.moveToState(Lifecycle.State.CREATED)
        Log.d(TAG, "set up -- ${rule.scenario.state}")
    }

    @Test
    fun testEvent() {
        // 启动 MainActivity
        rule.scenario.onActivity {
            LiveEventBus.instance.on(LiveEventBusObserveMultiTestEvent::class.java)
                .observe(it) {
                    Log.d(TAG, it.name)
                }
            launchSecond(it)
        }

    }

    fun launchSecond(it: Activity) {
//        onView(withId(R.id.textview)).perform(ViewActions.click())
    }

    fun launchThird(it: Activity) {

    }

    data class LiveEventBusObserveMultiTestEvent(val name: String)
}
