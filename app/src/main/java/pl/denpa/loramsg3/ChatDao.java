package pl.denpa.loramsg3;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ChatDao {
    @Query("SELECT * FROM chat WHERE chat = :chat")
    Chat getChat(String chat);

    @Query("SELECT * FROM chat WHERE chat IS NULL")
    Chat getBroadcastChat();

    @Query("SELECT * FROM chat")
    List<Chat> getAllChats();

    @Insert
    void insert(Chat... chats);

    @Delete
    void delete(Chat chat);
}
