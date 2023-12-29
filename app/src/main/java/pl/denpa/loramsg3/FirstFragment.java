package pl.denpa.loramsg3;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

import pl.denpa.loramsg3.databinding.FragmentFirstBinding;

public class FirstFragment extends Fragment implements OnItemClickListener {

    private FragmentFirstBinding binding;

    private final ArrayList<ListItem> listItems = new ArrayList<>();
    private ArrayAdapter<ListItem> listAdapter;
    private MsgStore msgStore = MsgStore.getInstance();

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        setHasOptionsMenu(true);

        binding = FragmentFirstBinding.inflate(inflater, container, false);


        listAdapter = new ArrayAdapter<ListItem>(getActivity(), 0, listItems) {
            @NonNull
            @Override
            public View getView(int position, View view, @NonNull ViewGroup parent) {
                ListItem item = listItems.get(position);
                if (view == null)
                    view = getActivity().getLayoutInflater().inflate(R.layout.device_list_item, parent, false);
                TextView text1 = view.findViewById(R.id.text1);
//                TextView text2 = view.findViewById(R.id.text2);
//                if (item.chat == null) {
//                    text1.setText("BROADCAST");
//                } else {
                    text1.setText(item.chat);
//                }
//                text2.setText(item.lastMessage);
                return view;
            }
        };

        binding.fab.setOnClickListener(view -> {
            NewChatDialog newChatDialog = new NewChatDialog();
            newChatDialog.show(getActivity().getSupportFragmentManager(), "newchatdialog");
        });

        ListView listview = binding.list;
        listview.setOnItemClickListener(this);
        View header = getActivity().getLayoutInflater().inflate(R.layout.device_list_header, listview, false);

        listview.addHeaderView(header, new ListItem(null), true);
        listview.setAdapter(listAdapter);
        msgStore.chatsFragment = this;
        refresh();
        return binding.getRoot();
    }

//    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_devices, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.preferences) {
            NavHostFragment.findNavController(FirstFragment.this).navigate(R.id.action_FirstFragment_to_preferencesFragment);
//            getFragmentManager().beginTransaction().replace(R.id.nav_host_fragment_content_main, new PreferencesFragment()).addToBackStack(null).commit();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void navigateToChat(String chat) {
        Bundle bundle = new Bundle();
        System.out.println("moving to second fragment chat= " + chat);
        bundle.putString("chat", chat);
        NavHostFragment.findNavController(FirstFragment.this).navigate(R.id.action_FirstFragment_to_SecondFragment, bundle);
    }

    void refresh() {
        listItems.clear();
        System.out.println("msgStore.getConversations() = " + msgStore.getConversations());
        for(String conversation : msgStore.getConversations()) {
            listItems.add(new ListItem(conversation));
        }
        listAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        System.out.println("FirstFragment.onItemClick(i=" + i + ")");
        try {
            msgStore.askForPermission();
        } catch (Exception e) {
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
        if (i == 0) {
            navigateToChat(null);
        } else {
            navigateToChat(listItems.get(i-1).chat);
        }
    }

    static class ListItem {
        String chat;
//        String lastMessage;

        ListItem(String chat/*, String lastMessage*/) {
            this.chat = chat;
//            this.lastMessage = lastMessage;
        }
    }

}