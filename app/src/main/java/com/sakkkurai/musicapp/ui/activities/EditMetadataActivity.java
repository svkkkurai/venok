package com.sakkkurai.musicapp.ui.activities;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.sakkkurai.musicapp.R;
import com.sakkkurai.musicapp.callback.MetadataManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditMetadataActivity extends AppCompatActivity {

    private TextInputEditText title, artist, album, comment, year, genre, lyricist;
    private ShapeableImageView cover;
    private LinearProgressIndicator progressIndicator;
    private ScrollView scrollView;
    private String path, sourceTrackComment, sourceTrackYear, sourceTrackAlbum, sourceTrackArtist, sourceTrackTitle, sourceTrackGenre, sourceTrackLyricist;
    private ShapeableImageView tArt;
    private int genreNum;

    private Bitmap sourceTrackArt;
    MetadataManager metadataManager = new MetadataManager(this);
    private Button editmd_save;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_metadata);
        this.path = getIntent().getStringExtra("PATH");
        this.tArt = findViewById(R.id.editmd_trackCover);
        this.title = findViewById(R.id.editmd_trackNameInput);
        this.artist = findViewById(R.id.editmd_trackArtistInput);
        this.album = findViewById(R.id.editmd_trackAlbumInput);
        this.comment = findViewById(R.id.editmd_trackCommentInput);
        this.year = findViewById(R.id.editmd_trackYearInput);
        this.progressIndicator = findViewById(R.id.editmd_progressIndicator);
        this.scrollView = findViewById(R.id.editmd_scroll);
        this.lyricist = findViewById(R.id.editmd_trackLyricistInput);
        this.editmd_save = findViewById(R.id.editmd_save);
        editmd_save.setEnabled(false);
        editmd_save.setClickable(false);
        this.genre = findViewById(R.id.editmd_trackGenreInput);
        if (!genre.getText().toString().isEmpty()) {
            genreNum = Integer.parseInt(genre.getText().toString());
        }
        title.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                saveButtonActiveManager(editmd_save);
            }
        });
        artist.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                saveButtonActiveManager(editmd_save);
            }
        });
        album.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                saveButtonActiveManager(editmd_save);
            }
        });
        year.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                saveButtonActiveManager(editmd_save);
            }
        });
        genre.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                saveButtonActiveManager(editmd_save);
            }
        });
        lyricist.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                saveButtonActiveManager(editmd_save);
            }
        });
        comment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                saveButtonActiveManager(editmd_save);
            }
        });
        MaterialToolbar mTB = findViewById(R.id.editmetadata_bar);
        mTB.setNavigationOnClickListener(v -> {
            finish();
        });
        editmd_save.setOnClickListener(v -> {
            saveMetadata();
        });
        if (path == null) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle(R.string.editmetadata_wrongpath_title)
                    .setCancelable(true)
                    .setMessage(R.string.editmetadata_wrongpath)
                    .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                        dialog.dismiss();
                        finish();
                    })
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            Log.d("EditMetadataActivity", "closed");
                            finish();
                        }
                    })
                    .show();
        } else {
            loadMetadata(path);
        }
    }

    private void saveMetadata() {
        File origAudioFile = new File(path);
        long mediaID = getFilePathToMediaID(origAudioFile.getAbsolutePath(), this);
        Uri fileUri = ContentUris.withAppendedId(MediaStore.Audio.Media.getContentUri("external"), mediaID);
        List<Uri> uri = new ArrayList<>();
        uri.add(fileUri);

        String tempPath = String.valueOf(this.getFilesDir() + File.separator + origAudioFile.getName());
        PendingIntent pendingIntent = MediaStore.createWriteRequest(getContentResolver(), uri);
        try {
            startIntentSenderForResult(pendingIntent.getIntentSender(), 1488, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1488 && resultCode == Activity.RESULT_OK) {
            try {
                ContentResolver resolver = getContentResolver();
                File tempFile = new File(getCacheDir(), "temp.mp3");
                try (InputStream in = resolver.openInputStream(getMediaStoreUri(path));
                     OutputStream out = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                AudioFile audioFile = AudioFileIO.read(tempFile);
                Tag tag = audioFile.getTag();
                tag.setField(FieldKey.TITLE, title.getText().toString());
                tag.setField(FieldKey.ARTIST, artist.getText().toString());
                tag.setField(FieldKey.ALBUM, album.getText().toString());
                tag.setField(FieldKey.GENRE, genre.getText().toString());
                tag.setField(FieldKey.LYRICIST, lyricist.getText().toString());
                tag.setField(FieldKey.COMMENT, comment.getText().toString());
                tag.setField(FieldKey.YEAR, year.getText().toString());
                AudioFileIO.write(audioFile);
                try (OutputStream out = resolver.openOutputStream(getMediaStoreUri(path), "wt");
                     InputStream in = new FileInputStream(tempFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                MediaScannerConnection.scanFile(this, new String[]{path}, null, (path, uri) -> {
                });
                SharedPreferences prefs = getSharedPreferences("temp", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("ISMDCHANGED", true);
                editor.apply();
                sourceTrackComment = String.valueOf(comment.getText());
                sourceTrackYear = String.valueOf(year.getText());
                sourceTrackAlbum = String.valueOf(album.getText());
                sourceTrackArtist = String.valueOf(artist.getText());
                sourceTrackTitle = String.valueOf(title.getText());
                sourceTrackGenre = String.valueOf(genre.getText());
                sourceTrackLyricist = String.valueOf(lyricist.getText());

                editmd_save.setEnabled(false);
                editmd_save.setActivated(false);
                View rootView = findViewById(R.id.editmd_main);
                Snackbar.make(rootView, R.string.editmd_savesuccess, Snackbar.LENGTH_SHORT).show();
            } catch (Exception e) {
                FirebaseCrashlytics.getInstance().recordException(e);
                new MaterialAlertDialogBuilder(this)
                        .setTitle(this.getString(R.string.editmd_error))
                        .setMessage(this.getString(R.string.editmd_errormessage) + e.getMessage())
                        .setPositiveButton(R.string.dialog_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }
        }
    }

    private Uri getMediaStoreUri(String filePath) {
        Uri externalUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Audio.Media._ID};

        Cursor cursor = getContentResolver().query(
                externalUri,
                projection,
                MediaStore.Audio.Media.DATA + "=?",
                new String[]{filePath},
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
            cursor.close();
            return ContentUris.withAppendedId(externalUri, id);
        }

        if (cursor != null) cursor.close();
        return null;
    }


    public long getFilePathToMediaID (String songPath, Context context)
    {
        long id = 0;
        ContentResolver cr = context.getContentResolver();

        Uri uri = MediaStore.Files.getContentUri("external");
        String selection = MediaStore.Audio.Media.DATA;
        String[] selectionArgs = {songPath};
        String[] projection = {MediaStore.Audio.Media._ID};
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        Cursor cursor = cr.query(uri, projection, selection + "=?", selectionArgs, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int idIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
                id = Long.parseLong(cursor.getString(idIndex));
            }
        }

        return id;
    }



    private void saveButtonActiveManager(View v) {
        if (sourceTrackGenre != null && !sourceTrackGenre.equals(genre.getText().toString()) || sourceTrackTitle != null && !sourceTrackTitle.equals(title.getText().toString()) || sourceTrackArtist != null && !sourceTrackArtist.equals(artist.getText().toString()) || sourceTrackAlbum != null && !sourceTrackAlbum.equals(album.getText().toString()) || sourceTrackYear!= null && !sourceTrackYear.equals(year.getText().toString()) || sourceTrackLyricist!= null && !sourceTrackLyricist.equals(lyricist.getText().toString()) || sourceTrackGenre != null && !sourceTrackGenre.equals(genre.getText().toString().trim()) || sourceTrackComment != null && !sourceTrackComment.equals(comment.getText().toString()))
        {
            v.setEnabled(true);
            v.setClickable(true);
        }
        else {
            v.setEnabled(false);
            v.setClickable(false);
        }
    }


    private void loadMetadata(String path) {
        progressIndicator.setVisibility(View.VISIBLE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            progressIndicator.setVisibility(View.VISIBLE);
            scrollView.setVisibility(View.GONE);
            sourceTrackTitle = metadataManager.getTrackName(path);
            progressIndicator.setProgress(6);
            sourceTrackArtist = metadataManager.getTrackArtist(path);
            progressIndicator.setProgress(12);
            sourceTrackAlbum = metadataManager.getTrackAlbum(path);
            progressIndicator.setProgress(18);
            sourceTrackYear = metadataManager.getTrackYear(path);
            progressIndicator.setProgress(24);
            try {
                sourceTrackComment = metadataManager.getTrackComment(path);
            } catch (CannotReadException e) {
                throw new RuntimeException(e);
            } catch (TagException e) {
                throw new RuntimeException(e);
            } catch (InvalidAudioFrameException e) {
                throw new RuntimeException(e);
            } catch (ReadOnlyFileException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            progressIndicator.setProgress(30);
            try {
                sourceTrackGenre = metadataManager.getGenre(path);
            } catch (CannotReadException e) {
                throw new RuntimeException(e);
            } catch (TagException e) {
                throw new RuntimeException(e);
            } catch (InvalidAudioFrameException e) {
                throw new RuntimeException(e);
            } catch (ReadOnlyFileException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            progressIndicator.setProgress(36);
            try {
                sourceTrackLyricist = metadataManager.getLyricist(path);
            } catch (CannotReadException e) {
                throw new RuntimeException(e);
            } catch (TagException e) {
                throw new RuntimeException(e);
            } catch (InvalidAudioFrameException e) {
                throw new RuntimeException(e);
            } catch (ReadOnlyFileException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            progressIndicator.setProgress(42);
            sourceTrackArt = metadataManager.getTrackCover(path);
            progressIndicator.setProgress(48);
            SharedPreferences prefs = getSharedPreferences("userPrefs", Context.MODE_PRIVATE);
            int np_savedround = prefs.getInt("appearance_nowplaying_trackCoverRound", 16);
            String np_savedcolor = prefs.getString("appearance_nowplaying_trackCoverBackgroundColor", "#80b2b2b2");
            int np_savedcolor_parsed = Color.parseColor(np_savedcolor);

            GradientDrawable gGrawable = new GradientDrawable();
            gGrawable.setColor(np_savedcolor_parsed);
            gGrawable.setCornerRadius(np_savedround);

            handler.post(() -> {
                title.setText(sourceTrackTitle);
                progressIndicator.setProgress(54);
                artist.setText(sourceTrackArtist);
                progressIndicator.setProgress(60);
                album.setText(sourceTrackAlbum);
                progressIndicator.setProgress(66);
                year.setText(sourceTrackYear);
                progressIndicator.setProgress(72);
                comment.setText(sourceTrackComment);
                progressIndicator.setProgress(78);
                lyricist.setText(sourceTrackLyricist);
                progressIndicator.setProgress(85);
                genre.setText(sourceTrackGenre);
                if (sourceTrackArt != null) {
                    RoundedBitmapDrawable rbd = RoundedBitmapDrawableFactory.create(getResources(), sourceTrackArt);
                    rbd.setCornerRadius(np_savedround);
                    tArt.setImageDrawable(rbd);
                    progressIndicator.setProgress(100);
                }
                else {
                    tArt.setImageDrawable(null);
                    progressIndicator.setProgress(100);
                }
                scrollView.setVisibility(View.VISIBLE);
                progressIndicator.setVisibility(View.GONE);
            });
        });
    }


    public static String getDeviceInfo() {
        StringBuilder deviceInfo = new StringBuilder();

        String deviceModel = Build.MODEL;
        String deviceCodename = Build.DEVICE;
        deviceInfo.append("Device Model: ").append(deviceModel)
                .append(" (").append(deviceCodename).append(")\n");
        String locale = Locale.getDefault().toString();
        deviceInfo.append("Locale: ").append(locale).append("\n");
        String androidVersion = Build.VERSION.RELEASE;
        deviceInfo.append("Android Version: ").append(androidVersion).append("\n");
        String securityPatch = Build.VERSION.SECURITY_PATCH;
        deviceInfo.append("Security Patch: ").append(securityPatch).append("\n");
        return deviceInfo.toString();
    }


    public static String getAppInfo(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();

            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);

            String versionName = packageInfo.versionName;
            int versionCode = packageInfo.versionCode;
            String pkgName = context.getPackageName();

            StringBuilder appInfo = new StringBuilder();
            appInfo.append(pkgName).append(", \n")
                    .append(versionName).append(", \n")
                    .append(versionCode).append("");


            return appInfo.toString();

        } catch (PackageManager.NameNotFoundException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            return "Error fetching app info.";
        }
    }
}