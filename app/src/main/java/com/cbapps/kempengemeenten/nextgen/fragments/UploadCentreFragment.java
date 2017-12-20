package com.cbapps.kempengemeenten.nextgen.fragments;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cb.kempengemeenten.R;
import com.cbapps.kempengemeenten.nextgen.DefaultFileInfo;
import com.cbapps.kempengemeenten.nextgen.FTPFileBrowser;
import com.cbapps.kempengemeenten.nextgen.FTPFileConnection;
import com.cbapps.kempengemeenten.nextgen.FTPFileTransferer;
import com.cbapps.kempengemeenten.nextgen.FileBrowser;
import com.cbapps.kempengemeenten.nextgen.FileBrowserAdapter;
import com.cbapps.kempengemeenten.nextgen.FileInfo;
import com.cbapps.kempengemeenten.nextgen.PermissionManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * @author CoenB95
 */

public class UploadCentreFragment extends DialogFragment {

	private static final String TAG = "UploadCentreFragment";
	private static final int WRITE_PERMISSION_REQUEST_CODE = 101;

	private FTPFileConnection connection;
	private FTPFileBrowser fileBrowser;
	private FTPFileTransferer transferer;

	private Handler handler;
	private Button downloadButton;
	private Button uploadButton;
	private FileBrowserAdapter adapter;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.main_screen, container, false);

		handler = new Handler();

		adapter = new FileBrowserAdapter();

		RecyclerView uploadStatusRecyclerView = view.findViewById(R.id.uploadStatusRecyclerView);
		uploadStatusRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		uploadStatusRecyclerView.setAdapter(adapter);

		downloadButton = view.findViewById(R.id.downloadButton);
		uploadButton = view.findViewById(R.id.uploadButton);
		downloadButton.setEnabled(false);
		uploadButton.setEnabled(false);

		downloadButton.setOnClickListener(this::onDownloadButtonClicked);

		uploadButton.setOnClickListener(v -> {
			FileBrowserFragment browser = new FileBrowserFragment();
			browser.browseFTP();
			browser.show(getChildFragmentManager(), "FileBrowser");
		});

		startFTP();

		return view;
	}

	public void onDownloadButtonClicked(View view) {
		PermissionManager.requestPermission(WRITE_PERMISSION_REQUEST_CODE, (requestCode, results) -> {
			for (PermissionManager.PermissionResult result : results) {
				if (result.getPermission().equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
					if (result.isGranted()) {
						Log.d(TAG, "Storage writing granted.");
						downloadFiles();
					} else
						Log.d(TAG, "Storage writing denied.");
				} else
					Log.d(TAG, "Storage writing permission yet undecided.");
			}
		}, Manifest.permission.WRITE_EXTERNAL_STORAGE);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	private void startFTP() {
		connection = FTPFileConnection.getConnection();
		fileBrowser = new FTPFileBrowser(connection);
		transferer = new FTPFileTransferer(connection);
		ExecutorService service = Executors.newCachedThreadPool();
		service.submit(() -> {
			if (connection.checkConnection()) {
				handler.post(() -> {
					downloadButton.setEnabled(true);
					uploadButton.setEnabled(true);
				});
			}
		});
		service.shutdown();
	}

	private void downloadFiles() {
		Log.i(TAG, "Downloading files...");
		FileInfo file = new DefaultFileInfo(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
		fileBrowser.moveIntoDirectory("/+Geodata/sqlite voor locus", result -> {
			fileBrowser.listFiles(result1 -> {
				adapter.clearFiles();
				StringBuilder builder = new StringBuilder("The following files are in the directory:");
				for (FileInfo info : result1)
					builder.append('\n').append(info.getName())
							.append(" (").append(info.getSize()).append(" bytes)");
				Log.d(TAG, builder.toString());

				transferer.downloadFiles(result1, file, f -> {
					if (f.getName().startsWith(".")) {
						Log.w(TAG, "Skiping '" + f.getName() + "'");
						return false;
					}
					adapter.addFile(f, true);
					return true;
				}, info -> {
					Log.i(TAG, String.format("Successfully downloaded '%s'", info.getName()));
					adapter.showProgress(info, false);
				}, (info, progress) -> {
					Log.d(TAG, String.format("Downloading '%s': %.1f%%",
							info.getName(), progress * 100));
					adapter.updateProgress(info, progress);
				}, (info, error) -> {
					if (info == null)
						Log.e(TAG, "Error while downloading: " + error);
					else
						Log.e(TAG, String.format("Error downloading '%s': %s",
								info.getName(), error));
				});
			}, (info, error) -> Log.e(TAG, "Error while listing files: " + error));
		}, (info, error) -> {
			Log.e(TAG, "Changing directory failed.");
		});
	}
}
