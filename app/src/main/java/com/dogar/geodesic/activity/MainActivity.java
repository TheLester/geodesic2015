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

package com.dogar.geodesic.activity;

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
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.dogar.geodesic.R;
import com.dogar.geodesic.adapters.NavDrawerListAdapter;
import com.dogar.geodesic.map.GoogleMapFragment;
import com.dogar.geodesic.map.PointSearcher;
import com.dogar.geodesic.dialog.AboutInfoDialog;
import com.dogar.geodesic.sync.PointsContract;
import com.dogar.geodesic.sync.SyncAdapter;
import com.dogar.geodesic.sync.SyncUtils;
import com.google.android.gms.maps.GoogleMap;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Main Activity,contains design pattern navigation drawer with actions.Have
 * options menu and responsible for authorization with Google Account.
 *
 * @author lester
 */
public class MainActivity extends AppCompatActivity {
    @InjectView(R.id.main_toolbar) Toolbar toolbar;


    private static final int REQUEST_ACCOUNT_PICKER = 2;
    private Menu mOptionsMenu;

    private GoogleMapFragment GMFragment;
    private Object mSyncObserverHandle;

    private Drawer.Result result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        setGMFragment();


        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);
        result = new Drawer()
                .withActivity(this)
                .withToolbar(toolbar)
                .withHeader(R.layout.header)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.direct_geodesic).withIcon(GoogleMaterial.Icon.gmd_landscape),
                        new PrimaryDrawerItem().withName(R.string.direct_geodesic).withIcon(GoogleMaterial.Icon.gmd_grade),
                        new SectionDrawerItem().withName(R.string.direct_geodesic),
                        new PrimaryDrawerItem().withName(R.string.direct_geodesic).withIcon(GoogleMaterial.Icon.gmd_location_city),
                        new PrimaryDrawerItem().withName(R.string.direct_geodesic).withIcon(GoogleMaterial.Icon.gmd_local_bar),
                        new PrimaryDrawerItem().withName(R.string.direct_geodesic).withIcon(GoogleMaterial.Icon.gmd_local_florist),
                        new PrimaryDrawerItem().withName(R.string.direct_geodesic).withIcon(GoogleMaterial.Icon.gmd_style),
                        new PrimaryDrawerItem().withName(R.string.direct_geodesic).withIcon(GoogleMaterial.Icon.gmd_person),
                        new PrimaryDrawerItem().withName(R.string.direct_geodesic).withIcon(GoogleMaterial.Icon.gmd_local_see)
                )
//                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
//                    @Override
//                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l, IDrawerItem drawerItem) {
//                        if (drawerItem != null) {
//                            if (drawerItem instanceof Nameable) {
//                                toolbar.setTitle(((Nameable) drawerItem).getNameRes());
//                            }
//                            if (onFilterChangedListener != null) {
//                                onFilterChangedListener.onFilterChanged(drawerItem.getIdentifier());
//                            }
//                        }
//                    }
//                })
                .build();

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

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (resultCode == RESULT_CANCELED)
//            this.finish();
//        switch (requestCode) {
//            case REQUEST_ACCOUNT_PICKER:
//                if (data != null && data.getExtras() != null) {
//                    String accountName = data.getExtras().getString(
//                            AccountManager.KEY_ACCOUNT_NAME);
//                    if (accountName != null) {
//                        SharedPreferences.Editor editor = settings.edit();
//                        editor.putString("ACCOUNT_NAME", accountName);
//                        editor.commit();
//                        setAccountName(accountName);
//                        if (mAdapter != null) {
//                            mAdapter.setHeaderTitle(accountName, 0);
//                            mAdapter.notifyDataSetChanged();
//                            mDrawerList.setAdapter(mAdapter);
//                        }
//                        ContentResolver.setIsSyncable(
//                                credential.getSelectedAccount(), AUTHORITY, 1);
//                        setGMFragment();
//                    }
//                }
//                break;
//        }
//    }

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

    private void setGMFragment() {
        if (GMFragment == null) {
            FragmentManager fragmentManager = getFragmentManager();
            GMFragment = new GoogleMapFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.frame_container, GMFragment).commit();
        }
    }


    private SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
        /** Callback invoked with the sync adapter status changes. */
        @Override
        public void onStatusChanged(int which) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    @SuppressWarnings("deprecation")
                    SyncInfo currentSync = ContentResolver.getCurrentSync();
//                    setRefreshActionButtonState(currentSync != null
//                            && currentSync.account.equals(credential
//                            .getSelectedAccount())
//                            && currentSync.authority.equals(AUTHORITY));
                }
            });
        }
    };

//    private void openDeleteMarkersChooseDialog() {
//        AlertDialog.Builder alert = new AlertDialog.Builder(this);
//        alert.setTitle("Deleting markers");
//        alert.setIcon(R.drawable.ic_tool);
//        alert.setMessage("Do you really want to delete all markers?");
//        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
//
//            @Override
//            public void onClick(DialogInterface arg0, int arg1) {
//                ContentValues newValues = new ContentValues();
//                newValues.put(PointsContract.Entry.COLUMN_NAME_DELETE, 1);
//                getContentResolver().update(PointsContract.Entry.CONTENT_URI,
//                        newValues, SyncAdapter.ACCOUNT_FILTER,
//                        new String[]{accountName});
//                GMFragment.clearMarkersAndDrawNew();
//            }
//        });
//        alert.setNegativeButton("Cancel",
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int whichButton) {
//                    }
//                });
//        alert.show();
//    }
}
