package com.cbapps.kempengemeenten.nextgen.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cb.kempengemeenten.R;
import com.cbapps.kempengemeenten.nextgen.FTPFileBrowser;
import com.cbapps.kempengemeenten.nextgen.FTPFileConnection;
import com.cbapps.kempengemeenten.nextgen.FileBrowser;
import com.cbapps.kempengemeenten.nextgen.FileBrowserAdapter;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * @author CoenB95
 */

public class FileBrowserFragment extends PreferenceDialogFragmentCompat {

	public static final String TAG = "FileBrowserFragment";
	public static final int MODE_FTP_DIRECTORIES = 1;
	public static final int MODE_FTP_FILES = 2;
	public static final int MODE_LOCAL_DIRECTORIES = 3;
	public static final int MODE_LOCAL_FILES = 4;

	private FileBrowser browser;
	private FileBrowserAdapter fileBrowserAdapter;
	private RecyclerView fileBrowserRecyclerView;
	private ProgressBar progressCircle;
	private TextView filePathEditText;
	private Handler handler;

	private int mode;
	private String path;

	public static FileBrowserFragment newInstance(String key) {
		FileBrowserFragment fragment = new FileBrowserFragment();
		Bundle b = new Bundle(1);
		b.putString(ARG_KEY, key);
		fragment.setArguments(b);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			path = savedInstanceState.getString("path");
			mode = savedInstanceState.getInt("mode");
			switch (mode) {
				case MODE_FTP_DIRECTORIES:
				case MODE_FTP_FILES:
					Log.d(TAG, "Restored FTP browsing mode");
					break;
				case MODE_LOCAL_DIRECTORIES:
				case MODE_LOCAL_FILES:
					Log.d(TAG, "Restored local browsing mode");
					break;
				default:
					Log.d(TAG, "Could not restore browsing mode");
					break;
			}
			setBrowseMode(mode);
		}
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		//View view = inflater.inflate(R.layout.file_browser_layout, container, false);

		handler = new Handler();

		progressCircle = view.findViewById(R.id.progressBar);
		progressCircle.setVisibility(View.GONE);
		filePathEditText = view.findViewById(R.id.filePathEditText);

		ImageButton backButton = view.findViewById(R.id.backButton);
		backButton.setOnClickListener(view1 -> {
			int index = path.lastIndexOf('/');
			moveAndList(index > 0 ? path.substring(0, index) : "");
		});

		fileBrowserAdapter = new FileBrowserAdapter();
		fileBrowserAdapter.setListener(info -> {
			switch (mode) {
				case MODE_FTP_DIRECTORIES:
				case MODE_LOCAL_DIRECTORIES:
					if (info.isDirectory())
						moveAndList(info.getPath());
					break;
				case MODE_FTP_FILES:
				case MODE_LOCAL_FILES:
					moveAndList(info.getPath());
					break;
			}
		});

		fileBrowserRecyclerView = view.findViewById(R.id.fileRecyclerView);
		fileBrowserRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		fileBrowserRecyclerView.setAdapter(fileBrowserAdapter);

		if (browser != null)
			moveAndList(path);
	}

	public void setBrowseMode(int browseMode) {
		switch (browseMode) {
			case MODE_FTP_DIRECTORIES:
			case MODE_FTP_FILES:
				browser = new FTPFileBrowser(FTPFileConnection.getConnection());
				if (isVisible()) moveAndList(null);
				break;
			case MODE_LOCAL_DIRECTORIES:
			case MODE_LOCAL_FILES:
				break;
			default:
				throw new IllegalArgumentException("Not a mode");
		}
		mode = browseMode;
	}

	private void moveAndList(String path) {
		Log.i(TAG, "Listing files...");
		fileBrowserRecyclerView.animate().alpha(0).start();
		progressCircle.animate().alpha(1).setListener(null).start();
		progressCircle.setVisibility(View.VISIBLE);
		browser.moveIntoDirectory(path == null || path.isEmpty() ? "/" : path, result -> {
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
			Log.e(TAG, "Could not move into '" + path + "': " + errorMessage);
		});
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("path", path);
		outState.putInt("mode", mode);
	}

	@Override
	public void onDialogClosed(boolean positiveResult) {

	}
}
