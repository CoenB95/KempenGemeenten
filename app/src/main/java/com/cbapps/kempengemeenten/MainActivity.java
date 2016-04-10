package com.cbapps.kempengemeenten;

import com.cbapps.kempengemeenten.BrowserAdapter.OnBrowserEventListener;
import com.cb.kempengemeenten.R;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

	SharedPreferences sp;
	BrowserAdapter br_adapter;
	Context context;
	ProgressBar cur_loading;
	LinearLayout cur_buttons;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;
		setContentView(R.layout.main_screen);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		cur_loading = (ProgressBar) findViewById(R.id.progressBar1);
		assert cur_loading != null;
		cur_loading.setVisibility(View.GONE);
		cur_buttons = (LinearLayout) findViewById(R.id.button_layout);
		final Button upload_button = (Button) (cur_buttons != null ? cur_buttons.findViewById(R.id.button1) : null);
		assert upload_button != null;
		upload_button.setText(R.string.upload);
		final Button download_button = (Button) cur_buttons.findViewById(R.id.button2);
		download_button.setText(R.string.download);
		upload_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				clickUpload();
			}
		});
		download_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				clickDownload();
			}
		});
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		try {
			sp.edit().putString("version_code",
					getPackageManager().getPackageInfo(getPackageName(), 0).versionName).apply();
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		br_adapter = new BrowserAdapter(this, BrowserAdapter.FTP_SEARCH)
				             .setUserAndPassword(sp.getString("gebruikersnaam", ""),
				                                 sp.getString("wachtwoord", ""));
		br_adapter.setOnBrowserEventListener(new OnBrowserEventListener() {

			@Override
			public void moveToDirDone(boolean success) {

			}

			@Override
			public void moveUpDone(boolean success) {

			}

			@Override
			public void moveFurtherDone(boolean success) {

			}

			@Override
			public void toast(String text) {
				Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
			}

			@Override
			public void uploadDone(boolean success, String detail) {
				upload_button.setEnabled(true);
				Toast.makeText(context, detail, Toast.LENGTH_SHORT).show();
			}

			@Override
			public void downloadDone(boolean success, String detail, Intent intent) {
                download_button.setEnabled(true);
                if (success && intent != null) {
                    startActivity(intent);
                    Toast.makeText(context, R.string.intent_auto_start, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, detail, Toast.LENGTH_SHORT).show();
                }
            }
		});
	}
	
	public void clickDownload() {
		br_adapter.donwloadFiles();
	}
	
	public void clickUpload() {
		br_adapter.uploadFiles();
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
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
					intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT,
					                SettingsFragment.class.getName());
					intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
				}
				startActivity(intent);
		}
		return true;
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}
}