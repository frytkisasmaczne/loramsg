package pl.denpa.loramsg3;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import javax.crypto.SecretKey;

@Entity
public class Chat {
    public Chat (String chat, byte[] key) {
        this.chat = chat;
        this.key = key;
    }

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "chat")
    public String chat;
    @ColumnInfo(name = "key")
    public byte[] key;
}
