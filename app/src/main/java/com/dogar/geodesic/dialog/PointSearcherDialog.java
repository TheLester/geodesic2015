package com.dogar.geodesic.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Address;
import android.location.Geocoder;
import android.text.TextUtils;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.dogar.geodesic.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;

/**
 * Created by lester on 09.05.15.
 */
public class PointSearcherDialog {
    private GoogleMap map;
    private Context   context;

    public PointSearcherDialog(GoogleMap map, Context context) {
        this.map = map;
        this.context = context;
    }

    public void showSearchDialog() {
        new MaterialDialog.Builder(context).customView(R.layout.search_edit_text, false)
                .positiveText(R.string.search).negativeText(R.string.cancel)
                .positiveColorRes(R.color.dark_blue).negativeColorRes(R.color.black)
                .iconRes(android.R.drawable.ic_search_category_default)
                .title(R.string.dialog_search_location_title).callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog dialog) {
                super.onPositive(dialog);
                EditText searchInput = ButterKnife.findById(dialog, R.id.input_search_location);
                String enteredLocation = searchInput.getText().toString();
                if (!TextUtils.isEmpty(enteredLocation)) {
                    searchGeoPoint(enteredLocation);
                }
            }
        }).show();
    }

    private void searchGeoPoint(String name) {
        Geocoder geoCoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geoCoder.getFromLocationName(name, 5);
            if (addresses.size() > 0) {

                Double lat = addresses.get(0).getLatitude();
                Double lon = addresses.get(0).getLongitude();
                final LatLng searchPoint = new LatLng(lat, lon);
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(searchPoint, 15));
                // Zoom in, animating the camera.
                map.animateCamera(CameraUpdateFactory.zoomTo(10), 2000, null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
