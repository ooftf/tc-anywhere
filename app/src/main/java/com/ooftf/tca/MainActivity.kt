package com.ooftf.tca

import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

open class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        test(5)
        5 / 0
    }

    open fun test(isd: Int) {
        if (isd.equals(6)) {
        } else {
            5 / 0
            Log.e("test", "555555")
        }
    }
}