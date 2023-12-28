package pl.denpa.loramsg3;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ViewHolder> implements View.OnClickListener {

    public MsgStore msgStore = MsgStore.getInstance();
    public List<String> chats;
    private ChatsFragment chatsFragment;

    public ChatsAdapter() {
        super();
        refresh();
    }

    public void refresh() {
        chats = msgStore.getConversations();
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        public ViewHolder(View view) {
            super(view);
            System.out.println("ViewHolder()");
            // Define click listener for the ViewHolder's View
            textView = view.findViewById(R.id.textView);
        }

        public TextView getTextView() {
            return textView;
        }
    }

    public ChatsAdapter(ChatsFragment chatsFragment) {
        this.chatsFragment = chatsFragment;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.chat_list_item, viewGroup, false);
        System.out.println("onCreateViewHolder");

        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
//        Message msg = db.messageDao().getPrivConversation(openChat.chat).get(position);
        String chat = chats.get(position);
        viewHolder.getTextView().setText(chat);
        System.out.println(chat);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
//        return db.messageDao().getPrivConversation(openChat.chat).size();
        return chats.size();
    }

    @Override
    public void onClick(View view) {
        System.out.println("clicked position " + chats.get(chatsFragment.recyclerView.getChildLayoutPosition(view)));
        chatsFragment.chatClicked(chats.get(chatsFragment.recyclerView.getChildLayoutPosition(view)));
    }

}
