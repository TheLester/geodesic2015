package com.dogar.geodesic.sync;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import static com.dogar.geodesic.sync.PointsContract.Entry.*;

public class PointsProvider extends ContentProvider {
	PointsDatabase mDatabaseHelper;

	// Content authority for this provider.
	private static final String AUTHORITY = PointsContract.CONTENT_AUTHORITY;
	/**
	 * URI ID for route: /entries
	 */
	public static final int ROUTE_ENTRIES = 1;

	/**
	 * URI ID for route: /entries/{ID}
	 */
	public static final int ROUTE_ENTRIES_ID = 2;

	/**
	 * UriMatcher, used to decode incoming URIs.
	 */
	private static final UriMatcher sUriMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);
	static {
		sUriMatcher.addURI(AUTHORITY, "entries", ROUTE_ENTRIES);
		sUriMatcher.addURI(AUTHORITY, "entries/*", ROUTE_ENTRIES_ID);
	}

	@Override
	public boolean onCreate() {
		mDatabaseHelper = new PointsDatabase(getContext());
		return true;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SelectionBuilder builder = new SelectionBuilder();
		final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		final int match = sUriMatcher.match(uri);
		int count;
		switch (match) {
		case ROUTE_ENTRIES:
			count = builder.table(TABLE_NAME)
					.where(selection, selectionArgs).delete(db);
			break;
		case ROUTE_ENTRIES_ID:
			String id = uri.getLastPathSegment();
			count = builder.table(TABLE_NAME)
					.where(_ID + "=?", id)
					.where(selection, selectionArgs).delete(db);
			break;
		default:
			throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
		// Send broadcast to registered ContentObservers, to refresh UI.
		Context ctx = getContext();
		assert ctx != null;
		ctx.getContentResolver().notifyChange(uri, null, false);
		return count;
	}

	/**
	 * Determine the mime type for entries returned by a given URI.
	 */
	@Override
	public String getType(Uri uri) {
		final int match = sUriMatcher.match(uri);
		switch (match) {
		case ROUTE_ENTRIES:
			return CONTENT_TYPE;
		case ROUTE_ENTRIES_ID:
			return CONTENT_ITEM_TYPE;
		default:
			throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		assert db != null;
		final int match = sUriMatcher.match(uri);
		Uri result;
		switch (match) {
		case ROUTE_ENTRIES:
			long id = db.insertOrThrow(TABLE_NAME, null,
					values);
			result = Uri.parse(CONTENT_URI + "/" + id);
			break;
		case ROUTE_ENTRIES_ID:
			throw new UnsupportedOperationException(
					"Insert not supported on URI: " + uri);
		default:
			throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
		// Send broadcast to registered ContentObservers, to refresh UI.
		Context ctx = getContext();
		assert ctx != null;
		ctx.getContentResolver().notifyChange(uri, null, false);
		return result;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
		SelectionBuilder builder = new SelectionBuilder();
		int uriMatch = sUriMatcher.match(uri);
		switch (uriMatch) {
		case ROUTE_ENTRIES_ID:
			// Return a single entry, by ID.
			String id = uri.getLastPathSegment();
			builder.where(_ID + "=?", id);
		case ROUTE_ENTRIES:
			// Return all known entries.
			builder.table(TABLE_NAME).where(selection,
					selectionArgs);
			Cursor c = builder.query(db, projection, sortOrder);
			// Note: Notification URI must be manually set here for loaders to
			// correctly
			// register ContentObservers.
			Context ctx = getContext();
			assert ctx != null;
			c.setNotificationUri(ctx.getContentResolver(), uri);
			return c;
		default:
			throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SelectionBuilder builder = new SelectionBuilder();
		final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		final int match = sUriMatcher.match(uri);
		int count;
		switch (match) {
		case ROUTE_ENTRIES:
			count = builder.table(TABLE_NAME)
					.where(selection, selectionArgs).update(db, values);
			break;
		case ROUTE_ENTRIES_ID:
			String id = uri.getLastPathSegment();
			count = builder.table(TABLE_NAME)
					.where(_ID + "=?", id)
					.where(selection, selectionArgs).update(db, values);
			break;
		default:
			throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
		Context ctx = getContext();
		assert ctx != null;
		ctx.getContentResolver().notifyChange(uri, null, false);
		return count;
	}

	/**
	 * SQLite backend for @{link PointsProvider}.
	 *
	 * Provides access to an disk-backed, SQLite datastore which is utilized by
	 * PointsProvider. This database should never be accessed by other parts of
	 * the application directly.
	 */
	private static class PointsDatabase extends SQLiteOpenHelper {
		/** Schema version. */
		public static final int DATABASE_VERSION = 1;
		/** Filename for SQLite file. */
		public static final String DATABASE_NAME = "feed.db";

		private static final String TYPE_TEXT = " TEXT";
		private static final String TYPE_INTEGER = " INTEGER";
		private static final String COMMA_SEP = ",";
		/** SQL statement to create "entry" table. */
		private static final String SQL_CREATE_ENTRIES = "CREATE TABLE "
				+ TABLE_NAME + " ("
				+ _ID + " INTEGER PRIMARY KEY,"
				+ COLUMN_NAME_POINT_ID + TYPE_INTEGER
				+ COMMA_SEP + COLUMN_NAME_LATITUDE
				+ TYPE_TEXT + COMMA_SEP
				+ COLUMN_NAME_LONGITUDE + TYPE_TEXT
				+ COMMA_SEP + COLUMN_NAME_DATE_OF_INSERT
				+ TYPE_INTEGER + COMMA_SEP + COLUMN_NAME_TITLE + TYPE_TEXT
				+ COMMA_SEP + COLUMN_NAME_INFO + TYPE_TEXT
				+ COMMA_SEP + COLUMN_NAME_ACCOUNT
				+ TYPE_TEXT + COMMA_SEP
				+ COLUMN_NAME_DIRTY + TYPE_INTEGER
				+ COMMA_SEP + COLUMN_NAME_DELETE
				+ TYPE_INTEGER + ")";

		/** SQL statement to drop "entry" table. */
		private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS "
				+ TABLE_NAME;

		public PointsDatabase(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE_ENTRIES);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// This database is only a cache for online data, so its upgrade
			// policy is
			// to simply to discard the data and start over
			db.execSQL(SQL_DELETE_ENTRIES);
			onCreate(db);
		}
	}
}
