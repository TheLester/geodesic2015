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

package com.dogar.geodesic.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SyncInfo;
import android.content.SyncStatusObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
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
import com.dogar.geodesic.dialogs.PointSearcherDialog;
import com.dogar.geodesic.enums.GeodesicProblemType;
import com.dogar.geodesic.eventbus.event.EventsWithoutParams;
import com.dogar.geodesic.eventbus.event.MapTypeChangedEvent;
import com.dogar.geodesic.fragments.GoogleMapFragment;
import com.dogar.geodesic.dialogs.AboutInfoDialog;
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
import de.greenrobot.event.EventBus;

import static com.dogar.geodesic.utils.Constants.*;
import static com.dogar.geodesic.utils.SharedPreferencesUtils.*;

public class MainActivity extends AppCompatActivity implements AccountHeader.OnAccountHeaderListener, Drawer.OnDrawerItemClickListener {
    @InjectView(R.id.main_toolbar) Toolbar toolbar;

    private EventBus bus = EventBus.getDefault();
    private Menu   mOptionsMenu;
    private Object mSyncObserverHandle;

    private AccountHeader.Result headerResult;
    private Drawer.Result        drawerResult;
    private ArrayList<IProfile> profiles      = new ArrayList();
    private int                 selectedMapID = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);
        initDrawerMenu();
        if (savedInstanceState != null) {
            selectedMapID = savedInstanceState.getInt(MENU_MAP_TYPE_SELECTED);
            return;
        }

        if (isLoggedIn(this)) {
            setGMFragment();
        } else {
            chooseAccount();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(MENU_MAP_TYPE_SELECTED, selectedMapID);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
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

    private void initDrawerMenu() {
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

        if (selectedMapID == -1) {
            return true;
        }
        MenuItem menuItem;

        switch (selectedMapID) {
            case R.id.map_terrain:
                menuItem = menu.findItem(R.id.map_terrain);
                menuItem.setChecked(true);
                bus.post(new MapTypeChangedEvent(GoogleMap.MAP_TYPE_TERRAIN));
                break;

            case R.id.map_normal:
                menuItem = menu.findItem(R.id.map_normal);
                menuItem.setChecked(true);
                bus.post(new MapTypeChangedEvent(GoogleMap.MAP_TYPE_NORMAL));
                break;

            case R.id.map_hybrid:
                menuItem = menu.findItem(R.id.map_hybrid);
                menuItem.setChecked(true);
                bus.post(new MapTypeChangedEvent(GoogleMap.MAP_TYPE_HYBRID));
                break;
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.delete_mode:
                reverseCheck(item);
                //  GMFragment.setDeleteMode(item.isChecked());
                return true;
            case R.id.menu_refresh:
                SyncUtils.TriggerRefresh();
                return true;
            case R.id.map_terrain:
                reverseCheck(item);
                selectedMapID = id;
                bus.post(new MapTypeChangedEvent(GoogleMap.MAP_TYPE_TERRAIN));
                return true;
            case R.id.map_normal:
                reverseCheck(item);
                selectedMapID = id;
                bus.post(new MapTypeChangedEvent(GoogleMap.MAP_TYPE_NORMAL));
                return true;
            case R.id.map_hybrid:
                reverseCheck(item);
                selectedMapID = id;
                bus.post(new MapTypeChangedEvent(GoogleMap.MAP_TYPE_HYBRID));
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
                        //setGMFragment();
                    }
                }
                break;
        }
    }

    private int getSelectedMapRadioButtonID(MenuItem[] items) {
        for (MenuItem item : items) {
            if (item.isChecked()) {
                return item.getItemId();
            }
        }
        return 0;
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
//                GMFragment.clearMarkersAndDrawNew();
            }
        }
    }

    private void setGMFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .add(R.id.frame_container, GoogleMapFragment.newInstance()).commit();
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
                startProblemResolveActivity(GeodesicProblemType.DIRECT);
                break;
            case 2:
                startProblemResolveActivity(GeodesicProblemType.INDIRECT);
                break;
            case 3:
                openDeleteMarkersChooseDialog();
                break;
            case 4:
                EventBus.getDefault().post(new EventsWithoutParams.ClearPinsEvent());
                break;
            case 5:
                PointSearcherDialog pointSearcher = new PointSearcherDialog(null,this);
                pointSearcher.showSearchDialog();
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
                                EventBus.getDefault().post(new EventsWithoutParams.DeleteMarkersEvent());
                    }
                }).show();
    }

    private void startProblemResolveActivity(GeodesicProblemType problem) {
        Intent intent = new Intent(this, GeodesicProblemActivity.class);
        intent.putExtra(GEODESIC_PROBLEM, problem);
        startActivity(intent);
    }
}
