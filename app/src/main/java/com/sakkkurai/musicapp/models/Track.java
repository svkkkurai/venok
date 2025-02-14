package com.sakkkurai.musicapp.models;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class Track {
    private String trackName;
    private String artistName;
    private String albumName;
    private String audioPath;
    private String duration;



    public Track(String trackName, String artistName, String albumName, String duration, String audioPath) {
        this.trackName = trackName;
        this.artistName = artistName;
        this.albumName = albumName;
        this.duration = duration;
        this.audioPath = audioPath;
    }

    public String getTrackName() {
        return trackName;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getAlbumName()
    {
        return albumName;
    }

    public String getAudioSize()
    {
        String size = getFileSizeInMB(audioPath);
        return size;
    }


    public int getBitrate() throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this.getAudioPath());
            String bitrateString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            return bitrateString != null ? Integer.parseInt(bitrateString)/1000 : -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        } finally {
            retriever.release();
        }
    }

    public int getDurationMs() {
        return Integer.parseInt(duration);
    }

    private String getFileSizeInMB(String path) {
        File file = new File(path);
        if (file.exists()) {
            double sizeInMB = file.length() / (1024.0 * 1024.0);
            return String.format("%.2f", sizeInMB);
        } else {
            return "0.00";
        }
    }

    public Bitmap getBitmap() throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(audioPath);
            byte[] art = retriever.getEmbeddedPicture();
            if (art != null) {
                return BitmapFactory.decodeByteArray(art, 0, art.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            retriever.release();
        }
        return null;
    }


    public String getDuration()
    {
        try {
            long totalSeconds = Long.parseLong(duration) / 1000; // Предполагаем, что duration хранится в секундах
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;
            long hours = minutes / 60;
            minutes = minutes % 60;

            if (totalSeconds < 600) { // < 10 min
                return String.format("%d:%02d", minutes, seconds);
            } else if (totalSeconds < 3600) { // // >10 min and <60 min
                return String.format("%02d:%02d", minutes, seconds);
            } else if (totalSeconds < 36000) { // >60 min and <600 min
                return String.format("%d:%02d:%02d", hours, minutes, seconds);
            } else { // // >600 min and <1440 min
                return String.format("%02d:%02d:%02d", hours, minutes, seconds);
            }
        } catch (NumberFormatException e) {
            return "0:00";
        }
    }


    public String getAudioPath() {
        return audioPath;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }
}
