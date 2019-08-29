package com.example.animview

import android.graphics.PixelFormat
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        anim.setAnimResource(R.array.refresh)
//        anim.setAnimAssets("refresh")
//        anim.start()

        start.setOnClickListener {
            anim.start()
        }

        stop.setOnClickListener {
            anim.stop()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                anim.setProgress(progress / 100f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })


        show.setOnClickListener {
            anim.setAnimResource(R.array.refresh)
//            anim.setZOrderMediaOverlay(true)
//            anim.holder.setFormat(PixelFormat.TRANSLUCENT)
        }

        gone.setOnClickListener {
            anim.setAnimAssets("refresh")
        }

    }
}
