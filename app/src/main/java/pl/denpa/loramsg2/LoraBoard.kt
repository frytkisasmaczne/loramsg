package pl.denpa.loramsg2

import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.lang.Exception

class LoraBoard : SerialInputOutputManager.Listener {

    constructor()

    override fun onNewData(data: ByteArray?) {
        TODO("Not yet implemented")
    }

    override fun onRunError(e: Exception?) {
        TODO("Not yet implemented")
    }
}