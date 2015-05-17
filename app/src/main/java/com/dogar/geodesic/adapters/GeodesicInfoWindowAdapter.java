package com.dogar.geodesic.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.dogar.geodesic.R;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;

import butterknife.ButterKnife;

public class GeodesicInfoWindowAdapter implements InfoWindowAdapter {
    private LayoutInflater inflater;

    public GeodesicInfoWindowAdapter(Context context) {
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getInfoContents(Marker marker) {
        // Getting view from the layout file
        View view = inflater.inflate(R.layout.custom_info_contents, null);

        TextView coordinates = ButterKnife.findById(view, R.id.coordinates);
        coordinates.setText(marker.getPosition().toString());

        TextView title = ButterKnife.findById(view, R.id.title);
        title.setText(marker.getTitle());

        TextView info = ButterKnife.findById(view, R.id.snippet);
        info.setText(marker.getSnippet());

        return view;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

}
