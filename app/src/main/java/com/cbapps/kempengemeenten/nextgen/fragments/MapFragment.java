package com.cbapps.kempengemeenten.nextgen.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.cbapps.kempengemeenten.nextgen.PermissionManager;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

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
				}
			}
		}, Manifest.permission.ACCESS_FINE_LOCATION);
	}
}
