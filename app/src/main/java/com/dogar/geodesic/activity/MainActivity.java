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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SyncInfo;
import android.content.SyncStatusObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.dogar.geodesic.R;
import com.dogar.geodesic.dialog.PointSearcherDialog;
import com.dogar.geodesic.map.GoogleMapFragment;
import com.dogar.geodesic.dialog.AboutInfoDialog;
import com.dogar.geodesic.sync.PointsContract;
import com.dogar.geodesic.sync.SyncAdapter;
import com.dogar.geodesic.sync.SyncUtils;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.maps.GoogleMap;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeader;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static com.dogar.geodesic.utils.Constants.*;
import static com.dogar.geodesic.utils.SharedPreferencesUtils.*;

public class MainActivity extends AppCompatActivity implements AccountHeader.OnAccountHeaderListener, Drawer.OnDrawerItemClickListener {
    @InjectView(R.id.main_toolbar) Toolbar toolbar;


    private Menu mOptionsMenu;

    private GoogleMapFragment GMFragment;
    private Object            mSyncObserverHandle;

    private AccountHeader.Result headerResult;
    private Drawer.Result        drawerResult;
    private ArrayList<IProfile> profiles = new ArrayList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        if (isLoggedIn(this)) {
            setGMFragment();
        } else {
            chooseAccount();
        }

        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);
        initMenu();
    }

    @Override
    public boolean onProfileChanged(View view, IProfile iProfile, boolean b) {
        Toast.makeText(this, iProfile.getEmail(), Toast.LENGTH_LONG).show();
        saveLogin(this, iProfile.getEmail());
        headerResult.setActiveProfile(getSavedProfile());
        //TODO Refresh map
        return true;
    }

    private void initProfiles() {
        Account[] accounts = AccountManager.get(this).getAccountsByType(GOOGLE_TYPE);
        for (Account account : accounts) {
            profiles.add(new ProfileDrawerItem().withEmail(account.name));
            Log.i("test", account.name);
        }
    }

    private IProfile getSavedProfile() {
        String savedEmail = getLoginEmail(this);
        for (IProfile profile : profiles) {
            if (profile.getEmail().equals(savedEmail)) {
                return profile;
            }
        }
        return null;
    }

    private void initMenu() {
        if (profiles.isEmpty()) {
            initProfiles();
        }
        /**Header*/
        headerResult = new AccountHeader()
                .withActivity(this)
                .withHeaderBackground(R.drawable.header_backgr)
                .withOnAccountHeaderListener(this)
                .withProfiles(profiles)
                .build();
        if (isLoggedIn(this)) {
            headerResult.setActiveProfile(getSavedProfile());
        }
        /**Menu*/
        String[] headers = getResources().getStringArray(R.array.actions_types);
        drawerResult = new Drawer()
                .withActivity(this)
                .withToolbar(toolbar)

                .withHeader(R.layout.header)
                .withAccountHeader(headerResult)
                .withOnDrawerItemClickListener(this)
                .addDrawerItems(
                        new SectionDrawerItem().withName(headers[0]),
                        new PrimaryDrawerItem().withName(headers[1]).withIcon(R.drawable.ic_calc),
                        new PrimaryDrawerItem().withName(headers[2]).withIcon(R.drawable.ic_calc),
                        new PrimaryDrawerItem().withName(headers[3]).withIcon(R.drawable.ic_del),
                        new PrimaryDrawerItem().withName(headers[4]).withIcon(R.drawable.ic_clear),
                        new PrimaryDrawerItem().withName(headers[5]).withIcon(R.drawable.ic_location),
                        new SectionDrawerItem().withName(headers[6]),
                        new PrimaryDrawerItem().withName(headers[7]).withIcon(R.drawable.ic_info)
                ).build();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            this.finish();
        }
        switch (requestCode) {
            case REQUEST_ACCOUNT_PICKER:
                if (data != null && data.getExtras() != null) {
                    String accountName = data.getExtras().getString(
                            AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        saveLogin(this, accountName);
                        headerResult.setActiveProfile(getSavedProfile());
                        setGMFragment();
                    }
                }
                break;
        }
    }

    private void chooseAccount() {
        Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[]{GOOGLE_TYPE},
                false, null, null, null, null);
        startActivityForResult(intent, REQUEST_ACCOUNT_PICKER);
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

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l, IDrawerItem iDrawerItem) {

        switch (position) {
            case 1:
                Intent intentD = new Intent(this, DirectProblemActivity.class);
                startActivity(intentD);
                break;
            case 2:
                Intent intentUnd = new Intent(this, IndirectProblemActivity.class);
                startActivity(intentUnd);
                break;
            case 3:
                openDeleteMarkersChooseDialog();
                break;
            case 4:
                GMFragment.clearPins();
                break;
            case 5:
                PointSearcherDialog pointSearcher = new PointSearcherDialog(GMFragment.getMap(),this);
                pointSearcher.showSearchDialog();
                break;
            case 6:
//                removeAccountName();
//                restartApp();
                break;
            case 7:
                new AboutInfoDialog(this).showDialogWindow();
                break;
            default:
                break;
        }
    }

    private void openDeleteMarkersChooseDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(this).title(R.string.delete_markers).
                icon(getResources().getDrawable(R.drawable.ic_del)).
                positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .content(R.string.delete_markers_question)
                .negativeColorRes(R.color.black)
                .positiveColorRes(R.color.dark_blue)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        ContentValues newValues = new ContentValues();
                        newValues.put(PointsContract.Entry.COLUMN_NAME_DELETE, 1);
                        getContentResolver().update(PointsContract.Entry.CONTENT_URI,
                                newValues, SyncAdapter.ACCOUNT_FILTER,
                                new String[]{getLoginEmail(MainActivity.this)});
                        GMFragment.clearMarkersAndDrawNew();
                    }
                }).show();
    }
}
