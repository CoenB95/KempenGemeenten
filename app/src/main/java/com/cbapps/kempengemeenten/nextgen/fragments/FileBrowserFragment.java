package com.cbapps.kempengemeenten.nextgen.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
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

public class FileBrowserFragment extends DialogFragment {

	private static final String TAG = "FileBrowserFragment";
	private static final int MODE_FTP = 1;
	private static final int MODE_LOCAL_FILES = 2;

	private FileBrowser browser;
	private FileBrowserAdapter fileBrowserAdapter;
	private RecyclerView fileBrowserRecyclerView;
	private ProgressBar progressCircle;
	private ImageButton backButton;
	private TextView filePathEditText;
	private Handler handler;
	private int mode;
	private String path;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.file_browser_layout, container, false);

		handler = new Handler();

		progressCircle = view.findViewById(R.id.progressBar);
		progressCircle.setVisibility(View.GONE);
		filePathEditText = view.findViewById(R.id.filePathEditText);

		backButton = view.findViewById(R.id.backButton);
		backButton.setOnClickListener(view1 -> {
			int index = path.lastIndexOf('/');
			moveAndList(index > 0 ? path.substring(0, index) : "");
		});

		fileBrowserAdapter = new FileBrowserAdapter();
		fileBrowserAdapter.setListener(info -> {
			moveAndList(info.getPath());
		});

		fileBrowserRecyclerView = view.findViewById(R.id.fileRecyclerView);
		fileBrowserRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		fileBrowserRecyclerView.setAdapter(fileBrowserAdapter);

		path = "";

		if (savedInstanceState != null) {
			path = savedInstanceState.getString("path");
			mode = savedInstanceState.getInt("mode");
			switch (mode) {
				case MODE_FTP:
					Log.d(TAG, "Restored FTP browsing mode");
					browseFTP();
					break;
				case MODE_LOCAL_FILES:
					Log.d(TAG, "Restored local files browsing mode");
					browseLocalFiles();
					break;
				default:
					Log.d(TAG, "Could not restore browsing mode");
					break;
			}
		}

		if (browser != null)
			moveAndList(path);

		return view;
	}

	public void browseFTP() {
		mode = MODE_FTP;
		browser = new FTPFileBrowser(FTPFileConnection.getConnection());
		if (isVisible()) moveAndList(null);
	}

	public void browseLocalFiles() {
		mode = MODE_LOCAL_FILES;
	}

	private void moveAndList(String path) {
		Log.i(TAG, "Listing files...");
		fileBrowserRecyclerView.animate().alpha(0).start();
		progressCircle.animate().alpha(1).setListener(null).start();
		progressCircle.setVisibility(View.VISIBLE);
		browser.moveIntoDirectory(path == null || path.isEmpty() ? "/" : path, result -> {
			Log.i(TAG, "Moved into '" + result.getName() + "'");
			handler.post(() -> {
				this.path = result.getName();
				filePathEditText.setText(result.getName());
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
	public void onResume() {
		super.onResume();
		if (getDialog() != null && getDialog().getWindow() != null)
			getDialog().getWindow().setLayout(WRAP_CONTENT, MATCH_PARENT);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("path", path);
		outState.putInt("mode", mode);
	}
}
