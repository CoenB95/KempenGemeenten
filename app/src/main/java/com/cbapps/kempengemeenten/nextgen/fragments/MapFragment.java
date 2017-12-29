package com.cbapps.kempengemeenten.nextgen.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.preference.PreferenceManager;
import android.widget.Toast;

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

	private ExecutorService service;
	private Handler handler;

	public MapFragment() {
		service = Executors.newCachedThreadPool();
		handler = new Handler();
		getMapAsync(this);
	}

	//Permission is taken care of, but lint doesn't believe it.
	@SuppressLint("MissingPermission")
	@Override
	public void onMapReady(GoogleMap googleMap) {
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
			});
		});
	}
}
