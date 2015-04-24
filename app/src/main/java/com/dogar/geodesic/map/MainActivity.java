/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dogar.geodesic.map;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncInfo;
import android.content.SyncStatusObserver;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.dogar.geodesic.R;
import com.dogar.geodesic.adapters.NavDrawerListAdapter;
import com.dogar.geodesic.screens.AboutInfoDialog;
import com.dogar.geodesic.screens.DirectProblemActivity;
import com.dogar.geodesic.screens.IndirectProblemActivity;
import com.dogar.geodesic.sync.PointsContract;
import com.dogar.geodesic.sync.SyncAdapter;
import com.dogar.geodesic.sync.SyncUtils;
import com.google.android.gms.maps.GoogleMap;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

/**
 * Main Activity,contains design pattern navigation drawer with actions.Have
 * options menu and responsible for authorization with Google Account.
 * 
 * @author lester
 *
 */
public class MainActivity extends Activity {
	private static final int REQUEST_ACCOUNT_PICKER = 2;

	private static final String AUTHORITY = "com.dogar.geodesic";
	private static final String AUDIENCE = "server:client_id:274430009138-4f8ufdnqj56u9mrclvamdfdah9kujcs1.apps.googleusercontent.com";

	private Menu mOptionsMenu;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private NavDrawerListAdapter mAdapter;

	private CharSequence mDrawerTitle;
	private CharSequence mTitle;

	private String[] mHeaders;
	private String[] actionTitles;
	private String[] helpTitles;

	private TypedArray mIcons;
	private static int itemMenuPosition;

	private SharedPreferences settings;
	private String accountName;
	private static GoogleAccountCredential credential;
	private GoogleMapFragment GMFragment;
	private Object mSyncObserverHandle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Account stuff
		settings = getSharedPreferences("Geodesic", 0);
		credential = GoogleAccountCredential.usingAudience(this, AUDIENCE);
		setAccountName(settings.getString("ACCOUNT_NAME", null));
		if (credential.getSelectedAccountName() != null) {
			// Already signed in, begin app!
			Toast.makeText(getBaseContext(),
					"Logged in with : " + credential.getSelectedAccountName(),
					Toast.LENGTH_LONG).show();
			if (savedInstanceState == null) {
				setGMFragment();
			}
		} else {
			// Not signed in, show login window or request an account.
			chooseAccount();
		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mTitle = mDrawerTitle = getTitle();

		mIcons = getResources().obtainTypedArray(R.array.nav_items_icons);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.list_slidermenu);
		mAdapter = new NavDrawerListAdapter(this);
		mHeaders = getResources().getStringArray(R.array.menu_headers);

		mAdapter.addHeader(accountName);
		/******************* Actions *********************/
		actionTitles = getResources().getStringArray(R.array.actions_types);
		mAdapter.addHeader(mHeaders[0]);
		int i;
		for (i = 0; i < actionTitles.length; i++)
			mAdapter.addItem(actionTitles[i], mIcons.getResourceId(i, -1));

		/******************* About *********************/
		mAdapter.addHeader(mHeaders[1]);
		helpTitles = getResources().getStringArray(R.array.help_items);
		mAdapter.addItem(helpTitles[0], mIcons.getResourceId(i++, -1));
        mIcons.recycle();
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
		mDrawerList.setAdapter(mAdapter);

