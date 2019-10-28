/*
    MIT License

    Copyright (C) 2019 Aftermoon

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
*/

package dev.aftermoon.minifountain

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.android.synthetic.main.activity_main.*
import android.util.Log
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import app.akexorcist.bluetotohspp.library.BluetoothSPP
import app.akexorcist.bluetotohspp.library.BluetoothState

class MainActivity : AppCompatActivity() {

    private lateinit var bluetooth : BluetoothSPP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Connect Bluetooth
        connectBluetooth()
    }

    override fun onBackPressed() {
        bluetooth.stopService()
        super.onBackPressed()
    }

    private fun setColorPickerView() {
        colorPickerView.setColorListener(ColorEnvelopeListener { envelope, fromUser ->
            Log.d("MainActivity", "User Selected Color : " + envelope.hexCode)
            // send Hex Color Code for LED
            bluetooth.send("color;" + envelope.hexCode, true)
        })
    }

    private fun setWaterSeek() {
        waterSeek.setOnSeekBarChangeListener(object: OnSeekBarChangeListener {
            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                Log.d("MainActivity", "User Selected Power : $p1")
                // send power (0 ~ 100)
                bluetooth.send("power;$p1", true)
            }
        })
    }

    private fun connectBluetooth() {
        // Bluetooth Loading
        bluetooth = BluetoothSPP(this)
        // Connected To mini-fountain-aftermoon
        bluetooth.autoConnect("mini-fountain-aftermoon")
        // Start Bluetooth Service
        bluetooth.startService(BluetoothState.DEVICE_OTHER)

        bluetooth.setBluetoothStateListener {
            when (it) {
                BluetoothState.STATE_CONNECTED -> {
                    Log.d("Bluetooth", "Bluetooth Connected to Arduino!")

                    // Setting Water Power Seekbar
                    setWaterSeek()

                    // Setting LED ColorPickerView
                    setColorPickerView()
                }
                BluetoothState.STATE_CONNECTING -> Log.d("Bluetooth", "Bluetooth Connecting.....")
                else -> Log.d("Bluetooth", "Bluetooth Error. Please check your Bluetooth Module!")
            }
        }
    }
}
