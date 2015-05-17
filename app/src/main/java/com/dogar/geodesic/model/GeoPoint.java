package com.dogar.geodesic.model;

import android.content.ContentValues;
import android.database.Cursor;

import lombok.AllArgsConstructor;
import lombok.Data;

import static com.dogar.geodesic.sync.PointsContract.Entry.*;
import static com.dogar.geodesic.sync.SyncAdapter.COLUMN_DATE_OF_INSERT;
import static com.dogar.geodesic.sync.SyncAdapter.COLUMN_DELETE;
import static com.dogar.geodesic.sync.SyncAdapter.COLUMN_DIRTY;
import static com.dogar.geodesic.sync.SyncAdapter.COLUMN_INFO;
import static com.dogar.geodesic.sync.SyncAdapter.COLUMN_LATITUDE;
import static com.dogar.geodesic.sync.SyncAdapter.COLUMN_LONGITUDE;
import static com.dogar.geodesic.sync.SyncAdapter.COLUMN_TITLE;
import static com.dogar.geodesic.utils.Constants.SQLITE_TRUE;

/**
 * Created by lester on 13.05.15.
 */
@Data
@AllArgsConstructor(suppressConstructorProperties = true)
public class GeoPoint {
    private String title;
    private String info;
    private long   insertDate;
    private int    dirty;
    private int    deleted;
    private String latitude;
    private String longitude;
    private String accountName;

    public GeoPoint(Cursor c) {
        title = c.getString(COLUMN_TITLE);
        info = c.getString(COLUMN_INFO);
        insertDate = c.getLong(COLUMN_DATE_OF_INSERT);
        dirty = c.getInt(COLUMN_DIRTY);
        deleted = c.getInt(COLUMN_DELETE);
        latitude = c.getString(COLUMN_LATITUDE);
        longitude = c.getString(COLUMN_LONGITUDE);
    }

    public ContentValues toCVWithoutId() {
        ContentValues cv = new ContentValues();
        cv.putNull(COLUMN_NAME_POINT_ID);
        cv.put(COLUMN_NAME_TITLE, title);
        cv.put(COLUMN_NAME_INFO, info);
        cv.put(COLUMN_NAME_DATE_OF_INSERT, insertDate);
        cv.put(COLUMN_NAME_LATITUDE, latitude);
        cv.put(COLUMN_NAME_LONGITUDE, longitude);
        cv.put(COLUMN_NAME_DIRTY, dirty);
        cv.put(COLUMN_NAME_DELETE, deleted);
        cv.put(COLUMN_NAME_ACCOUNT, accountName);
        return cv;
    }

}
