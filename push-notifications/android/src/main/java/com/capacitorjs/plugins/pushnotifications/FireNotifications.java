package com.capacitorjs.plugins.pushnotifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import android.app.NotificationManager;

import com.google.firebase.messaging.CommonNotificationBuilder;
import com.google.firebase.messaging.RemoteMessage;

public class FireNotifications {
    public FireNotifications(Context context, RemoteMessage remoteMessage) {
        Bundle bundle = null;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                ApplicationInfo applicationInfo = context
                        .getPackageManager()
                        .getApplicationInfo(
                                context.getPackageName(),
                                PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA)
                        );
                bundle = applicationInfo.metaData;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            bundle = context.getApplicationInfo().metaData;
        }

        String channelId = CommonNotificationBuilder.getOrCreateChannel(
                context,
                remoteMessage.getData().get("channelId"),
                bundle
        );

        Intent notificationIntent = context.
                getPackageManager()
                .getLaunchIntentForPackage(context.getPackageName())
                .setPackage(null)
                .setAction(Intent.ACTION_VIEW)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        notificationIntent.putExtra("title", remoteMessage.getData().get("title"));
        notificationIntent.putExtra("message", remoteMessage.getData().get("message"));
        notificationIntent.putExtra("google.message_id", remoteMessage.getMessageId());

        PendingIntent pendingNotificationIntent = PendingIntent.getActivity(context,123,notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PushNotificationImages bitmapImage = new PushNotificationImages(remoteMessage.getData().get("image"));
        // Process Image, http caller should not be the main thread
        Thread processImage = new Thread(bitmapImage);
        processImage.start();

        // To fire action event for the Pushnotification event listner function
        NotificationCompat.Action clickAction = new NotificationCompat.Action(context.getApplicationInfo().icon, remoteMessage.getData().get("title"), pendingNotificationIntent);

        try {
            processImage.join();
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context.getApplicationContext(), channelId)
                    .setSmallIcon(context.getApplicationInfo().icon)
                    .setContentTitle(remoteMessage.getData().get("title"))
                    .setContentText(remoteMessage.getData().get("message"))
                    .setLargeIcon(bitmapImage.myBitmap)
                    .setAutoCancel(true)
                    .setContentIntent(pendingNotificationIntent);
            notificationManager.notify(remoteMessage.getData().get("tag"), Integer.parseInt(remoteMessage.getData().get("id")), mBuilder.build());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
