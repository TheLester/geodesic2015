package com.dogar.geodesic.screens;

import com.dogar.geodesic.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutInfoDialog {
	private Context context;

	public AboutInfoDialog(Context context) {
		this.context = context;
	}

	public void showDialogWindow() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(context)
				.setIcon(R.drawable.ic_launcher)
				.setTitle("Geodesic")
				.setMessage(
						Html.fromHtml("Geodesic Application.Developed by:"
								+ " <i>Dmitri Dogar (AE-101)</i>. "
								+ "<br><b><u>Contact Information:</u></b>"
								+ "<br><a href=\"mailto:dimsdevelop@gmail.com\">Author's Em@il</a>"
								+ "<br><br>Open source at<br><a href=\"https://github.com/TheLester\">GitHub Page</a>"))
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int id) {
					}
				});
		final AlertDialog alert = builder.create();
		alert.show();
		((TextView) alert.findViewById(android.R.id.message))
				.setMovementMethod(LinkMovementMethod.getInstance());
	}
}
