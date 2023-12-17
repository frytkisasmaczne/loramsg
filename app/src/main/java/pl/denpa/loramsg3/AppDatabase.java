package pl.denpa.loramsg3;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Message.class, Chat.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract MessageDao messageDao();
    public abstract ChatDao chatDao();
}
