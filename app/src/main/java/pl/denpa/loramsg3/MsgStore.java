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
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.room.Room;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;

import kotlin.random.Random;

public class MsgStore implements SerialInputOutputManager.Listener {

    public static MsgStore oneandonly = null;

    private enum UsbPermission { Unknown, Requested, Granted, Denied }
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
    private static final int WRITE_WAIT_MILLIS = 2000;
    private static final int READ_WAIT_MILLIS = 2000;
    public int deviceId;
    public int portNum = 0;
    public int baudrate = -1;
    private final BroadcastReceiver broadcastReceiver;
    private final Handler mainLooper;
    private SerialInputOutputManager usbIoManager;
    private UsbSerialPort usbSerialPort;
    private UsbPermission usbPermission = UsbPermission.Unknown;
    private boolean connected = false;
    private Context context = null;
    public AppDatabase db = null;
    public SecondFragment openChat = null;
    public FirstFragment chatsFragment = null;
    private final StringBuilder receiveBuffer = new StringBuilder();
    private int lastRssi;
    private int lastSnr;
    public String nick = "kielecki";
    public HashMap<String, Cipher[]> ciphers = new HashMap<>();
    //ciphers[i][0] = encryptCipher; ciphers[i][1] = decryptCipher;
    private SecretKeyFactory secretKeyFactory;

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
//                            connect();
                            askForPermission();
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
        System.out.println("received " + receiveBuffer);
        for (String command : receiveBuffer.toString().split("\r\n")) {
            receiveCommand(command);
        }
        receiveBuffer.setLength(0);
    }

    private void receiveCommand(String command) {
        System.out.println("receiveCommand(" + command + ")");
        if (command.equals("+AT: OK")) {
            System.out.println("pong");
            send("AT+MODE=TEST");
        }
        else if (command.equals("+MODE: TEST")) {
            send("AT+TEST=RFCFG,868,SF12,500,8,8,14,ON,OFF,OFF");
        }
        else if (command.startsWith("+TEST: RFCFG ")) {
            send("AT+TEST=RXLRPKT");
        }
        else if (command.equals("+TEST: RXLRPKT")) {
            System.out.println("connected = true");
            connected = true;
        }
        else if (command.equals("+TEST: TX DONE")) {
            send("AT+TEST=RXLRPKT");
        }
        else if (command.startsWith("+TEST: LEN:")) {
            Matcher rssiRaport = Pattern.compile("\\+TEST: LEN:\\d+, RSSI:(-?\\d+), SNR:(-?\\d+)").matcher(command);
            if (rssiRaport.find()) {
                lastRssi = Integer.parseInt(rssiRaport.group(1));
                lastSnr = Integer.parseInt(rssiRaport.group(2));
            }
        }
        else {
            Matcher protoMsg = Pattern.compile("\\+TEST: RX \\\"([\\dA-F]*)\\\"").matcher(command);
            if (protoMsg.find()) {
                byte[] pktbytes = hexStringTobyteArray(protoMsg.group(1));
                byte header = pktbytes[0];
                byte[] pktbytesnoheader = Arrays.copyOfRange(pktbytes, 1, pktbytes.length);
                if (header == (byte) 0xFE) {
                    int colonIndex = 0;
                    for (int i = 0; i < pktbytesnoheader.length; i++) {
                        if (pktbytesnoheader[i] == ':') {
                            colonIndex = i;
                            break;
                        }
                    }
                    if (colonIndex == 0) return;
                    String author = new String(Arrays.copyOfRange(pktbytesnoheader, 0, colonIndex), StandardCharsets.UTF_8);
                    byte[] encrypted = Arrays.copyOfRange(pktbytesnoheader, colonIndex+1, pktbytesnoheader.length);
                    String clear;
                    try {
                        clear = decrypt(author, encrypted);
                    } catch (Exception e) {
                        System.out.println("what from decrypt in receiveCommand: " + e);
                        return;
                    }
                    receivePrivMsg(author, clear);

                } else if (header == (byte) 0xFF) {
                    String decoded = new String(pktbytesnoheader, StandardCharsets.UTF_8);
                    System.out.println("received lora packet " + decoded);
                    Matcher msgMatcher = Pattern.compile("(\\w*):(.*)").matcher(decoded);
                    if (!msgMatcher.find()) return;
                    String author = msgMatcher.group(1);
                    String msg = msgMatcher.group(2);
                    if ("".equals(author)) {
                        author = null;
                    }
                    receiveBroadcastMsg(author, msg);
//                if (openChat != null && openChat.chat == null) {
//                    openChat.receive(msg);
//                }
                }
            }
            else {
                System.out.println("received unknown command " + command);
            }
        }
    }

    void receiveBroadcastMsg(String author, String text) {
        Message msg = new Message(author, null, text, lastRssi, lastSnr);
        db.messageDao().insert(msg);
        if (openChat.chat == null) {
            openChat.msgAdapter.msgs.add(msg);
            openChat.msgAdapter.notifyItemInserted(openChat.msgAdapter.getItemCount() - 1);
            int lastCompletelyVisibleItem = ((LinearLayoutManager)openChat.recyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition();
            if (lastCompletelyVisibleItem == openChat.msgAdapter.getItemCount() - 2) {
                System.out.println("lastCompletelyVisibleItem " + lastCompletelyVisibleItem + " " + openChat.msgAdapter.getItemCount());
                openChat.recyclerView.scrollToPosition(openChat.msgAdapter.getItemCount() - 1);
            }
        }
    }

    void receivePrivMsg(String author, String text) {
        System.out.println("receivePrivMsg(" + author + ", " + text +")");
        Message msg = new Message(author, author, text, lastRssi, lastSnr);
        db.messageDao().insert(msg);
        if (author.equals(openChat.chat)) {
            openChat.msgAdapter.msgs.add(msg);
            openChat.msgAdapter.notifyItemInserted(openChat.msgAdapter.getItemCount() - 1);
            int lastCompletelyVisibleItem = ((LinearLayoutManager)openChat.recyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition();
            if (lastCompletelyVisibleItem == openChat.msgAdapter.getItemCount() - 2) {
                System.out.println("lastCompletelyVisibleItem " + lastCompletelyVisibleItem + " " + openChat.msgAdapter.getItemCount());
                openChat.recyclerView.scrollToPosition(openChat.msgAdapter.getItemCount() - 1);
            }
        }
    }

    public List<Message> getMessages(String chat) {
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
        return db.chatDao().getAllChats().stream().map((chat) -> chat.chat).collect(Collectors.toList());
    }

    public void addChat(String chat, byte[] key) {
        db.chatDao().upsert(new Chat(chat, key));
    }

    public void createChat(String chat) {
        byte[] key = new byte[16];
        Random.Default.nextBytes(key);
        addChat(chat, key);
        Toast.makeText(context, chat, Toast.LENGTH_SHORT).show();
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
            System.out.println("plytka sie rozlaczyla");
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

        Message db_msg = null;
        byte[] msg_bytes = null;

        if (recipient == null) {
            String protomsg = nick + ":" + msg;
            msg_bytes = protomsg.getBytes(StandardCharsets.UTF_8);
            msg_bytes = Arrays.copyOf(msg_bytes, msg_bytes.length + 1);
            System.arraycopy(msg_bytes, 0, msg_bytes, 1, msg_bytes.length-1);
            msg_bytes[0] = (byte) 0xFF;
        }
        else {
            System.out.println("sure is priv for " + recipient);
            msg_bytes = (nick + ":").getBytes(StandardCharsets.UTF_8);
            byte[] encrypted = null;
            byte[] clear = msg.getBytes(StandardCharsets.UTF_8);
            String decrypted_back;
            try {
                encrypted = encrypt(recipient, clear);
                System.out.println("encrypted = " + Arrays.toString(encrypted));
                decrypted_back = decrypt(recipient, encrypted);
                System.out.println("decrypted_back = " + decrypted_back);
            } catch (Exception e) {
                System.out.println("what in send from encrypt: " + e);
                return;
            }


            int nickcolonlen = msg_bytes.length;
            msg_bytes = Arrays.copyOf(msg_bytes, msg_bytes.length + encrypted.length + 1);
            System.arraycopy(msg_bytes, 0, msg_bytes, 1, nickcolonlen);
            msg_bytes[0] = (byte) 0xFE;
            System.arraycopy(encrypted, 0, msg_bytes, nickcolonlen+1, encrypted.length);
        }

        //todo 528chars max command len per docs page11
        StringBuilder cmd = new StringBuilder("AT+TEST=TXLRPKT,");
        for (byte b : msg_bytes) {
            cmd.append(String.format("%02x", b));
        }

        send(cmd.toString());

        db_msg = new Message(nick, recipient, msg, 999, 999);
        db.messageDao().insert(db_msg);
        System.out.println("asdf" + openChat.chat + " " + nick);
        if ( (openChat.chat == null && recipient == null) || ((openChat.chat != null && recipient != null) && openChat.chat.equals(recipient)) ) {
            openChat.msgAdapter.msgs.add(db_msg);
            openChat.msgAdapter.notifyItemInserted(openChat.msgAdapter.getItemCount() - 1);
            int lastCompletelyVisibleItem = ((LinearLayoutManager)openChat.recyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition();
            if (lastCompletelyVisibleItem == openChat.msgAdapter.getItemCount() - 2) {
                System.out.println("lastCompletelyVisibleItem " + lastCompletelyVisibleItem + " " + openChat.msgAdapter.getItemCount());
                openChat.recyclerView.scrollToPosition(openChat.msgAdapter.getItemCount() - 1);
            }
        }
        else if (openChat.chat.equals(recipient)) {
            openChat.msgAdapter.msgs.add(db_msg);
            openChat.msgAdapter.notifyItemInserted(openChat.msgAdapter.getItemCount() - 1);
            int lastCompletelyVisibleItem = ((LinearLayoutManager)openChat.recyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition();
            if (lastCompletelyVisibleItem == openChat.msgAdapter.getItemCount() - 2) {
                System.out.println("lastCompletelyVisibleItem " + lastCompletelyVisibleItem + " " + openChat.msgAdapter.getItemCount());
                openChat.recyclerView.scrollToPosition(openChat.msgAdapter.getItemCount() - 1);
            }
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
        System.out.println("MsgStore.setContext()");
        this.context = context;
        db = Room.databaseBuilder(context, AppDatabase.class, "chats").allowMainThreadQueries().build();
//        db.messageDao().insert(new Message("kielecki", "e", "death to all the other letters"));
//        if (db.chatDao().getChat("e") == null) {
//            db.chatDao().upsert(new Chat("e", "bajojajobajojajo".getBytes(StandardCharsets.UTF_8)));
//        }
        nick = getNick();
        baudrate = getBaudrate();
    }

    public void setOpenChat(SecondFragment chat) {
        openChat = chat;
    }

    public void setNick(String nick) {
        this.nick = nick;
        db.bloatDao().upsert(new Bloat("nick", nick));
    }

    public String getNick() {
        if (db.bloatDao().getValue("nick") == null) {
            db.bloatDao().upsert(new Bloat("nick", "e"));
        }
        return (String) db.bloatDao().getValue("nick");
    }

    public void setBaudrate(int baudrate) {
        this.baudrate = baudrate;
        db.bloatDao().upsert(new Bloat("baudrate", String.valueOf(baudrate)));
    }

    public int getBaudrate() {
        if (db.bloatDao().getValue("baudrate") == null) {
            db.bloatDao().upsert(new Bloat("baudrate", "9600"));
        }
        return Integer.parseInt(db.bloatDao().getValue("baudrate"));
    }

    public int[] getDevice() throws Exception {
        if (db.bloatDao().getValue("deviceId") == null || db.bloatDao().getValue("portNum") == null) {
            throw new Exception("device not set");
        }
        return new int[]{Integer.parseInt(db.bloatDao().getValue("deviceId")), Integer.parseInt(db.bloatDao().getValue("portNum"))};
    }

//    public void setDevice(int deviceId, int portNum) {
//        this.deviceId = deviceId;
//        this.portNum = portNum;
//        db.bloatDao().upsert(new Bloat("deviceId", String.valueOf(deviceId)));
//        db.bloatDao().upsert(new Bloat("portNum", String.valueOf(portNum)));
//    }

    public void askForPermission() throws Exception {
        System.out.println("askForPermission()");
//        System.out.println("askForPermission() deviceId="+deviceId+" portNum="+portNum+" baudRate="+ baudrate);
//        if (deviceId == -1 || portNum == -1 || baudrate == -1 || context == null) {
//            throw new Exception("device not set");
//        }
        context.registerReceiver(broadcastReceiver, new IntentFilter(INTENT_ACTION_GRANT_USB));

        UsbDevice device = null;
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        Collection<UsbDevice> devlist= usbManager.getDeviceList().values();
        if (devlist.size() <= 0) {
            throw new Exception("connection failed: device not found");
        }
        System.out.println("devlist= " + devlist);
        device = (UsbDevice) devlist.toArray()[0];
        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if(driver == null) {
            driver = CustomProber.getCustomProber().probeDevice(device);
        }
        if(driver == null) {
            throw new Exception("connection failed: no driver for device");
        }
        System.out.println("ports= " + driver.getPorts());
        if(driver.getPorts().size() < 1) {
            throw new Exception("connection failed: not enough ports at device");
        }
        usbSerialPort = driver.getPorts().get(0);
        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());
        if(usbConnection == null && !usbManager.hasPermission(driver.getDevice())) {
            usbPermission = MsgStore.UsbPermission.Requested;
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_MUTABLE : 0;
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(INTENT_ACTION_GRANT_USB), flags);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
        } else {
            deviceId = device.getDeviceId();
            connect();
        }
    }

    private void connect() throws Exception {
        System.out.println("connect()");
        if (deviceId == -1 || portNum == -1 || baudrate == -1) {
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
                usbSerialPort.setParameters(baudrate, 8, 1, UsbSerialPort.PARITY_NONE);
            }catch (UnsupportedOperationException e){
                System.out.println("not supported setparameters");
            }
            usbIoManager = new SerialInputOutputManager(usbSerialPort, this);
            usbIoManager.start();
            System.out.println("usbiomanager starteth");
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

    byte[] hexStringTobyteArray(String hex) {
        byte[] result = new byte[hex.length()/2];
        for (int i = 0; i < hex.length(); i += 2) {
            byte currentChar = 0;
            if (hex.getBytes()[i] >= '0' && hex.getBytes()[i] <= '9') {
                currentChar = (byte) ((hex.getBytes()[i] - '0') * 16);
            } else if (hex.getBytes()[i] >= 'A' && hex.getBytes()[i] <= 'F') {
                currentChar = (byte) ((hex.getBytes()[i] - 'A' + 10) * 16);
            }

            if (hex.getBytes()[i+1] >= '0' && hex.getBytes()[i+1] <= '9') {
                currentChar += (byte) (hex.getBytes()[i+1] - '0');
            } else if (hex.getBytes()[i+1] >= 'A' && hex.getBytes()[i+1] <= 'F') {
                currentChar += (byte) (hex.getBytes()[i+1] - 'A' + 10);
            }

            result[i/2] = currentChar;
        }
        return result;
    }

    byte[] encrypt(String chat, byte[] text) throws Exception {
        int clearlen = ((int)Math.ceil((text.length+1)/16.0))*16;
        byte[] clear = new byte[clearlen];
        clear[0] = '#';
        System.arraycopy(text, 0, clear, 1, text.length);
        Cipher encodeCipher = getCiphers(chat)[0];
        return encodeCipher.doFinal(clear);
    }

    String decrypt(String chat, byte[] text) throws Exception {
        Cipher decodeCipher = getCiphers(chat)[1];
        byte[] clear = decodeCipher.doFinal(text);
        int reallen = clear.length;
        for (; clear[reallen-1] != 0; reallen--);
        if (clear[0] != '#') {
            throw new Exception("received encrypted chat not for you");
        }
        clear = Arrays.copyOfRange(clear, 1, reallen);
        return new String(clear, StandardCharsets.UTF_8);
    }

    Cipher[] getCiphers(String chat) throws Exception {
        Chat chatobj = db.chatDao().getChat(chat);
        if (chatobj == null) {
            throw new Exception("no saved key for chat " + chat);
        }

        Cipher encryptCipher = Cipher.getInstance("AES_128/ECB/NoPadding");
        encryptCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(chatobj.key, "AES"));
        Cipher decryptCipher = Cipher.getInstance("AES_128/ECB/NoPadding");
        decryptCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(chatobj.key, "AES"));
        ciphers.put(chat, new Cipher[]{encryptCipher, decryptCipher});
        return new Cipher[]{encryptCipher, decryptCipher};
    }

}
