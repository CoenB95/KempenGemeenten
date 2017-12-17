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
import android.support.v4.app.ActivityCompat;
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

import com.cb.kempengemeenten.R;
import com.cbapps.kempengemeenten.nextgen.DefaultFileInfo;
import com.cbapps.kempengemeenten.nextgen.FTPFileBrowser;
import com.cbapps.kempengemeenten.nextgen.FTPFileConnection;
import com.cbapps.kempengemeenten.nextgen.FTPFileTransferer;
import com.cbapps.kempengemeenten.nextgen.FileInfo;
import com.cbapps.kempengemeenten.nextgen.fragments.FileBrowserFragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

	private static final int WRITE_PERMISSION_REQUEST_CODE = 101;
	private static final String TAG = "MainActivity";

	private boolean awaitingDownloadPermission;

	private FTPFileConnection connection;
	private FTPFileBrowser fileBrowser;
	private FTPFileTransferer transferer;

	private Button downloadButton;
	private Button uploadButton;
	ActionBarDrawerToggle drawerToggle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		DrawerLayout drawerLayout = findViewById(R.id.drawer);
		drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
				R.string.drawer_open, R.string.drawer_close);
		drawerLayout.addDrawerListener(drawerToggle);

		if (getSupportActionBar() == null)
			throw new IllegalStateException("We can not have no action bar!");
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		downloadButton = findViewById(R.id.downloadButton);
		uploadButton = findViewById(R.id.uploadButton);
		downloadButton.setEnabled(false);
		uploadButton.setEnabled(false);

		uploadButton.setOnClickListener(view -> {
			FileBrowserFragment browser = new FileBrowserFragment();
			browser.browseFTP();
			browser.show(getSupportFragmentManager(), "FileBrowser");
		});
		startFTP();
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

	private void startFTP() {
		connection = FTPFileConnection.getConnection();
		fileBrowser = new FTPFileBrowser(connection);
		transferer = new FTPFileTransferer(connection);
		ExecutorService service = Executors.newCachedThreadPool();
		service.submit(() -> {
			if (connection.checkConnection()) {
				runOnUiThread(() -> {
					downloadButton.setEnabled(true);
					uploadButton.setEnabled(true);
				});
			}
		});
		service.shutdown();
	}

	public void onDownloadButtonClicked(View view) {
		downloadFiles();
	}

	private void downloadFiles() {
		awaitingDownloadPermission = false;
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {
			Log.w(TAG, "Write permission not yet granted.");
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
					WRITE_PERMISSION_REQUEST_CODE);
			awaitingDownloadPermission = true;
			return;
		}

		Log.i(TAG, "Downloading files...");
		FileInfo file = new DefaultFileInfo(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
		fileBrowser.moveIntoDirectory("/+Geodata/foto's gsms",
				result -> {
					fileBrowser.listFiles(result1 -> {
						StringBuilder builder = new StringBuilder("The following files are in the directory:");
						for (FileInfo info : result1)
							builder.append('\n').append(info.getName())
									.append(" (").append(info.getSize()).append(" bytes)");
						Log.d(TAG, builder.toString());

						transferer.downloadFiles(result1, file,
								f -> {
									if (f.getName().startsWith(".")) {
										Log.w(TAG, "Skiping '" + f.getName() + "'");
										return false;
									}
									return true;
								}, info -> {
									Log.i(TAG, String.format("Successfully downloaded '%s'", info.getName()));
								}, (info, progress) -> {
									Log.d(TAG, String.format("Downloading '%s': %.1f%%",
											info.getName(), progress * 100));
								}, (info, error) -> {
									Log.e(TAG, String.format("Error downloading '%s': %s",
											info.getName(), error));
								});
					}, (info, error) -> Log.e(TAG, "Error while listing files: " + error));
				}, (info, error) -> {
					Log.e(TAG, "Changing directory failed.");
				});
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
				Intent intent = new Intent(getBaseContext(), SettingsActivity.class);
				intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT,
						SettingsFragment.class.getName());
				intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
				startActivity(intent);
		}
		return true;
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		switch (requestCode) {
			case WRITE_PERMISSION_REQUEST_CODE:
				if (awaitingDownloadPermission)
					downloadFiles();
				break;
			default:
				super.onRequestPermissionsResult(requestCode, permissions, grantResults);
				break;
		}
	}
}