package com.dogar.geodesic.dialogs;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.dogar.geodesic.R;
import com.dogar.geodesic.eventbus.event.MoveMapCameraEvent;
import com.dogar.geodesic.utils.ToastUtils;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

/**
 * Created by lester on 09.05.15.
 */
public class PointSearcherDialog extends DialogFragment {
    public static final String TAG = PointSearcherDialog.class.getSimpleName();

    public static PointSearcherDialog create() {
        PointSearcherDialog dialog = new PointSearcherDialog();
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog searchDialog = new MaterialDialog.Builder(getActivity()).customView(R.layout.search_edit_text, false)
                .positiveText(R.string.search).negativeText(R.string.cancel)
                .positiveColorRes(R.color.dark_blue).negativeColorRes(R.color.black)
                .iconRes(android.R.drawable.ic_search_category_default)
                .title(R.string.dialog_search_location_title)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        EditText searchInput = ButterKnife.findById(dialog, R.id.input_search_location);
                        String enteredLocation = searchInput.getText().toString();
                        if (!TextUtils.isEmpty(enteredLocation)) {
                            new SearchPlaceTask().execute(enteredLocation);
                        }
                    }
                }).build();
        searchDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return searchDialog;
    }

    /**
     * ThreadTask for geocoding search
     */
    private class SearchPlaceTask extends AsyncTask<String, Void, LatLng> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("Location search");
            progressDialog.show();
        }

        @Override
        protected LatLng doInBackground(String[] params) {
            String locationName = params[0];
            Geocoder geoCoder = new Geocoder(getActivity(), Locale.getDefault());
            LatLng searchPoint = null;
            try {
                List<Address> addresses = geoCoder.getFromLocationName(locationName, 1);
                if (addresses.size() > 0) {

                    Double lat = addresses.get(0).getLatitude();
                    Double lon = addresses.get(0).getLongitude();
                    searchPoint = new LatLng(lat, lon);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return searchPoint;
        }

        @Override
        protected void onPostExecute(LatLng point) {
            progressDialog.dismiss();
            if (point != null) {
                EventBus.getDefault().post(new MoveMapCameraEvent(point));
            } else {
                Context context = getActivity();
                if (context != null) {
                    ToastUtils.show(getActivity(), "Nothing found");
                }
            }
        }
    }
}
