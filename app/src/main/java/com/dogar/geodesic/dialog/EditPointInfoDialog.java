package com.dogar.geodesic.dialog;

import android.content.Context;
import android.text.TextUtils;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.andreabaccega.widget.FormEditText;
import com.dogar.geodesic.R;
import com.google.android.gms.maps.GoogleMap;

import butterknife.ButterKnife;


public class EditPointInfoDialog {
    private Context          context;
    private EditDoneListener editDoneListener;
    private FormEditText     titleInput;
    private FormEditText     descrInput;

    private String[] prefiledValues;

    public EditPointInfoDialog(Context context, String[] prefiledValues, EditDoneListener editDoneListener) {
        this.context = context;
        this.editDoneListener = editDoneListener;
        this.prefiledValues = prefiledValues;
    }

    public interface EditDoneListener {
        void onEditDone(String title, String desc);
    }


    public void showDialog() {
        MaterialDialog dialog = new MaterialDialog.Builder(context).customView(R.layout.dialog_edit_point_info, false)
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
                            editDoneListener.onEditDone(titleInput.getText().toString(), descrInput.getText().toString());
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
        titleInput.setText(prefiledValues[0]);
        descrInput.setText(prefiledValues[1]);
        dialog.show();
    }
}