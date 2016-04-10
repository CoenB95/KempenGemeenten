package com.cbapps.kempengemeenten;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.io.CopyStreamAdapter;
import org.apache.commons.net.io.CopyStreamEvent;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.cb.kempengemeenten.R;

public class BrowserAdapter extends BaseAdapter {

	@IntDef({LIST, MOVE_FORWARD, MOVE_FORWARD_AND_LIST, MOVE_BACK, MOVE_BACK_AND_LIST,
			        DOWNLOAD, UPLOAD, CHANGE_DIR_AND_LIST, QUIT})
	@Retention(RetentionPolicy.SOURCE)
	public @interface Action {}

	@IntDef({LOCAL_SEARCH, FTP_SEARCH})
	@Retention(RetentionPolicy.SOURCE)
	public @interface Browse {}

	public final static int LIST = 101;
	public final static int MOVE_FORWARD = 102;
	public final static int MOVE_FORWARD_AND_LIST = 103;
	public final static int MOVE_BACK = 104;
	public final static int MOVE_BACK_AND_LIST = 105;
	public final static int DOWNLOAD = 106;
	public final static int UPLOAD = 107;
	public final static int CHANGE_DIR_AND_LIST = 108;
	public final static int QUIT = 110;
	public static final int LOCAL_SEARCH = 1;
	public static final int FTP_SEARCH = 2;
	
	private LayoutInflater inflater;
	private FTPClient client;
	private FTPFile[] ftp_files;
	private File local_dir;
	private File[] local_files;
	private String username, password = "";
	private String pathname = "/";
	private int depth = 0;
	private boolean logged_in = false;
	private Context context;
	
	//private NotificationManager not_manager;
	//private NotificationCompat.Builder not_builder;
	private SharedPreferences prefer;
	private @Browse int browse_method = 0;
	
