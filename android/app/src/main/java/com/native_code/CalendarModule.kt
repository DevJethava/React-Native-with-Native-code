package com.native_code

import android.view.Gravity
import android.widget.Toast
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class CalendarModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "CalendarModule";
    private var eventCount = 0
    @ReactMethod
    fun createCalendarEvent(name: String, location: String, promise: Promise) {
        val toast = Toast.makeText(reactContext, "name", Toast.LENGTH_LONG)
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()

        try {
            val eventId = 12345
            promise.resolve(eventId)
        } catch (e: Throwable) {
            promise.reject("Create Event Error", e)
        }
    }
//    @ReactMethod(isBlockingSynchronousMethod = true)

}