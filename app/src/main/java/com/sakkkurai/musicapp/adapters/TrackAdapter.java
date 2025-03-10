package com.sakkkurai.musicapp.adapters;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sakkkurai.musicapp.R;
import com.sakkkurai.musicapp.callback.MetadataManager;
import com.sakkkurai.musicapp.database.QueueDatabase;
import com.sakkkurai.musicapp.database.dao.QueueDao;
import com.sakkkurai.musicapp.models.Track;
import com.sakkkurai.musicapp.services.MusicService;
import com.sakkkurai.musicapp.ui.activities.EditMetadataActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.ViewHolder> {

    private Context context;
    private ArrayList<Track> tracks;
    int np_savedround, np_savedcolor_parsed;
    String np_savedcolor;
    int playreason;
    private QueueDao queueDao;
    private QueueDatabase qDB;
    private Activity activity;
    public TrackAdapter(Context context, ArrayList<Track> tracks, int playreason, Activity activity) {
        this.context = context;
        this.tracks = tracks;
        this.playreason = playreason;
        this.activity = activity;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_music, parent, false);
        qDB = QueueDatabase.getInstance(context);
        queueDao = qDB.queueDao();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Track currentTrack = tracks.get(position);

        holder.trackName.setText(currentTrack.getTrackName());
        holder.artistName.setText(currentTrack.getArtistName());
        holder.albumName.setText(currentTrack.getAlbumName());
        holder.duration.setText(currentTrack.getFormattedDuration());
        holder.trackInfo.setOnClickListener(v -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(v.getContext());
            Bitmap trackArtBm = getTrackArt(currentTrack.getAudioPath());
            if (trackArtBm != null) {
                builder.setIcon(new BitmapDrawable(context.getResources(), trackArtBm));
            }
            else {
                builder.setIcon(R.drawable.setup_info);
            }
            String[] music_items = v.getResources().getStringArray(R.array.music_options_menu);
            builder.setItems(music_items, (((dialog, which) -> {
                        try {
                            musicItemsMenuHandler(which, tracks.get(position));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })))
                    .setTitle(tracks.get(position).getTrackName())
                    .show();

        });

        SharedPreferences prefs = context.getSharedPreferences("userPrefs", Context.MODE_PRIVATE);
        np_savedround = prefs.getInt("cornerradius_library", activity.getResources().getInteger(R.integer.cornerradius_library));
        np_savedcolor = prefs.getString("appearance_nowplaying_trackCoverBackgroundColor", "#80b2b2b2");
        np_savedcolor_parsed = Color.parseColor(np_savedcolor);

        GradientDrawable gGrawable = new GradientDrawable();
        gGrawable.setColor(np_savedcolor_parsed);

        getTrackArtAsync(currentTrack, holder.trackImage);


        holder.itemView.setOnClickListener( v -> {
            playMusic(position);

        });
    }

    private void musicItemsMenuHandler(int which, Track track) throws IOException {
        switch (which) {
            case 0:
                Bitmap trackArtBm = getTrackArt(track.getAudioPath());
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                if (trackArtBm != null) {
                    builder.setIcon(new BitmapDrawable(context.getResources(), trackArtBm));
                }
                else {
                    Log.d("SongInfo", "Cover not found. Path: " + track.getAudioPath());
                    builder.setIcon(R.drawable.setup_info);
                }
                        builder.setNegativeButton(R.string.music_edit, (dialog, which1) -> {
                                    Intent intent = new Intent(context, EditMetadataActivity.class);
                                    intent.putExtra("PATH", track.getAudioPath());
                                    context.startActivity(intent);
                        })
                            .setPositiveButton(R.string.music_done, (dialog, which1) -> {
                                dialog.dismiss();
                            })
                            .setCancelable(true)
                            .setMessage(android.text.Html.fromHtml(
                                    "<b>" + context.getString(R.string.music_songinfo_title) + "</b> " + track.getTrackName() + "<br>" +
                                            "<b>" + context.getString(R.string.music_songinfo_artist) + "</b> " + track.getArtistName() + "<br>" +
                                            "<b>" + context.getString(R.string.music_songinfo_album) + "</b> " + track.getAlbumName() + "<br>" +
                                            "<b>" + context.getString(R.string.music_songinfo_duration) + "</b> " + track.getFormattedDuration() + "<br>" +
                                            "<b>" + context.getString(R.string.music_songinfo_location) + "</b> " + track.getAudioPath() + "<br>" +
                                            "<b>" + context.getString(R.string.music_songinfo_size) + "</b> " + track.getAudioSize() + " " +
                                            context.getString(R.string.music_songinfo_megabytes) + "<br>" +
                                            "<b>" + context.getString(R.string.music_songinfo_bitrate) + " </b>" + track.getBitrate() + " " +
                                            context.getString(R.string.music_songinfo_bps)

                            ))
                            .setTitle(context.getString(R.string.music_songinfo_dialog_title))
                        .show();
                break;
            case 1:
                Intent intent = new Intent(context, EditMetadataActivity.class);
                intent.putExtra("PATH", track.getAudioPath());
                context.startActivity(intent);
                break;
            case 2:
                File file = new File(track.getAudioPath());
                String name = track.getTrackName();
                MaterialAlertDialogBuilder builder1 = new MaterialAlertDialogBuilder(context);
                builder1.setTitle(R.string.music_songinfo_delete_title)
                        .setMessage(context.getString(R.string.music_songinfo_delete_confirmation, name))
                        .setCancelable(true)
                        .setPositiveButton(R.string.music_songinfo_delete, (dialog, which1) -> {
                            MetadataManager mdg = new MetadataManager(this.context);
                            mdg.deleteTrack(track.getAudioPath(), this.context);
                        })
                        .setNegativeButton(R.string.music_songinfo_delete_cancel, (dialog, which1) -> {
                            dialog.dismiss();
                        })
                        .show();
                break;
            default:
                break;
        }
    }

    private void playMusic(int pos) {
            SharedPreferences prefs = context.getSharedPreferences("queue", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<?> dbOperation = executor.submit(() -> {
                Log.d("QueueUpdate", "Starting DB operations");
                queueDao.deleteAllTracks();
                queueDao.createQueue(new ArrayList<>(tracks));
                Log.d("QueueUpdate", "DB operations completed");
            });

            try {
                dbOperation.get();
                editor.putInt("queuePosition", pos);
                editor.apply();
                Intent startPlayback = new Intent(context, MusicService.class);
                startPlayback.putExtra("reason", "START_PLAYBACK_ADAPTER");
                activity.startForegroundService(startPlayback);
                BottomNavigationView navbar = activity.findViewById(R.id.main_navbar);
                if (navbar != null) {
                    navbar.setSelectedItemId(R.id.id_navbar_nowplaying);
                } else {
                    Log.w("NowPlayingFragment", "BottomNavigationView not found");
                }
            } catch (InterruptedException | ExecutionException e) {
                Log.e("QueueUpdate", "Error during DB operation", e);
            } finally {
                executor.shutdown();
            }
    }

    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        SharedPreferences prefs = context.getSharedPreferences("userPrefs", MODE_PRIVATE);
        boolean advancedDebug = prefs.getBoolean("debug_use.extended.debug", false);
        if (advancedDebug) {
            Toast.makeText(context, context.getString(R.string.track_adapter_debug_initwith_toast, tracks.size()), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    private Bitmap getTrackArt(String path) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        Bitmap albumArt = null;

        try {
            retriever.setDataSource(path);
            byte[] artBytes = retriever.getEmbeddedPicture();
            if (artBytes != null) {
                albumArt = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.length);
            }
        } catch (Exception e) {
            Log.e("Library", "Failed to get album art: " + e.getMessage());
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return albumArt;
    }

    private void getTrackArtAsync(Track currentTrack, ImageView trackImage) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            Bitmap trackArt = getTrackArt(currentTrack.getAudioPath());
            new Handler(Looper.getMainLooper()).post(() -> {
                if (trackArt != null) {
                    RoundedBitmapDrawable rbd = RoundedBitmapDrawableFactory.create(context.getResources(), trackArt);
                    rbd.setCornerRadius(np_savedround);
                    trackImage.setImageDrawable(rbd);
                } else {
                    trackImage.setImageDrawable(null);
                }
            });
        });
    }




    public static class ViewHolder extends RecyclerView.ViewHolder {


        ImageView trackImage;
        TextView trackName;
        TextView artistName;
        TextView albumName;
        TextView duration;
        ImageButton trackInfo;

        public ViewHolder(View itemView) {
            super(itemView);
            trackImage = itemView.findViewById(R.id.music_trackImage);
            trackName = itemView.findViewById(R.id.music_trackName);
            artistName = itemView.findViewById(R.id.music_artistName);
            albumName = itemView.findViewById(R.id.music_albumName);
            duration = itemView.findViewById(R.id.music_trackDuration);
            trackInfo = itemView.findViewById(R.id.music_optionsButton);

        }
    }
}
