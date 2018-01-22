package com.cbapps.kempengemeenten.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.cb.kempengemeenten.R;
import com.cbapps.kempengemeenten.util.CoordinateConverter;
import com.cbapps.kempengemeenten.GeofenceTransitionListener;
import com.cbapps.kempengemeenten.callback.OnMapActionListener;
import com.cbapps.kempengemeenten.database.LmsDatabase;
import com.cbapps.kempengemeenten.database.LmsPoint;
import com.cbapps.kempengemeenten.util.PermissionManager;
import com.cbapps.kempengemeenten.util.RDToWGS84Converter;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
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

public class MapFragment extends DialogFragment implements OnMapReadyCallback {

	public static final String TAG = "MapFragment";
	public static final String EXTRA_SHOW_LMS_DETAIL = "show-lms-point-detail";
	private static final CoordinateConverter CONVERTER = new RDToWGS84Converter();

	/**The default geofencing radius in meters.*/
	private static final int GEOFENCE_RADIUS = 100;
	/**The expiration time of geofences in miliseconds.*/
	private static final int GEOFENCE_EXPIRATION = 12 * 3600 * 1000;

	private static ExecutorService service;
	private Handler handler;
	private GoogleMap googleMap;
	private List<Marker> markers;
	private LiveData<LmsPoint> observedPoint;
	private BottomSheetBehavior bottomSheetBehavior;
	private LmsDetailFragment detailFragment;

	private LmsPoint shownPoint;
	private List<Polyline> shownPolyLine;

	private OnMapActionListener listener;

	public MapFragment() {
		service = Executors.newCachedThreadPool();
		handler = new Handler();
		shownPolyLine = new ArrayList<>();
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
		if (from == null || to == null)
			return;
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

				shownPolyLine.add(googleMap.addPolyline(new PolylineOptions()
						.addAll(PolyUtil.decode(encodedPoly))
						.color(ContextCompat.getColor(getContext(), R.color.colorAccent))));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}, error -> Log.w(TAG, "Too bad!")));
	}

	public boolean onBackPressed() {
		switch (bottomSheetBehavior.getState()) {
			case BottomSheetBehavior.STATE_DRAGGING:
			case BottomSheetBehavior.STATE_SETTLING:
				return true;
			case BottomSheetBehavior.STATE_EXPANDED:
				bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
				return true;
			case BottomSheetBehavior.STATE_COLLAPSED:
				bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
				return true;
			case BottomSheetBehavior.STATE_HIDDEN:
			default:
				return false;
		}
	}

	@Override
	public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
		View view = layoutInflater.inflate(R.layout.map_layout, viewGroup, false);

		SupportMapFragment map = new SupportMapFragment();

		if (getActivity() == null)
			throw new IllegalStateException("No activity?!");

		getActivity().getSupportFragmentManager().beginTransaction()
				.replace(R.id.mapFrame, map, "GoogleMap")
				.commit();
		map.getMapAsync(this);

		detailFragment = new LmsDetailFragment();
		getActivity().getSupportFragmentManager().beginTransaction()
				.replace(R.id.bottom_sheet_frame, detailFragment, "Detail")
				.commit();

		DisplayMetrics metrics = getResources().getDisplayMetrics();

		bottomSheetBehavior = BottomSheetBehavior.from(view.findViewById(R.id.bottom_sheet));
		bottomSheetBehavior.setHideable(true);
		bottomSheetBehavior.setPeekHeight((int) (90 * metrics.density));
		bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

		if (bundle != null) {
			shownPoint = (LmsPoint) bundle.getSerializable(MapFragment.EXTRA_SHOW_LMS_DETAIL);
		}

		return view;
	}

	//Permission is taken care of, but lint doesn't believe it.
	@SuppressLint("MissingPermission")
	@Override
	public void onMapReady(GoogleMap googleMap) {
		this.googleMap = googleMap;
		Observer<LmsPoint> observer = marker -> showLmsDetail(marker, true);

		googleMap.setOnMarkerClickListener(marker -> {
			if (observedPoint != null)
				observedPoint.removeObserver(observer);
			observedPoint = LmsDatabase.newInstance(getContext()).lmsDao()
					.findLiveByLmsNumber((int) marker.getTag());
			observedPoint.observe(this, observer);
			return false;
		});

		googleMap.setOnMapClickListener(latLng -> {
			handler.post(() -> {
				if (observedPoint != null)
					observedPoint.removeObserver(observer);
				observedPoint = null;
				showLmsDetail(null, false);
			});
		});

		service.submit(() -> {
			List<LmsPoint> lmsPoints = LmsDatabase.newInstance(getContext()).lmsDao().getAll();
			handler.post(() -> {
				createLmsMarkers(googleMap, lmsPoints);
				if (shownPoint == null) {
					LatLng pos = fetchLastLocation();
					if (pos != null)
						googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 10));
				} else {
					showLmsDetail(shownPoint, false);
				}
				startGeofencing(getContext(), lmsPoints);
			});
		});

		PermissionManager.requestPermission(getActivity(), PermissionManager.READ_PERMISSION_REQUEST_CODE, (requestCode, results) -> {
			for (PermissionManager.PermissionResult result : results) {
				if (result.getPermission().equals(Manifest.permission.ACCESS_FINE_LOCATION) &&
						result.isGranted()) {

					googleMap.setMyLocationEnabled(true);
					LatLng pos = fetchLastLocation();
					if (pos != null)
						googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 10));
				}
			}
		}, Manifest.permission.ACCESS_FINE_LOCATION);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(MapFragment.EXTRA_SHOW_LMS_DETAIL, shownPoint);
	}

	public void setOnMapActionListener(OnMapActionListener listener) {
		this.listener = listener;
	}

	public void showLmsDetail(LmsPoint point, boolean animateCamera) {
		for (Polyline polyline : shownPolyLine)
			polyline.remove();

		shownPoint = point;
		if (listener != null)
			listener.onLmsPointSelected(point);

		if (point == null) {
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
		} else {
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
			detailFragment.showDetail(point);
		}

		if (point == null)
			return;
		if (googleMap == null)
			return;

		if (animateCamera)
			googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(CONVERTER.toLatLng(point.getRdX(), point.getRdY()), 14));
		else
			googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(CONVERTER.toLatLng(point.getRdX(), point.getRdY()), 14));

		for (Marker marker : markers) {
			if (marker.getTag() != null && ((int) marker.getTag()) == point.getLmsNumber()) {
				marker.showInfoWindow();
				marker.setIcon(BitmapDescriptorFactory.defaultMarker(point.isMeasured() ?
						BitmapDescriptorFactory.HUE_GREEN : BitmapDescriptorFactory.HUE_RED));
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

		Intent intent = new Intent(context, GeofenceTransitionListener.class);
		geofencingClient.addGeofences(new GeofencingRequest.Builder()
				.addGeofences(fences)
				.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
				.build(),
				PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))
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
}
