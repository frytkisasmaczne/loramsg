package pl.denpa.loramsg3;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import javax.crypto.SecretKey;

@Entity
public class Chat {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "chat")
    public String chat;

    @ColumnInfo(name = "key")
    public SecretKey key;
}
