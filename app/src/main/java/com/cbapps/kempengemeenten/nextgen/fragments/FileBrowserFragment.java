package com.cbapps.kempengemeenten.nextgen.fragments;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cb.kempengemeenten.R;
import com.cbapps.kempengemeenten.LocationChooserDialog;
import com.cbapps.kempengemeenten.nextgen.FTPFileBrowser;
import com.cbapps.kempengemeenten.nextgen.FTPFileConnection;
import com.cbapps.kempengemeenten.nextgen.FileBrowser;
import com.cbapps.kempengemeenten.nextgen.FileBrowserAdapter;
import com.cbapps.kempengemeenten.nextgen.LocalFileBrowser;
import com.cbapps.kempengemeenten.nextgen.PermissionManager;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * @author CoenB95
 */

public class FileBrowserFragment extends PreferenceDialogFragmentCompat {

	public static final String TAG = "FileBrowserFragment";

	private FileBrowser browser;
	private FileBrowserAdapter fileBrowserAdapter;
	private RecyclerView fileBrowserRecyclerView;
	private ProgressBar progressCircle;
	private TextView filePathEditText;
	private Handler handler;

	private String path;

	public static FileBrowserFragment newInstance(String key) {
		FileBrowserFragment fragment = new FileBrowserFragment();
		Bundle b = new Bundle(1);
		b.putString(ARG_KEY, key);
		fragment.setArguments(b);
		return fragment;
	}

	public LocationChooserDialog getFileLocationPreference() {
		return ((LocationChooserDialog) getPreference());
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			path = savedInstanceState.getString("path");
		}
		setBrowseMode();
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);

		handler = new Handler();

		progressCircle = view.findViewById(R.id.progressBar);
		progressCircle.setVisibility(View.GONE);
		filePathEditText = view.findViewById(R.id.filePathEditText);

		ImageButton backButton = view.findViewById(R.id.backButton);
		backButton.setOnClickListener(view1 -> {
			int index = path.lastIndexOf('/');
			moveAndList(index > 0 ? path.substring(0, index) : "/");
		});

		EditText filePathEditText = view.findViewById(R.id.filePathEditText);
		filePathEditText.setOnEditorActionListener((textView, i, keyEvent) -> {
			moveAndList(filePathEditText.getText().toString());
			return true;
		});

		fileBrowserAdapter = new FileBrowserAdapter();
		fileBrowserAdapter.setListener(info -> {
			if (getFileLocationPreference().isDirectoriesMode()) {
				if (info.isDirectory())
					moveAndList(info.getPath());
			} else {
				moveAndList(info.getPath());
			}
		});

		fileBrowserRecyclerView = view.findViewById(R.id.fileRecyclerView);
		fileBrowserRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		fileBrowserRecyclerView.setAdapter(fileBrowserAdapter);

		if (browser != null)
			moveAndList(path);
	}

	private void setBrowseMode() {
		if (getFileLocationPreference().isFTPMode()) {
			browser = new FTPFileBrowser(FTPFileConnection.getConnection());
			if (isVisible()) moveAndList(null);
		} else if (getFileLocationPreference().isLocalMode()) {
			browser = new LocalFileBrowser();
			if (isVisible()) moveAndList(null);
		} else
			throw new IllegalArgumentException("Not a mode");
	}

	private void moveAndList(String path) {
		Log.i(TAG, "Listing files...");

		if (getFileLocationPreference().isLocalMode()) {
			Log.i(TAG, "Permission needed: external storage");
			PermissionManager.requestPermission(getActivity(), PermissionManager.READ_PERMISSION_REQUEST_CODE, (requestCode, results) -> {
				for (PermissionManager.PermissionResult result : results) {
					if (result.getPermission().equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
						if (result.isGranted()) {
							Log.d(TAG, "Storage reading granted.");
							moveAndListGranted(path);
						} else
							Log.d(TAG, "Storage reading denied.");
					} else
						Log.d(TAG, "Storage reading permission yet undecided.");
				}
			}, Manifest.permission.READ_EXTERNAL_STORAGE);
		} else {
			moveAndListGranted(path);
		}
	}

	private void moveAndListGranted(String path) {
		fileBrowserRecyclerView.animate().alpha(0).start();
		progressCircle.animate().alpha(1).setListener(null).start();
		progressCircle.setVisibility(View.VISIBLE);
		if (path == null || path.isEmpty()) {
			if (getFileLocationPreference().isLocalMode())
				path = Environment.getExternalStorageDirectory().getPath();
			else
				path = "/";
		}

		final String fpath = path;

		browser.moveIntoDirectory(path, result -> {
			Log.i(TAG, "Moved into '" + result.getPath() + "'");
			handler.post(() -> {
				this.path = result.getPath();
				filePathEditText.setText(result.getPath());
			});
			browser.listFiles(result2 -> {
				Log.i(TAG, "Done fetching files.");
				handler.post(() -> {
					fileBrowserAdapter.setAllFiles(result2);
					fileBrowserRecyclerView.animate().alpha(1).start();
					progressCircle.animate().alpha(0).setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							progressCircle.setVisibility(View.GONE);
						}
					}).start();
				});
			}, (info, errorMessage) -> {
				Log.e(TAG, "Could not list files: " + errorMessage);
			});
		}, (info, errorMessage) -> {
			Log.e(TAG, "Could not move into '" + fpath + "': " + errorMessage);
		});
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("path", path);
	}

	@Override
	public void onDialogClosed(boolean positiveResult) {
		if (positiveResult)
			getFileLocationPreference().setPath(path);
	}
}
