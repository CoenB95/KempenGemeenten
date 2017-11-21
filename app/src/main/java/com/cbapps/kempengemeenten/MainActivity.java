package com.cbapps.kempengemeenten;

import com.cbapps.kempengemeenten.BrowserAdapter.OnBrowserEventListener;
import com.cb.kempengemeenten.R;
import com.cbapps.kempengemeenten.nextgen.FTPFileBrowser;
import com.cbapps.kempengemeenten.nextgen.FileInfo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {

	private static final int WRITE_PERMISSION_REQUEST_CODE = 101;
	private static final String TAG = "MainActivity";

	SharedPreferences sp;
	BrowserAdapter br_adapter;
	Context context;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;
		setContentView(R.layout.main_screen);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {

			// Should we show an explanation?
			if (ActivityCompat.shouldShowRequestPermissionRationale(this,
					Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

				// Show an expanation to the user *asynchronously* -- don't block
				// this thread waiting for the user's response! After the user
				// sees the explanation, try again to request the permission.

			} else {

				// No explanation needed, we can request the permission.

				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						WRITE_PERMISSION_REQUEST_CODE);
			}
		} else {
			test();
		}
	}

	private void test() {
		Log.i(TAG, "Testing...");
		FTPFileBrowser fileBrowser = new FTPFileBrowser();
		File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		fileBrowser.moveIntoDirectory("/+Geodata/foto's gsms",///Handleiding.txt",
				result -> {
					fileBrowser.listFiles(result1 -> {
						StringBuilder builder = new StringBuilder("The following files are in the directory:");
						for (FileInfo info : result1)
							builder.append('\n').append(info.getName())
									.append(" (").append(info.getSize()).append(" bytes)");
						Log.d(TAG, builder.toString());

						fileBrowser.downloadFiles(file, f -> {
									if (f.getName().equals(".") || f.getName().equals("..")) {
										Log.w(TAG, "Skip '" + f.getName() + "'.");
										return false;
									}
									return true;
								},
								result2 -> {
									Log.i(TAG, "Successfully downloaded files");
								}, (fileInfo, progress) -> {
									Log.i(TAG, String.format("Downloading '%s': %.1f%%", fileInfo.getName(), progress * 100));
								}, (fileInfo, progress) -> {
									Log.i(TAG, String.format("Overall progress: %.1f%%", progress * 100));
								}, error -> {
									Log.e(TAG, "Error while downloading file: " + error);
								});
					}, error -> Log.e(TAG, "Error while listing files: " + error));
				}, error -> {
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
				test();
				break;
			default:
				super.onRequestPermissionsResult(requestCode, permissions, grantResults);
				break;
		}
	}
}