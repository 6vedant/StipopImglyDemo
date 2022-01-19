package com.vedant.stipopimglydemo
import android.app.Application
import io.stipop.Stipop

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Stipop.configure(this)
    }
}