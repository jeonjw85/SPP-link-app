package kr.jjw.spp_link_app

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import com.google.android.material.textfield.TextInputEditText
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var tvStatus: TextView
    private lateinit var tvReceived: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var etSend: TextInputEditText
    private lateinit var btnSend: Button
    private lateinit var btnConnect: Button
    private lateinit var btnDisconnect: Button

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    @Volatile
    private var isConnected = false
    private var readThread: Thread? = null

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val TARGET_MAC_ADDRESS = BuildConfig.TARGET_MAC_ADDRESS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvReceived = findViewById(R.id.tvReceived)
        scrollView = findViewById(R.id.scrollView)
        etSend = findViewById(R.id.etSend)
        btnSend = findViewById(R.id.btnSend)
        btnConnect = findViewById(R.id.btnConnect)
        btnDisconnect = findViewById(R.id.btnDisconnect)

        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter == null) {
            tvStatus.text = "이 기기는 블루투스를 지원하지 않습니다."
            btnConnect.isEnabled = false
            return
        }

        btnConnect.setOnClickListener {
            if (checkBluetoothPermissions()) {
                startBluetoothConnection()
            } else {
                requestBluetoothPermissions()
            }
        }

        btnDisconnect.setOnClickListener {
            disconnect()
        }

        btnSend.setOnClickListener {
            sendData(etSend.text?.toString() ?: "")
        }
    }

    private fun startBluetoothConnection() {
        if (TARGET_MAC_ADDRESS.isBlank()) {
            tvStatus.text = "MAC 주소가 설정되지 않았습니다."
            Toast.makeText(
                this,
                "local.properties 에 TARGET_MAC_ADDRESS 를 설정하세요",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        tvStatus.text = "SPP 연결 시도 중.."
        val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(TARGET_MAC_ADDRESS)

        if (device == null) {
            tvStatus.text = "지정된 장치를 찾을 수 없습니다."
            return
        }
        thread(start = true) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {

                    bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                    bluetoothSocket?.connect()

                    inputStream = bluetoothSocket?.inputStream
                    outputStream = bluetoothSocket?.outputStream
                    isConnected = true

                    runOnUiThread {
                        tvStatus.text = "SPP 연결 성공"
                        Toast.makeText(this, "연결되었습니다.", Toast.LENGTH_SHORT).show()
                        setConnectedUi(true)
                    }

                    startReadLoop()
                }
            } catch (e: IOException) {
                isConnected = false
                try {
                    bluetoothSocket?.close()
                } catch (closeException: IOException) {
                    closeException.printStackTrace()
                }
                runOnUiThread {
                    tvStatus.text = "연결 실패"
                    Toast.makeText(this, "연결에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    setConnectedUi(false)
                }
            }
        }
    }

    private fun startReadLoop() {
        readThread = thread(start = true) {
            val buffer = ByteArray(1024)
            val lineBytes = ByteArrayOutputStream()

            while (isConnected) {
                try {
                    val stream = inputStream ?: break
                    val bytes = stream.read(buffer)
                    if (bytes > 0) {
                        for (i in 0 until bytes) {
                            val b = buffer[i]
                            when (b.toInt()) {
                                '\n'.code -> {
                                    val line = lineBytes.toString(StandardCharsets.UTF_8.name())
                                    lineBytes.reset()
                                    appendReceived(line.trimEnd('\r'))
                                }
                                '\r'.code -> {
                                }
                                else -> lineBytes.write(b.toInt())
                            }
                        }
                    }
                } catch (e: IOException) {
                    if (isConnected) {
                        isConnected = false
                        runOnUiThread {
                            tvStatus.text = "연결이 끊어졌습니다."
                            setConnectedUi(false)
                        }
                    }
                    break
                }
            }
        }
    }

    private fun appendReceived(text: String) {
        runOnUiThread {
            tvReceived.append("⬇ $text\n")
            scrollToBottom()
        }
    }

    private fun sendData(message: String) {
        if (!isConnected || outputStream == null) {
            Toast.makeText(this, "먼저 연결하세요", Toast.LENGTH_SHORT).show()
            return
        }
        if (message.isBlank()) return

        thread(start = true) {
            try {
                outputStream?.write((message + "\n").toByteArray(StandardCharsets.UTF_8))
                outputStream?.flush()
                runOnUiThread {
                    tvReceived.append("⬆ $message\n")
                    etSend.text?.clear()
                    scrollToBottom()
                }
            } catch (e: IOException) {
                runOnUiThread {
                    Toast.makeText(this, "전송 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun disconnect() {
        isConnected = false
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            inputStream = null
            outputStream = null
            bluetoothSocket = null
            tvStatus.text = "연결 해제됨"
            setConnectedUi(false)
        }
    }

    private fun setConnectedUi(connected: Boolean) {
        btnConnect.isEnabled = !connected
        btnDisconnect.isEnabled = connected
        btnSend.isEnabled = connected
    }

    private fun scrollToBottom() {
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun checkBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), 101)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
    }
}