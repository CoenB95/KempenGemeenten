/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.cbapps.kempengemeenten;

import com.cbapps.kempengemeenten.BrowserAdapter.OnBrowserEventListener;
import com.cb.kempengemeenten.R;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class FTPChooser extends DialogPreference {

		EditText text;
		ListView list;
		ImageButton back;
		ProgressBar wait;
		Context context;
		BrowserAdapter adapter;
		
		public FTPChooser(Context _context, AttributeSet attrs) {
			super(_context, attrs);
			context = _context;
			setSummary("Kies een map");
			setDialogLayoutResource(R.layout.choose_preference);
			setPositiveButtonText(android.R.string.ok);
			setNegativeButtonText(android.R.string.cancel);
			setDialogIcon(null);
		}
		
		@Override
		protected void onSetInitialValue(boolean restorePersistedValue,
			Object defaultValue) {
			if (!restorePersistedValue) {
				setSummary("Kies een map");
			}else{
				if (getKey().equals("quickdownload")) {
					setSummary("Van "+getPersistedString(""));
				}else{
					setSummary("Naar "+getPersistedString(""));
				}
			}
			super.onSetInitialValue(restorePersistedValue, defaultValue);
		}
		
		@Override
		protected void onBindDialogView(@NonNull View view) {
			super.onBindDialogView(view);
			text = (EditText) view.findViewById(R.id.filePathEditText);
			list = (ListView) view.findViewById(R.id.listView1);
			list.setVisibility(View.GONE);
			back = (ImageButton) view.findViewById(R.id.upButton);
			wait = (ProgressBar) view.findViewById(R.id.progressBar1);
			back.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (adapter.canGoUp()) crossfade(list, wait);
					adapter.handleBack();
					text.setText(adapter.getCurrentLink());
				}
			});
			SharedPreferences p = getSharedPreferences();
			adapter = new BrowserAdapter(context, BrowserAdapter.FTP_SEARCH)
					          .setUserAndPassword(p.getString("gebruikersnaam", ""),
					                              p.getString("wachtwoord", ""));
			if (!(getPersistedString("").equals(""))) {
				if (getKey().equals("serverlocatie")) {
					adapter.setCurrentLink(getPersistedString(""));
					text.setText(getPersistedString(""));
				}else if (getKey().equals("quickdownload")) {
					int point = getPersistedString("").lastIndexOf("/");
					adapter.setCurrentLink(getPersistedString("").substring(0, point+1));
					text.setText(getPersistedString("").substring(0, point+1));
				}
			}
			adapter.setOnBrowserEventListener(new OnBrowserEventListener() {
				@Override
				public void toast(String text) {
					Toast.makeText(context, text, Toast.LENGTH_LONG).show();
				}

				@Override
				public void uploadDone(boolean success, String detail) {
					crossfade(wait, list);
					text.setText(adapter.getCurrentLink());
				}

				@Override
				public void downloadDone(boolean success, String detail, Intent intent) {
					crossfade(wait, list);
				}

				@Override
				public void moveToDirDone(boolean success) {
					crossfade(wait, list);
					text.setText(adapter.getCurrentLink());
				}

				@Override
				public void moveUpDone(boolean success) {
					if (success) crossfade(wait, list);
					text.setText(adapter.getCurrentLink());
				}

				@Override
				public void moveFurtherDone(boolean success) {
					crossfade(wait, list);
					text.setText(adapter.getCurrentLink());
				}
			});
			list.setAdapter(adapter);
			list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int position, long arg3) {
					crossfade(list, wait);
					adapter.handleClick(position);
					text.setText(adapter.getCurrentLink());
				}
			});
			list.setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> parent,
						View view, int position, long id) {
					adapter.handleLongClick(position);
					return true;
				}
			});
		}
		
		private View fade_from;
		private View fade_to;
		
		public void crossfade(final View from, View to) {
			to.setAlpha(0f);
			to.setVisibility(View.VISIBLE);
			to.animate()
					.alpha(1f)
					.setDuration(500)
					.setListener(null);

			from.animate()
					.alpha(0f)
					.setDuration(500)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							from.setVisibility(View.GONE);
						}
					});
		}
		
		@Override
		protected void onDialogClosed(boolean positive) {
			if (positive) {
				if (text != null) {
					String value = text.getText().toString();
					if (value.equals("")) {
						Log.d("Preference", "You have upload_not choosed a directory");
					}else{
						Log.d("Preference", "You choosed "+value);
						persistString(value);
						if (getKey().equals("quickdownload")) {
							setSummary("Van "+getPersistedString(""));
						}else{
							setSummary("Naar "+getPersistedString(""));
						}
					}
				}
			}
		}
	}