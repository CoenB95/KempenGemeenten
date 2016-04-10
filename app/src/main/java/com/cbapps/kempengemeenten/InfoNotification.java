package com.cbapps.kempengemeenten;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.IntDef;
import android.support.v4.app.NotificationCompat;

import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

public class InfoNotification {

	@IntDef({DOWNLOAD, UPLOAD})
	@Retention(RetentionPolicy.SOURCE)
	public @interface Progress {}

	@IntDef({WAITING, BUSY, SUCCESS, FAILURE, LOCAL_FILE_NOT_DELETED, IS_NO_FILE, FILE_NOT_FOUND,
	        IO_EXCEPTION})
	@Retention(RetentionPolicy.SOURCE)
	@interface Status {}
	public static final int WAITING = 1;
	public static final int BUSY = 2;
	public static final int SUCCESS = 3;
	public static final int FAILURE = 4;
	public static final int LOCAL_FILE_NOT_DELETED = 5;
	public static final int IS_NO_FILE = 6;
	public static final int FILE_NOT_FOUND = 7;
	public static final int IO_EXCEPTION = 8;

	public static final int DOWNLOAD = 0;
	public static final int UPLOAD = 1;

	private @Status int global_status = WAITING;
	private boolean contains_failed = false;
	private @Progress int progress_method;
	private int max_progress = 0;
	private int cur_progress = 0;
	private int files_done = 0;
	private String from_dir = "";
	private String to_dir = "";
	private Context context;
	private NotificationCompat.Builder notification;
	private NotificationManager notificationManager;
	private PendingIntent pentent;
	private ArrayList<FileHolder> filearray = new ArrayList<>();

	public InfoNotification(Context c, @Progress int method) {
		context = c;
		progress_method = method;
		notificationManager =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		pentent = PendingIntent.getActivity(context, 0, new Intent(context, this.getClass()), 0);
		notification = createNotification();
	}

	public void setMaxProgress(int max) {
		max_progress = max;
	}

	public void setFrom(String location) {
		from_dir = location;
	}

	public void setTo(String location) {
		to_dir = location;
	}

	public void addFile(FTPFile f) {
		FileHolder fh = new FileHolder();
		fh.name = f.getName();
		fh.status = WAITING;
		filearray.add(fh);
	}

	public void addFile(File f) {
		FileHolder fh = new FileHolder();
		fh.name = f.getName();
		fh.status = WAITING;
		filearray.add(fh);
	}

	public void setStatus(@Status int status) {
		if (progress_method == DOWNLOAD) {
			global_status = status;
			switch (global_status) {
				case SUCCESS:
				case FAILURE:
				case LOCAL_FILE_NOT_DELETED:
				case IS_NO_FILE:
				case FILE_NOT_FOUND:
				case IO_EXCEPTION:
					notification.setProgress(0, 0, false);
					notification.setSmallIcon(android.R.drawable.stat_sys_download_done);
					notification.setOngoing(false);
					break;
				case BUSY:
					break;
				case WAITING:
					break;
			}
			updateNotification(true, true, true);
			notificationManager.notify(progress_method, notification.build());
		}
	}

	public void setStatus(int position, @Status int status) {
		if (progress_method == UPLOAD) {
			switch (status) {
				case FAILURE:
				case FILE_NOT_FOUND:
				case IO_EXCEPTION:
					contains_failed = true;
				case SUCCESS:
				case LOCAL_FILE_NOT_DELETED:
				case IS_NO_FILE:
					files_done++;
					break;
				case BUSY:
					global_status = BUSY;
					break;
				case WAITING:
					break;
			}
			filearray.get(position).status = status;
			if (files_done == filearray.size()) {
				if (contains_failed) global_status = FAILURE;
				else global_status = SUCCESS;
				notification.setProgress(0, 0, false);
				notification.setSmallIcon(android.R.drawable.stat_sys_upload_done);
				notification.setOngoing(false);
				updateNotification(true, true, true);
			}
			updateNotification(false, false, true);
			notificationManager.notify(progress_method, notification.build());
		}
	}

	public void setIntent(PendingIntent intent) {
		pentent = intent;
		notification.setContentIntent(intent);
		notificationManager.notify(progress_method, notification.build());
	}

	public void show() {
		updateNotification(true, true, true);
		notificationManager.notify(progress_method, notification.build());
	}

	public void update(int progress) {
		cur_progress = progress;
		notification.setProgress(max_progress, cur_progress, false);
		notificationManager.notify(progress_method, notification.build());
	}

	public void dismiss() {
		notificationManager.cancel(progress_method);
	}

	private String getTitle() {
		switch (progress_method) {
			case UPLOAD:
				switch (global_status) {
					case WAITING:
					case BUSY:
						return "Bestanden uploaden...";
					case SUCCESS:
						return "Bestanden succesvol geüpload";
					case FAILURE:
						return "Bestanden uploaden mislukt";
					default:
						return "";
					case FILE_NOT_FOUND:
						break;
					case IO_EXCEPTION:
						break;
					case IS_NO_FILE:
						break;
					case LOCAL_FILE_NOT_DELETED:
						break;
				}
			case DOWNLOAD:
				return from_dir;
			default: return "";
		}
	}

	private String getMessage() {
		switch (progress_method) {
			case UPLOAD:
				return "Van " + from_dir + " naar " + to_dir + " (" + files_done
						       + "/" + filearray.size() + ")";
			case DOWNLOAD:
				switch (global_status) {
					case WAITING:
					case BUSY:
						return "Bezig met downloaden...";
					case SUCCESS:
						return "Succesvol gedownload. Klik om te openen.";
					case FAILURE:
						return "Downloaden mislukt";
					default:
						return "";
					case FILE_NOT_FOUND:
						break;
					case IO_EXCEPTION:
						break;
					case IS_NO_FILE:
						break;
					case LOCAL_FILE_NOT_DELETED:
						break;
				}
			default: return "";
		}
	}

	private String getBigMessage() {
		String x = getMessage()+"\nDetails:";
		for (FileHolder fh : filearray) {
			x += "\n-"+fh.name+": "+fh.getStatus();
		}
		return x;
	}

	private void updateNotification(boolean title, boolean message, boolean big_message) {
		if (title) notification.setContentTitle(getTitle());
		if (message) notification.setContentText(getMessage());
		if (big_message) notification.setStyle(new NotificationCompat.BigTextStyle()
				                                       .bigText(getBigMessage()));
	}

	private NotificationCompat.Builder createNotification() {
		int status_icon;
		switch (progress_method) {
			case UPLOAD: status_icon = android.R.drawable.stat_sys_upload; break;
			case DOWNLOAD: status_icon = android.R.drawable.stat_sys_download; break;
			default: status_icon = android.R.drawable.stat_sys_upload; break;
		}
		return new NotificationCompat.Builder(context)
				       .setContentTitle(getTitle())
				       .setContentText(getMessage())
				       .setContentIntent(pentent)
				       .setSmallIcon(status_icon)
				       .setOngoing(true)
					   .setAutoCancel(true)
				       .setProgress(0, 0, true);
	}

	private class FileHolder {
		String name;
		@Status int status;

		public String getStatus() {
			switch (status) {
				case WAITING: return "in de wacht";
				case BUSY: return "bezig...";
				case SUCCESS: return "succesvol";
				case FAILURE: return "mislukt";
				case LOCAL_FILE_NOT_DELETED: return "verzonden, niet verwijderd";
				case IS_NO_FILE: return "is geen bestand";
				case FILE_NOT_FOUND: return "niet gevonden";
				case IO_EXCEPTION: return "internet-fout";
				default: return "";
			}
		}
	}
}
