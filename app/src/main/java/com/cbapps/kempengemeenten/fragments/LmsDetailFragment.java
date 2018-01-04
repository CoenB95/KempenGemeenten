package com.cbapps.kempengemeenten.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.cb.kempengemeenten.R;
import com.cbapps.kempengemeenten.database.LmsDatabase;
import com.cbapps.kempengemeenten.database.LmsPoint;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
	private ExecutorService service;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.point_detail_layout, container, false);

		preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
		service = Executors.newCachedThreadPool();

		pointTownView = view.findViewById(R.id.pointTown);
		pointStreetView = view.findViewById(R.id.pointStreet);
		pointMeasuredSwitch = view.findViewById(R.id.pointMeasuredSwitch);

		pointMeasuredSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
			if (shownPoint == null)
				return;
			service.submit(() ->
				LmsDatabase.newInstance(getContext()).lmsDao().update(shownPoint.measured()));
		});

		if (shownPoint != null)
			showDetail(shownPoint);
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
		if (pointTownView == null || pointStreetView == null)
			return;
		pointStreetView.setText(point.getAddress());
		pointTownView.setText(point.getTown());
		pointMeasuredSwitch.setChecked(point.isMeasured());
		pointMeasuredSwitch.setEnabled(!point.isMeasured());
	}
}
