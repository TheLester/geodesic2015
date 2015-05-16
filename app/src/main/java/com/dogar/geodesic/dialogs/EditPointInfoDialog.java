package com.dogar.geodesic.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.andreabaccega.widget.FormEditText;
import com.dogar.geodesic.R;
import com.dogar.geodesic.eventbus.event.PointInfoEditedEvent;
import com.google.android.gms.maps.model.LatLng;

import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;


public class EditPointInfoDialog extends DialogFragment {
    public static final  String TAG                   = EditPointInfoDialog.class.getSimpleName();
    private static final String TITLE_DIALOG          = "dialog_edit_title";
    private static final String INFO_DIALOG           = "dialog_edit_info";
    private static final String POINT_POSITION_DIALOG = "dialog_edit_position";

    private FormEditText titleInput;
    private FormEditText descrInput;

    public static EditPointInfoDialog create(String title, String info, LatLng position) {
        EditPointInfoDialog dialog = new EditPointInfoDialog();
        Bundle args = new Bundle();
        args.putString(TITLE_DIALOG, title);
        args.putString(INFO_DIALOG, info);
        args.putParcelable(POINT_POSITION_DIALOG, position);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity()).customView(R.layout.dialog_edit_point_info, false)
                .positiveText(R.string.ok).negativeText(R.string.cancel)
                .positiveColorRes(R.color.dark_blue).negativeColorRes(R.color.black)
                .iconRes(R.drawable.ic_marker_location)
                .autoDismiss(false)
                .title(R.string.edit_title_dialog).callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        boolean isTitleEmpty = !titleInput.testValidity();
                        boolean isDescrEmpty = !descrInput.testValidity();
                        if (!isDescrEmpty && !isTitleEmpty) {
                            EventBus.getDefault().post(new PointInfoEditedEvent(titleInput.getText().toString(), descrInput.getText().toString()));
                            dialog.dismiss();
                        }
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        super.onNegative(dialog);
                        dialog.dismiss();
                    }
                }).build();
        titleInput = ButterKnife.findById(dialog, R.id.input_title);
        descrInput = ButterKnife.findById(dialog, R.id.input_description);
        titleInput.setText(getArguments().getString(TITLE_DIALOG));
        descrInput.setText(getArguments().getString(INFO_DIALOG));
        return dialog;
    }

}