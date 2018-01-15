package com.cbapps.kempengemeenten;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.cb.kempengemeenten.R;
import com.cbapps.kempengemeenten.database.LmsPoint;

/**
 * @author CoenB95
 */

public class OreoNotificationManager {

	private static final String GEOFENCE_CHANNEL_ID = "geofence-channel";

	public static final String NEARBY_LMS_POINT = "nearby-lms-point";

	public static void createNearbyPointNotification(Context context, LmsPoint point) {
		if (context == null)
			return;

		if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("showNotifications", true))
			return;

		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		if (manager != null) {
			Intent intent = new Intent(context, MainActivity.class);
			intent.putExtra(NEARBY_LMS_POINT, point);
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

			Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

			Notification notification = new NotificationCompat.Builder(context, GEOFENCE_CHANNEL_ID)
					.setContentTitle(context.getString(R.string.geofence_notification_title))
					.setContentText(context.getString(R.string.geofence_notification_summary))
					.setContentIntent(pendingIntent)
					.setSmallIcon(R.drawable.logo)
					.setSound(alarmSound)
					.build();
			manager.notify(0, notification);
		}
	}

	private static void createNotificationChannels(Context context) {
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
			//No need for channels. Or better: just can't create them.
			return;
		}

		NotificationManager manager = (NotificationManager)
				context.getSystemService(Context.NOTIFICATION_SERVICE);

		if (manager != null) {
			//Channel 1: Geofences.
			NotificationChannel channel = new NotificationChannel(GEOFENCE_CHANNEL_ID,
					context.getString(R.string.geofence_channel_name),
					NotificationManager.IMPORTANCE_DEFAULT);
			channel.setDescription(context.getString(R.string.geofence_channel_description));
			channel.enableVibration(true);
			manager.createNotificationChannel(channel);
		}
	}

	public static void init(Context context) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (preferences.getBoolean("firstRun", true)) {
			//On the first ever run, setup the notification channels for Oreo.
			//This way they can manage the incoming notifications.
			createNotificationChannels(context);

			preferences.edit()
					.putBoolean("firstRun", false)
					.apply();
		}
	}
}
