package com.dogar.geodesic.map;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.PolygonArea;
import net.sf.geographiclib.PolygonResult;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dogar.geodesic.R;
import com.dogar.geodesic.adapters.GeodesicInfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import static com.dogar.geodesic.sync.PointsContract.Entry.*;
import static com.dogar.geodesic.sync.SyncAdapter.*;

/**
 * * Fragment that displays Google Map with geodesic info.Contains methods to
 * set and to delete markers/pins.
 *
 * @author lester
 */
public class GoogleMapFragment extends Fragment {
    private final String PERIMETER = "Perimeter:";
    private final String AREA = "Area:";
    private final String METERS = " meters";
    private final String IN_SQUARE = "^2";
    private final String LATITUDE = "Lat.:";
    private final String LONGITUDE = "Lon.:";
    private final String DATE_DEF = "\n--------\nInfo added at ";
    private final String DEFAULT_TITLE = "Title";
    private final String DEFAULT_DESCR = "Description";
    private final int TRUE = 1;
    private final int FALSE = 0;
    private final PolygonArea polygonArea = new PolygonArea(Geodesic.WGS84,
            false);
    private final List<LatLng> pointsOfPolygons = new ArrayList<LatLng>();
    private final List<Marker> pins = new ArrayList<Marker>();
    private final List<Polygon> polygons = new ArrayList<Polygon>();

    private final List<Marker> points = new ArrayList<Marker>();
    private final Map<Marker, LatLng> tableOfPreviousPositions = new HashMap<Marker, LatLng>();
    private PolygonOptions polygonOptions;
    private MarkerOptions pinOptions;

    private TextView areaLabel;
    private TextView perimeterLabel;
    private TextView latitudeLabel;
    private TextView longitudeLabel;
    private GoogleMap googleMap;

