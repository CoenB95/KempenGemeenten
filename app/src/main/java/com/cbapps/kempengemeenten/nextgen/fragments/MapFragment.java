package com.cbapps.kempengemeenten.nextgen.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.cb.kempengemeenten.R;
import com.cbapps.kempengemeenten.nextgen.CoordinateConverter;
import com.cbapps.kempengemeenten.nextgen.database.LmsDatabase;
import com.cbapps.kempengemeenten.nextgen.database.LmsPoint;
import com.cbapps.kempengemeenten.nextgen.PermissionManager;
import com.cbapps.kempengemeenten.nextgen.RDToWGS84Converter;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author CoenB95
 */

public class MapFragment extends SupportMapFragment implements OnMapReadyCallback {

	public static final String TAG = "MapFragment";

	/**The default geofencing radius in meters.*/
	private static final int GEOFENCE_RADIUS = 100;

	private ExecutorService service;
	private Handler handler;
	private GoogleMap googleMap;
	private GeofencingClient geofencingClient;
	private List<LmsPoint> points;
	private CoordinateConverter converter;

	public MapFragment() {
		service = Executors.newCachedThreadPool();
		handler = new Handler();
		converter = new RDToWGS84Converter();
		getMapAsync(this);
	}

	//Permission is taken care of, but lint doesn't believe it.
	@SuppressLint("MissingPermission")
	@Override
	public void onMapReady(GoogleMap googleMap) {
		this.googleMap = googleMap;
		Toast.makeText(getContext(), "Map initialized", Toast.LENGTH_SHORT).show();
		PermissionManager.requestPermission(getActivity(), PermissionManager.READ_PERMISSION_REQUEST_CODE, (requestCode, results) -> {
			for (PermissionManager.PermissionResult result : results) {
				if (result.getPermission().equals(Manifest.permission.ACCESS_FINE_LOCATION) &&
						result.isGranted()) {
					Toast.makeText(getContext(), "Location granted!", Toast.LENGTH_SHORT).show();

					googleMap.setMyLocationEnabled(true);
					googleMap.setOnMarkerClickListener(marker -> {
						Toast.makeText(getContext(), marker.getSnippet() + " (lms " + marker.getTag() + ") clicked", Toast.LENGTH_SHORT).show();
						return false;
					});

					loadCSV(googleMap);
				}
			}
		}, Manifest.permission.ACCESS_FINE_LOCATION);
	}

	public void onPointSelected(LmsPoint point) {
		if (getContext() == null) {
			Log.d(TAG, "onPointSelected: no context");
			return;
		}

		if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
				!= PackageManager.PERMISSION_GRANTED) {
			Log.d(TAG, "onPointSelected: no permission");
			return;
		}

		LocationManager manager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
		if (manager == null) {
			Log.d(TAG, "onPointSelected: no manager");
			return;
		}

		Location location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (location == null) {
			Log.d(TAG, "onPointSelected: no known location");
			return;
		}

		LatLng latLng = converter.toLatLng(point.rdX, point.rdY);
		String apiKey = getString(R.string.google_maps_key);
		RequestQueue queue = Volley.newRequestQueue(getContext());
		queue.add(new JsonObjectRequest(Request.Method.GET,
				"https://maps.googleapis.com/maps/api/directions/json?" +
						"origin=" + location.getLatitude() + "," + location.getLongitude() +
						"&destination=" + latLng.latitude + "," + latLng.longitude +
						"&mode=driving" +
						"&key=" + apiKey, null, response -> {
			Log.w(TAG, "We got a response!");
			try {
				String encodedPoly = response.getJSONArray("routes")
						.getJSONObject(0)
						.getJSONObject("overview_polyline")
						.getString("points");

				googleMap.addPolyline(new PolylineOptions()
						.addAll(PolyUtil.decode(encodedPoly))
						.color(ContextCompat.getColor(getContext(), R.color.colorAccent)));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}, error -> {
			Log.w(TAG, "Too bad!");
		}));
	}

	private void loadCSV(GoogleMap googleMap) {
		service.submit(() -> {
			points = LmsDatabase.newInstance(getContext()).lmsDao().getAll();

			handler.post(() -> {
				for (LmsPoint point : points) {
					LatLng test = converter.toLatLng(point.rdX, point.rdY);
					Marker marker = googleMap.addMarker(new MarkerOptions()
							.position(test)
							.title(point.town)
							.snippet(point.address));
					marker.setTag(point.getLmsNumber());
				}

				startGeofencing();

				if (points.size() > 0)
					onPointSelected(points.get(0));
			});
		});
	}

	private void startGeofencing() {
		if (getContext() == null)
			return;

		geofencingClient = LocationServices.getGeofencingClient(getContext());
		if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) !=
				PackageManager.PERMISSION_GRANTED)
			return;

		List<Geofence> fences = new ArrayList<>();
		for (LmsPoint point : points) {
			LatLng latLng = converter.toLatLng(point.rdX, point.rdY);
			Geofence gf = new Geofence.Builder()
					.setRequestId(String.valueOf(point.getLmsNumber()))
					.setCircularRegion(latLng.latitude, latLng.longitude, GEOFENCE_RADIUS)
					.build();
		}

		Intent intent = new Intent(getContext(), GeofenceTransitionBroadcastReceiver.class);
		geofencingClient.addGeofences(new GeofencingRequest.Builder()
				.addGeofences(fences)
				.build(), PendingIntent.getBroadcast(getContext(), 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT));
	}

	private class GeofenceTransitionBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
			if (geofencingEvent.hasError()) {
				String errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode());
				Log.e(TAG, errorMessage);
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
//				String geofenceTransitionDetails = getGeofenceTransitionDetails(
//						this,
//						geofenceTransition,
//						triggeringGeofences
//				);

				// Send notification and log the transition details.
//				sendNotification(geofenceTransitionDetails);
//				Log.i(TAG, geofenceTransitionDetails);
			} else {
				// Log the error.
				Log.e(TAG, "geofence_transition_invalid_type: " + geofenceTransition);
			}
		}
	}
}
