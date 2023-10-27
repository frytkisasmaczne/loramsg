package pl.denpa.loramsg2

import android.app.PendingIntent
import android.hardware.usb.UsbManager
import android.app.Activity
import android.content.*
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.util.Log
import android.widget.Toast
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.lang.Exception

private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

class BoardConnector(context: Context) : SerialInputOutputManager.Listener {

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.apply {
                            openConnection()
                        }
                    } else {
                        Log.d(ContentValues.TAG, "permission denied for device $device")
                    }
    }}}}

    private val WRITE_WAIT_MILLIS = 2000
    private val READ_WAIT_MILLIS = 2000
    private lateinit var port: UsbSerialPort
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val permissionIntent = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), 0)
    private val filter = IntentFilter(ACTION_USB_PERMISSION)

    private lateinit var usbDevice: UsbDevice
    private var driver: UsbSerialDriver
    private var connection: UsbDeviceConnection

    init {
        context.registerReceiver(usbReceiver, filter)

        val deviceList = usbManager.deviceList
        for (mutableEntry in deviceList) {
            Toast.makeText(context, mutableEntry.key, Toast.LENGTH_SHORT).show()
        }

        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()) {
            Toast.makeText(context, "no driver found", Toast.LENGTH_SHORT).show()
            throw AssertionError("no driver found")
        }

        driver = availableDrivers[0]
        connection = usbManager.openDevice(driver.device)
        if (connection == null) {
            Toast.makeText(context, "no permission to open device", Toast.LENGTH_SHORT).show()
            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
            usbManager.requestPermission(driver.device, permissionIntent)
//            throw AssertionError("no permission to open device")
        }
        else {
            openConnection()
        }

//        // requesting permission
//        val filter = IntentFilter(ACTION_USB_PERMISSION)
//        context.registerReceiver(usbReceiver, filter)
////        usbManager.requestPermission(device, permissionIntent)

    }

    private fun openConnection() {
        port = driver.ports[0] // Most devices have just one port (port 0)

        port.open(connection)
        port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

        SerialInputOutputManager(port, this).start()
//        Toast.makeText(context, "opened port POGCHAMP", Toast.LENGTH_SHORT).show()
    }

    override fun onNewData(data: ByteArray?) {
        TODO("Not yet implemented")
    }

    override fun onRunError(e: Exception?) {
        TODO("Not yet implemented")
    }



}


