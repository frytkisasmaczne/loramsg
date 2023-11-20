package pl.denpa.loramsg3;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;

import com.hoho.android.usbserial.driver.UsbSerialDriver;

import java.util.ArrayList;

public class ChatsFragment extends ListFragment {

    static class ListItem {
        String user;
        String lastMessage;

        ListItem(String user, String lastMessage) {
            this.user = user;
            this.lastMessage = lastMessage;
        }
    }

    private final ArrayList<ListItem> listItems = new ArrayList<>();
    private ArrayAdapter<ListItem> listAdapter;
    private MsgStore msgStore = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        listAdapter = new ArrayAdapter<ListItem>(getActivity(), 0, listItems) {
            @NonNull
            @Override
            public View getView(int position, View view, @NonNull ViewGroup parent) {
                ListItem item = listItems.get(position);
                if (view == null)
                    view = getActivity().getLayoutInflater().inflate(R.layout.device_list_item, parent, false);
                TextView text1 = view.findViewById(R.id.text1);
                TextView text2 = view.findViewById(R.id.text2);
                text1.setText(item.user);
                text2.setText(item.lastMessage);
                return view;
            }
        };
        msgStore = MsgStore.getInstance();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(null);
        View header = getActivity().getLayoutInflater().inflate(R.layout.device_list_header, null, false);
        getListView().addHeaderView(header, null, false);
        setEmptyText("<no USB devices found>");
        ((TextView) getListView().getEmptyView()).setTextSize(18);
        setListAdapter(listAdapter);
    }

    @Override
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        ListItem item = listItems.get(position-1);
        Bundle args = new Bundle();
        args.putString("user", item.user);
        TerminalFragment fragment = new TerminalFragment();
        fragment.setArguments(args);
//        msgStore.connect(getActivity(), fragment, item.device.getDeviceId(), item.port, baudRate);
        System.out.println("starting terminal fragment");
        getFragmentManager().beginTransaction().replace(R.id.fragment, fragment, "terminal").addToBackStack(null).commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    void refresh() {
        listItems.clear();
        for(String[] conversation : msgStore.getConversations()) {
            listItems.add(new ChatsFragment.ListItem(conversation[0], conversation[1]));
        }
        listAdapter.notifyDataSetChanged();
    }

}
