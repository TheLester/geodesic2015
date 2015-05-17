package com.dogar.geodesic.dialogs;

import com.afollestad.materialdialogs.MaterialDialog;
import com.dogar.geodesic.R;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Html;

public class AboutInfoDialog extends DialogFragment {
    public static final String TAG = AboutInfoDialog.class.getSimpleName();

    public static AboutInfoDialog create() {
        AboutInfoDialog dialog = new AboutInfoDialog();
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog aboutDialog = new MaterialDialog.Builder(getActivity()).iconRes(R.drawable.ic_launcher)
                .title(R.string.app_name)
                .content(getInfoContent())
                .positiveColorRes(R.color.blue)
                .positiveText(R.string.ok)
                .build();
        return aboutDialog;
    }

    private CharSequence getInfoContent() {
        return Html.fromHtml("Geodesic Application.Developed by:"
                + " <i>Dmitri Dogar (AE-101)</i>. "
                + "<br><b><u>Contact Information:</u></b>"
                + "<br><a style=\"color: rgb(0,0,255)\" href=\"mailto:dimsdevelop@gmail.com\">Author's Em@il</a>"
                + "<br><br>Open source at<br><a style=\"color: rgb(0,0,255)\" href=\"https://github.com/TheLester\">GitHub Page</a>");

    }
}