		// enable ActionBar app icon to behave as action to toggle nav drawer
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		// ActionBarDrawerToggle ties together the the proper interactions
		// between the sliding drawer and the action bar app icon
		mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
		mDrawerLayout, /* DrawerLayout object */
		R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
		R.string.drawer_open, /* "open drawer" description for accessibility */
		R.string.drawer_close /* "close drawer" description for accessibility */
		) {
			public void onDrawerClosed(View view) {
				getActionBar().setTitle(mTitle);
				invalidateOptionsMenu(); // creates call to
											// onPrepareOptionsMenu()
			}

			public void onDrawerSlide(View drawerView, float slideOffset) {
				super.onDrawerSlide(drawerView, slideOffset);
				mDrawerLayout.bringChildToFront(drawerView);
				mDrawerLayout.requestLayout();
				mDrawerLayout.setScrimColor(Color.TRANSPARENT);
			}

			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle(mDrawerTitle);
				invalidateOptionsMenu(); // creates call to
											// onPrepareOptionsMenu()
			}
		};
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
				GravityCompat.START);
		mDrawerLayout.setDrawerListener(mDrawerToggle);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		mOptionsMenu = menu;
		inflater.inflate(R.menu.main_activity_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	/* Called whenever we call invalidateOptionsMenu() */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		// Handle action buttons
		switch (item.getItemId()) {
		case R.id.delete_mode:
			reverseCheck(item);
			GMFragment.setDeleteMode(item.isChecked());
			return true;
		case R.id.menu_refresh:
			SyncUtils.TriggerRefresh();
			return true;
		case R.id.map_terrain:
			reverseCheck(item);
			GMFragment.getMap().setMapType(GoogleMap.MAP_TYPE_TERRAIN);
			return true;
		case R.id.map_normal:
			reverseCheck(item);
			GMFragment.getMap().setMapType(GoogleMap.MAP_TYPE_NORMAL);
			return true;
		case R.id.map_hybrid:
			reverseCheck(item);
			GMFragment.getMap().setMapType(GoogleMap.MAP_TYPE_HYBRID);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void reverseCheck(MenuItem item) {
		if (item.isChecked())
			item.setChecked(false);
		else
			item.setChecked(true);
	}

	public static GoogleAccountCredential getCredential() {
		return credential;
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getActionBar().setTitle(mTitle);
	}

	/**
	 * When using the ActionBarDrawerToggle, you must call it during
	 * onPostCreate() and onConfigurationChanged()...
	 */

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggls
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public void onResume() {
		super.onResume();
		mSyncStatusObserver.onStatusChanged(0);
		// Watch for sync state changes
		final int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING
				| ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE;
		mSyncObserverHandle = ContentResolver.addStatusChangeListener(mask,
				mSyncStatusObserver);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mSyncObserverHandle != null) {
			ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
			mSyncObserverHandle = null;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mSyncObserverHandle != null) {
			ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
			mSyncObserverHandle = null;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_CANCELED)
			this.finish();
		switch (requestCode) {
		case REQUEST_ACCOUNT_PICKER:
			if (data != null && data.getExtras() != null) {
				String accountName = data.getExtras().getString(
						AccountManager.KEY_ACCOUNT_NAME);
				if (accountName != null) {
					SharedPreferences.Editor editor = settings.edit();
					editor.putString("ACCOUNT_NAME", accountName);
					editor.commit();
					setAccountName(accountName);
					if (mAdapter != null) {
						mAdapter.setHeaderTitle(accountName, 0);
						mAdapter.notifyDataSetChanged();
						mDrawerList.setAdapter(mAdapter);
					}
					ContentResolver.setIsSyncable(
							credential.getSelectedAccount(), AUTHORITY, 1);
					setGMFragment();
				}
			}
			break;
		}
	}

	private void setRefreshActionButtonState(boolean refreshing) {
		if (mOptionsMenu == null) {
			return;
		}
		final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
		if (refreshItem != null) {
			if (refreshing) {
				refreshItem
						.setActionView(R.layout.actionbar_indeterminate_progress);
			} else {
				refreshItem.setActionView(null);
				GMFragment.clearMarkersAndDrawNew();
			}
		}
	}

	/* The click listner for ListView in the navigation drawer */
	private class DrawerItemClickListener implements
			ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			if (itemMenuPosition != position)
				selectItem(position);
		}
	}

	private void setGMFragment() {
		if (GMFragment == null) {
			FragmentManager fragmentManager = getFragmentManager();
			GMFragment = new GoogleMapFragment();
			fragmentManager.beginTransaction()
					.replace(R.id.frame_container, GMFragment).commit();
		}
	}

	private void selectItem(int position) {

		switch (position) {
		case 2:
			Intent intentD = new Intent(this, DirectProblemActivity.class);
			startActivity(intentD);
			break;
		case 3:
			Intent intentUnd = new Intent(this, IndirectProblemActivity.class);
			startActivity(intentUnd);
			break;
		case 4:
			openDeleteMarkersChooseDialog();
			break;
		case 5:
			GMFragment.clearPins();
			break;
		case 6:
			mDrawerLayout.closeDrawer(mDrawerList);
			new PointSearcher(GMFragment.getMap(), this).showSearchDialog();
			break;
		case 7:
			removeAccountName();
			restartApp();
			break;
		case 9:
			new AboutInfoDialog(this).showDialogWindow();
			break;
		default:
			break;
		}
	}

	private void restartApp() {
		Intent mStartActivity = new Intent(this, MainActivity.class);
		int mPendingIntentId = 123456;
		PendingIntent mPendingIntent = PendingIntent.getActivity(this,
				mPendingIntentId, mStartActivity,
				PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager mgr = (AlarmManager) this
				.getSystemService(Context.ALARM_SERVICE);
		mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100,
				mPendingIntent);
		System.exit(0);
	}

	// setAccountName definition
	private void setAccountName(String accountName) {
		credential.setSelectedAccountName(accountName);
		this.accountName = accountName;

	}

	private void removeAccountName() {
		SharedPreferences.Editor editor = settings.edit();
		editor.remove("ACCOUNT_NAME");
		editor.commit();
	}

	private void chooseAccount() {
		startActivityForResult(credential.newChooseAccountIntent(),
				REQUEST_ACCOUNT_PICKER);
	}

	private SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
		/** Callback invoked with the sync adapter status changes. */
		@Override
		public void onStatusChanged(int which) {

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (credential.getSelectedAccount() == null) {
						setRefreshActionButtonState(false);
						return;
					}
					@SuppressWarnings("deprecation")
					SyncInfo currentSync = ContentResolver.getCurrentSync();
					setRefreshActionButtonState(currentSync != null
							&& currentSync.account.equals(credential
									.getSelectedAccount())
							&& currentSync.authority.equals(AUTHORITY));
				}
			});
		}
	};

	private void openDeleteMarkersChooseDialog() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Deleting markers");
		alert.setIcon(R.drawable.ic_tool);
		alert.setMessage("Do you really want to delete all markers?");
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				ContentValues newValues = new ContentValues();
				newValues.put(PointsContract.Entry.COLUMN_NAME_DELETE, 1);
				getContentResolver().update(PointsContract.Entry.CONTENT_URI,
						newValues, SyncAdapter.ACCOUNT_FILTER,
						new String[] { accountName });
				GMFragment.clearMarkersAndDrawNew();
			}
		});
		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				});
		alert.show();
	}
}
