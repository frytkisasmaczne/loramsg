package pl.denpa.loramsg3;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class NewChatDialog extends DialogFragment {

    MsgStore msgStore = MsgStore.getInstance();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog.
        // Pass null as the parent view because it's going in the dialog layout.
        builder.setView(inflater.inflate(R.layout.new_chat_dialog, null))
                // Add action buttons
                .setPositiveButton("Create new chat",
                        (dialog, id) -> {
                            String newChat = ((EditText) NewChatDialog.this.getDialog().findViewById(R.id.username)).getText().toString();
                            if (newChat.equals("")) {
                                return;
                            }
                            msgStore.createChat(newChat);
                            msgStore.chatsFragment.navigateToChat(newChat);
                        })
                .setNegativeButton("cancel",
                        (dialog, id) -> NewChatDialog.this.getDialog().cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

        ((Button) dialog.findViewById(R.id.button)).setOnClickListener(view -> barcodeLauncher.launch(new ScanOptions()));

        return dialog;
    }

    final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
        result -> {
            if(result.getContents() == null) {
                System.out.println("qr scanning Cancelled");
            } else {
                System.out.println("Scanned: " + result.getContents());
                String text = result.getContents();
                String[] nowsplit = text.split(":", 1);
                if (nowsplit.length != 2 || nowsplit[0].length() == 0 || nowsplit[1].length() != 24) {
                    System.out.println("scanned qr is not a chat");
                    Toast.makeText(getContext(), "scanned qr is not a chat", Toast.LENGTH_SHORT).show();
                    return;
                }
                String username = nowsplit[0];
                String b64key = nowsplit[1];
                msgStore.addChat(username, Base64.decode(b64key, Base64.DEFAULT));
                msgStore.chatsFragment.navigateToChat(username);
            }
        });

}
