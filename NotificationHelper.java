package com.example.deviceadmin;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;

import androidx.core.app.NotificationCompat;

public class NotificationHelper {

    private static final String CHANNEL_ID = "admin_events";
    private static int notifId = 1000;

    public static void createChannel(Context ctx) {
        NotificationChannel ch = new NotificationChannel(
            CHANNEL_ID,
            ctx.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        );
        ch.setDescription(ctx.getString(R.string.channel_desc));
        ch.setLightColor(Color.CYAN);
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(ch);
    }

    public static void show(Context ctx, String title, String body) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        nm.notify(notifId++, b.build());
    }
}
