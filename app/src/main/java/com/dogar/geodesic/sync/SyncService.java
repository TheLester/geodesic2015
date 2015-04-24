package com.dogar.geodesic.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class SyncService extends Service {
	private static final Object sSyncAdapterLock = new Object();
	private static SyncAdapter sSyncAdapter = null;

	/**
	 * Thread-safe constructor, creates static {@link SyncAdapter} instance.
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		synchronized (sSyncAdapterLock) {
			if (sSyncAdapter == null) {
				sSyncAdapter = new SyncAdapter(getApplicationContext(), true);
			}
		}
	}

	/**
	 * Return Binder handle for IPC communication with {@link SyncAdapter}.
	 *
	 * <p>
	 * New sync requests will be sent directly to the SyncAdapter using this
	 * channel.
	 *
	 * @param intent
	 *            Calling intent
	 * @return Binder handle for {@link SyncAdapter}
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return sSyncAdapter.getSyncAdapterBinder();
	}

}
