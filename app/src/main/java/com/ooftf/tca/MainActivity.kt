package com.ooftf.tca

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ooftf.iorderfix.R

open class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        test(2)
    }

    open fun test(isd: Int) {
        if (isd == 6) {
            return
        } else {
            Log.e("test", "555555")
        }
    }
}