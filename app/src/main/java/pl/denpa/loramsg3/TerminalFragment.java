package pl.denpa.loramsg3;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hoho.android.usbserial.util.HexDump;

import java.util.ArrayList;
import java.util.List;

public class TerminalFragment extends Fragment {

//    private enum UsbPermission { Unknown, Requested, Granted, Denied }

//    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
//    private static final int WRITE_WAIT_MILLIS = 2000;
//    private static final int READ_WAIT_MILLIS = 2000;

    public String recipient;
//    private boolean withIoManager;

//    private final BroadcastReceiver broadcastReceiver;
    private final Handler mainLooper;
    private TextView receiveText;
    private ControlLines controlLines;

//    private SerialInputOutputManager usbIoManager;
//    private UsbSerialPort usbSerialPort;
//    private UsbPermission usbPermission = UsbPermission.Unknown;
    private boolean connected = false;

    MsgStore msgStore;

    public TerminalFragment() {
        msgStore = MsgStore.getInstance();
        mainLooper = new Handler(Looper.getMainLooper());
    }

    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        recipient = getArguments().getString("user");
//        portNum = getArguments().getInt("port");
//        baudRate = getArguments().getInt("baud");
//        withIoManager = getArguments().getBoolean("withIoManager");
//        Toast.makeText(getActivity().getApplicationContext(), "" + deviceId + " " + portNum + " " + baudRate + " " + withIoManager, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onResume() {
        super.onResume();
        msgStore.setOpenChat(this);
    }

//    @Override
//    public void onPause() {
//        if(connected) {
//            status("disconnected");
//            disconnect();
//        }
//        getActivity().unregisterReceiver(broadcastReceiver);
//        super.onPause();
//    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());
        TextView sendText = view.findViewById(R.id.send_text);
        View sendBtn = view.findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(v -> {
            send(sendText.getText().toString());
            sendText.clearComposingText();
        });
        View receiveBtn = view.findViewById(R.id.receive_btn);
        controlLines = new ControlLines(view);
        controlLines.start();
        receiveBtn.setVisibility(View.GONE);
        List<Message> messages = msgStore.getMessages(recipient);
        if (messages != null) {
            for (Message msg : messages) {
                appendText(msg.author + ": " + msg.text);
            }
        }
        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//        if (id == R.id.clear) {
//            receiveText.setText("");
//            return true;
//        } else if( id == R.id.send_break) {
//            if(!connected) {
//                Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
//            } else {
//                try {
//                    usbSerialPort.setBreak(true);
//                    Thread.sleep(100); // should show progress bar instead of blocking UI thread
//                    usbSerialPort.setBreak(false);
//                    SpannableStringBuilder spn = new SpannableStringBuilder();
//                    spn.append("send <break>\n");
//                    spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                    receiveText.append(spn);
//                } catch(UnsupportedOperationException ignored) {
//                    Toast.makeText(getActivity(), "BREAK not supported", Toast.LENGTH_SHORT).show();
//                } catch(Exception e) {
//                    Toast.makeText(getActivity(), "BREAK failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                }
//            }
//            return true;
//        } else {
//            return super.onOptionsItemSelected(item);
//        }
        return super.onOptionsItemSelected(item);
    }

//    /*
//     * Serial
//     */
//    @Override
//    public void onNewData(byte[] data) {
//        mainLooper.post(() -> {
//            receive(data);
//
//        });
//    }

//    @Override
//    public void onRunError(Exception e) {
//        mainLooper.post(() -> {
//            status("connection lost: " + e.getMessage());
//            disconnect();
//        });
//    }

//    /*
//     * Serial + UI
//     */
//    private void connect() {
//        try {
//            status("connected");
//            connected = true;
//
//        } catch (Exception e) {
//            status(e.getMessage());
//        }
//    }

//    private void disconnect() {
//        connected = false;
//        controlLines.stop();
//        if(usbIoManager != null) {
//            usbIoManager.setListener(null);
//            usbIoManager.stop();
//        }
//        usbIoManager = null;
//        try {
//            usbSerialPort.close();
//        } catch (IOException ignored) {}
//        usbSerialPort = null;
//    }

    private void send(String str) {
//        if(!connected) {
//            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
//            return;
//        }
        try {
            msgStore.send(recipient, str);
            byte[] data = (str).getBytes();
            SpannableStringBuilder spn = new SpannableStringBuilder();
//            spn.append("send " + data.length + " bytes\n");
//            spn.append(HexDump.dumpHexString(data)).append("\n");
            spn.append(msgStore.user).append(": ").append(str);
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            receiveText.append(spn);
        } catch (Exception e) {
            mainLooper.post(() -> {
                status("no connection, error: " + e.getMessage());
            });
        }
    }

    private void appendText(String text) {
        SpannableStringBuilder spn = new SpannableStringBuilder();
        spn.append(text).append("\n");
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }

