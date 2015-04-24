package com.dogar.geodesic.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.dogar.geodesic.CloudEndpointUtils;
import com.dogar.geodesic.geopointinfoendpoint.Geopointinfoendpoint;
import com.dogar.geodesic.geopointinfoendpoint.model.GeoPointInfo;
import com.dogar.geodesic.map.MainActivity;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.jackson.JacksonFactory;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
	private static Geopointinfoendpoint.Builder endpointBuilder;
	private static Geopointinfoendpoint geopointInfoEndpoint;
	private static GoogleAccountCredential credential;
	public static final String ACCOUNT_FILTER = "account = ?";
	public static final String LAT_LONG_FILTER = " AND latitude =? AND longitude =?";

	private static final String TAG = "SyncAdapter";
	/**
	 * Content resolver, for performing database operations.
	 */
	private final ContentResolver mContentResolver;
	/**
	 * Project used when querying content provider. Returns all known fields.
	 */
	public static final String[] PROJECTION = new String[] {
			PointsContract.Entry._ID,
			PointsContract.Entry.COLUMN_NAME_POINT_ID,
			PointsContract.Entry.COLUMN_NAME_LATITUDE,
			PointsContract.Entry.COLUMN_NAME_LONGITUDE,
			PointsContract.Entry.COLUMN_NAME_DATE_OF_INSERT,
			PointsContract.Entry.COLUMN_NAME_TITLE,
			PointsContract.Entry.COLUMN_NAME_INFO,
			PointsContract.Entry.COLUMN_NAME_ACCOUNT,
			PointsContract.Entry.COLUMN_NAME_DIRTY,
			PointsContract.Entry.COLUMN_NAME_DELETE };
	// Constants representing column positions from PROJECTION.
	public static final int COLUMN_ID = 0;
	public static final int COLUMN_POINT_ID = 1;
	public static final int COLUMN_LATITUDE = 2;
	public static final int COLUMN_LONGITUDE = 3;
	public static final int COLUMN_DATE_OF_INSERT = 4;
	public static final int COLUMN_TITLE = 5;
	public static final int COLUMN_INFO = 6;
	public static final int COLUMN_DIRTY = 8;
	public static final int COLUMN_DELETE = 9;
	private Account account;

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		mContentResolver = context.getContentResolver();

	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {
		this.account = account;
		updateLocalFeedData(syncResult);

	}

	private void updateLocalFeedData(SyncResult syncResult) {
		final ContentResolver contentResolver = getContext()
				.getContentResolver();

		ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
		List<Long> nonInsertIDs = new ArrayList<Long>();
		// Get list of all items
		Log.i(TAG, "Fetching local entries for merge");
		Uri uri = PointsContract.Entry.CONTENT_URI; // Get all entries
		Cursor c = contentResolver.query(uri, PROJECTION, ACCOUNT_FILTER,
				new String[] { account.name }, null);

		Log.i(TAG, "Found " + c.getCount()
				+ " local entries. Computing merge solution...");

		// Find stale data
		int id;
		Long pointId;
		Double latitude;
		Double longitude;
		Long dateOfInsert;
		String title;
		String info;
		boolean isDirty;
		boolean isDelete;

		while (c.moveToNext()) {
			syncResult.stats.numEntries++;
			id = c.getInt(COLUMN_ID);// getInt returns 0 if val=null
			pointId = c.getLong(COLUMN_POINT_ID);
			latitude = Double.valueOf(c.getString(COLUMN_LATITUDE));
			longitude = Double.valueOf(c.getString(COLUMN_LONGITUDE));
			dateOfInsert = c.getLong(COLUMN_DATE_OF_INSERT);
			title = c.getString(COLUMN_TITLE);
			info = c.getString(COLUMN_INFO);
			isDirty = (c.getInt(COLUMN_DIRTY) == 1);
			isDelete = (c.getInt(COLUMN_DELETE) == 1);
			if (pointId != 0) {
				nonInsertIDs.add(pointId);
				GeoPointInfo geoPointToUpdate = getGeoPointFromDataStore(pointId);
				// Check to see if the entry needs to be updated
				Uri existingUri = PointsContract.Entry.CONTENT_URI.buildUpon()
						.appendPath(Integer.toString(id)).build();
				if (isDirty && geoPointToUpdate!=null) {
					// Update existing record
					Log.i(TAG, "Scheduling update: " + existingUri);
					try {
						geoPointToUpdate.setLongitude(longitude);
						geoPointToUpdate.setLatitude(latitude);
						geoPointToUpdate.setTitleInfo(title);
						geoPointToUpdate.setTextInfo(info);
						geoPointToUpdate.setTimestamp(dateOfInsert);
						getEndpoint().updateGeoPointInfo(geoPointToUpdate)
								.execute();
					} catch (IOException e1) {
						Log.i(TAG, "Exception when update geoPoint");
						e1.printStackTrace();
					}
					syncResult.stats.numUpdates++;
					batch.add(ContentProviderOperation
							.newUpdate(existingUri)
							.withValue(PointsContract.Entry.COLUMN_NAME_DIRTY,
									0).build());
				} else {
					Log.i(TAG, "No action: " + existingUri);
				}
				Log.i(TAG, "isDELETE" + isDelete);
				if (isDelete && geoPointToUpdate!=null) {
					geoPointToUpdate.setDeleted(true);
					try {
						getEndpoint().updateGeoPointInfo(geoPointToUpdate)
								.execute();
						deleteEntry(id, batch);
						syncResult.stats.numDeletes++;
					} catch (IOException e) {
						e.printStackTrace();
					}

				}
			} else if (pointId == 0) {
				try {
					GeoPointInfo geoPointToInsert = new GeoPointInfo();
					geoPointToInsert.setLatitude(latitude);
					geoPointToInsert.setLongitude(longitude);
					geoPointToInsert.setTimestamp(dateOfInsert);
					geoPointToInsert.setTitleInfo(title);
					geoPointToInsert.setTextInfo(info);
					if (isDelete)
						geoPointToInsert.setDeleted(true);
					getEndpoint().insertGeoPointInfo(geoPointToInsert)
							.execute();
					Log.i(TAG, "Sheduling insert to datastore point latitude"
							+ latitude + ",longitude-" + longitude);
					// delete
					deleteEntry(id, batch);
					syncResult.stats.numDeletes++;
				} catch (IOException e1) {
					Log.i(TAG, "Exception when insert to datastore");
					e1.printStackTrace();
				}
			}
		}
		c.close();
		List<GeoPointInfo> insertedRemoteEntries = getGeoPointsFromDataStore();
		Log.i(TAG, "SIZE-" + insertedRemoteEntries.size());
		for (GeoPointInfo g : insertedRemoteEntries) {
			if (!nonInsertIDs.contains(g.getId())) {
				Log.i(TAG, "Scheduling insert: entry_id=" + g.getId());
				insertToBatch(batch, g);
				syncResult.stats.numInserts++;
			}
		}
		Log.i(TAG, "Merge solution ready. Applying batch update");
		try {
			mContentResolver
					.applyBatch(PointsContract.CONTENT_AUTHORITY, batch);
		} catch (RemoteException e1) {
			e1.printStackTrace();
		} catch (OperationApplicationException e1) {
			e1.printStackTrace();
		}

		mContentResolver.notifyChange(PointsContract.Entry.CONTENT_URI, // URI
																		// where
																		// data
																		// was
																		// modified
				null, // No local observer
				false); // IMPORTANT: Do not sync to network
		// This sample doesn't support uploads, but if *your* code does, make
		// sure you set
		// syncToNetwork=false in the line above to prevent duplicate syncs.

		// Add new items

	}

	private void deleteEntry(int id, ArrayList<ContentProviderOperation> batch) {
		Uri deleteUri = PointsContract.Entry.CONTENT_URI.buildUpon()
				.appendPath(Integer.toString(id)).build();
		Log.i(TAG, "Scheduling delete: " + deleteUri);
		batch.add(ContentProviderOperation.newDelete(deleteUri).build());

	}

	private void insertToBatch(ArrayList<ContentProviderOperation> batch,
			GeoPointInfo g) {
		batch.add(ContentProviderOperation
				.newInsert(PointsContract.Entry.CONTENT_URI)
				.withValue(PointsContract.Entry.COLUMN_NAME_POINT_ID, g.getId())
				.withValue(PointsContract.Entry.COLUMN_NAME_LATITUDE,
						String.valueOf(g.getLatitude()))
				.withValue(PointsContract.Entry.COLUMN_NAME_LONGITUDE,
						String.valueOf(g.getLongitude()))
				.withValue(PointsContract.Entry.COLUMN_NAME_DATE_OF_INSERT,
						g.getTimestamp())
				.withValue(PointsContract.Entry.COLUMN_NAME_TITLE,
						g.getTitleInfo())
				.withValue(PointsContract.Entry.COLUMN_NAME_INFO,
						g.getTextInfo())
				.withValue(PointsContract.Entry.COLUMN_NAME_ACCOUNT,
						account.name).build());

	}

	private GeoPointInfo getGeoPointFromDataStore(Long id) {
		try {
			return getEndpoint().getGeoPointInfo(id).execute();
		} catch (IOException e) {
			Log.i(TAG, "Error when getting point with id " + id);
			e.printStackTrace();
			return null;
		}
	}

	private List<GeoPointInfo> getGeoPointsFromDataStore() {
		List<GeoPointInfo> listOfPoints;
		try {
			listOfPoints = getEndpoint().listGeoPointInfo().execute()
					.getItems();
		} catch (IOException e) {
			Log.i(TAG, "Exception when get all points from datastore");
			e.printStackTrace();
			return Collections.emptyList();
		}
		if (listOfPoints != null)
			return listOfPoints;
		else
			return Collections.emptyList();
	}

	private Geopointinfoendpoint getEndpoint() {
		try {
			credential = MainActivity.getCredential();
		} catch (Exception ex) {
			credential = createAndGetCredential();
		}

		if (endpointBuilder == null) {
			endpointBuilder = new Geopointinfoendpoint.Builder(
					AndroidHttp.newCompatibleTransport(), new JacksonFactory(),
					credential == null ? createAndGetCredential() : credential);
		}
		return geopointInfoEndpoint == null ? CloudEndpointUtils.updateBuilder(
				endpointBuilder).build() : geopointInfoEndpoint;
	}

	private GoogleAccountCredential createAndGetCredential() {
		SharedPreferences settings = getContext().getSharedPreferences(
				"Geodesic", 0);
		GoogleAccountCredential credential = GoogleAccountCredential
				.usingAudience(
						getContext(),
						"server:client_id:274430009138-4f8ufdnqj56u9mrclvamdfdah9kujcs1.apps.googleusercontent.com");
		credential.setSelectedAccountName(settings.getString("ACCOUNT_NAME",
				null));
		return credential;
	}
}
