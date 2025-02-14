package com.sakkkurai.musicapp.callback;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import com.sakkkurai.musicapp.R;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MetadataManager {

    private MediaMetadataRetriever retriever;

    private Context context;

    public MetadataManager(Context context) {
        this.context = context;
        this.retriever = new MediaMetadataRetriever();
    }

    public void deleteTrack(String trackPath, Context context) {
        ContentResolver resolver = context.getContentResolver();

        Uri uri = getMediaStoreUri(trackPath, resolver);
        if (uri == null) {
            Toast.makeText(context, R.string.music_songinfo_delete_error_notfound, Toast.LENGTH_SHORT).show();
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                Intent intent = activity.getIntent();
                activity.finish();
                activity.startActivity(intent);
                return;
            }
        }

        List<Uri> uris = new ArrayList<>();
        uris.add(uri);

        try {
            PendingIntent pendingIntent = MediaStore.createDeleteRequest(resolver, uris);
            ((Activity) context).startIntentSenderForResult(
                    pendingIntent.getIntentSender(),
                    1001,
                    null, 0, 0, 0
            );
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.music_songinfo_delete_error_mdget, Toast.LENGTH_SHORT).show();
        }
    }

    private Uri getMediaStoreUri(String filePath, ContentResolver resolver) {
        Uri externalUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Audio.Media._ID};
        String selection = MediaStore.Audio.Media.DATA + "=?";
        String[] selectionArgs = {filePath};

        try (Cursor cursor = resolver.query(externalUri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                return ContentUris.withAppendedId(externalUri, id);
            }
        }
        return null;
    }

    public String getLyricist(String path) throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException {
        AudioFile audioFile = AudioFileIO.read(new File(path));
        Tag tag = audioFile.getTag();
        String lyricist = tag.getFirst(FieldKey.LYRICIST);
        if (lyricist != null || lyricist.equals("")) {
            return lyricist;

        } else {
            return "";
        }

    }

    public String getGenre(String path) throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException {
        AudioFile audioFile = AudioFileIO.read(new File(path));
        Tag tag = audioFile.getTag();
        String genre = tag.getFirst(FieldKey.GENRE);
        if (genre != null || genre.equals("")) {
            return genre;

        } else {
            return "";
        }
    }

    public Bitmap getTrackCover(String path) {
        retriever.setDataSource(path);
        byte[] artwork = retriever.getEmbeddedPicture();
        if (artwork != null) {
            return BitmapFactory.decodeByteArray(artwork, 0, artwork.length);
        } else {
            return null;
        }
    }

    public String getTrackName(String path) {
        retriever.setDataSource(path);
        return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
    }

    public String getTrackArtist(String path) {
        retriever.setDataSource(path);
        return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
    }

    public String getTrackAlbum(String path) {
        retriever.setDataSource(path);
        return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
    }

    public String getTrackYear(String path) {
        retriever.setDataSource(path);
        return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR);
    }

    public String getTrackComment(String filePath) throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException {
        try {
            Mp3File mp3file = new Mp3File(filePath);
            if (mp3file.hasId3v2Tag()) {
                ID3v2 id3v2Tag = mp3file.getId3v2Tag();
                return id3v2Tag.getComment();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}