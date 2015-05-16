package com.dogar.geodesic.dialogs;

import com.afollestad.materialdialogs.MaterialDialog;
import com.dogar.geodesic.R;

import android.content.Context;
import android.text.Html;

public class AboutInfoDialog {
    private Context context;

    public AboutInfoDialog(Context context) {
        this.context = context;
    }

    public void showDialogWindow() {
        new MaterialDialog.Builder(context).iconRes(R.drawable.ic_launcher)
                .title(R.string.app_name)
                .content(getInfoContent())
                .positiveColorRes(R.color.blue)
                .positiveText(R.string.ok)
                .show();
    }

    private CharSequence getInfoContent() {
        return Html.fromHtml("Geodesic Application.Developed by:"
                + " <i>Dmitri Dogar (AE-101)</i>. "
                + "<br><b><u>Contact Information:</u></b>"
                + "<br><a style=\"color: rgb(0,0,255)\" href=\"mailto:dimsdevelop@gmail.com\">Author's Em@il</a>"
                + "<br><br>Open source at<br><a style=\"color: rgb(0,0,255)\" href=\"https://github.com/TheLester\">GitHub Page</a>");

    }
}
