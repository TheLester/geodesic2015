package com.dogar.geodesic.model;

import android.content.ContentValues;

import lombok.AllArgsConstructor;
import lombok.Data;

import static com.dogar.geodesic.sync.PointsContract.Entry.*;

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
