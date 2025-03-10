package com.sakkkurai.musicapp.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.sakkkurai.musicapp.models.Track;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface QueueDao {

    @Insert
    void createQueue(List<Track> tracks);

    @Query("SELECT * FROM queue")
    List<Track> getAll();

    @Query("DELETE FROM queue")
    void deleteAllTracks();
}
