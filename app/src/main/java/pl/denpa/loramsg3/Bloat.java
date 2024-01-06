package pl.denpa.loramsg3;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Bloat {
    public Bloat (String key, String value) {
        this.key = key;
        this.value = value;
    }

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "name")
    public String key;

    @ColumnInfo(name = "value")
    public String value;
}
