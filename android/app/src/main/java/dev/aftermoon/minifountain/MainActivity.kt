package dev.aftermoon.minifountain

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.android.synthetic.main.activity_main.*
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.util.Log
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setWaterSeek()
        setColorPickerView()
    }

    fun setColorPickerView() {
        colorPickerView.setColorListener(ColorEnvelopeListener { envelope, fromUser ->
            Log.d("MainActivity", "User Selected Color : " + envelope.hexCode)
        })
    }

    fun setWaterSeek() {
        waterSeek.setOnSeekBarChangeListener(object: OnSeekBarChangeListener {
            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                Log.d("MainActivity", "User Selected Power : $p1")
            }
        })
    }
}
