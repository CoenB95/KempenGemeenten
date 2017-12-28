package com.cbapps.kempengemeenten.nextgen.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.cbapps.kempengemeenten.nextgen.CoordinateConverter;
import com.cbapps.kempengemeenten.nextgen.LMSPoint;
import com.cbapps.kempengemeenten.nextgen.PermissionManager;
import com.cbapps.kempengemeenten.nextgen.RDToWGS84Converter;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author CoenB95
 */

public class MapFragment extends SupportMapFragment implements OnMapReadyCallback {

	public MapFragment() {
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
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
		String filePath = preferences.getString("toDoListFilePath", null);
		if (filePath == null) {
			Toast.makeText(getContext(), "ToDo list not yet downloaded.", Toast.LENGTH_SHORT).show();
			return;
		}

		List<LMSPoint> points = new ArrayList<>();

		try {
			BufferedReader reader = new BufferedReader(new FileReader(filePath));
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.replace("\"", "");
				String[] split = line.split(";", -1);
				if (split.length < 8)
					continue;
				LMSPoint point = new LMSPoint(Integer.valueOf(split[0]), Integer.valueOf(split[2]),
						Integer.valueOf(split[3]), split[4], split[6], Integer.valueOf(split[7]),
						split[8]);
				points.add(point);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		CoordinateConverter cc = new RDToWGS84Converter();
		for (LMSPoint point : points) {
			LatLng test = cc.toLatLng(point.rdX, point.rdY);
			Marker marker = googleMap.addMarker(new MarkerOptions()
					.position(test)
					.title(point.town)
					.snippet(point.street + " " + point.streetNumber));
			marker.setTag(point.getLmsNumber());
		}
	}
}
