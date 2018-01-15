package com.cbapps.kempengemeenten;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.cb.kempengemeenten.R;
import com.cbapps.kempengemeenten.callback.OnMapActionListener;
import com.cbapps.kempengemeenten.database.LmsPoint;
import com.cbapps.kempengemeenten.fragments.MapFragment;
import com.cbapps.kempengemeenten.fragments.UploadCentreFragment;
import com.jakewharton.threetenabp.AndroidThreeTen;

public class MainActivity extends AppCompatActivity implements
		FragmentManager.OnBackStackChangedListener, OnMapActionListener {

	private static final String TAG = "MainActivity";

	private MapFragment mapFragment;
	private UploadCentreFragment uploadFragment;

	private SharedPreferences preferences;

	private DrawerLayout drawerLayout;
	private NavigationView navigationView;
	private ActionBarDrawerToggle drawerToggle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);
		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		AndroidThreeTen.init(getBaseContext());
		OreoNotificationManager.init(this);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		mapFragment = (MapFragment) getSupportFragmentManager().findFragmentByTag("Map");
		uploadFragment = (UploadCentreFragment) getSupportFragmentManager().findFragmentByTag("UploadCentre");

		if (mapFragment == null) {
			mapFragment = new MapFragment();
		}

		if (uploadFragment == null) {
			uploadFragment = new UploadCentreFragment();
		}

		mapFragment.setOnMapActionListener(this);

		drawerLayout = findViewById(R.id.drawer);
		drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
				R.string.drawer_open, R.string.drawer_close);
		drawerLayout.addDrawerListener(drawerToggle);

		navigationView = findViewById(R.id.navigationView);
		navigationView.setNavigationItemSelectedListener(item -> {
			drawerLayout.closeDrawers();
			switch (item.getItemId()) {
				case R.id.map:
					//MapFragment newMapFragment = new MapFragment();
					setMainFragment(mapFragment, "Map", false);
					return true;
				case R.id.home:
					setMainFragment(uploadFragment, "UploadCentre", true);
					return true;
			}
			return false;
		});

		if (getSupportActionBar() == null)
			throw new IllegalStateException("We can not have no action bar!");
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		PermissionManager.setup();

		if (savedInstanceState == null) {
			navigationView.setCheckedItem(R.id.map);
			getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.content, mapFragment, "Map")
					.commit();
		} else {
			LmsPoint nearbyGeofence = (LmsPoint) savedInstanceState.getSerializable(
					OreoNotificationManager.NEARBY_LMS_POINT);
			if (nearbyGeofence != null)
				mapFragment.showLmsDetail(nearbyGeofence, false);
		}
	}

	private void setMainFragment(Fragment fragment, String tag, boolean backStack) {
		getSupportFragmentManager().popBackStack("main-backstack", FragmentManager.POP_BACK_STACK_INCLUSIVE);
		FragmentTransaction ft = getSupportFragmentManager()
				.beginTransaction();
		ft.replace(R.id.content, fragment, tag);
		if (backStack)
			ft.addToBackStack("main-backstack");
		ft.commit();
	}

	@Override
	protected void onPostCreate(@Nullable Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		drawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		drawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (drawerToggle.onOptionsItemSelected(item))
			return true;

		switch (item.getItemId()) {
			case R.id.action_settings:
				SettingsFragment settingsFragment = new SettingsFragment();
				getSupportFragmentManager()
						.beginTransaction()
						.addToBackStack(null)
						.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
						.replace(R.id.content, settingsFragment, "Settings")
						.commit();
		}
		return true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		getSupportFragmentManager().removeOnBackStackChangedListener(this);
	}

	@Override
	public void onBackPressed() {
		MapFragment map = (MapFragment) getSupportFragmentManager().findFragmentByTag("Map");
		if (map == null || !map.isVisible() || !map.onBackPressed()) {
			if (drawerLayout.isDrawerVisible(GravityCompat.START)) {
				drawerLayout.closeDrawer(GravityCompat.START);
			} else {
				super.onBackPressed();
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (!PermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults))
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	@Override
	protected void onResume() {
		super.onResume();
		onBackStackChanged();
		getSupportFragmentManager().addOnBackStackChangedListener(this);
	}

	@Override
	public void onBackStackChanged() {
		Fragment home = getSupportFragmentManager().findFragmentByTag("Map");
		if (home != null && home.isVisible())
			navigationView.setCheckedItem(R.id.map);
		Fragment fragment = getSupportFragmentManager().findFragmentByTag("Settings");
		if (fragment == null || !fragment.isVisible()) {
			drawerToggle.setDrawerIndicatorEnabled(true);
			drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
		} else {
			drawerToggle.setDrawerIndicatorEnabled(false);
			drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
		}
	}

	@Override
	public void onLmsPointSelected(LmsPoint point) {

	}
}