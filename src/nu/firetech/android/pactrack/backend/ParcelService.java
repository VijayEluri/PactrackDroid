/*
 * Copyright (C) 2009 Joakim Andersson
 * 
 * This file is part of PactrackDroid, an Android application to keep
 * track of parcels sent with the Swedish mail service (Posten).
 * 
 * PactrackDroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * PactrackDroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package nu.firetech.android.pactrack.backend;

import nu.firetech.android.pactrack.common.ContextListener;
import nu.firetech.android.pactrack.common.RefreshContext;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class ParcelService extends Service implements RefreshContext {
	private static final String TAG = "<PactrackDroid> ParcelService";

	private Handler mHandler;
	private ParcelDbAdapter mDbAdapter;

	@Override
	public void onCreate() {
		mHandler = new Handler();
		mDbAdapter = new ParcelDbAdapter(this);
		mDbAdapter.open();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		Log.d(TAG, "Automatic update initiated");
		ParcelUpdater.updateAll(true, this, mDbAdapter);
	}

	@Override
	public void onDestroy() {
		mDbAdapter.close();
	}

	@Override
	public Handler getProgressHandler() {
		return mHandler;
	}

	@Override
	public void startRefreshProgress(int maxValue, ContextListener listener) {
		Log.d(TAG, "Automatic update running");
	}

	@Override
	public void refreshDone() {
		stopSelf();	
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
