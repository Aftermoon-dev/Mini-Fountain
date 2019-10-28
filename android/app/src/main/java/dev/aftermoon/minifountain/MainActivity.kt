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

import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.android.synthetic.main.activity_main.*
import android.util.Log
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import app.akexorcist.bluetotohspp.library.BluetoothSPP
import app.akexorcist.bluetotohspp.library.BluetoothSPP.BluetoothConnectionListener
import app.akexorcist.bluetotohspp.library.BluetoothState
import com.skydoves.colorpickerview.ActionMode

class MainActivity : AppCompatActivity() {

    private lateinit var bluetooth : BluetoothSPP
    private val REQUEST_ENABLE_BT: Int = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Connect Bluetooth
        connectBluetooth()
    }

    override fun onStart() {
        super.onStart()
        // If Bluetooth not Enabled
        if(!bluetooth.isBluetoothEnabled) run {
            // Request Bluetooth On
            val btIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(btIntent, REQUEST_ENABLE_BT)
        }
        else {
            if(!bluetooth.isServiceAvailable) {
                bluetooth.setupService()
                bluetooth.startService(BluetoothState.DEVICE_OTHER)
            }
        }
    }

    override fun onBackPressed() {
        bluetooth.stopService()
        super.onBackPressed()
    }

    private fun setColorPickerView() {
        colorPickerView.actionMode = ActionMode.LAST
        colorPickerView.setColorListener(ColorEnvelopeListener { envelope, fromUser ->
            Log.d("MainActivity", "User Selected Color : " + envelope.hexCode)
            // send Hex Color Code for LED
            bluetooth.send("color;" + envelope.hexCode, true)
        })
    }

    private fun setWaterSeek() {
        waterSeek.setOnSeekBarChangeListener(object: OnSeekBarChangeListener {
            var pw: Int = 0
            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                // send power (0 ~ 100)
                bluetooth.send("power;$pw", true)
            }

            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                Log.d("MainActivity", "User Selected Power : $p1")
                pw = p1
            }
        })
    }

    private fun connectBluetooth() {
        // Bluetooth Loading
        bluetooth = BluetoothSPP(this)

        if(bluetooth.isBluetoothAvailable) {
            selectBTDevice()

            // Bluetooth Connection Listener
            bluetooth.setBluetoothConnectionListener(object : BluetoothConnectionListener {
                override fun onDeviceConnected(name: String, address: String) {
                    Toast.makeText(applicationContext, "$name 과 연결되었습니다.", Toast.LENGTH_SHORT).show()
                    Log.d("Bluetooth", "Device Connected! Device Name : $name")
                    setColorPickerView()
                    setWaterSeek()
                }

                override fun onDeviceDisconnected() {
                    Toast.makeText(applicationContext, "블루투스 연결이 해제되었습니다.", Toast.LENGTH_SHORT).show()
                    Log.d("Bluetooth", "Device Disconnected.")
                    selectBTDevice()
                }

                override fun onDeviceConnectionFailed() {
                    Toast.makeText(applicationContext, "블루투스 연결에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    Log.d("Bluetooth", "Device Connection Failed!")
                    selectBTDevice()
                }
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                if(resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "블루투스가 활성화 되었습니다!", Toast.LENGTH_SHORT).show()
                }
                else {
                    Toast.makeText(this, "블루투스 활성화에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun selectBTDevice() {
        val deviceList: Set<BluetoothDevice> = bluetooth.bluetoothAdapter.bondedDevices
        val deviceCount: Int = deviceList.size

        if(deviceCount == 0) {
            Toast.makeText(this, "페어링이 되어있는 디바이스가 없습니다. 먼저 블루투스 설정에서 페어링을 진행해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val btDeviceDialog: AlertDialog.Builder = AlertDialog.Builder(this)
        btDeviceDialog.setTitle("블루투스 장치 선택")

        val listItem: ArrayList<String> = ArrayList()
        for (device in deviceList) {
            listItem.add(device.name)
        }

        val items = listItem.toArray(arrayOfNulls<CharSequence>(listItem.size))
        btDeviceDialog.setItems(items) { dialogInterface, i ->
            Log.d("selectBTDevice", items[i].toString())
            for (device in deviceList) {
                if(device.name == items[i].toString()) {
                    Log.d("selectBTDevice", device.address)
                    bluetooth.connect(device.address)
                }
            }
        }

        btDeviceDialog.setCancelable(false)
        val alert = btDeviceDialog.create()
        alert.show()
    }
}
