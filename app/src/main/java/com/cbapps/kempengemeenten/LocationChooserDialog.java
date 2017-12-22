package com.cbapps.kempengemeenten;

import com.cb.kempengemeenten.R;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class LocationChooserDialog extends DialogPreference {
	private String path;

	public LocationChooserDialog(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDialogLayoutResource(R.layout.file_browser_layout);
	}

	public String getPath() {
		return path;
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		setPath(restoreValue ? getPersistedString(path) : (String) defaultValue);
	}

	public void setPath(String path) {
		this.path = path;
		persistString(path);
		setSummary(path);
	}
}