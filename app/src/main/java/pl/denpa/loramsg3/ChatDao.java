package pl.denpa.loramsg3;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Upsert;

import java.util.List;

@Dao
public interface ChatDao {
    @Query("SELECT * FROM chat WHERE chat = :chat")
    Chat getChat(String chat);
    @Query("SELECT * FROM chat")
    List<Chat> getAllChats();
    @Upsert
    void upsert(Chat... chats);
    @Delete
    void delete(Chat chat);
}
