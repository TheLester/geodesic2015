package com.dogar.geodesic.dialogs;

import android.app.Dialog;
import android.content.ContentValues;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.dogar.geodesic.eventbus.event.EventsWithoutParams;
import com.dogar.geodesic.sync.PointsContract;
import com.dogar.geodesic.sync.SyncAdapter;
import com.dogar.geodesic.utils.SharedPreferencesUtils;

import de.greenrobot.event.EventBus;
import com.dogar.geodesic.R;

/**
 * Created by lester on 16.05.15.
 */
public class DeleteMarkersDialog extends DialogFragment {
    public static final String TAG = DialogFragment.class.getSimpleName();

    public static DeleteMarkersDialog create() {
        DeleteMarkersDialog dialog = new DeleteMarkersDialog();
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog deleteMarkersDialog = new MaterialDialog.Builder(getActivity()).title(R.string.delete_markers).
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
                        getActivity().getContentResolver().update(PointsContract.Entry.CONTENT_URI,
                                newValues, SyncAdapter.ACCOUNT_FILTER,
                                new String[]{SharedPreferencesUtils.getLoginEmail(getActivity())});
                        EventBus.getDefault().post(new EventsWithoutParams.DeleteMarkersEvent());
                    }
                }).build();
        return deleteMarkersDialog;
    }
}
