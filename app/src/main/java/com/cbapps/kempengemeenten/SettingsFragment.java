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

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.cb.kempengemeenten.R;
import com.cbapps.kempengemeenten.nextgen.fragments.FileBrowserFragment;

public class SettingsFragment extends PreferenceFragmentCompat {

	private static final String DIALOG_FRAGMENT_TAG = "SettingsFragment.DIALOG";

	private DialogFragment onCreatePreferenceDialogFragment(Preference preference) {
		if (preference instanceof LocationChooserDialog) {
			FileBrowserFragment browserFragment = new FileBrowserFragment();
			browserFragment.browseFTP();
			return browserFragment;
		}
		return null;
	}

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
		addPreferencesFromResource(R.xml.preferences);
	}

	@Override
	public void onDisplayPreferenceDialog(Preference preference) {
		if (getFragmentManager() == null)
			return;

		// Check if dialog is already showing
		if (getFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG) != null)
			return;

		DialogFragment fragment;
		// Check for custom dialogs
		if ((fragment = onCreatePreferenceDialogFragment(preference)) == null) {
			// No custom dialog for this preference, use default inflation.
			super.onDisplayPreferenceDialog(preference);
			return;
		}

		fragment.setTargetFragment(this, 0);
		fragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
	}
}