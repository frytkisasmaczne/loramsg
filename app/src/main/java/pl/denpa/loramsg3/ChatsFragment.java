package pl.denpa.loramsg3;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ChatsFragment extends Fragment {
    private final Handler mainLooper;
    private MsgStore msgStore;
    public RecyclerView recyclerView;
    public ChatsAdapter chatsAdapter = new ChatsAdapter(this);

    public ChatsFragment() {
        msgStore = MsgStore.getInstance();
        mainLooper = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    public void chatClicked(String chat) {
        Bundle args = new Bundle();
        args.putString("chat", chat);
        MsgFragment fragment = new MsgFragment();
        fragment.setArguments(args);
        System.out.println("starting terminal fragment");
        getFragmentManager().beginTransaction().replace(R.id.fragment, fragment, "msg").addToBackStack(null).commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        msgStore.restoreConnection();
        chatsAdapter.refresh();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        System.out.println("ChatsFragment.onCreateView()");
        View view = inflater.inflate(R.layout.chats_fragment, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setAdapter(chatsAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(llm);
        return view;
    }

}
