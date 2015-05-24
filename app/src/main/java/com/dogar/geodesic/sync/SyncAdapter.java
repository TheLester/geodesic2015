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
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.dogar.geodesic.CloudEndpointUtils;
import com.dogar.geodesic.geopointinfoendpoint.Geopointinfoendpoint;
import com.dogar.geodesic.geopointinfoendpoint.model.GeoPointInfo;
import com.dogar.geodesic.model.LocalGeoPoint;
import com.dogar.geodesic.utils.Constants;
import com.dogar.geodesic.utils.SharedPreferencesUtils;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.jackson.JacksonFactory;

import static com.dogar.geodesic.utils.Constants.*;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String ACCOUNT_FILTER  = "account = ?";
    public static final String LAT_LONG_FILTER = " AND latitude =? AND longitude =?";

    private static final String TAG = "SyncAdapter";
    /**
     * Content resolver, for performing database operations.
     */
    private final ContentResolver mContentResolver;
    /**
     * Project used when querying content provider. Returns all known fields.
     */
    public static final String[] PROJECTION = new String[]{
            PointsContract.Entry._ID,
            PointsContract.Entry.COLUMN_NAME_REMOTE_POINT_ID,
            PointsContract.Entry.COLUMN_NAME_LATITUDE,
            PointsContract.Entry.COLUMN_NAME_LONGITUDE,
            PointsContract.Entry.COLUMN_NAME_DATE_OF_INSERT,
            PointsContract.Entry.COLUMN_NAME_TITLE,
            PointsContract.Entry.COLUMN_NAME_INFO,
            PointsContract.Entry.COLUMN_NAME_ACCOUNT,
            PointsContract.Entry.COLUMN_NAME_DIRTY,
            PointsContract.Entry.COLUMN_NAME_DELETE};

    // Constants representing column positions from PROJECTION.
    public static final int COLUMN_ID              = 0;
    public static final int COLUMN_POINT_REMOTE_ID = 1;
    public static final int COLUMN_LATITUDE        = 2;
    public static final int COLUMN_LONGITUDE       = 3;
    public static final int COLUMN_DATE_OF_INSERT  = 4;
    public static final int COLUMN_TITLE           = 5;
    public static final int COLUMN_INFO            = 6;
    public static final int COLUMN_DIRTY           = 8;
    public static final int COLUMN_DELETE          = 9;
    private Account                 account;
    private GoogleAccountCredential credential;
    private Geopointinfoendpoint    geopointInfoEndpoint;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContentResolver = context.getContentResolver();

    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        this.account = account;
        initCredential();
        initEndpoint();
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
        Cursor cursor = contentResolver.query(uri, PROJECTION, ACCOUNT_FILTER,
                new String[]{account.name}, null);

        Log.i(TAG, "Found " + cursor.getCount()
                + " local entries. Computing merge solution...");

        try {
            while (cursor.moveToNext()) {
                syncResult.stats.numEntries++;
                LocalGeoPoint localGeoPoint = new LocalGeoPoint(cursor);
                long pointId = localGeoPoint.getRemoteId();
                boolean isDirty = localGeoPoint.getDirty() == SQLITE_TRUE;
                boolean isDelete = localGeoPoint.getDeleted() == SQLITE_TRUE;
                Double longitude = Double.valueOf(localGeoPoint.getLongitude());
                Double latitude = Double.valueOf(localGeoPoint.getLatitude());

                if (pointId != 0) {
                    nonInsertIDs.add(pointId);
                    GeoPointInfo geoPointToUpdate = getGeoPointFromDataStore(pointId);
                    // Check to see if the entry needs to be updated
                    Uri existingUri = PointsContract.Entry.CONTENT_URI.buildUpon()
                            .appendPath(Integer.toString(localGeoPoint.getId())).build();

                    if (isDirty && geoPointToUpdate != null) {//point is dirty(should be updated)
                        // Update existing record
                        Log.i(TAG, "Scheduling update: " + existingUri);
                        try {
                            geoPointToUpdate.setLongitude(longitude);
                            geoPointToUpdate.setLatitude(latitude);
                            geoPointToUpdate.setTitleInfo(localGeoPoint.getTitle());
                            geoPointToUpdate.setTextInfo(localGeoPoint.getInfo());
                            geoPointToUpdate.setTimestamp(localGeoPoint.getInsertDate());
                            geopointInfoEndpoint.updateGeoPointInfo(geoPointToUpdate)
                                    .execute();
                        } catch (IOException e1) {
                            Log.i(TAG, "Exception when update geoPoint");
                            e1.printStackTrace();
                        }
                        syncResult.stats.numUpdates++;
                        markPointAsClean(batch, existingUri);
                    } else {
                        Log.i(TAG, "No action: " + existingUri);
                    }

                    Log.i(TAG, "isDELETE" + isDelete);

                    if (isDelete && geoPointToUpdate != null) {
                        geoPointToUpdate.setDeleted(true);
                        try {
                            geopointInfoEndpoint.updateGeoPointInfo(geoPointToUpdate)
                                    .execute();
                            deletePoint(localGeoPoint.getId(), batch);
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
                        geoPointToInsert.setTimestamp(localGeoPoint.getInsertDate());
                        geoPointToInsert.setTitleInfo(localGeoPoint.getTitle());
                        geoPointToInsert.setTextInfo(localGeoPoint.getInfo());
                        //send deleted point as a history to datastore
                        if (isDelete) {
                            geoPointToInsert.setDeleted(true);
                        }
                        geopointInfoEndpoint.insertGeoPointInfo(geoPointToInsert)
                                .execute();
                        Log.i(TAG, "Sheduling insert to datastore point latitude"
                                + latitude + ",longitude-" + longitude);
                        // delete
                        deletePoint(localGeoPoint.getId(), batch);
                        syncResult.stats.numDeletes++;
                    } catch (IOException e1) {
                        Log.i(TAG, "Exception when insert to datastore");
                        e1.printStackTrace();
                    }
                }
            }

        } finally {
            cursor.close();
        }
        //Retrieve inserted points
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
        applyCPBatch(batch);
    }

    private void applyCPBatch(ArrayList<ContentProviderOperation> batch) {

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
    }


    private void markPointAsClean(ArrayList<ContentProviderOperation> batch, Uri existingUri) {
        //mark point as "clean" after successful updating
        batch.add(ContentProviderOperation
                .newUpdate(existingUri)
                .withValue(PointsContract.Entry.COLUMN_NAME_DIRTY,
                        0).build());
    }

    private void deletePoint(int id, ArrayList<ContentProviderOperation> batch) {
        Uri deleteUri = PointsContract.Entry.CONTENT_URI.buildUpon()
                .appendPath(Integer.toString(id)).build();
        Log.i(TAG, "Scheduling delete: " + deleteUri);
        batch.add(ContentProviderOperation.newDelete(deleteUri).build());

    }

    private void insertToBatch(ArrayList<ContentProviderOperation> batch,
                               GeoPointInfo g) {
        batch.add(ContentProviderOperation
                .newInsert(PointsContract.Entry.CONTENT_URI)
                .withValue(PointsContract.Entry.COLUMN_NAME_REMOTE_POINT_ID, g.getId())
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
            return geopointInfoEndpoint.getGeoPointInfo(id).execute();
        } catch (IOException e) {
            Log.i(TAG, "Error when getting point with id " + id);
            e.printStackTrace();
            return null;
        }
    }

    private List<GeoPointInfo> getGeoPointsFromDataStore() {
        List<GeoPointInfo> listOfPoints;
        try {
            listOfPoints = geopointInfoEndpoint.listGeoPointInfo().execute()
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

    private void initEndpoint() {
        Log.i(TAG, "getEndpoint");
        Geopointinfoendpoint.Builder endpointBuilder = new Geopointinfoendpoint.Builder(
                AndroidHttp.newCompatibleTransport(), new JacksonFactory(), credential);
        geopointInfoEndpoint = CloudEndpointUtils.updateBuilder(
                endpointBuilder).build();
    }

    private void initCredential() {
        Log.i(TAG, "initCredential");
        credential = GoogleAccountCredential
                .usingAudience(
                        getContext(),
                        Constants.AUDIENCE);
        credential.setSelectedAccountName(SharedPreferencesUtils.getLoginEmail(getContext()));
    }
}
