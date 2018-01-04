package com.cbapps.kempengemeenten.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.cb.kempengemeenten.R;
import com.cbapps.kempengemeenten.MainActivity;
import com.cbapps.kempengemeenten.CoordinateConverter;
import com.cbapps.kempengemeenten.database.LmsDatabase;
import com.cbapps.kempengemeenten.database.LmsPoint;
import com.cbapps.kempengemeenten.PermissionManager;
import com.cbapps.kempengemeenten.RDToWGS84Converter;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author CoenB95
 */

public class MapFragment extends SupportMapFragment implements OnMapReadyCallback {

	public static final String TAG = "MapFragment";
	public static final String EXTRA_SHOW_LMS_DETAIL = "show-lms-point-detail";
	private static final String GEOFENCE_CHANNEL_ID = "geofence-channel";
	private static final CoordinateConverter CONVERTER = new RDToWGS84Converter();

	/**The default geofencing radius in meters.*/
	private static final int GEOFENCE_RADIUS = 500;
	/**The expiration time of geofences in miliseconds.*/
	private static final int GEOFENCE_EXPIRATION = 12 * 3600 * 1000;

	private static ExecutorService service;
	private Handler handler;
	private GoogleMap googleMap;
	private List<Marker> markers;
	private LiveData<LmsPoint> observedPoint;
	private Observer<LmsPoint> markerClickObserver;

	private LmsPoint shownPoint;
	private Polyline shownPolyLine;

	private OnLmsPointSelectedListener listener;

	public MapFragment() {
		service = Executors.newCachedThreadPool();
		handler = new Handler();
		getMapAsync(this);
	}

	private void createLmsMarkers(GoogleMap googleMap, List<LmsPoint> points) {
		if (markers != null) {
			for (Marker marker : markers)
				marker.remove();
		}
		markers = new ArrayList<>();
		for (LmsPoint point : points) {
			LatLng test = CONVERTER.toLatLng(point.getRdX(), point.getRdY());
			Marker marker = googleMap.addMarker(new MarkerOptions()
					.position(test)
					.icon(BitmapDescriptorFactory.defaultMarker(point.isMeasured() ?
							BitmapDescriptorFactory.HUE_GREEN : BitmapDescriptorFactory.HUE_RED))
					.title(point.getAddress())
					.snippet(point.getTown()));
			marker.setTag(point.getLmsNumber());
			markers.add(marker);
		}
	}

