package pl.denpa.loramsg3;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Message {
    public Message(String author, String chat, String text) {
        this.author = author;
        this.chat = chat;
        this.text = text;
    }
    @PrimaryKey(autoGenerate = true)
    public int uid;
    @ColumnInfo(name = "chat")
    public String chat;
    @ColumnInfo(name = "author")
    public String author;
    @ColumnInfo(name = "text")
    public String text;
}
