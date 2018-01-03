package com.cbapps.kempengemeenten.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import com.cb.kempengemeenten.R;
import com.cbapps.kempengemeenten.database.LmsPoint;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.File;

/**
 * @author CoenB95
 */

public class LmsDetailFragment extends DialogFragment {

	public static final int CAPTURE_REQUEST_CODE = 301;

	private TextView pointTownView;
	private TextView pointStreetView;
	private Switch pointMeasuredSwitch;

	private LmsPoint shownPoint;
	private SharedPreferences preferences;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.point_detail_layout, container, false);

		preferences = PreferenceManager.getDefaultSharedPreferences(getContext());

		pointTownView = view.findViewById(R.id.pointTown);
		pointStreetView = view.findViewById(R.id.pointStreet);
		pointMeasuredSwitch = view.findViewById(R.id.pointMeasuredSwitch);

//		Button pointCaptureButton = view.findViewById(R.id.pointUploadButton);
//		pointCaptureButton.setOnClickListener(v -> {
//			String storageDir = preferences.getString("localUploadLocation", null);
//			if (storageDir == null) {
//				new AlertDialog.Builder(getContext())
//						.setTitle(R.string.location_not_set_title)
//						.setMessage(R.string.location_not_set_description)
//						.setPositiveButton(R.string.ok, null)
//						.show();
//				return;
//			}
//			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//			if (intent.resolveActivity(getContext().getPackageManager()) != null) {
//				String timeStamp = LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE);
//				String fileName = "LMS_" + shownPoint.getLmsNumber() + "_" + timeStamp + ".jpg";
//				File image = new File(storageDir, fileName);
//
//				intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(getContext(),
//						"com.cbapps.kempengemeenten.fileprovider", image));
//				startActivity(intent);
//			}
//		});

		return view;
	}

	public void showDetail(LmsPoint point) {
		shownPoint = point;
		pointStreetView.setText(point.getAddress());
		pointTownView.setText(point.getTown());
		pointMeasuredSwitch.setChecked(point.isMeasured());
	}
}
