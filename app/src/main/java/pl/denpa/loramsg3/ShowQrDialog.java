package pl.denpa.loramsg3;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class ShowQrDialog extends DialogFragment {

    MsgStore msgStore = MsgStore.getInstance();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.show_qr_dialog, null))
                .setPositiveButton("Close", (dialog, id) -> {
                    dialog.dismiss();
                });

        AlertDialog dialog = builder.create();
        dialog.show();

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(msgStore.nick + ":" + Base64.encodeToString(msgStore.db.chatDao().getChat(msgStore.openChat.chat).key, Base64.DEFAULT), BarcodeFormat.QR_CODE, 400, 400);
            ImageView imageViewQrCode = (ImageView) dialog.findViewById(R.id.imageView);
            imageViewQrCode.setImageBitmap(bitmap);
        } catch(Exception e) {
            System.out.println("what while generating qrcode: " + e);
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return dialog;
    }

}
