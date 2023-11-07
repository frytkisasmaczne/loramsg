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
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class MsgStore extends Application implements SerialInputOutputManager.Listener {

    private enum UsbPermission { Unknown, Requested, Granted, Denied }
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
    private static final int WRITE_WAIT_MILLIS = 2000;
    private static final int READ_WAIT_MILLIS = 2000;
    private int deviceId, portNum, baudRate;
    private boolean withIoManager;
    private final BroadcastReceiver broadcastReceiver;
    private final Handler mainLooper;
    private SerialInputOutputManager usbIoManager;
    private UsbSerialPort usbSerialPort;
    private UsbPermission usbPermission = UsbPermission.Unknown;
    private boolean connected = false;

    HashMap<String, ArrayList<String[]>> chats = new HashMap<>();

    public MsgStore() {
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
        Toast.makeText(getApplicationContext(), "majonez kielecki", Toast.LENGTH_SHORT).show();
    }



    //called from MainActivity onNewData sorter NAI
    public void receive(String user, String message) {
        if (!chats.containsKey(user)) {
            chats.put(user, new ArrayList<>());
        }
        chats.get(user).add(new String[]{user, message});

        //if main fragment is a chat then
        //pass forward to it to append so full reload isn't required
    }

    //called from TerminalFragment
    public void send(String user, String message) {
        if (!chats.containsKey(user)) {
            chats.put(user, new ArrayList<>());
        }
    }

    public ArrayList<String> get_messages(String user) {
        if (chats.containsKey(user)) {
            chats.get(user);
        }
        return null;
    }

    /*
     * Serial
     */
    @Override
    public void onNewData(byte[] data) {
//        mainLooper.post(() -> {
//            receive(data);
//
//        });
    }

    @Override
    public void onRunError(Exception e) {
//        mainLooper.post(() -> {
//            status("connection lost: " + e.getMessage());
//            disconnect();
//        });
    }

    /*
     * Serial + UI
     */
    private void connect(int deviceId, int portNum, int baudRate) throws Exception {
        Toast.makeText(getApplicationContext(), "" + deviceId + " " + portNum + " " + baudRate + " " + withIoManager, Toast.LENGTH_SHORT).show();
        UsbDevice device = null;
        UsbManager usbManager = (UsbManager) getApplicationContext().getSystemService(Context.USB_SERVICE);
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
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(INTENT_ACTION_GRANT_USB), flags);
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
                status("unsupport setparameters");
            }
            if(withIoManager) {
                usbIoManager = new SerialInputOutputManager(usbSerialPort, this);
                usbIoManager.start();
            }
            status("connected");
            connected = true;
            controlLines.start();
        } catch (Exception e) {
            status("connection failed: " + e.getMessage());
            disconnect();
        }
    }

    private void disconnect() {
        connected = false;
        controlLines.stop();
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
