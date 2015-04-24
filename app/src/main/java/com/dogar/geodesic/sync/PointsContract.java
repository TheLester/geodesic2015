package com.dogar.geodesic.sync;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public class PointsContract {
	private PointsContract() {
	}

	public static final String CONTENT_AUTHORITY = "com.dogar.geodesic";
	public static final Uri BASE_CONTENT_URI = Uri.parse("content://"
			+ CONTENT_AUTHORITY);
	public static final String ACCOUNT_TYPE = "com.google";
	private static final String PATH_ENTRIES = "entries";

	public static class Entry implements BaseColumns {
		/**
		 * MIME type for lists of entries.
		 */
		public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
				+ "/vnd.basicsyncadapter.entries";
		/**
		 * MIME type for individual entries.
		 */
		public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
				+ "/vnd.basicsyncadapter.entry";
		/**
		 * Fully qualified URI for "entry" resources.
		 */
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
				.appendPath(PATH_ENTRIES).build();
		public static final String TABLE_NAME = "entry";
		public static final String COLUMN_NAME_POINT_ID = "point_id";
		public static final String COLUMN_NAME_LATITUDE = "latitude";
		public static final String COLUMN_NAME_LONGITUDE = "longitude";
		public static final String COLUMN_NAME_DATE_OF_INSERT = "insert_date";
		public static final String COLUMN_NAME_TITLE = "title";
		public static final String COLUMN_NAME_INFO = "info";
		public static final String COLUMN_NAME_ACCOUNT = "account";
		public static final String COLUMN_NAME_DIRTY = "dirty";
		public static final String COLUMN_NAME_DELETE = "deleted";
	}

}
