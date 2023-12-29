package pl.denpa.loramsg3;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class NewChatDialog extends DialogFragment {

    MsgStore msgStore = MsgStore.getInstance();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater.
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog.
        // Pass null as the parent view because it's going in the dialog layout.
        builder.setView(inflater.inflate(R.layout.new_chat_dialog, null))
                // Add action buttons
                .setPositiveButton("Generate key",
                        (dialog, id) -> {
                            String newChat = ((EditText) NewChatDialog.this.getDialog().findViewById(R.id.username)).getText().toString();
                            msgStore.createChat(newChat);
                            msgStore.chatsFragment.navigateToChat(newChat);
                        })
                .setNegativeButton("cancel",
                        (dialog, id) -> NewChatDialog.this.getDialog().cancel());
        return builder.create();
    }

}