package com.dogar.geodesic.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.dogar.geodesic.R;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;

public class GeodesicInfoWindowAdapter implements InfoWindowAdapter {
	private LayoutInflater inflater;

	public GeodesicInfoWindowAdapter(Context context) {
		inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getInfoContents(Marker marker) {
		// Getting view from the layout file
		View v = inflater.inflate(R.layout.custom_info_contents, null);

		TextView coordinates = (TextView) v.findViewById(R.id.coordinates);
		coordinates.setText(marker.getPosition().toString());

		TextView title = (TextView) v.findViewById(R.id.title);
		title.setText(marker.getTitle());

		TextView info = (TextView) v.findViewById(R.id.snippet);
		info.setText(marker.getSnippet());

		return v;
	}

	@Override
	public View getInfoWindow(Marker marker) {
		return null;
	}

}
