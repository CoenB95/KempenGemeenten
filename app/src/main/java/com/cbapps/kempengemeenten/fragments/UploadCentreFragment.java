package com.cbapps.kempengemeenten.fragments;

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
import com.cbapps.kempengemeenten.DefaultFileInfo;
import com.cbapps.kempengemeenten.FTPFileBrowser;
import com.cbapps.kempengemeenten.FTPFileConnection;
import com.cbapps.kempengemeenten.FTPFileTransferer;
import com.cbapps.kempengemeenten.FileBrowserAdapter;
import com.cbapps.kempengemeenten.FileInfo;
import com.cbapps.kempengemeenten.LocalFileBrowser;
import com.cbapps.kempengemeenten.PermissionManager;
import com.cbapps.kempengemeenten.callback.Predicate;
import com.cbapps.kempengemeenten.database.LmsDatabase;
import com.cbapps.kempengemeenten.database.LmsPoint;

import org.threeten.bp.LocalDateTime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
	private SharedPreferences preferences;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.main_screen, container, false);

		preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
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
						String localDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
						String remoteDirectoryName = preferences.getString("ftpDownloadLocation", null);
						if (remoteDirectoryName != null) {
							downloadToDoFile(remoteDirectoryName, localDirectory);
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

	private void downloadToDoFile(String remoteFileName, String localFileName) {
		Log.i(TAG, "Downloading todo list...");
		ftpFileBrowser.moveIntoDirectory(remoteFileName, result -> {
					try {
						File tempFile = File.createTempFile("mutations", ".csv", getContext().getCacheDir());

						FileInfo localFile = new DefaultFileInfo(tempFile);
						transferer.downloadFile(result, localFile,
								info -> {
									Log.i(TAG, String.format("Successfully downloaded '%s'",
											info.getName()));

									List<LmsPoint> points = new ArrayList<>();

									try {
										BufferedReader reader = new BufferedReader(new FileReader(localFile.getPath()));
										String line;
										while ((line = reader.readLine()) != null) {
											line = line.replace("\"", "");
											String[] split = line.split(";", -1);
											if (split.length < 8)
												continue;
											LmsPoint point = new LmsPoint(
													Integer.valueOf(split[0]), //lms
													Integer.valueOf(split[1]), //x
													Integer.valueOf(split[2]), //y
													split[3], //town
													split[4], //address
													split[7]); //date
											points.add(point);
										}
									} catch (NumberFormatException e) {
										Log.e(TAG, "Could not parse csv.");
									} catch (FileNotFoundException e) {
										e.printStackTrace();
									} catch (IOException e) {
										e.printStackTrace();
									}

									LmsDatabase.newInstance(getContext()).lmsDao().insertOrReplaceAll(points);

									Log.i(TAG, String.format("Inserted %d points into database." +
													" Temp-file deleted=%b",
											points.size(), tempFile.delete()));
								},
								(info, progress) ->
										Log.d(TAG, String.format("Downloading '%s': %.1f%%",
												info.getName(), progress * 100)),
								(info, error) -> {
									Log.e(TAG, String.format("Error downloading '%s': %s." +
													" Temp-file deleted=%b",
											result.getName(), error, tempFile.delete()));
								});
					} catch (IOException e) {
						e.printStackTrace();
					}
				},
				(info, error) -> Log.e(TAG, "Error while listing files: " + error));
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
