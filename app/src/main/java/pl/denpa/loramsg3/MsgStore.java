package pl.denpa.loramsg3;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MsgStore extends RecyclerView.Adapter<MsgStore.ViewHolder> implements SerialInputOutputManager.Listener {

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
    private Context context = null;
    AppDatabase db = null;
    public MsgFragment openChat = null;
    private final StringBuilder receiveBuffer = new StringBuilder();
    public String user = "e";

    public static MsgStore getInstance() {
        System.out.println("getInstance()");
        if (oneandonly == null) {
            oneandonly = new MsgStore();
        }
        return oneandonly;
    }

    private MsgStore() {
        System.out.println("MsgStore() constructor");
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
                    usbPermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                            ? UsbPermission.Granted : UsbPermission.Denied;
                    if (usbPermission == UsbPermission.Granted) {
                        try {
                            connect();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        };
        mainLooper = new Handler(Looper.getMainLooper());
    }

    // serial port onNewData callback
    public void receive(byte[] data) {
        String decoded = new String(data, StandardCharsets.UTF_8);
        receiveBuffer.append(decoded);
        if (!receiveBuffer.toString().endsWith("\r\n")) return;
        System.out.println("received " + receiveBuffer.toString());
        for (String command : receiveBuffer.toString().split("\r\n")) {
            receiveCommand(command);
        }
        receiveBuffer.setLength(0);
    }

    private void receiveCommand(String command) {
        System.out.println("receiveCommand(" + command + ")");
        if (command.toString().equals("+AT: OK")) {
            System.out.println("pong");
            send("AT+MODE=TEST");
        }
        else if (command.toString().equals("+MODE: TEST")) {
            send("AT+TEST=RFCFG,868,SF7,125,8,8,14,ON,OFF,OFF");
        }
        else if (command.toString().startsWith("+TEST: RFCFG ")) {
            send("AT+TEST=RXLRPKT");
        }
        else if (command.toString().equals("+TEST: RXLRPKT")) {
            System.out.println("connected = true");
            connected = true;
        }
        else if (command.toString().equals("+TEST: TX DONE")) {
            send("AT+TEST=RXLRPKT");
        }
        else {
            Matcher protoMsg = Pattern.compile("\\+TEST: RX \\\"([\\dA-F]*)\\\"").matcher(command);
            if (protoMsg.find()) {
                String decoded = hexStringToString(protoMsg.group(1));
                System.out.println("received lora packet " + decoded);
                Matcher msgMatcher = Pattern.compile("(\\w):(.*)").matcher(decoded);
                if (!msgMatcher.find()) return;
                String author = msgMatcher.group(1);
                String msg = msgMatcher.group(2);
                db.messageDao().insert(new Message(author, null, msg));
                if (openChat != null && openChat.chat == null) {
                    openChat.receive(msg);
                }
            }
            else {
                System.out.println("received unknown command " + command);
            }
        }
    }

    public List<Message> getMessages(String chat) {
//        if (chats.containsKey(user)) {
//            return chats.get(user);
//        }
//        return null;
        return chat == null ? db.messageDao().getBroadcastConversation() : db.messageDao().getPrivConversation(chat);
    }

    public List<String> getConversations() {
//        ArrayList<String[]> conversations = new ArrayList<>();
////        System.out.println(chats);
//        for (String recipient : chats.keySet()) {
//            System.out.println(recipient);
//            conversations.add(new String[]{recipient, chats.get(recipient).get(chats.size()-1)[0] + ": " + chats.get(recipient).get(chats.size()-1)[1]});
//        }
//        return conversations;
        return db.messageDao().getAllPrivUsers();
    }

    /*
     * Serial
     */
    @Override
    public void onNewData(byte[] data) {
//        System.out.println("onNewData " + HexDump.dumpHexString(data));
        mainLooper.post(() -> {
//            System.out.println("receive " + HexDump.dumpHexString(data));
            receive(data);

        });
    }

    @Override
    public void onRunError(Exception e) {
        System.out.println("onRunError " + e.getMessage());
        if (e.getMessage().equals("USB get_status request failed")) {
            System.out.println("plytka sie rozlaczyla but what now");
            connected = false;
        }

//        mainLooper.post(() -> {
//            status("connection lost: " + e.getMessage());
//            disconnect();
//        });
    }

    //called from TerminalFragment to transmit
    public void send(String recipient, String msg) {
        if (!connected) {
            System.out.println("not connected");
            Toast.makeText(context, "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        if (recipient == null) {
            String protomsg = user + ":" + msg;
            //todo 528chars max command len per docs page11
            StringBuilder cmd = new StringBuilder("AT+TEST=TXLRPKT,");
            for (byte b : protomsg.getBytes(StandardCharsets.UTF_8)) {
                cmd.append(String.format("%x", b));
            }
            send(cmd.toString());
            db.messageDao().insert(new Message(user, null, msg));
        }
        else {
            Toast.makeText(context, "idk how to send priv yet", Toast.LENGTH_SHORT).show();
            System.out.println("idk how to send priv yet");
        }
    }

    private void send(String command) {
//        if(!connected) {
//            System.out.println("not connected");
//            return;
//        }
        System.out.println("send(" + command + ")");
        try {
            byte[] data = (command + '\n').getBytes();
            usbSerialPort.write(data, WRITE_WAIT_MILLIS);
        } catch (Exception e) {
            onRunError(e);
        }
    }

    public void setContext(Context context) {
        this.context = context;
        db = Room.databaseBuilder(context, AppDatabase.class, "chats").allowMainThreadQueries().build();
        db.messageDao().insert(new Message("kielecki", null, "smierc winiarskim gnidom"));
    }

    public void setOpenChat(MsgFragment chat) {
        openChat = chat;
    }

    public void setDevice(int deviceId, int portNum, int baudRate) {
        this.deviceId = deviceId;
        this.portNum = portNum;
        this.baudRate = baudRate;
    }

    public void askForPermission() throws Exception {
        if (deviceId == -1 || portNum == -1 || baudRate == -1 || context == null) {
            throw new Exception("device not set");
        }
        context.registerReceiver(broadcastReceiver, new IntentFilter(INTENT_ACTION_GRANT_USB));

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
        if(usbConnection == null && !usbManager.hasPermission(driver.getDevice())) {
            usbPermission = MsgStore.UsbPermission.Requested;
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_MUTABLE : 0;
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(INTENT_ACTION_GRANT_USB), flags);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
        } else {
            connect();
        }
    }

    private void connect() throws Exception {
        System.out.println("connect()");
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
                System.out.println("usbiomanager starteth");
            }
            send("AT");
            System.out.println("ping");
            //response caught in onnewdata
        } catch (Exception e) {
            disconnect();
            throw new Exception("connection failed: " + e.getMessage());
        }
    }

    public void restoreConnection() {
        System.out.println("restoreConnection()");
        if (!connected) {
            try {
                askForPermission();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
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

    String hexStringToString(String hex) {
        StringBuilder result = new StringBuilder(hex.length()/2);
        for (int i = 0; i < hex.length(); i += 2) {
            char currentChar = 0;
            if (hex.getBytes()[i] >= '0' && hex.getBytes()[i] <= '9') {
                currentChar = (char) ((hex.getBytes()[i] - '0') * 16);
            } else if (hex.getBytes()[i] >= 'A' && hex.getBytes()[i] <= 'F') {
                currentChar = (char) ((hex.getBytes()[i] - 'A' + 10) * 16);
            }

            if (hex.getBytes()[i+1] >= '0' && hex.getBytes()[i+1] <= '9') {
                currentChar += (char) (hex.getBytes()[i+1] - '0');
            } else if (hex.getBytes()[i+1] >= 'A' && hex.getBytes()[i+1] <= 'F') {
                currentChar += (char) (hex.getBytes()[i+1] - 'A' + 10);
            }

            result.append(currentChar);
        }
        return result.toString();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View
            textView = (TextView) view.findViewById(R.id.textView);
        }

        public TextView getTextView() {
            return textView;
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MsgStore.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.msg_list_item, viewGroup, false);

        return new MsgStore.ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MsgStore.ViewHolder viewHolder, final int position) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        Message msg = db.messageDao().getPrivConversation(openChat.recipient).get(position);
        viewHolder.getTextView().setText(msg.author + ": " + msg.text);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return db.messageDao().getPrivConversation(openChat.recipient).size();
    }

}
