package com.cbapps.kempengemeenten.fragments;

import android.Manifest;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
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
	private TextView statusTextView;
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
		statusTextView = view.findViewById(R.id.statusTextView);
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
						String localDirectory = preferences.getString("localUploadLocation", null);
						String remoteDirectoryName = preferences.getString("ftpUploadLocation", null);
						if (localDirectory == null || remoteDirectoryName == null) {
							new AlertDialog.Builder(getContext())
									.setTitle(R.string.location_not_set_title)
									.setMessage(R.string.location_not_set_description)
									.setPositiveButton(R.string.ok, null)
									.show();
							return;
						}
						uploadFiles(localDirectory, remoteDirectoryName);
					} else
						Log.d(TAG, "Storage reading denied.");
				} else
					Log.d(TAG, "Storage reading permission yet undecided.");
			}
		}, Manifest.permission.READ_EXTERNAL_STORAGE);
	}

	private void setStatus(@StringRes int text) {
		handler.post(() -> {
			statusTextView.setText(text);
		});
	}

	private void setStatus(String text) {
		handler.post(() -> {
			statusTextView.setText(text);
		});
	}

	private void startFTP() {
		connection = FTPFileConnection.getDefaultConnection(getContext());
		ftpFileBrowser = new FTPFileBrowser(connection);
		transferer = new FTPFileTransferer(connection);
		ExecutorService service = Executors.newCachedThreadPool();
		service.submit(() -> {
			if (connection.connect()) {
				setStatus(R.string.ftp_connected);
				handler.post(() -> {
					downloadButton.setEnabled(true);
					uploadButton.setEnabled(true);
				});
			} else {
				setStatus(connection.getError());
			}
		});
		service.shutdown();
	}

	private void downloadToDoFile(String remoteFileName, String localFileName) {
		Log.i(TAG, "Downloading todo list...");
		setStatus("");
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
													split[4], //street
													Integer.valueOf(split[5]), //number
													split[6], //appendix
													split[7], //measured-date
													split[8].isEmpty() ? 0 : Integer.valueOf(split[8]), //measuring duration
													split[9]); //photos json-array
											points.add(point);
										}

										LmsDatabase.newInstance(getContext()).lmsDao().insertOrReplaceAll(points);
										setStatus(R.string.updatefile_succeeded);
									} catch (NumberFormatException e) {
										Log.e(TAG, "Could not parse csv.");
										setStatus(R.string.updatefile_csv_wrong);
									} catch (FileNotFoundException e) {
										e.printStackTrace();
									} catch (IOException e) {
										e.printStackTrace();
									}
								},
								(info, progress) ->
										Log.d(TAG, String.format("Downloading '%s': %.1f%%",
												info.getName(), progress * 100)),
								(info, error) -> {
									setStatus(String.format("%s: %s",
											getString(R.string.updatefile_error), error));
								});
					} catch (IOException e) {
						e.printStackTrace();
					}
				},
				(info, error) -> Log.e(TAG, "Error while listing files: " + error));
	}

	private void uploadFiles(String localDirectoryName, String remoteDirectoryName) {
		Log.i(TAG, "Uploading files...");
		setStatus("");
		localFileBrowser.moveIntoDirectory(localDirectoryName, result -> {
			localFileBrowser.listFiles(result1 -> {
				Predicate<FileInfo> filter = f ->
						!f.getName().startsWith(".");

				adapter.setAllFiles(result1, filter, true, FileBrowserAdapter.FILE_TYPE_UPLOAD);
				StringBuilder builder = new StringBuilder("The following files are in the directory:");
				for (FileInfo info : result1)
					builder.append('\n').append(info.getName())
							.append(" (").append(info.getSize()).append(" bytes)");
				Log.d(TAG, builder.toString());

				ftpFileBrowser.moveIntoDirectory(remoteDirectoryName, remoteDirectory -> {
					transferer.uploadFiles(result1, remoteDirectory, filter, info -> {
						Log.i(TAG, String.format("Successfully uploaded '%s'", info.getName()));
						adapter.showProgress(info, false);
						if (preferences.getBoolean("deleteAfterUpload", false)) {
							if (!new File(info.getPath()).delete())
								Log.e(TAG, "Could not delete file!");
							else Log.i(TAG, "File deleted!");
						}
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
