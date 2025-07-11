package com.sakkkurai.venok.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.sakkkurai.venok.database.dao.QueueDao;
import com.sakkkurai.venok.models.Track;

@Database(entities = {Track.class}, version = 1)
public abstract class QueueDatabase extends RoomDatabase {
    public abstract QueueDao queueDao();
    private static volatile QueueDatabase INSTANCE;

    public static QueueDatabase getInstance(Context context){
        if (INSTANCE == null) {
            synchronized (QueueDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            QueueDatabase.class,
                            "queue.db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }

}