//    private void read() {
//        if(!connected) {
//            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        try {
//            byte[] buffer = new byte[8192];
//            int len = usbSerialPort.read(buffer, READ_WAIT_MILLIS);
//            receive(Arrays.copyOf(buffer, len));
//        } catch (IOException e) {
//            // when using read with timeout, USB bulkTransfer returns -1 on timeout _and_ errors
//            // like connection loss, so there is typically no exception thrown here on error
//            status("connection lost: " + e.getMessage());
//            disconnect();
//        }
//    }

    public void receive(String msg) {
        SpannableStringBuilder spn = new SpannableStringBuilder();
        spn.append(msg).append("\n");
        receiveText.append(spn);
    }

    void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }

    class ControlLines {
        private static final int refreshInterval = 200; // msec

        private final Runnable runnable;
        private final ToggleButton rtsBtn, ctsBtn, dtrBtn, dsrBtn, cdBtn, riBtn;

        ControlLines(View view) {
            runnable = this::run; // w/o explicit Runnable, a new lambda would be created on each postDelayed, which would not be found again by removeCallbacks

            rtsBtn = view.findViewById(R.id.controlLineRts);
            ctsBtn = view.findViewById(R.id.controlLineCts);
            dtrBtn = view.findViewById(R.id.controlLineDtr);
            dsrBtn = view.findViewById(R.id.controlLineDsr);
            cdBtn = view.findViewById(R.id.controlLineCd);
            riBtn = view.findViewById(R.id.controlLineRi);
            rtsBtn.setOnClickListener(this::toggle);
            dtrBtn.setOnClickListener(this::toggle);
        }

        private void toggle(View v) {
            ToggleButton btn = (ToggleButton) v;
            if (!connected) {
                btn.setChecked(!btn.isChecked());
                Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
                return;
            }
            String ctrl = "";
//            try {
//                if (btn.equals(rtsBtn)) { ctrl = "RTS"; usbSerialPort.setRTS(btn.isChecked()); }
//                if (btn.equals(dtrBtn)) { ctrl = "DTR"; usbSerialPort.setDTR(btn.isChecked()); }
//            } catch (IOException e) {
//                status("set" + ctrl + "() failed: " + e.getMessage());
//            }
        }

        private void run() {
            if (!connected)
                return;
//            try {
//                EnumSet<UsbSerialPort.ControlLine> controlLines = usbSerialPort.getControlLines();
//                rtsBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.RTS));
//                ctsBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.CTS));
//                dtrBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.DTR));
//                dsrBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.DSR));
//                cdBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.CD));
//                riBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.RI));
//                mainLooper.postDelayed(runnable, refreshInterval);
//            } catch (Exception e) {
//                status("getControlLines() failed: " + e.getMessage() + " -> stopped control line refresh");
//            }
        }

        void start() {
            if (!connected)
                return;
//            try {
//                EnumSet<UsbSerialPort.ControlLine> controlLines = usbSerialPort.getSupportedControlLines();
//                if (!controlLines.contains(UsbSerialPort.ControlLine.RTS)) rtsBtn.setVisibility(View.INVISIBLE);
//                if (!controlLines.contains(UsbSerialPort.ControlLine.CTS)) ctsBtn.setVisibility(View.INVISIBLE);
//                if (!controlLines.contains(UsbSerialPort.ControlLine.DTR)) dtrBtn.setVisibility(View.INVISIBLE);
//                if (!controlLines.contains(UsbSerialPort.ControlLine.DSR)) dsrBtn.setVisibility(View.INVISIBLE);
//                if (!controlLines.contains(UsbSerialPort.ControlLine.CD))   cdBtn.setVisibility(View.INVISIBLE);
//                if (!controlLines.contains(UsbSerialPort.ControlLine.RI))   riBtn.setVisibility(View.INVISIBLE);
//                run();
//            } catch (Exception e) {
//                Toast.makeText(getActivity(), "getSupportedControlLines() failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                rtsBtn.setVisibility(View.INVISIBLE);
//                ctsBtn.setVisibility(View.INVISIBLE);
//                dtrBtn.setVisibility(View.INVISIBLE);
//                dsrBtn.setVisibility(View.INVISIBLE);
//                cdBtn.setVisibility(View.INVISIBLE);
//                cdBtn.setVisibility(View.INVISIBLE);
//                riBtn.setVisibility(View.INVISIBLE);
//            }
            rtsBtn.setVisibility(View.INVISIBLE);
            ctsBtn.setVisibility(View.INVISIBLE);
            dtrBtn.setVisibility(View.INVISIBLE);
            dsrBtn.setVisibility(View.INVISIBLE);
            cdBtn.setVisibility(View.INVISIBLE);
            cdBtn.setVisibility(View.INVISIBLE);
            riBtn.setVisibility(View.INVISIBLE);
        }

        void stop() {
            mainLooper.removeCallbacks(runnable);
            rtsBtn.setChecked(false);
            ctsBtn.setChecked(false);
            dtrBtn.setChecked(false);
            dsrBtn.setChecked(false);
            cdBtn.setChecked(false);
            riBtn.setChecked(false);
        }
    }
}
