package com.dogar.geodesic.map;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Address;
import android.location.Geocoder;
import android.widget.EditText;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

public class PointSearcher {
	private GoogleMap map;
	private Context context;

	public PointSearcher(GoogleMap map, Context context) {
		this.map = map;
		this.context = context;
	}

	public void showSearchDialog() {
		AlertDialog.Builder alert = new AlertDialog.Builder(context);
		alert.setTitle("Searh location");
		alert.setMessage("Type Location Name:");
		alert.setIcon(R.drawable.ic_search_category_default);
		final EditText input = new EditText(context);
		alert.setView(input);
		alert.setPositiveButton("Search",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						searchGeoPoint(input.getText().toString());
					}
				});
		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				});
		alert.show();
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
