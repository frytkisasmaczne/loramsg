package pl.denpa.loramsg3;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Message {
    public Message(String author, String chat, String text, int rssi, int snr) {
        this.author = author;
        this.chat = chat;
        this.text = text;
        this.rssi = rssi;
        this.snr = snr;
    }
    @PrimaryKey(autoGenerate = true)
    public int uid;
    @ColumnInfo(name = "chat")
    public String chat;
    @ColumnInfo(name = "author")
    public String author;
    @ColumnInfo(name = "text")
    public String text;
    @ColumnInfo(name = "rssi")
    public int rssi;
    @ColumnInfo(name = "snr")
    public int snr;
}
