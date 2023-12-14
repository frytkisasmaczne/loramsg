package pl.denpa.loramsg3;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MsgFragment extends Fragment {
    public String chat;
    private final Handler mainLooper;
    private MsgStore msgStore;

    public MsgFragment() {
        msgStore = MsgStore.getInstance();
        mainLooper = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        chat = getArguments().getString("chat");
    }

    @Override
    public void onResume() {
        super.onResume();
        msgStore.setOpenChat(this);
        msgStore.restoreConnection();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.msg_fragment, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setAdapter(msgStore);
        TextView sendText = view.findViewById(R.id.send_text);
        View sendBtn = view.findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(v -> {
            send(sendText.getText().toString());
            sendText.clearComposingText();
        });

        return view;
    }

    private void send(String str) {
//        if(!connected) asdf
        try {
            msgStore.send(chat, str);
        } catch (Exception e) {
            Toast.makeText(getActivity().getApplicationContext(), "not connected according to MsgFragment", Toast.LENGTH_SHORT).show();
        }
    }

}
