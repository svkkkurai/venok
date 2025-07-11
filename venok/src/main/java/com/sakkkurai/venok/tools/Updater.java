package com.sakkkurai.venok.tools;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.sakkkurai.venok.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Updater {
    private final static String updaterLink = "https://raw.githubusercontent.com/svkkkurai/venokUpdates/main/update.json";
    private static final String TAG = "Updater";
    public static final String jsonName = "update.json";
    public static final String apkName = "update.apk";
    public static final int REQUEST_INSTALL_UNKNOWNSOURCES = 16092008;
    public static void checkUpdates(Context context) throws PackageManager.NameNotFoundException {
        File file = new File(context.getFilesDir(), jsonName);
        file.delete();
        // if (BuildConfig.DEBUG) return;
        AtomicInteger versionCode = new AtomicInteger();
        new Thread(()-> {
            // Saving application versionCode
            PackageManager pm = context.getPackageManager();
            try {
                PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
                versionCode.set(pi.versionCode);
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }

            downloadFile(context, updaterLink, jsonName, new DownloadCallback() {
                @Override
                public void onSuccess(File file) {
                    Log.d(TAG, "Download json success");
                    if (getType(file) == types.json) {
                        Gson json = new GsonBuilder().setPrettyPrinting().create();
                        try {
                            FileReader reader = new FileReader(file);
                            UpdateConfig config = json.fromJson(reader, UpdateConfig.class);
                            reader.close();

                            Log.d(TAG, "Local versionCode = " + versionCode.get() + ", Remote versionCode = " + config.versionCode);
                            if (versionCode.get() == config.versionCode) {
                                Log.d(TAG, "Version from json and this application are same, update skipping.");
                            } else  {
                                Log.d(TAG, "Different versions, showing update dialog.");
                                String lang = Locale.getDefault().getLanguage();
                                String localizedChangelog = config.changelog.getOrDefault(lang, config.changelog.get("en"));
                                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                                builder
                                        .setTitle(R.string.updater_updateavailable_title)
                                        .setMessage(context.getResources().getString(R.string.updater_updateavailable) + localizedChangelog)
                                        .setNegativeButton(R.string.close, (dialog, which) -> { dialog.dismiss(); })
                                        .setPositiveButton(R.string.update, (dialog, which) -> {
                                            File apk = new File(context.getFilesDir(), apkName);
                                            PackageManager pm = context.getPackageManager();
                                            PackageInfo info = pm.getPackageArchiveInfo(apk.getAbsolutePath(), 0);

                                            if (apk.exists() && apk.length() > 0 && info.versionCode == config.versionCode) {
                                                installApk(context, apk);
                                            } else {
                                                Log.d(TAG, "APK not found or corrupted. Downloading...");
                                                downloadFile(context, config.downloadLink, apkName, new DownloadCallback() {
                                                    @Override
                                                    public void onSuccess(File file) {
                                                        Log.d(TAG, "APK downloaded. Installing...");
                                                        installApk(context, file);
                                                    }

                                                    @Override
                                                    public void onError(Exception e) {
                                                        Log.e(TAG, "Download failed: " + e);
                                                    }
                                                });
                                            }

                                        })
                                        .show();
                            }

                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Download failed. " + e);
                }
            });


        }).start();
    }

    public static void downloadFile(Context context, String link, String fileName, DownloadCallback dlCallback) {
        new Thread(() -> {
            try {
                URL url = new URL(link);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                InputStream inputStream = connection.getInputStream();
                File file = new File(context.getFilesDir(), fileName);
                File apk = new File(context.getFilesDir(), apkName);
                apk.delete();
                FileOutputStream outputStream = new FileOutputStream(file);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
                inputStream.close();
                connection.disconnect();

                new Handler(Looper.getMainLooper()).post(() -> dlCallback.onSuccess(file));

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> dlCallback.onError(e));
            }
        }).start();
    }


    public static void installApk(Context context, File file) {
        if (!context.getPackageManager().canRequestPackageInstalls()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:" + context.getPackageName()));
            if (context instanceof Activity) {
                ((Activity) context).startActivityForResult(intent, REQUEST_INSTALL_UNKNOWNSOURCES);
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
            return;
        }
        Uri apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setData(apkUri);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(intent);
    }

    public static types getType(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        String format = (lastDot == -1) ? "" : name.substring(lastDot + 1).toLowerCase();

        switch (format) {
            case "json":
                return types.json;
            case "apk":
                return types.apk;
            default:
                return null;
        }
    }

    public class UpdateConfig {
        @SerializedName("versionCode")
        public int versionCode;
        @SerializedName("downloadLink")
        public String downloadLink;
        @SerializedName("changelog")
        public Map<String, String> changelog;
    }


    private interface DownloadCallback {
        void onSuccess(File file);
        void onError(Exception e);
    }

    private enum types {
        apk,
        json
    }
}
