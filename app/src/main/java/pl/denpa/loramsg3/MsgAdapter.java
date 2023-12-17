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

        public ViewHolder(View view) {
            super(view);
            System.out.println("ViewHolder()");
            // Define click listener for the ViewHolder's View
            textView = (TextView) view.findViewById(R.id.textView);
        }

        public TextView getTextView() {
            return textView;
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.msg_list_item, viewGroup, false);
        System.out.println("onCreateViewHolder");

        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
//        Message msg = db.messageDao().getPrivConversation(openChat.chat).get(position);
        Message msg = msgs.get(position);
        viewHolder.getTextView().setText(msg.author + ": " + msg.text);
        System.out.println(msg.author + ": " + msg.text);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
//        return db.messageDao().getPrivConversation(openChat.chat).size();
        return msgs.size();
    }

}