    private SharedPreferences settings;
    private String accountName;
    private int pinCounter;
    private LatLng bufferPoint = new LatLng(0, 0);
    private boolean isDeleteMode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.google_map_layout, container,
                false);

        googleMap = getMapFragment().getMap();
        googleMap.setMyLocationEnabled(true);
        googleMap.setInfoWindowAdapter(new GeodesicInfoWindowAdapter(
                getActivity()));
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        settings = getActivity().getSharedPreferences("Geodesic", 0);
        setMapClickListeners();
        drawMarkersFromLocal();
        areaLabel = (TextView) getView().findViewById(R.id.area_info);
        perimeterLabel = (TextView) getView().findViewById(R.id.perim_info);
        latitudeLabel = (TextView) getView().findViewById(R.id.latitude_info);
        longitudeLabel = (TextView) getView().findViewById(R.id.longitude_info);
    }

    public void clearPins() {
        for (Marker pin : pins)
            pin.remove();
        pins.clear();
        for (Polygon polygon : polygons)
            polygon.remove();
        polygons.clear();
        polygonArea.Clear();
        polygonOptions = null;
        pinOptions = null;
        pinCounter = 0;
        bufferPoint = new LatLng(0, 0);
        pointsOfPolygons.clear();
        areaLabel.setText(AREA + 0 + METERS + IN_SQUARE);
        perimeterLabel.setText(PERIMETER + 0 + METERS);
    }

    public void clearMarkersAndDrawNew() {
        for (Marker point : points)
            point.remove();
        points.clear();
        tableOfPreviousPositions.clear();
        drawMarkersFromLocal();
    }

    public GoogleMap getMap() {
        return googleMap;
    }

    public void setDeleteMode(boolean isDeleteMode) {
        this.isDeleteMode = isDeleteMode;
    }

    private MapFragment getMapFragment() {
        FragmentManager fm = null;


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            fm = getFragmentManager();
        } else {
            fm = getChildFragmentManager();
        }

        return (MapFragment) fm.findFragmentById(R.id.map);
    }

    private void drawMarkersFromLocal() {
        this.accountName = settings.getString("ACCOUNT_NAME", null);
        Cursor c = getActivity().getContentResolver().query(CONTENT_URI,
                PROJECTION, ACCOUNT_FILTER, new String[]{accountName}, null);
        while (c.moveToNext()) {
            Double latitude = Double.valueOf(c.getString(COLUMN_LATITUDE));
            Double longitude = Double.valueOf(c.getString(COLUMN_LONGITUDE));
            Long dateOfInsert = c.getLong(COLUMN_DATE_OF_INSERT);
            String title = c.getString(COLUMN_TITLE);
            String info = c.getString(COLUMN_INFO);
            boolean isDeleted = (c.getInt(COLUMN_DELETE) == TRUE);
            if (!isDeleted)
                drawMarker(new LatLng(latitude, longitude), title, info,
                        new Date(dateOfInsert));
        }
    }

    private void setMapClickListeners() {
        googleMap.setOnMapLongClickListener(new OnMapLongClickListener() {

            @Override
            public void onMapLongClick(LatLng point) {
                drawPinPolygon(point);
            }
        });
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {
                ContentValues mNewValues = new ContentValues();
                Date timestamp = new Date();
                mNewValues.putNull(COLUMN_NAME_POINT_ID);
                mNewValues.put(COLUMN_NAME_LATITUDE,
                        String.valueOf(point.latitude));
                mNewValues.put(COLUMN_NAME_LONGITUDE,
                        String.valueOf(point.longitude));
                mNewValues.put(COLUMN_NAME_DATE_OF_INSERT, timestamp.getTime());
                mNewValues.put(COLUMN_NAME_TITLE, DEFAULT_TITLE);
                mNewValues.put(COLUMN_NAME_INFO, DEFAULT_DESCR);
                mNewValues.put(COLUMN_NAME_ACCOUNT, accountName);
                mNewValues.put(COLUMN_NAME_DIRTY, FALSE);
                mNewValues.put(COLUMN_NAME_DELETE, FALSE);
                getActivity().getContentResolver().insert(CONTENT_URI,
                        mNewValues);
                drawMarker(point, DEFAULT_TITLE, DEFAULT_DESCR, timestamp);
            }
        });
        googleMap
                .setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                    @Override
                    public void onInfoWindowClick(Marker marker) {
                        openInputWindow(marker);
                    }
                });
        googleMap
                .setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {

                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        if (pins.contains(marker))
                            return true;
                        if (isDeleteMode) {
                            deletePoint(marker);
                            tableOfPreviousPositions.remove(marker);
                            marker.remove();
                            return true;
                        } else
                            return false;

                    }
                });
        googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {

            @Override
            public void onMarkerDragStart(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                latitudeLabel.setText(LATITUDE);
                longitudeLabel.setText(LONGITUDE);
                ContentValues updateValues = new ContentValues();
                updateValues.put(COLUMN_NAME_LATITUDE,
                        String.valueOf(marker.getPosition().latitude));
                updateValues.put(COLUMN_NAME_LONGITUDE,
                        String.valueOf(marker.getPosition().longitude));
                updateValues.put(COLUMN_NAME_DIRTY, TRUE);
                updatePoint(updateValues, tableOfPreviousPositions.get(marker));
                tableOfPreviousPositions.put(marker, marker.getPosition());
                marker.showInfoWindow();
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                LatLng pos = marker.getPosition();
                latitudeLabel.setText(LATITUDE + pos.latitude);
                longitudeLabel.setText(LONGITUDE + pos.longitude);
            }
        });
    }

    private void drawMarker(LatLng point, String title, String info, Date date) {
        MarkerOptions markerOptions = new MarkerOptions();
        BitmapDescriptor icon = BitmapDescriptorFactory
                .fromResource(R.drawable.ic_marker);
        markerOptions.position(point);
        markerOptions.draggable(true);
        markerOptions.icon(icon);
        markerOptions.title(title);
        markerOptions.snippet(info + DATE_DEF + date);
        Marker newMarker = googleMap.addMarker(markerOptions);
        points.add(newMarker);
        tableOfPreviousPositions.put(newMarker, point);
    }

    private void drawPinPolygon(LatLng point) {
        pinCounter++;
        createPinOptions();
        pinOptions.title(point.toString());
        pinOptions.position(point);
        pins.add(googleMap.addMarker(pinOptions));

        createPolygonOptions();
        polygonOptions.add(point);
        polygons.add(googleMap.addPolygon(polygonOptions));
        pointsOfPolygons.add(point);

        polygonArea.AddPoint(point.latitude, point.longitude);
        PolygonResult rez = polygonArea.Compute();
        if (pinCounter > 2) {
            areaLabel.setText(AREA + Math.round(rez.area) + METERS + IN_SQUARE);
            perimeterLabel.setText(PERIMETER + Math.round(rez.perimeter)
                    + METERS);
        } else if (pinCounter == 2) {
            perimeterLabel.setText(PERIMETER
                    + Math.round(Geodesic.WGS84.Inverse(bufferPoint.latitude,
                    bufferPoint.longitude, point.latitude,
                    point.longitude).s12) + METERS);
            bufferPoint = point;
        } else if (pinCounter == 1) {
            bufferPoint = point;
        }
    }

    private void createPinOptions() {
        if (this.pinOptions == null) {
            this.pinOptions = new MarkerOptions();
            BitmapDescriptor icon = BitmapDescriptorFactory
                    .fromResource(R.drawable.ic_action);
            pinOptions.draggable(false);
            pinOptions.anchor(0.5f, 1.0f);// center bottom of marker icon
            pinOptions.icon(icon);
        }
    }

    private void createPolygonOptions() {
        if (this.polygonOptions == null) {
            this.polygonOptions = new PolygonOptions().strokeColor(Color.RED)
                    .strokeWidth(2.0f)
                    .fillColor(Color.argb(127, 139, 137, 137)).geodesic(true);
        }
    }

    private int updatePoint(ContentValues values, LatLng position) {
        return getActivity().getContentResolver().update(
                CONTENT_URI,
                values,
                ACCOUNT_FILTER + LAT_LONG_FILTER,
                new String[]{accountName, String.valueOf(position.latitude),
                        String.valueOf(position.longitude)});
    }

    private void deletePoint(Marker marker) {
        ContentValues newValues = new ContentValues();
        newValues.put(COLUMN_NAME_DELETE, TRUE);
        getActivity().getContentResolver().update(
                CONTENT_URI,
                newValues,
                ACCOUNT_FILTER + LAT_LONG_FILTER,
                new String[]{accountName,
                        String.valueOf(marker.getPosition().latitude),
                        String.valueOf(marker.getPosition().longitude)});
    }

    private void openInputWindow(final Marker marker) {
        Context context = getActivity();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle("Type info about this point");

        final EditText inputTitle = new EditText(getActivity());
        inputTitle.setText(marker.getTitle(), TextView.BufferType.EDITABLE);
        layout.addView(inputTitle);

        final EditText inputDescription = new EditText(context);
        inputDescription.setText(marker.getSnippet().split(DATE_DEF, 2)[0],
                TextView.BufferType.EDITABLE);
        layout.addView(inputDescription);

        alert.setView(layout);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String title = inputTitle.getText().toString();
                String info = inputDescription.getText().toString();
                marker.setTitle(title);
                marker.setSnippet(info + DATE_DEF + new Date());
                ContentValues updateValues = new ContentValues();
                updateValues.put(COLUMN_NAME_TITLE, title);
                updateValues.put(COLUMN_NAME_INFO, info);
                updateValues.put(COLUMN_NAME_DATE_OF_INSERT,
                        new Date().getTime());
                updateValues.put(COLUMN_NAME_DIRTY, TRUE);
                updatePoint(updateValues, marker.getPosition());
                marker.showInfoWindow();
            }
        });

        alert.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
        alert.show();
    }
}
