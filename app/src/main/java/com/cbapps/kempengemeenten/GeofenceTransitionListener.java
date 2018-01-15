package com.cbapps.kempengemeenten;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AlertDialog;

import com.cbapps.kempengemeenten.database.LmsDatabase;
import com.cbapps.kempengemeenten.database.LmsPoint;
import com.cbapps.kempengemeenten.util.OreoNotificationManager;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author CoenB95
 */
public class GeofenceTransitionListener extends BroadcastReceiver {


	@Override
	public void onReceive(Context context, Intent intent) {
		GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
		if (geofencingEvent.hasError()) {
			String errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode());
			new AlertDialog.Builder(context)
					.setTitle("Error")
					.setMessage(errorMessage)
					.show();
			return;
		}

		// Get the transition type.
		int geofenceTransition = geofencingEvent.getGeofenceTransition();

		// Test that the reported transition was of interest.
		if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {

			// Get the geofences that were triggered. A single event can trigger
			// multiple geofences.
			List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

			ExecutorService service = Executors.newFixedThreadPool(1);
			service.submit(() -> {
				for (Geofence fence : triggeringGeofences) {
					LmsPoint p = LmsDatabase.newInstance(context).lmsDao()
							.findByLmsNumber(Integer.parseInt(fence.getRequestId()));

					OreoNotificationManager.createNearbyPointNotification(context, p);
				}
			});
		}
	}
}
