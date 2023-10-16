package pl.denpa.loramsg2

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import com.hoho.android.usbserial.driver.SerialTimeoutException
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager


class MainActivity : AppCompatActivity(), SerialInputOutputManager.Listener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
    }

    val boardConnector = BoardConnector(this, intent.getParcelableExtra(UsbManager.EXTRA_DEVICE))

    fun init() {

        permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter)
        manager.requestPermission(device, permissionIntent)


    }

    fun connect() {
        if (::port.isInitialized) {
            if (port.isOpen) {
                Toast.makeText(applicationContext, "port already opened", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
        if (availableDrivers.isEmpty()) {
            Toast.makeText(applicationContext, "no driver found", Toast.LENGTH_SHORT).show()
            return;
        }

        val driver = availableDrivers[0]
        val connection = manager.openDevice(driver.device)
        if (connection == null) {
            Toast.makeText(applicationContext, "can't open device", Toast.LENGTH_SHORT).show()
            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
            return
        }

        port = driver.ports[0] // Most devices have just one port (port 0)

        port.open(connection)
        port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

        SerialInputOutputManager(port, this).start()
//        Toast.makeText(applicationContext, "opened port POGCHAMP", Toast.LENGTH_SHORT).show()
    }

    fun sendbs(v: View) {
        if (::port.isInitialized) {
            if (port.isOpen) {
                try {
                    val edittext = findViewById<EditText>(R.id.edittext)
                    port.write((edittext.text.toString()+"\n").encodeToByteArray(), WRITE_WAIT_MILLIS)
                } catch (e: SerialTimeoutException) {
                    Toast.makeText(applicationContext, "SerialTimeoutException: $e", Toast.LENGTH_SHORT).show()
                }

                return
            }
        }
        Toast.makeText(applicationContext, "port not open", Toast.LENGTH_SHORT).show()
    }

    fun settings(v: View) {

//        if (::port.isInitialized) {
//            if (port.isOpen) {
//                try {
//                    var edittext = findViewById<EditText>(R.id.edittext)
//                    port.write(edittext.text.toString().encodeToByteArray(), WRITE_WAIT_MILLIS)
//                } catch (e: SerialTimeoutException) {
//                    Toast.makeText(applicationContext, "SerialTimeoutException: $e", Toast.LENGTH_SHORT).show()
//                }
//
//                return
//            }
//        }
//        Toast.makeText(applicationContext, "port not open", Toast.LENGTH_SHORT).show()
    }

    override fun onNewData(data: ByteArray?) {
        var text: String = data?.let { String(it) }.toString();
        this.runOnUiThread { Toast.makeText(this.applicationContext, "\"${text}\"", Toast.LENGTH_SHORT).show() }
        var listview = findViewById<LinearLayout>(R.id.listview)
        if (listview.children.lastOrNull() != null && text.last() != '\n') {
            var entry = listview.children.last() as TextView;
            entry.text = "${entry.text}${text}";
        } else {
            var entry = TextView(applicationContext)
            entry.text = data?.let { String(it) }
            this.runOnUiThread { listview.addView(entry) }
        }

    }

    override fun onRunError(e: Exception?) {
        this.runOnUiThread { Toast.makeText(this.applicationContext, "onRunError: $e", Toast.LENGTH_SHORT).show() }

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
                    Log.d(TAG, "permission denied for device $device")
                }
            }
        }
    }
}


