package com.jjlf.sampleripplewrapper

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import com.jjlf.jjkit_ripplewrapper.JJRippleWrapper

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rp = findViewById<JJRippleWrapper>(R.id.ripple)

        rp.setOnClickListener {
            Toast.makeText(this,"CLICK RippleWrapper",Toast.LENGTH_SHORT).show()
        }
        findViewById<AppCompatTextView>(R.id.textView).setOnClickListener {
            Toast.makeText(this,"CLICK on child",Toast.LENGTH_SHORT).show()
        }

        rp.setOnRippleCompleteListener {
            Log.e("Ripple","Animation end")
        }
    }
}
