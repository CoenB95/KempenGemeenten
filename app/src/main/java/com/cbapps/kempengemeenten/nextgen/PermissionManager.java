package com.cbapps.kempengemeenten.nextgen;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author CoenB95
 */

public class PermissionManager {

	public static final int WRITE_PERMISSION_REQUEST_CODE = 101;
	public static final int READ_PERMISSION_REQUEST_CODE = 102;

	private static final String TAG = "PermissionManager";

	private static PermissionManager permissionManager;

	private Activity activity;
	private SparseArray<OnPermissionResultListener> listenMap;

	private PermissionManager(Activity activity) {
		this.activity = activity;
		listenMap = new SparseArray<>();
	}

	public static boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (permissionManager == null)
			return false;

		OnPermissionResultListener listener = permissionManager.listenMap.get(requestCode);
		if (listener == null)
			return false;

		List<PermissionResult> results = new ArrayList<>();
		for (int i = 0; i < permissions.length; i++) {
			results.add(new PermissionResult(permissions[i], grantResults[i] == PackageManager.PERMISSION_GRANTED));
		}

		listener.onPermissionResult(requestCode, results);
		permissionManager.listenMap.remove(requestCode);
		return true;
	}

	public static void requestPermission(int requestCode, OnPermissionResultListener listener,
	                                     String... permissions) {
		if (permissionManager == null)
			throw new IllegalStateException("PermissionManager not yet setup.");

		List<PermissionResult> grantedPermissions = new ArrayList<>();
		List<String> notYetGrantedPermissions = new ArrayList<>();

		for (String permission : permissions) {
			if (ContextCompat.checkSelfPermission(permissionManager.activity,
					permission) == PackageManager.PERMISSION_GRANTED) {
				//Permission has been granted already.
				grantedPermissions.add(new PermissionResult(permission, true));
			} else {
				notYetGrantedPermissions.add(permission);
			}
		}

		if (!grantedPermissions.isEmpty())
			listener.onPermissionResult(requestCode, grantedPermissions);

		if (notYetGrantedPermissions.isEmpty())
			return;

		permissionManager.listenMap.put(requestCode, listener);
		ActivityCompat.requestPermissions(permissionManager.activity,
				notYetGrantedPermissions.toArray(new String[]{}),
				requestCode);
	}

	public static void setup(Activity activity) {
		permissionManager = new PermissionManager(activity);
	}

	@FunctionalInterface
	public interface OnPermissionResultListener {
		void onPermissionResult(int requestCode, List<PermissionResult> results);
	}

	public static class PermissionResult {
		private String permission;
		private boolean granted;

		PermissionResult(String permission, boolean granted) {
			this.permission = permission;
			this.granted = granted;
		}

		public String getPermission() {
			return permission;
		}

		public boolean isGranted() {
			return granted;
		}
	}
}
