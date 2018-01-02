package com.cbapps.kempengemeenten.nextgen.callback;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.util.Log;

import com.cb.kempengemeenten.R;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

/**
 * @author CoenB95
 */

public class GeofenceTransitionsIntentService extends BroadcastReceiver {

	private static final String TAG ="";
	public GeofenceTransitionsIntentService(String name) {
		super(name);
	}

	protected void onHandleIntent(Intent intent) {
		GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
		if (geofencingEvent.hasError()) {
			int errorMessage = geofencingEvent.getErrorCode();
			Log.e(TAG, String.valueOf(errorMessage));
			return;
		}

		// Get the transition type.
		int geofenceTransition = geofencingEvent.getGeofenceTransition();

		// Test that the reported transition was of interest.
		if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
				geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

			// Get the geofences that were triggered. A single event can trigger
			// multiple geofences.
			List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

			// Get the transition details as a String.
			String geofenceTransitionDetails = getGeofenceTransitionDetails(
					this,
					geofenceTransition,
					triggeringGeofences
			);

			// Send notification and log the transition details.
			sendNotification(geofenceTransitionDetails);
			Log.i(TAG, geofenceTransitionDetails);
		} else {
			// Log the error.
			Log.e(TAG, getString(R.string.geofence_transition_invalid_type,
					geofenceTransition));
		}
	}
