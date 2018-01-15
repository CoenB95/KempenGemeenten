package com.cbapps.kempengemeenten;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cb.kempengemeenten.R;
import com.cbapps.kempengemeenten.callback.Predicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author CoenB95
 */

public class FileBrowserAdapter extends RecyclerView.Adapter<FileBrowserAdapter.FileViewHolder> {

	public static final int FILE_TYPE_DOWNLOAD = R.drawable.ic_cloud_download_black_24dp;
	public static final int FILE_TYPE_UPLOAD = R.drawable.ic_cloud_upload_black_24dp;
	public static final int FILE_TYPE_FOLDER = R.drawable.ic_folder;

	private List<FileItem> fileInfos;
	private OnFileSelectedListener listener;
	private Handler handler;

	public FileBrowserAdapter() {
		fileInfos = new ArrayList<>();
		handler = new Handler();
	}

	public void addFile(FileInfo info, boolean enableProgress) {
		handler.post(() -> {
			FileItem item = new FileItem(info);
			item.enableProgress(enableProgress);
			fileInfos.add(item);
			Collections.sort(fileInfos);
			notifyDataSetChanged();
		});
	}

	public void clearFiles() {
		handler.post(() -> {
			fileInfos.clear();
			notifyDataSetChanged();
		});
	}

	public void updateProgress(FileInfo info, double progress) {
		handler.post(() -> {
			for (int i = 0; i < fileInfos.size(); i++) {
				FileItem item = fileInfos.get(i);
				if (item.info.equals(info)) {
					item.setProgress(progress);
					notifyItemChanged(i);
					break;
				}
			}
		});
	}

	public void removeFile(FileInfo info) {
		handler.post(() -> {
			for (int i = 0; i < fileInfos.size(); i++) {
				FileItem item = fileInfos.get(i);
				if (item.info.equals(info)) {
					fileInfos.remove(item);
					notifyItemRemoved(i);
					break;
				}
			}
		});
	}

	public void setAllFiles(Collection<FileInfo> infos) {
		setAllFiles(infos, false);
	}

	public void setAllFiles(Collection<FileInfo> infos, boolean enableProgress) {
		setAllFiles(infos, f -> true, enableProgress);
	}

	public void setAllFiles(Collection<FileInfo> infos, Predicate<FileInfo> filter, boolean enableProgress) {
		setAllFiles(infos, filter, enableProgress, FILE_TYPE_FOLDER);
	}

	public void setAllFiles(Collection<FileInfo> infos, Predicate<FileInfo> filter, boolean enableProgress, int fileType) {
		handler.post(() -> {
			fileInfos.clear();
			for (FileInfo info : infos) {
				if (filter.test(info)) {
					FileItem item = new FileItem(info);
					item.enableProgress(enableProgress);
					item.setFileType(fileType);
					fileInfos.add(item);
				}
			}
			Collections.sort(fileInfos);
			notifyDataSetChanged();
		});
	}

	public void setFileType(FileInfo info, int type) {
		handler.post(() -> {
			for (int i = 0; i < fileInfos.size(); i++) {
				FileItem item = fileInfos.get(i);
				if (item.info.equals(info)) {
					item.setFileType(type);
					notifyItemChanged(i);
					break;
				}
			}
		});
	}

	public void showProgress(FileInfo info, boolean value) {
		handler.post(() -> {
			for (int i = 0; i < fileInfos.size(); i++) {
				FileItem item = fileInfos.get(i);
				if (item.info.equals(info)) {
					item.enableProgress(value);
					notifyItemChanged(i);
					break;
				}
			}
		});
	}

	public void setListener(OnFileSelectedListener listener) {
		this.listener = listener;
	}

	@Override
	public FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.file_progress_list_item, parent, false);
		return new FileViewHolder(view);
	}

	@Override
	public int getItemCount() {
		return fileInfos.size();
	}

	@Override
	public void onBindViewHolder(FileViewHolder holder, int position) {
		holder.setFile(fileInfos.get(position));
	}

	public class FileViewHolder extends RecyclerView.ViewHolder {

		private FileItem item;
		private ImageView fileTypeImageView;
		private TextView fileNameTextView;
		private TextView progressTextView;
		private ProgressBar progressBar;

		public FileViewHolder(View itemView) {
			super(itemView);
			fileTypeImageView = itemView.findViewById(R.id.fileTypeImageView);
			fileNameTextView = itemView.findViewById(R.id.fileNameTextView);
			progressTextView = itemView.findViewById(R.id.progressTextView);
			progressBar = itemView.findViewById(R.id.progressBar);
			itemView.setOnClickListener(view -> {
				if (listener != null)
					listener.onFileSelected(item.info);
			});
		}

		public void setFile(FileItem item) {
			this.item = item;
			if (item.fileType == FILE_TYPE_FOLDER) {
				if (item.info.isDirectory())
					fileTypeImageView.setImageResource(item.fileType);
				else
					fileTypeImageView.setImageDrawable(null);
			} else {
				fileTypeImageView.setImageResource(item.fileType);
			}
			fileNameTextView.setText(item.info.getName());
			progressBar.setVisibility(item.progressEnabled ? View.VISIBLE : View.GONE);
			progressBar.setProgress((int) (item.progress * 100));
			progressTextView.setVisibility(item.progressEnabled ? View.VISIBLE : View.GONE);
			progressTextView.setText(String.format("%.0f%%", Math.floor(item.progress * 100)));
		}
	}

	public interface OnFileSelectedListener {
		void onFileSelected(FileInfo info);
	}

	private static class FileItem implements Comparable<FileItem> {
		private FileInfo info;
		private int fileType;
		private double progress;
		private boolean progressEnabled;

		FileItem(FileInfo info) {
			this.info = info;
		}

		public void enableProgress(boolean value) {
			this.progressEnabled = value;
		}

		public void setProgress(double progress) {
			this.progress = progress;
		}

		public void setFileType(int fileType) {
			this.fileType = fileType;
		}

		@Override
		public int compareTo(@NonNull FileItem fileItem) {
			return info.compareTo(fileItem.info);
		}
	}
}
