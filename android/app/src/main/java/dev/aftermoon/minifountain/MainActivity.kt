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
import android.os.Handler
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.android.synthetic.main.activity_main.*
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import app.akexorcist.bluetotohspp.library.BluetoothSPP
import app.akexorcist.bluetotohspp.library.BluetoothSPP.BluetoothConnectionListener
import app.akexorcist.bluetotohspp.library.BluetoothState
import com.skydoves.colorpickerview.ActionMode
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private lateinit var bluetooth : BluetoothSPP
    private var isConnected: Boolean = false
    private var isPlaying: Boolean = false
    private var lastsendTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bluetooth = BluetoothSPP(applicationContext)

        lastsendTime = System.currentTimeMillis()

        if(!bluetooth.isBluetoothAvailable) {
            Toast.makeText(this, "블루투스가 지원되지 않는 기기에서는 사용할 수 없습니다. 앱을 종료합니다.", Toast.LENGTH_LONG).show()
            finish()
        }

        // If Bluetooth not Enabled
        if(!bluetooth.isBluetoothEnabled) run {
            // Request Bluetooth On
            val btIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(btIntent, BluetoothState.REQUEST_ENABLE_BT)
        }
        else {
            bluetooth.setupService()
            bluetooth.startService(BluetoothState.DEVICE_OTHER)
            connectBluetooth()
        }
    }

    override fun onDestroy() {
        isConnected = false
        sendBluetooth("bluetooth;disconnected", true)
        bluetooth.stopService()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.mainactivity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.connectBT -> {
                if(!isConnected) {
                    selectBTDevice()
                }
                else {
                    val alreadyConnectDialog: AlertDialog.Builder = AlertDialog.Builder(this)
                    alreadyConnectDialog.setTitle("경고")
                        .setMessage("이미 장치에 연결되어 있습니다.\n연결을 해제하고 다른 기기와 연결하시겠습니까?")
                        .setPositiveButton("확인") { _, _ ->
                            if(isConnected) {
                                isConnected = false
                                isPlaying = false
                                bluetooth.disconnect()
                            }
                            selectBTDevice()
                        }
                        .setNegativeButton("취소") { dialogInterface, _ ->
                            dialogInterface.cancel()
                        }
                    alreadyConnectDialog.show()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setColorPickerView() {
        colorPickerView.actionMode = ActionMode.LAST
        colorPickerView.setColorListener(ColorEnvelopeListener { envelope, _ ->
            Log.d("MainActivity", "User Selected Color : " + envelope.hexCode)
            // send Hex Color Code for LED
            sendBluetooth("color;" + envelope.hexCode, true)
        })
    }

    private fun setWaterSeek() {
        waterSeek.setOnSeekBarChangeListener(object: OnSeekBarChangeListener {
            var pw: Int = 0
            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                sendBluetooth("pw;$pw", true)
            }

            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                Log.d("MainActivity", "User Selected Power : $p1")
                pw = p1
            }
        })
    }

    private fun connectBluetooth() {
        if(bluetooth.isBluetoothAvailable) {
            // Bluetooth Connection Listener
            bluetooth.setBluetoothConnectionListener(object : BluetoothConnectionListener {
                override fun onDeviceConnected(name: String, address: String) {
                    isPlaying = false
                    isConnected = true
                    sendBluetooth("playaudio;stop", true)
                    setBTReceiving()
                    setColorPickerView()
                    setWaterSeek()
                    setPlayMusic()
                    setLEDBtn()
                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    Toast.makeText(applicationContext, "$name 에 연결되었습니다.", Toast.LENGTH_SHORT).show()
                    sendBluetooth("bluetooth;connected", true)
                    Log.d("Bluetooth", "Device Connected! Device Name : $name")
                }

                override fun onDeviceDisconnected() {
                    isPlaying = false
                    isConnected = false
                    Toast.makeText(applicationContext, "블루투스 연결이 해제되었습니다.", Toast.LENGTH_SHORT).show()
                    Log.d("Bluetooth", "Device Disconnected.")
                }

                override fun onDeviceConnectionFailed() {
                    isPlaying = false
                    isConnected = false
                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    Toast.makeText(applicationContext, "블루투스 연결에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    Log.d("Bluetooth", "Device Connection Failed!")
                }
            })
        }
    }

    private fun setBTReceiving() {
        bluetooth.setOnDataReceivedListener { _, message ->
            Log.d("BTReceive", "Message Received : $message")
            if (message == "bluetooth;connect?") {
                bluetooth.send("bluetooth;connect!", true)
            }
            else if(message == "playaudio;failedbegin") {
                Toast.makeText(applicationContext, "MP3 플레이어 모듈과의 통신이 실패하였습니다. 음악 기능을 사용하실 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
            else if(message == "audio;stop") {
                Toast.makeText(applicationContext, "노래가 정지되었습니다.", Toast.LENGTH_SHORT).show()
            }
            else if(message == "audio;start") {
                Toast.makeText(applicationContext, "노래가 시작되었습니다.", Toast.LENGTH_SHORT).show()
            }
            else if(message == "led;rainbow_start") {
                Toast.makeText(applicationContext, "무지개색이 적용되었습니다.", Toast.LENGTH_SHORT).show()
                colorPickerView.selectCenter()
            }
            else if(message == "led;stop") {
                Toast.makeText(applicationContext, "LED가 꺼졌습니다.", Toast.LENGTH_SHORT).show()
                colorPickerView.selectCenter()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            BluetoothState.REQUEST_ENABLE_BT -> {
                if(resultCode == Activity.RESULT_OK) {
                    bluetooth.setupService()
                    bluetooth.startService(BluetoothState.DEVICE_OTHER)
                    connectBluetooth()
                }
                else {
                    Toast.makeText(this, "블루투스 활성화에 실패했습니다. 앱을 사용하시려면 블루투스 활성화가 필요합니다.", Toast.LENGTH_SHORT).show()
                    finish()
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
        btDeviceDialog.setItems(items) { _, i ->
            Log.d("selectBTDevice", items[i].toString())
            for (device in deviceList) {
                if(device.name == items[i].toString()) {
                    try {
                        colorPickerView.selectCenter()
                        waterSeek.progress = 0
                        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                        Toast.makeText(this, "연결중입니다...", Toast.LENGTH_SHORT).show()
                        Log.d("selectBTDevice", device.address)
                        bluetooth.connect(device.address)
                    }
                    catch(e: Exception) {
                        Toast.makeText(this, "에러가 발생했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                        selectBTDevice()
                    }
                }
            }
        }

        val alert = btDeviceDialog.create()
        alert.show()
    }

    private fun setPlayMusic() {
        if(isConnected) {
            val musicNum = playMusicNum.text
            playMusicBtn.setOnClickListener {
                sendBluetooth("playaudio;play$musicNum", true)
            }

            stopMusicBtn.setOnClickListener {
                bluetooth.send("audstop", true)
            }
        }
    }

    private fun setLEDBtn() {
        if(isConnected) {
            ledOffBtn.setOnClickListener {
                sendBluetooth("led;stop", true)
            }

            ledRainbowBtn.setOnClickListener {
                sendBluetooth("rainbow;start", true)
            }
        }
    }

    private fun sendBluetooth(data: String, crlf: Boolean) {
        val nowTime = System.currentTimeMillis()

        if((nowTime - lastsendTime) <= 3000 ) {
            Log.d("send", "Need Delay! - $data")
            val delayHandle = Handler()
            delayHandle.postDelayed({
                bluetooth.send(data, crlf)
                Log.d("send", "Delay Send : $data")
            }, 2000)
        }
        else {
            Log.d("send", "Send : $data")
            bluetooth.send(data, crlf)
        }

        lastsendTime = nowTime
    }
}
