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

	public LocationChooserDialog(Context context, AttributeSet attrs) {
		super(context, attrs);
		setTitle(R.string.download);
		setSummary(R.string.choose_directory);
		setDialogLayoutResource(R.layout.choose_preference);
		setPositiveButtonText(android.R.string.ok);
		setNegativeButtonText(android.R.string.cancel);
		setDialogIcon(null);
	}
}