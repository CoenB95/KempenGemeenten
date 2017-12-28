package com.cbapps.kempengemeenten.nextgen.fragments;

import android.Manifest;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.cb.kempengemeenten.R;
import com.cbapps.kempengemeenten.nextgen.DefaultFileInfo;
import com.cbapps.kempengemeenten.nextgen.FTPFileBrowser;
import com.cbapps.kempengemeenten.nextgen.FTPFileConnection;
import com.cbapps.kempengemeenten.nextgen.FTPFileInfo;
import com.cbapps.kempengemeenten.nextgen.FTPFileTransferer;
import com.cbapps.kempengemeenten.nextgen.FileBrowser;
import com.cbapps.kempengemeenten.nextgen.FileBrowserAdapter;
import com.cbapps.kempengemeenten.nextgen.FileInfo;
import com.cbapps.kempengemeenten.nextgen.LocalFileBrowser;
import com.cbapps.kempengemeenten.nextgen.PermissionManager;
import com.cbapps.kempengemeenten.nextgen.callback.Predicate;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author CoenB95
 */

public class UploadCentreFragment extends DialogFragment {

	private static final String TAG = "UploadCentreFragment";

	private FTPFileConnection connection;
	private FTPFileBrowser ftpFileBrowser;
	private LocalFileBrowser localFileBrowser;
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
		uploadButton.setOnClickListener(this::onUploadButtonClicked);

		localFileBrowser = new LocalFileBrowser();
		startFTP();

		return view;
	}

	public void onDownloadButtonClicked(View view) {
		PermissionManager.requestPermission(getActivity(), PermissionManager.WRITE_PERMISSION_REQUEST_CODE, (requestCode, results) -> {
			for (PermissionManager.PermissionResult result : results) {
				if (result.getPermission().equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
					if (result.isGranted()) {
						Log.d(TAG, "Storage writing granted.");
						SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
						String localDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
						String remoteDirectoryName = preferences.getString("ftpDownloadLocation", null);
						if (remoteDirectoryName != null) {
							downloadFiles(remoteDirectoryName, localDirectory);
						}
					} else
						Log.d(TAG, "Storage writing denied.");
				} else
					Log.d(TAG, "Storage writing permission yet undecided.");
			}
		}, Manifest.permission.WRITE_EXTERNAL_STORAGE);
	}

	public void onUploadButtonClicked(View view) {
		PermissionManager.requestPermission(getActivity(), PermissionManager.READ_PERMISSION_REQUEST_CODE, (requestCode, results) -> {
			for (PermissionManager.PermissionResult result : results) {
				if (result.getPermission().equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
					if (result.isGranted()) {
						Log.d(TAG, "Storage reading granted.");
						SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
						String localDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
						String remoteDirectoryName = preferences.getString("ftpUploadLocation", null);
						if (remoteDirectoryName != null) {
							uploadFiles(localDirectory, remoteDirectoryName);
						}
					} else
						Log.d(TAG, "Storage reading denied.");
				} else
					Log.d(TAG, "Storage reading permission yet undecided.");
			}
		}, Manifest.permission.READ_EXTERNAL_STORAGE);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	private void startFTP() {
		connection = FTPFileConnection.getConnection();
		ftpFileBrowser = new FTPFileBrowser(connection);
		transferer = new FTPFileTransferer(connection);
		ExecutorService service = Executors.newCachedThreadPool();
		service.submit(() -> {
			if (connection.connect()) {
				handler.post(() -> {
					downloadButton.setEnabled(true);
					uploadButton.setEnabled(true);
				});
			}
		});
		service.shutdown();
	}

	private void downloadFiles(String remoteDirectoryName, String localDirectoryName) {
		Log.i(TAG, "Downloading files...");
		ftpFileBrowser.moveIntoDirectory(remoteDirectoryName, result -> {
			ftpFileBrowser.listFiles(result1 -> {

				Predicate<FileInfo> filter = f ->
						!f.getName().startsWith(".");

				adapter.setAllFiles(result1, filter, true);
				StringBuilder builder = new StringBuilder("The following files are in the directory:");
				for (FileInfo info : result1)
					builder.append('\n').append(info.getName())
							.append(" (").append(info.getSize()).append(" bytes)");
				Log.d(TAG, builder.toString());

				FileInfo localDirectory = new DefaultFileInfo(new File(localDirectoryName));

				transferer.downloadFiles(result1, localDirectory, filter, info -> {
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

	private void uploadFiles(String localDirectoryName, String remoteDirectoryName) {
		Log.i(TAG, "Uploading files...");
		localFileBrowser.moveIntoDirectory(localDirectoryName, result -> {
			localFileBrowser.listFiles(result1 -> {
				Predicate<FileInfo> filter = f ->
						!f.getName().startsWith(".");

				adapter.setAllFiles(result1, filter, true);
				StringBuilder builder = new StringBuilder("The following files are in the directory:");
				for (FileInfo info : result1)
					builder.append('\n').append(info.getName())
							.append(" (").append(info.getSize()).append(" bytes)");
				Log.d(TAG, builder.toString());

				ftpFileBrowser.moveIntoDirectory(remoteDirectoryName, remoteDirectory -> {
					transferer.uploadFiles(result1, remoteDirectory, filter, info -> {
						Log.i(TAG, String.format("Successfully uploaded '%s'", info.getName()));
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
				}, (info, error) -> Log.e(TAG, "Error resolving remote directory: " + error));
			}, (info, error) -> Log.e(TAG, "Error while listing files: " + error));
		}, (info, error) -> {
			Log.e(TAG, "Changing directory failed.");
		});
	}
}
