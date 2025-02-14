package com.sakkkurai.musicapp.callback;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class EmailSender {
    private Context context;
    public EmailSender(Context context) {
        this.context = context;
    }

    public void sendEmail(String sub, String body, String[] sup_email) {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
        emailIntent.putExtra(Intent.EXTRA_EMAIL, sup_email);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, sub);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);
        if (emailIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(emailIntent);
        }
    }
}
