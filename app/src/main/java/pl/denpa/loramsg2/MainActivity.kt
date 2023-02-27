package pl.denpa.loramsg2

import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hoho.android.usbserial.driver.SerialTimeoutException
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager


class MainActivity : AppCompatActivity(), SerialInputOutputManager.Listener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    private val WRITE_WAIT_MILLIS = 2000
    private val READ_WAIT_MILLIS = 2000
    private lateinit var port: UsbSerialPort

    fun connect(v: View) {
        if (::port.isInitialized) {
            if (port.isOpen) {
                Toast.makeText(applicationContext, "port already opened", Toast.LENGTH_SHORT).show()
                return
            }
        }
        val manager = getSystemService(Context.USB_SERVICE) as UsbManager
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
        if (availableDrivers.isEmpty()) {
            Toast.makeText(applicationContext, "no driver found", Toast.LENGTH_SHORT).show()
            return;
        }

        val driver = availableDrivers[0]
        val connection = manager.openDevice(driver.device)
        if (connection == null) {
            Toast.makeText(applicationContext, "cant open device - no permission", Toast.LENGTH_SHORT).show()
            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
            return
        }

        port = driver.ports[0] // Most devices have just one port (port 0)

        port.open(connection)
        port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

        SerialInputOutputManager(port, this).start()
        Toast.makeText(applicationContext, "opened port POGCHAMP", Toast.LENGTH_SHORT).show()
    }

    fun sendbs(v: View) {
        if (::port.isInitialized) {
            if (port.isOpen) {
                try {
                    var edittext = findViewById<EditText>(R.id.edittext)
                    port.write(edittext.text.toString().encodeToByteArray(), WRITE_WAIT_MILLIS)
                } catch (e: SerialTimeoutException) {
                    Toast.makeText(applicationContext, "SerialTimeoutException: $e", Toast.LENGTH_SHORT).show()
                }

                return
            }
        }
        Toast.makeText(applicationContext, "port not open", Toast.LENGTH_SHORT).show()
    }

    override fun onNewData(data: ByteArray?) {
        this.runOnUiThread { Toast.makeText(this.applicationContext, "\"${data?.let { String(it) }}\"", Toast.LENGTH_SHORT).show() }
        var listview = findViewById<LinearLayout>(R.id.listview)
        var entry = TextView(applicationContext)
        entry.text = data?.let { String(it) }
        this.runOnUiThread { listview.addView(entry) }
    }

    override fun onRunError(e: Exception?) {
        this.runOnUiThread { Toast.makeText(this.applicationContext, "onRunError: $e", Toast.LENGTH_SHORT).show() }

    }


}
