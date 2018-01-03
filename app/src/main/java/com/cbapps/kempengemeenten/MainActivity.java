package com.cbapps.kempengemeenten;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
import android.preference.PreferenceActivity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.cb.kempengemeenten.R;
import com.cbapps.kempengemeenten.nextgen.DefaultFileInfo;
import com.cbapps.kempengemeenten.nextgen.FTPFileBrowser;
import com.cbapps.kempengemeenten.nextgen.FTPFileConnection;
import com.cbapps.kempengemeenten.nextgen.FTPFileTransferer;
import com.cbapps.kempengemeenten.nextgen.FileInfo;
import com.cbapps.kempengemeenten.nextgen.PermissionManager;
import com.cbapps.kempengemeenten.nextgen.database.LmsPoint;
import com.cbapps.kempengemeenten.nextgen.fragments.FileBrowserFragment;
import com.cbapps.kempengemeenten.nextgen.fragments.MapFragment;
import com.cbapps.kempengemeenten.nextgen.fragments.UploadCentreFragment;
import com.google.android.gms.maps.SupportMapFragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener,
		MapFragment.OnLmsPointSelectedListener {

	private static final String TAG = "MainActivity";

	private BottomSheetBehavior bottomSheetBehavior;
	private TextView bottomSheetTitleView;
	private TextView bottomSheetTextView;
	private DrawerLayout drawerLayout;
	private ActionBarDrawerToggle drawerToggle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		bottomSheetTitleView = findViewById(R.id.bottom_sheet_title);

		bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet));
		bottomSheetBehavior.setHideable(true);
		bottomSheetBehavior.setPeekHeight(300);
		bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

		drawerLayout = findViewById(R.id.drawer);
		drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
				R.string.drawer_open, R.string.drawer_close);
		drawerLayout.addDrawerListener(drawerToggle);

		NavigationView navigationView = findViewById(R.id.navigationView);
		navigationView.setNavigationItemSelectedListener(item -> {
			drawerLayout.closeDrawers();
			switch (item.getItemId()) {
				case R.id.map:
					showMap(null);
					return true;
				case R.id.home:
					showButtons();
					return true;
			}
			return false;
		});

		if (getSupportActionBar() == null)
			throw new IllegalStateException("We can not have no action bar!");
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		PermissionManager.setup();

		LmsPoint point = (LmsPoint) getIntent().getSerializableExtra(MapFragment.EXTRA_SHOW_LMS_DETAIL);
		if (point != null)
			showMap(point);
		else if (savedInstanceState == null)
			showMap(null);
	}

	private void showButtons() {
		UploadCentreFragment uploadCentreFragment = new UploadCentreFragment();
		getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.content, uploadCentreFragment, "UploadCentre")
				.commit();
	}

	private void showMap(LmsPoint point) {
		MapFragment mapFragment = new MapFragment();
		mapFragment.setLmsPointSelectedListener(this);
		if (point != null)
			mapFragment.showLmsDetail(point);
		getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.content, mapFragment, "Map")
				.commit();
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

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
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
		super.onBackPressed();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (!PermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults))
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	@Override
	protected void onResume() {
		super.onResume();
		getSupportFragmentManager().addOnBackStackChangedListener(this);
	}

	@Override
	public void onBackStackChanged() {
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
		bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
		bottomSheetTitleView.setText(point.address);
	}
}