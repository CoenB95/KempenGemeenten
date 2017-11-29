package com.cbapps.kempengemeenten.nextgen;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cb.kempengemeenten.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author CoenB95
 */

public class FileBrowserAdapter extends RecyclerView.Adapter<FileBrowserAdapter.FileViewHolder> {

	private List<FileInfo> fileInfos;

	public FileBrowserAdapter() {
		fileInfos = new ArrayList<>();
	}

	public void clearFiles() {
		fileInfos.clear();
		notifyDataSetChanged();
	}

	public void setAllFiles(Collection<FileInfo> infos) {
		fileInfos.clear();
		fileInfos.addAll(infos);
		Collections.sort(fileInfos);
		notifyDataSetChanged();
	}

	@Override
	public FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.file_list_item, parent, false);
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

		private FileInfo info;
		private TextView fileNameTextView;

		public FileViewHolder(View itemView) {
			super(itemView);
			fileNameTextView = itemView.findViewById(R.id.fileNameTextView);
		}

		public void setFile(FileInfo info) {
			this.info = info;
			fileNameTextView.setText(info.getName());
		}
	}

	public interface OnFileSelectedListener {
		void onFileSelected(FileInfo info);
	}
}
