package pl.denpa.loramsg3;

import static com.google.android.material.internal.ContextUtils.getActivity;

import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class MsgStore implements SerialInputOutputManager.Listener {

    public static MsgStore oneandonly = null;

    private enum UsbPermission { Unknown, Requested, Granted, Denied }
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
    private static final int WRITE_WAIT_MILLIS = 2000;
    private static final int READ_WAIT_MILLIS = 2000;
    private int deviceId, portNum, baudRate = -1;
    private boolean withIoManager = true;
    private final BroadcastReceiver broadcastReceiver;
    private final Handler mainLooper;
    private SerialInputOutputManager usbIoManager;
    private UsbSerialPort usbSerialPort;
    private UsbPermission usbPermission = UsbPermission.Unknown;
    private boolean connected = false;
    private HashMap<String, ArrayList<String[]>> chats = new HashMap<>();
    public TerminalFragment openChat = null;

    public MsgStore getInstance() {
        if (oneandonly == null) {
            oneandonly = new MsgStore();
        }
        return oneandonly;
    }

    private MsgStore() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
                    usbPermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                            ? UsbPermission.Granted : UsbPermission.Denied;
                }
            }
        };
        mainLooper = new Handler(Looper.getMainLooper());
        System.out.println("majonez kielecki");
        oneandonly = this;
    }



    // internal serial port callback
    public void receive(byte[] data) {
//        System.out.println(data.toString());
        if (openChat != null) {
            openChat.receive(data);
        }


//        if (!chats.containsKey(user)) {
//            chats.put(user, new ArrayList<>());
//        }
//        chats.get(user).add(new String[]{user, message});

    }

    //called from TerminalFragment to broadcast msg
    public void send(String user, String message) {
        if (!chats.containsKey(user)) {
            chats.put(user, new ArrayList<>());
        }
    }

    public ArrayList<String[]> get_messages(String user) {
        if (chats.containsKey(user)) {
            return chats.get(user);
        }
        return null;
    }

    /*
     * Serial
     */
    @Override
    public void onNewData(byte[] data) {
//        System.out.println("onNewData " + HexDump.dumpHexString(data));
        mainLooper.post(() -> {
            receive(data);

        });
    }

    @Override
    public void onRunError(Exception e) {
        System.out.println("onRunError " + e.getMessage());
//        mainLooper.post(() -> {
//            status("connection lost: " + e.getMessage());
//            disconnect();
//        });
    }

    public void connect(Context context, TerminalFragment terminalFragment, int deviceId, int portNum, int baudRate) {
        this.deviceId = deviceId;
        this.portNum = portNum;
        this.baudRate = baudRate;

    }

    public void send(String str) {

        if(!connected) {
            System.out.println("not connected");
            return;
        }
        try {
            byte[] data = (str + '\n').getBytes();
            usbSerialPort.write(data, WRITE_WAIT_MILLIS);
            System.out.println(baudRate + " MsgStore.send " + str);
        } catch (Exception e) {
            onRunError(e);
        }
    }

    /*
     * Serial + UI
     */
    public void connect(Context context, TerminalFragment terminalFragment) throws Exception {
        if (deviceId == -1 || portNum == -1 || baudRate == -1) {
            throw new Exception("device not set");
        }
//        Toast.makeText(context, "" + deviceId + " " + portNum + " " + baudRate + " " + withIoManager, Toast.LENGTH_SHORT).show();
        UsbDevice device = null;
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        for(UsbDevice v : usbManager.getDeviceList().values())
            if(v.getDeviceId() == deviceId)
                device = v;
        if(device == null) {
            throw new Exception("connection failed: device not found");
        }
        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if(driver == null) {
            driver = CustomProber.getCustomProber().probeDevice(device);
        }
        if(driver == null) {
            throw new Exception("connection failed: no driver for device");
        }
        if(driver.getPorts().size() < portNum) {
            throw new Exception("connection failed: not enough ports at device");
        }
        usbSerialPort = driver.getPorts().get(portNum);
        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());
        if(usbConnection == null && usbPermission == MsgStore.UsbPermission.Unknown && !usbManager.hasPermission(driver.getDevice())) {
            usbPermission = MsgStore.UsbPermission.Requested;
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_MUTABLE : 0;
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(INTENT_ACTION_GRANT_USB), flags);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);

        }
        if(usbConnection == null) {
            if (!usbManager.hasPermission(driver.getDevice()))
                throw new Exception("connection failed: permission denied");
            else
                throw new Exception("connection failed: open failed");
        }

        try {
            usbSerialPort.open(usbConnection);
            try{
                usbSerialPort.setParameters(baudRate, 8, 1, UsbSerialPort.PARITY_NONE);
            }catch (UnsupportedOperationException e){
                System.out.println("not supported setparameters");
            }
            if(withIoManager) {
                usbIoManager = new SerialInputOutputManager(usbSerialPort, this);
                usbIoManager.start();
                System.out.println("usbiomanager startet");
            }
            connected = true;
            openChat = terminalFragment;
        } catch (Exception e) {
            disconnect();
            throw new Exception("connection failed: " + e.getMessage());
        }
    }

    private void disconnect() {
        connected = false;
        if(usbIoManager != null) {
            usbIoManager.setListener(null);
            usbIoManager.stop();
        }
        usbIoManager = null;
        try {
            usbSerialPort.close();
        } catch (IOException ignored) {}
        usbSerialPort = null;
    }


}
