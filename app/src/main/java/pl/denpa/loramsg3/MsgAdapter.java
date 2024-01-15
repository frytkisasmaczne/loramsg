package pl.denpa.loramsg3;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MsgAdapter extends RecyclerView.Adapter<MsgAdapter.ViewHolder> {

    MsgStore msgStore = MsgStore.getInstance();
    public List<Message> msgs;

    public MsgAdapter(String chat) {
        msgs = msgStore.getMessages(chat);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;
        private final TextView nickView;
        private final TextView rssiView;

        public ViewHolder(View view) {
            super(view);
//            System.out.println("ViewHolder()");
            // Define click listener for the ViewHolder's View
            textView = view.findViewById(R.id.textView6);
            nickView = view.findViewById(R.id.textView);
            rssiView = view.findViewById(R.id.textView3);
        }

        public TextView getTextView() {
            return textView;
        }
        public TextView getNickView() {
            return nickView;
        }
        public TextView getRssiView() {
            return rssiView;
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.msg_list_item, viewGroup, false);
//        System.out.println("onCreateViewHolder");

        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
//        Message msg = db.messageDao().getPrivConversation(openChat.chat).get(position);
        Message msg = msgs.get(position);
        viewHolder.getTextView().setText(msg.text);
        viewHolder.getNickView().setText(msg.author + ": ");
        if (msg.rssi == 999 && msg.snr == 999) {
            viewHolder.getRssiView().setText("rssi:n/a snr:n/a");
        } else {
            viewHolder.getRssiView().setText("rssi:" + msg.rssi + " snr:" + msg.snr);
        }
//        System.out.println(msg.author + ": " + msg.text);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
//        return db.messageDao().getPrivConversation(openChat.chat).size();
        return msgs.size();
    }

}
