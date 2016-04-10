package com.cbapps.kempengemeenten;

import com.cb.kempengemeenten.R;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class UploadFromLocationChooserDialog extends DialogPreference {

		EditText text;
		ListView list;
		ImageButton back;
		ProgressBar wait;
		Context context;
		BrowserAdapter adapter;
		
		public UploadFromLocationChooserDialog(Context _context, AttributeSet attrs) {
			super(_context, attrs);
			context = _context;
			setTitle("Uploaden");
			setSummary("Kies een map");
			setDialogLayoutResource(R.layout.choose_preference);
			setPositiveButtonText(android.R.string.ok);
			setNegativeButtonText(android.R.string.cancel);
			setDialogIcon(null);
		}
		
		@Override
		protected void onSetInitialValue(boolean restorePersistedValue,
			Object defaultValue) {
			if (!restorePersistedValue) {
				setSummary("Kies een map");
			}else{
				setSummary("Vanuit "+getPersistedString(""));
			}
			super.onSetInitialValue(restorePersistedValue, defaultValue);
		}

		@Override
		protected void onBindDialogView(@NonNull View view) {
			super.onBindDialogView(view);
			text = (EditText) view.findViewById(R.id.editText1);
			list = (ListView) view.findViewById(R.id.listView1);
			back = (ImageButton) view.findViewById(R.id.camera);
			wait = (ProgressBar) view.findViewById(R.id.progressBar1);
			wait.setVisibility(View.GONE);
			back.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					adapter.handleBack();
					text.setText(adapter.getCurrentLocation());
				}
			});
			adapter = new BrowserAdapter(context, BrowserAdapter.LOCAL_SEARCH);
			adapter.initialize();
			list.setAdapter(adapter);
			list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int position, long arg3) {
					Log.d("Preference", "You selected an item!");
					adapter.handleClick(position);
					text.setText(adapter.getCurrentLocation());
				}
			});
			String f = getPreferenceManager().getSharedPreferences().getString("bestandsmap", "");
			if (f.equals("")) {
				text.setText(adapter.getCurrentLocation());
			}else{
				text.setText(f);
			}
		}
		
		@Override
		protected void onDialogClosed(boolean positive) {
			if (positive) {
				if (text != null) {
					String value = text.getText().toString();
					if (value.equals("")) {
						Log.d("Preference", "You have upload_not choosed a directory");
					}else{
						Log.d("Preference", "You choosed "+value);
						getPreferenceManager().getSharedPreferences().edit().putString("bestandsmap", value).commit();
						setSummary("Vanuit "+getPersistedString(""));
					}
				}
			}
		}
	}