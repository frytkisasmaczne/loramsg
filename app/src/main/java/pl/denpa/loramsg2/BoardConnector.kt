package pl.denpa.loramsg2

import android.app.PendingIntent
import android.hardware.usb.UsbManager
import android.app.Activity
import android.content.*
import android.hardware.usb.UsbDevice
import android.util.Log
import android.widget.Toast
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.lang.Exception

class BoardConnector(context: Context, device: UsbDevice?) : SerialInputOutputManager.Listener {

    private val WRITE_WAIT_MILLIS = 2000
    private val READ_WAIT_MILLIS = 2000
    private lateinit var port: UsbSerialPort
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val permissionIntent = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), 0)

    private lateinit var usbDevice: UsbDevice

    init {

        if (device != null) {
            usbDevice = device
        }
        else {
            val deviceList = usbManager.deviceList
            for (mutableEntry in deviceList) {
                Toast.makeText(context, mutableEntry.key, Toast.LENGTH_SHORT).show()
            }


//            // requesting permission
//            val filter = IntentFilter(ACTION_USB_PERMISSION)
//            context.registerReceiver(usbReceiver, filter)
////        usbManager.requestPermission(device, permissionIntent)
        }
    }

    override fun onNewData(data: ByteArray?) {
        TODO("Not yet implemented")
    }

    override fun onRunError(e: Exception?) {
        TODO("Not yet implemented")
    }
}

private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

private val usbReceiver = object : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (ACTION_USB_PERMISSION == intent.action) {
            synchronized(this) {
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    device?.apply {
                        //call method to set up device communication
                    }
                } else {
                    Log.d(ContentValues.TAG, "permission denied for device $device")
                }
            }
        }
    }
}
