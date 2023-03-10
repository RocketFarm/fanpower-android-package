package com.fanpower.power

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.fan.power.R

import com.fanpower.lib.ui.views.FanPowerView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        var fanPowerView = findViewById<FanPowerView>(R.id.fanPowerView)

        fanPowerView.initView("your-tokenForJwtRequest",
            arrayOf(0, 0), // replace with your list of prop IDs.  Can be a list of a single ID.
            0, // your-publisherId
            "your-publisherToken",
            "your-shareUrl",
            this)
    }
}