	private static void createNotification(Context context, LmsPoint point) {
		if (context == null)
			return;

		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		if (manager != null) {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
				NotificationChannel channel = new NotificationChannel(GEOFENCE_CHANNEL_ID,
						context.getString(R.string.geofence_channel_name),
						NotificationManager.IMPORTANCE_DEFAULT);
				channel.setDescription(context.getString(R.string.geofence_channel_description));
				channel.enableVibration(true);
				manager.createNotificationChannel(channel);
			}

			Intent resultIntent = new Intent(context, MainActivity.class);
			resultIntent.putExtra(EXTRA_SHOW_LMS_DETAIL, point);
			TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
			stackBuilder.addParentStack(MainActivity.class);
			stackBuilder.addNextIntent(resultIntent);
			PendingIntent resultPendingIntent = stackBuilder
					.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

			Notification notification = new NotificationCompat.Builder(context, GEOFENCE_CHANNEL_ID)
					.setContentTitle(context.getString(R.string.geofence_notification_title))
					.setContentText(context.getString(R.string.geofence_notification_summary))
					.setContentIntent(resultPendingIntent)
					.setSmallIcon(R.drawable.logo)
					.build();
			manager.notify(0, notification);
		}
	}

	public LatLng fetchLastLocation() {
		if (getContext() == null) {
			Log.d(TAG, "fetchLastLocation: no context");
			return null;
		}

		if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
				!= PackageManager.PERMISSION_GRANTED) {
			Log.d(TAG, "fetchLastLocation: no permission");
			return null;
		}

		LocationManager manager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
		if (manager == null) {
			Log.d(TAG, "fetchLastLocation: no manager");
			return null;
		}

		Location location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (location == null) {
			Log.d(TAG, "fetchLastLocation: no known location");
			return null;
		}
		return new LatLng(location.getLatitude(), location.getLongitude());
	}

	private void fetchAndDisplayDirections(LatLng from, LatLng to) {

		String apiKey = getString(R.string.google_maps_key);
		RequestQueue queue = Volley.newRequestQueue(getContext());
		queue.add(new JsonObjectRequest(Request.Method.GET,
				"https://maps.googleapis.com/maps/api/directions/json?" +
						"origin=" + from.latitude + "," + from.longitude +
						"&destination=" + to.latitude + "," + to.longitude +
						"&mode=driving" +
						"&key=" + apiKey, null, response -> {
			Log.w(TAG, "We got a response!");
			try {
				String encodedPoly = response.getJSONArray("routes")
						.getJSONObject(0)
						.getJSONObject("overview_polyline")
						.getString("points");

				shownPolyLine = googleMap.addPolyline(new PolylineOptions()
						.addAll(PolyUtil.decode(encodedPoly))
						.color(ContextCompat.getColor(getContext(), R.color.colorAccent)));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}, error -> Log.w(TAG, "Too bad!")));
	}

	//Permission is taken care of, but lint doesn't believe it.
	@SuppressLint("MissingPermission")
	@Override
	public void onMapReady(GoogleMap googleMap) {
		this.googleMap = googleMap;
		Toast.makeText(getContext(), "Map initialized", Toast.LENGTH_SHORT).show();

		markerClickObserver = this::showLmsDetail;

		PermissionManager.requestPermission(getActivity(), PermissionManager.READ_PERMISSION_REQUEST_CODE, (requestCode, results) -> {
			for (PermissionManager.PermissionResult result : results) {
				if (result.getPermission().equals(Manifest.permission.ACCESS_FINE_LOCATION) &&
						result.isGranted()) {
					Toast.makeText(getContext(), "Location granted!", Toast.LENGTH_SHORT).show();

					googleMap.setMyLocationEnabled(true);
					LatLng pos = fetchLastLocation();
					if (pos != null)
						googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 10));
					googleMap.setOnMarkerClickListener(marker -> {
						if (observedPoint != null)
							observedPoint.removeObserver(markerClickObserver);
						observedPoint = LmsDatabase.newInstance(getContext()).lmsDao().findByLmsNumber((int) marker.getTag());
						observedPoint.observe(this, markerClickObserver);
						return false;
					});
					googleMap.setOnMapClickListener(latLng -> {
						if (observedPoint != null)
							observedPoint.removeObserver(markerClickObserver);
						observedPoint = null;
						showLmsDetail(null);
					});

					service.submit(() -> {
						LmsDatabase.newInstance(getContext()).lmsDao().getAll().observe(this, lmsPoints -> {
							createLmsMarkers(googleMap, lmsPoints);
							startGeofencing(getContext(), lmsPoints);
						});
					});
					if (shownPoint != null)
						showLmsDetail(shownPoint);
				}
			}
		}, Manifest.permission.ACCESS_FINE_LOCATION);
	}

	public void setLmsPointSelectedListener(OnLmsPointSelectedListener listener) {
		this.listener = listener;
	}

	public void showLmsDetail(LmsPoint point) {
		if (shownPolyLine != null) {
			shownPolyLine.remove();
			shownPolyLine = null;
		}
		shownPoint = point;
		if (listener != null)
			listener.onLmsPointSelected(point);
		if (point == null)
			return;
		if (googleMap == null)
			return;
		googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(CONVERTER.toLatLng(point.getRdX(), point.getRdY()), 14));
		for (Marker marker : markers) {
			if (marker.getTag() != null && ((int) marker.getTag()) == point.getLmsNumber()) {
				marker.showInfoWindow();
				break;
			}
		}
		fetchAndDisplayDirections(fetchLastLocation(), CONVERTER.toLatLng(point.getRdX(), point.getRdY()));
	}

	private static void startGeofencing(Context context, Collection<LmsPoint> points) {
		if (context == null)
			return;

		GeofencingClient geofencingClient = LocationServices.getGeofencingClient(context);
		if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
				PackageManager.PERMISSION_GRANTED)
			return;

		List<Geofence> fences = new ArrayList<>();
		for (LmsPoint point : points) {
			LatLng latLng = CONVERTER.toLatLng(point.getRdX(), point.getRdY());
			fences.add(new Geofence.Builder()
					.setRequestId(String.valueOf(point.getLmsNumber()))
					.setCircularRegion(latLng.latitude, latLng.longitude, GEOFENCE_RADIUS)
					.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
					.setExpirationDuration(GEOFENCE_EXPIRATION)
					.build());
		}

		if (fences.isEmpty())
			return;

		Intent intent = new Intent(context, GeofenceTransitionBroadcastReceiver.class);
		geofencingClient.addGeofences(new GeofencingRequest.Builder()
				.addGeofences(fences)
				.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
				.build(), PendingIntent.getBroadcast(context, 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT))
				.addOnFailureListener(runnable -> {
					new AlertDialog.Builder(context)
							.setTitle(R.string.google_location_required_title)
							.setMessage(R.string.google_location_required_description)
							.setPositiveButton(R.string.google_location_required_ok, (dialogInterface, i) -> {
								context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
							})
							.setNegativeButton(R.string.cancel, null)
							.show();
				});
	}

	public static class GeofenceTransitionBroadcastReceiver extends BroadcastReceiver {

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
				if (service == null)
					return;
				service.submit(() -> {
					for (Geofence fence : triggeringGeofences) {
						LmsPoint p = LmsDatabase.newInstance(context).lmsDao()
								.findByLmsNumber(Integer.parseInt(fence.getRequestId())).getValue();

						createNotification(context, p);
					}
				});
			}
		}
	}

	public interface OnLmsPointSelectedListener {
		void onLmsPointSelected(LmsPoint point);
	}
}
