package pl.denpa.loramsg3;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import pl.denpa.loramsg3.databinding.FragmentSecondBinding;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;

    public String chat;
    private final Handler mainLooper = new Handler(Looper.getMainLooper());
    private MsgStore msgStore = MsgStore.getInstance();;
    public RecyclerView recyclerView;
    public MsgAdapter msgAdapter;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        setHasOptionsMenu(true);
        System.out.println("SecondFragment.onCreateView()");
        binding = FragmentSecondBinding.inflate(inflater, container, false);

        chat = getArguments().getString("chat");
        msgAdapter = new MsgAdapter(chat);

        recyclerView = binding.getRoot().findViewById(R.id.recycler_view);
        recyclerView.setAdapter(msgAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(llm);
        recyclerView.scrollToPosition(msgAdapter.getItemCount() - 1);
        EditText sendText = binding.getRoot().findViewById(R.id.send_text);
        View sendBtn = binding.getRoot().findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(v -> {
            send(sendText.getText().toString());
            sendText.setText("");
        });

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

//        binding.buttonSecond.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                NavHostFragment.findNavController(SecondFragment.this)
//                        .navigate(R.id.action_SecondFragment_to_FirstFragment);
//            }
//        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_msg, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.show_qr) {
            ShowQrDialog showQrDialog = new ShowQrDialog();
            showQrDialog.show(getActivity().getSupportFragmentManager(), "showqrdialog");
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        msgStore.setOpenChat(this);
        msgStore.restoreConnection();
    }

    private void send(String str) {
//        if(!connected) asdf
        msgStore.send(chat, str);
    }

}
