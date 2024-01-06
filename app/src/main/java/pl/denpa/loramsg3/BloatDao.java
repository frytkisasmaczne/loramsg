package pl.denpa.loramsg3;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Query;
import androidx.room.Upsert;

@Dao
public interface BloatDao {
    @Query("SELECT value FROM bloat WHERE name = :key")
    String getValue(String key);

    @Upsert
    void upsert(Bloat... bloats);

    @Delete
    void delete(Bloat bloat);
}
