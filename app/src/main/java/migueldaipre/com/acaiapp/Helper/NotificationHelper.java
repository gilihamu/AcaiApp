package migueldaipre.com.acaiapp.Helper;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.os.Build;

import migueldaipre.com.acaiapp.R;

public class NotificationHelper extends ContextWrapper {

    private static final String TBC_CHANNEL_ID = "migueldaipre.com.acaiapp.AcaiApp";
    private static final String TBC_CHANNEL_NAME = "ACAI APP CLIENT";

    private NotificationManager manager;

    public NotificationHelper(Context base) {
        super(base);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            createChannel();
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createChannel() {
        NotificationChannel tbcChannel = new NotificationChannel(TBC_CHANNEL_ID,TBC_CHANNEL_NAME,NotificationManager.IMPORTANCE_DEFAULT);
        tbcChannel.enableLights(false);
        tbcChannel.enableVibration(true);
        tbcChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        getManager().createNotificationChannel(tbcChannel);
    }

    public NotificationManager getManager() {
        if (manager == null)    {
            manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
    }

    @TargetApi(Build.VERSION_CODES.O)
    public Notification.Builder getEatItChannelNotification(String title, String body, PendingIntent contentIntent, Uri soundUri)   {

        return new Notification.Builder(getApplicationContext(),TBC_CHANNEL_ID).setContentIntent(contentIntent)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setSound(soundUri)
                .setAutoCancel(false);
    }
}