	public BrowserAdapter(Context c, @Browse int browse_method) {
		this.browse_method = browse_method;
		context = c;
		client = new FTPClient();
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		prefer = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public BrowserAdapter setUserAndPassword(String user, String pass) {
		username = user;
		password = pass;
		return this;
	}

	public void initialize() {
		switch (browse_method) {
			case LOCAL_SEARCH:
				String s = prefer.getString("bestandslocatie", "");
				if (s.equals("")) {
					local_dir = Environment.getExternalStorageDirectory();
				} else {
					local_dir = new File(s);
				}
				local_files = local_dir.listFiles();
				local_files = sortOnlyDirs(local_files);
				notifyDataSetChanged();
				break;
			case FTP_SEARCH:
				new Background().execute(LIST);
				break;
		}
	}
	
	public String getCurrentLocation() {
		return local_dir.getAbsolutePath();
	}
	
	public File[] sortOnlyDirs(File[] array) {
		int size = 0;
		if (array != null) {
			size = array.length;
			Log.d("Sort", "There are "+size+" items");
			for (int r = 0;r<array.length;r++) {
				if (array[r].isFile()) {
					Log.d("Sort", "Item "+r+" is a File");
					array[r] = null;
					size--;
				}else{
					Log.d("Sort", "Item "+r+" is a directory");
				}
			}
			Log.d("Sort", "Total directories: "+size);
		}else{
			Log.d("Sort", "No files...");
		}
		File[] newarray = new File[size];
		if (size > 0) {
			int count = 0;
			Log.d("Sort", "Creating new Array");
			for (File anArray : array) {
				if (anArray != null) {
					Log.d("Sort", "Item " + count + ": " + anArray);
					newarray[count] = anArray;
					count++;
				}
			}
			if (count > 0) {
				Arrays.sort(newarray);
			}else{
				Log.d("Sort", "Only files...");
			}
		}else{
			Log.d("Sort", "No directories!");
		}
		return newarray;
	}

	/**
	 * Notifies the adapter that an item has been clicked.
	 * @param position the position of the item.
	 */
	public void handleClick(int position) {
		if (ftp_files != null) {
			new Background().execute(MOVE_FORWARD_AND_LIST, position);
		} else if (local_files != null) {
			local_dir = new File(local_files[position].getAbsolutePath());
			local_files = local_dir.listFiles();
			local_files = sortOnlyDirs(local_files);
			notifyDataSetChanged();
		}
	}

	public void handleLongClick(int position) {
		if (ftp_files != null && ftp_files[position].isFile())
			new Background().execute(DOWNLOAD, position);
	}
	
	/*public void shutDown() {
		new Background().execute(QUIT);
	}*/

	/**
	 * @return wheter we can go up.
	 */
	public boolean canGoUp() {
		return depth > 0;
	}
	
	public void handleBack() {
		switch (browse_method) {
			case FTP_SEARCH:
				if (canGoUp()) new Background().execute(MOVE_BACK_AND_LIST);
				else if (browse_listener != null) {
					browse_listener.moveUpDone(false);
					browse_listener.toast("Geen verdere mappen.");
				}
				break;
			case LOCAL_SEARCH:
				local_dir = local_dir.getParentFile();
				local_files = local_dir.listFiles();
				local_files = sortOnlyDirs(local_files);
				notifyDataSetChanged();
				break;
		}
	}

	@Override
	public int getCount() {
		if (ftp_files != null) return ftp_files.length;
		else if (local_files != null) return local_files.length;
		else return 0;
	}

	@Override
	public Object getItem(int arg0) {
		return null;
	}

	@Override
	public long getItemId(int arg0) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.list_item_custom, parent, false);
		}
		TextView t1 = (TextView) convertView.findViewById(R.id.textText);
		if (ftp_files != null) {
			t1.setText(ftp_files[position].getName());
		}else if (local_files != null) {
			t1.setText(local_files[position].getName());
		}else{
			t1.setText("");
		}
		return convertView;
	}
	
	public String getCurrentLink() {
		if (reached_file_name != null) return pathname + reached_file_name;
		else return pathname;
	}
	
	public void setCurrentLink(String value) {
		pathname = value;
		int last = 1;
		while (value.indexOf("/", last) > -1) {
			last = value.indexOf("/", last)+1;
			depth++;
		}
		new Background().execute(CHANGE_DIR_AND_LIST);
	}

	/**
	 * Checks known locations and starts downloading if possible.
	 * @return true if able to start, false otherwise
	 */
	public boolean donwloadFiles() {
		String onFTP = prefer.getString("quickdownload", "");
		if (onFTP.equals("")) {
			if (browse_listener != null) browse_listener.uploadDone(false, "Geen bestand bekend.");
			return false;
		}
		else {
			new Background().execute(DOWNLOAD, -2);
			return true;
		}
	}

	/**
	 * Checks known locations and starts uploading if possible.
	 * @return true if able to start, false otherwise
	 */
	public boolean uploadFiles() {
		String s = prefer.getString("bestandsmap", "");
		String o = prefer.getString("serverlocatie", "");
		if (!s.equals("") && !o.equals("")) {
			local_dir = new File(s);
			pathname = o;
			new Background().execute(UPLOAD);
			return true;
		}else{
			if (browse_listener != null) browse_listener.uploadDone(false, "Geen locatie bekend.");
			return false;
		}
	}

	InfoNotification download_not;

	private void downloadAction(int pos) {
		download_not = new InfoNotification(context, InfoNotification.DOWNLOAD);
		File store_place;
		String download_from_place;
		String download_name;
		if (pos < 0) {
			download_from_place = prefer.getString("quickdownload", "");
			download_name = download_from_place.substring(download_from_place.lastIndexOf("/"));
		} else {
			download_from_place = pathname + ftp_files[pos].getName();
			download_name = ftp_files[pos].getName();
		}
		String location = prefer.getString("bestandslocatie", "");
		if (location.equals(""))
			location = Environment.getExternalStorageDirectory().getPath();
		store_place = new File(location, download_name);
		download_not.setFrom(store_place.getName());
		download_not.show();
		try {
			OutputStream output = new FileOutputStream(store_place);
			CopyStreamAdapter streamlistener = new CopyStreamAdapter() {
				@Override
				public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
					if (streamSize != CopyStreamEvent.UNKNOWN_STREAM_SIZE) {
						download_not.setMaxProgress((int) streamSize);
					}
					download_not.setStatus(InfoNotification.BUSY);
					download_not.update((int) totalBytesTransferred);
				}
			}; 
			client.setCopyStreamListener(streamlistener);
			if (client.retrieveFile(download_from_place, output)) {
				/*Intent intent = new Intent();
				intent.setAction(android.content.Intent.ACTION_VIEW);
				int punt = download_name.lastIndexOf(".");
				String type = download_name.substring(punt + 1);
				String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(type);
				intent.setDataAndType(Uri.fromFile(store_place), mime);*/
                createIntent(store_place);
				PendingIntent pentent = PendingIntent.getActivity(context, 0, getIntent(),
				                                                  PendingIntent.FLAG_UPDATE_CURRENT);
				download_not.setIntent(pentent);
				download_not.setStatus(InfoNotification.SUCCESS);
			} else {
				Log.e("Download", "Download failed. Reply: " + client.getReplyString());
				download_not.setStatus(InfoNotification.FAILURE);
			}
		} catch (FileNotFoundException e) {
			download_not.setStatus(InfoNotification.FILE_NOT_FOUND);
		} catch (IOException e) {
			download_not.setStatus(InfoNotification.IO_EXCEPTION);
		}
	}

    private Intent intent;

    public Intent getIntent() {
        return intent;
    }

    /**Creates a custom intent for this file and stores it.
     * The result can be retrieved by calling {@link #getIntent()}
     * @param file a (downloaded) file.
     */
    private void createIntent(File file) {
        Uri uri = Uri.fromFile(file);
         intent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, getMimeType(file));
    }

    private String getMimeType(File file) {
        String name = file.getName();
        int index = name.lastIndexOf(".");
        if (index == -1) {
            return "*/*";
        }
        return MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(name.substring(index + 1));
    }

	InfoNotification upload_not;
	
	private void uploadAllAction() {
		InputStream input;
		upload_not = new InfoNotification(context, InfoNotification.UPLOAD);
		local_files = local_dir.listFiles();
		for (File f : local_files) {
			upload_not.addFile(f);
		}
		String from_location = local_dir.getName();//.getAbsolutePath();
		//from_location = from_location.substring(from_location.lastIndexOf("/"));
		String to_location = pathname.substring(pathname.lastIndexOf("/", pathname.length()-2)+1);
		upload_not.setFrom(from_location);
		upload_not.setTo(to_location);
		upload_not.show();
		for (int p = 0; p < local_files.length; p++) {
			try {
				File file = new File(local_files[p].getAbsolutePath());
				if (file.isFile()) {
					input = new FileInputStream(file);
					client.setFileType(FTP.BINARY_FILE_TYPE);
					client.setFileTransferMode(FTP.BINARY_FILE_TYPE);
					CopyStreamAdapter streamlistener = new CopyStreamAdapter() {
						@Override
						public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
							if (streamSize != CopyStreamEvent.UNKNOWN_STREAM_SIZE) {
								upload_not.setMaxProgress((int) streamSize);
							}
							upload_not.update((int) totalBytesTransferred);
						}
					};
					client.setCopyStreamListener(streamlistener);
					if (client.storeFile(file.getName(), input)) {
						if (file.delete()) upload_not.setStatus(p, InfoNotification.SUCCESS);
						else upload_not.setStatus(p, InfoNotification.LOCAL_FILE_NOT_DELETED);
					} else {
						upload_not.setStatus(p, InfoNotification.FAILURE);
					}
					input.close();
				} else {
					upload_not.setStatus(p, InfoNotification.IS_NO_FILE);
				}
			} catch (FileNotFoundException e) {
				upload_not.setStatus(p, InfoNotification.FILE_NOT_FOUND);
			} catch (IOException e) {
				upload_not.setStatus(p, InfoNotification.IO_EXCEPTION);
			}
		}
	}

	private String reached_file_name;
	
	public class Background extends AsyncTask<Integer, Void, FTPFile[]> {

		private
		@Action
		int previous_action = -1;
		private
		@Action
		int action = -1;
		private int position = -1;
		private String error = "";

		private Background setPreviousAction(@Action int prev) {
			previous_action = prev;
			return this;
		}

		private void catchFtpClosure() {
			logged_in = false;
			try {
				client.logout();
				client.disconnect();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			error = "Verbinding gesloten door server. Probeer later opnieuw.";
		}

		@Override
		protected FTPFile[] doInBackground(@Action Integer... arg0) {
			if (arg0.length >= 1) action = arg0[0];
			if (arg0.length >= 2) position = arg0[1];
			if (!client.isConnected()) {
				logged_in = false; // We can't be logged in
				try {
					String server = "ftp.kempengemeenten.nl";
					client.connect(server, 21);
				} catch (IOException e) {
					error = "Kon geen verbinding met de server maken.";
				}
			}
			if (client.isConnected() && !logged_in) {
				try {
					// This will reset on every connect()
					// MUST HAPPEN AFTER CONNECT AND BEFORE LOGGING IN
					client.enterLocalPassiveMode();
					logged_in = client.login(username, password);
					if (logged_in) {
						client.setControlKeepAliveTimeout(300);
						client.setBufferSize(1024 * 1024);
					} else {
						error = "Inloggen mislukt: gebruikersnaam en/of wachtwoord fout.";
					}
				} catch (FTPConnectionClosedException e) {
					catchFtpClosure();
				} catch (IOException e) {
					error = "Inloggen mislukt (I/O Error)";
				}
			}
			if (client.isConnected() && logged_in) {
				switch (action) {
					case LIST:
						if (reached_file_name == null) {
							try {
								return client.listFiles();
							} catch (FTPConnectionClosedException e) {
								catchFtpClosure();
							} catch (IOException e) {
								error = "Gegevens ophalen mislukt (I/O Error)";
							}
						}
						return null;
					case UPLOAD:
						try {
							client.changeWorkingDirectory(pathname);
							uploadAllAction();
						} catch (FTPConnectionClosedException e) {
							catchFtpClosure();
						} catch (IOException e) {
							error = "Uploaden mislukt (I/O Error)";
						}
						return null;
					case CHANGE_DIR_AND_LIST:
						reached_file_name = null;
						try {
							client.changeWorkingDirectory(pathname);
						} catch (FTPConnectionClosedException e) {
							catchFtpClosure();
						} catch (IOException e) {
							error = "Map ophalen mislukt (I/O Error)";
						}
						return null;
					case MOVE_FORWARD_AND_LIST:
						if (ftp_files != null) {
							if (ftp_files[position].isFile()) {
								reached_file_name = ftp_files[position].getName();
							} else pathname += ftp_files[position].getName() + "/";
							depth++;
							try {
								client.changeWorkingDirectory(pathname);

							} catch (FTPConnectionClosedException e) {
								catchFtpClosure();
							} catch (IOException e) {
								error = "Map ophalen mislukt (I/O Error)";
							}
						}
						return null;
					case MOVE_BACK_AND_LIST:
						reached_file_name = null;
						pathname = pathname.substring(0, pathname.lastIndexOf("/",
								pathname.length() - 2)) + "/";
						depth--;
						try {
							client.changeWorkingDirectory(pathname);
						} catch (FTPConnectionClosedException e) {
							catchFtpClosure();
						} catch (IOException e) {
							error = "Map ophalen mislukt (I/O Error)";
						}
						return null;
					case DOWNLOAD:
						downloadAction(position);
						return null;
					case QUIT:
						try {
							client.logout();
							client.disconnect();
						} catch (FTPConnectionClosedException e) {
							catchFtpClosure();
						} catch (IOException e) {
							error = "Uitloggen mislukt (I/O Error)";
						}
						return null;
					default:
						return null;
					case BrowserAdapter.MOVE_BACK:
						return null;
					case BrowserAdapter.MOVE_FORWARD:
						return null;
				}
			} else {
				if (!client.isConnected() && !logged_in) error = "Niet verbonden";
				else if (!logged_in) error = "Kon niet inloggen";
				return null;
			}
		}

		@Override
		public void onPostExecute(FTPFile[] result) {
			boolean success = error.equals("");
			if (browse_listener != null) {
				switch (action) {
					case DOWNLOAD:
						browse_listener.downloadDone(success, success ? "Gereed" : error, intent);
						break;
					case UPLOAD:
						browse_listener.uploadDone(success, success ? "Uploaden voltooid"
								: error);
						break;
					case CHANGE_DIR_AND_LIST:
						new Background().setPreviousAction(action).execute(LIST);
						break;
					case MOVE_FORWARD_AND_LIST:
						new Background().setPreviousAction(action).execute(LIST);
						break;
					case MOVE_BACK_AND_LIST:
						new Background().setPreviousAction(action).execute(LIST);
						break;
					case LIST:
						ftp_files = result;
						if (ftp_files == null || ftp_files.length == 0) {
							if (reached_file_name != null) {
								browse_listener.toast("Houd ingedrukt om te downloaden");
							} else {
								browse_listener.toast("Leeg");
							}
						}
						notifyDataSetChanged();
						switch (previous_action) {
							case CHANGE_DIR_AND_LIST:
								browse_listener.moveToDirDone(success);
								break;
							case MOVE_FORWARD_AND_LIST:
								browse_listener.moveFurtherDone(success);
								break;
							case MOVE_BACK_AND_LIST:
								browse_listener.moveUpDone(success);
								break;
							case BrowserAdapter.DOWNLOAD:
								break;
							case BrowserAdapter.LIST:
								break;
							case BrowserAdapter.MOVE_BACK:
								break;
							case BrowserAdapter.MOVE_FORWARD:
								break;
							case BrowserAdapter.QUIT:
								break;
							case BrowserAdapter.UPLOAD:
								break;
						}
						break;
					case BrowserAdapter.MOVE_BACK:
						break;
					case BrowserAdapter.MOVE_FORWARD:
						break;
					case BrowserAdapter.QUIT:
						break;
				}
			}
		}
	}
	
	OnBrowserEventListener browse_listener;
	
	public void setOnBrowserEventListener(OnBrowserEventListener l) {
		browse_listener = l;
	}
	
	interface OnBrowserEventListener {
		void moveToDirDone(boolean success);
		void moveUpDone(boolean success);
		void moveFurtherDone(boolean success);
		void toast(String text);
		void uploadDone(boolean success, String detail);
		void downloadDone(boolean success, String detail, Intent intent);
	}
}