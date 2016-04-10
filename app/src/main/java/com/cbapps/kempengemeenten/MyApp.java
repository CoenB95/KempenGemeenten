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

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

import com.cb.kempengemeenten.R;

@ReportsCrashes(mailTo = "boelhouwers.coen@gmail.com",
mode = ReportingInteractionMode.TOAST, resToastText = R.string.acra_toast/*,
resDialogTitle = R.string.acra_title, resDialogText = R.string.acra_text,
resDialogOkToast = R.string.acra_done*/)
public class MyApp extends Application {
	
	//Implementation of ACRA to send crashes
	
	@Override
	public void onCreate() {
		super.onCreate();
		ACRA.init(this);
	}
}