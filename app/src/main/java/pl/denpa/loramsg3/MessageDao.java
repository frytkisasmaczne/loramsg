package pl.denpa.loramsg3;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {
    @Query("SELECT * FROM message WHERE chat = :chat")
    List<Message> getPrivConversation(String chat);

    @Query("SELECT * FROM message WHERE chat IS NULL")
    List<Message> getBroadcastConversation();

    @Query("SELECT DISTINCT author FROM message WHERE chat IS NOT NULL")
    List<String> getPrivUsers();

    @Query("SELECT DISTINCT author FROM message WHERE chat IS NULL")
    List<String> getBroadcastUsers();

    @Query("SELECT DISTINCT chat FROM message")
    List<String> getAllPrivUsers();

    @Query("SELECT DISTINCT author FROM message")
    List<String> getAllUsers();

    @Insert
    void insert(Message... messages);

    @Delete
    void delete(Message message);
}
