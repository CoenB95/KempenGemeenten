package com.cbapps.kempengemeenten.nextgen.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceManager;
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
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author CoenB95
 */

public class MapFragment extends SupportMapFragment implements OnMapReadyCallback {

	public static final String TAG = "MapFragment";

	private ExecutorService service;
	private Handler handler;
	private GoogleMap googleMap;

	public MapFragment() {
		service = Executors.newCachedThreadPool();
		handler = new Handler();
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

		CoordinateConverter cc = new RDToWGS84Converter();
		LatLng latLng = cc.toLatLng(point.rdX, point.rdY);
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
			List<LmsPoint> points = LmsDatabase.newInstance(getContext()).lmsDao().getAll();

			handler.post(() -> {
				CoordinateConverter cc = new RDToWGS84Converter();
				for (LmsPoint point : points) {
					LatLng test = cc.toLatLng(point.rdX, point.rdY);
					Marker marker = googleMap.addMarker(new MarkerOptions()
							.position(test)
							.title(point.town)
							.snippet(point.address));
					marker.setTag(point.getLmsNumber());
				}

				if (points.size() > 0)
					onPointSelected(points.get(0));
			});
		});
	}
}
