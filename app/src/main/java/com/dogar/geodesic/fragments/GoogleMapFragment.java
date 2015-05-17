package com.dogar.geodesic.fragments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.PolygonArea;
import net.sf.geographiclib.PolygonResult;

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dogar.geodesic.R;
import com.dogar.geodesic.adapters.GeodesicInfoWindowAdapter;
import com.dogar.geodesic.dialogs.EditPointInfoDialog;
import com.dogar.geodesic.eventbus.event.EventsWithoutParams;
import com.dogar.geodesic.eventbus.event.MapTypeChangedEvent;
import com.dogar.geodesic.eventbus.event.MoveMapCameraEvent;
import com.dogar.geodesic.eventbus.event.PointInfoEditedEvent;
import com.dogar.geodesic.model.GeoPoint;
import com.dogar.geodesic.utils.SharedPreferencesUtils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import org.joda.time.DateTime;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

import static com.dogar.geodesic.sync.PointsContract.Entry.*;
import static com.dogar.geodesic.sync.SyncAdapter.*;
import static com.dogar.geodesic.utils.Constants.*;

/**
 * * Fragment that displays Google Map with geodesic info.Contains methods to
 * set and to delete markers/pins.
 *
 * @author lester
 */
public class GoogleMapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMarkerClickListener, GoogleMap.OnMarkerDragListener, OnMapLongClickListener {
    private final String PERIMETER = "Perimeter:";
    private final String AREA      = "Area:";
    private final String METERS    = " meters";
    private final String IN_SQUARE = "^2";
    private final String LATITUDE  = "Lat.:";
    private final String LONGITUDE = "Lon.:";
    private final String DATE_DEF  = "\n--------\nInfo added at ";


    private final PolygonArea   polygonArea      = new PolygonArea(Geodesic.WGS84,
            false);
    private final List<LatLng>  pointsOfPolygons = new ArrayList<LatLng>();
    private final List<Marker>  pins             = new ArrayList<Marker>();
    private final List<Polygon> polygons         = new ArrayList<Polygon>();

    private final List<Marker>        points                   = new ArrayList<Marker>();
    private final Map<Marker, LatLng> tableOfPreviousPositions = new HashMap<Marker, LatLng>();
    private PolygonOptions polygonOptions;
    private MarkerOptions  pinOptions;

    @InjectView(R.id.area_info)      TextView       areaLabel;
    @InjectView(R.id.perim_info)     TextView       perimeterLabel;
    @InjectView(R.id.latitude_info)  TextView       latitudeLabel;
    @InjectView(R.id.longitude_info) TextView       longitudeLabel;
    @InjectView(R.id.coord_panel)    RelativeLayout coordPanel;

    private GoogleMap googleMap;

    private String accountName;
    private int    pinCounter;
    private LatLng bufferPolygonPoint = new LatLng(0, 0);
    private LatLng bufferSelectedMarkerPos;

    public static GoogleMapFragment newInstance() {
        return new GoogleMapFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountName = SharedPreferencesUtils.getLoginEmail(getActivity());
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.google_map_layout, container,
                false);
        ButterKnife.inject(this, rootView);
        getMapFragment().getMapAsync(this);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        clearPins();
        points.clear();
        ButterKnife.reset(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setMyLocationEnabled(true);
        this.googleMap.setInfoWindowAdapter(new GeodesicInfoWindowAdapter(
                getActivity()));
        this.googleMap.setOnInfoWindowClickListener(this);
        this.googleMap.setOnMapLongClickListener(this);
        this.googleMap.setOnMapClickListener(this);
        this.googleMap.setOnMarkerClickListener(this);
        this.googleMap.setOnMarkerDragListener(this);
        drawMarkersFromLocalDB();
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        openInputDialog(marker);
        bufferSelectedMarkerPos = marker.getPosition();
    }

    /**
     * Called when got event for moving camera by specific LatLng
     *
     * @param moveMapCameraEvent
     */
    public void onEvent(MoveMapCameraEvent moveMapCameraEvent) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(moveMapCameraEvent.getPoint(), 15));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(10), 2000, null);
    }

    /**
     * Called when user clicks to delete all markers
     *
     * @param deleteMarkersEvent
     */
    public void onEvent(EventsWithoutParams.DeleteMarkersEvent deleteMarkersEvent) {
        for (Marker point : points) {
            point.remove();
        }
        points.clear();
        tableOfPreviousPositions.clear();
        drawMarkersFromLocalDB();
    }

    /**
     * Called when user clicks to delete all pins/polygons
     *
     * @param clearPinsEvent
     */
    public void onEvent(EventsWithoutParams.ClearPinsEvent clearPinsEvent) {
        clearPins();
    }

    private void clearPins() {
        for (Marker pin : pins) {
            pin.remove();
        }
        pins.clear();
        for (Polygon polygon : polygons) {
            polygon.remove();
        }
        polygons.clear();

        polygonArea.Clear();
        polygonOptions = null;
        pinOptions = null;
        pinCounter = 0;
        bufferPolygonPoint = new LatLng(0, 0);
        pointsOfPolygons.clear();
        areaLabel.setText(getString(R.string.area_empty));
        perimeterLabel.setText(getString(R.string.perimeter_empty));
    }

    /**
     * Called when map type {HYBRID,NORMAL,SATELLITE} changed
     *
     * @param mapTypeChangedEvent
     */
    public void onEvent(MapTypeChangedEvent mapTypeChangedEvent) {
        googleMap.setMapType(mapTypeChangedEvent.getMapType());
    }

    /**
     * Called when edit done on EditPointInfo dialog
     *
     * @param editDoneEvent
     */
    public void onEvent(PointInfoEditedEvent editDoneEvent) {
        Marker selectedMarker = getMarkerByPos(bufferSelectedMarkerPos);
        if (selectedMarker != null) {
            long dateNowInMs = DateTime.now().getMillis();
            selectedMarker.setTitle(editDoneEvent.getPointTitle());
            selectedMarker.setSnippet(editDoneEvent.getPointInfo() + DATE_DEF + getNowDateTime(dateNowInMs));
            selectedMarker.hideInfoWindow();
            selectedMarker.showInfoWindow();
            LatLng position = selectedMarker.getPosition();
            GeoPoint updatedPoint = new GeoPoint(editDoneEvent.getPointTitle(), editDoneEvent.getPointInfo(), dateNowInMs,
                    SQLITE_TRUE,
                    SQLITE_FALSE,
                    String.valueOf(position.latitude),
                    String.valueOf(position.longitude),
                    accountName
            );
            updatePoint(updatedPoint.toCVWithoutId(), position);
        }
    }

    @Override
    public void onMapClick(LatLng point) {
        long timeNow = DateTime.now().getMillis();
        GeoPoint geoPoint = new GeoPoint(
                getString(R.string.title),
                getString(R.string.description),
                timeNow,
                SQLITE_FALSE,
                SQLITE_FALSE,
                String.valueOf(point.latitude),
                String.valueOf(point.longitude),
                accountName);

        getActivity().getContentResolver().insert(CONTENT_URI,
                geoPoint.toCVWithoutId());
        drawMarker(geoPoint);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (pins.contains(marker))
            return true;
        if (SharedPreferencesUtils.isDeleteMode(getActivity())) {
            deletePoint(marker);
            tableOfPreviousPositions.remove(marker);
            marker.remove();
            return true;
        } else
            return false;
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        coordPanel.setVisibility(View.VISIBLE);
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        LatLng pos = marker.getPosition();
        latitudeLabel.setText(LATITUDE + pos.latitude);
        longitudeLabel.setText(LONGITUDE + pos.longitude);
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        ContentValues updateValues = new ContentValues();
        updateValues.put(COLUMN_NAME_LATITUDE,
                String.valueOf(marker.getPosition().latitude));
        updateValues.put(COLUMN_NAME_LONGITUDE,
                String.valueOf(marker.getPosition().longitude));
        updateValues.put(COLUMN_NAME_DIRTY, SQLITE_TRUE);
        updatePoint(updateValues, tableOfPreviousPositions.get(marker));
        tableOfPreviousPositions.put(marker, marker.getPosition());
        marker.showInfoWindow();
        coordPanel.setVisibility(View.GONE);
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        drawPinPolygon(latLng);
    }

    private Marker getMarkerByPos(LatLng pos) {
        for (Marker marker : points) {
            if (marker.getPosition().equals(pos)) return marker;
        }
        return null;
    }

    private SupportMapFragment getMapFragment() {
        FragmentManager fm = getChildFragmentManager();
        return (SupportMapFragment) fm.findFragmentById(R.id.map);
    }

    private void drawMarkersFromLocalDB() {
        Cursor c = getActivity().getContentResolver().query(CONTENT_URI,
                PROJECTION, ACCOUNT_FILTER, new String[]{accountName}, null);
        while (c.moveToNext()) {
            GeoPoint geoPoint = new GeoPoint(c);
            boolean isDeleted = (geoPoint.getDeleted() == SQLITE_TRUE);
            if (!isDeleted) {
                drawMarker(geoPoint);
            }
        }
    }

    private void drawMarker(GeoPoint geoPoint) {
        MarkerOptions markerOptions = new MarkerOptions();
        BitmapDescriptor icon = BitmapDescriptorFactory
                .fromResource(R.drawable.ic_marker);
        LatLng position = new LatLng(Double.valueOf(geoPoint.getLatitude()), Double.valueOf(geoPoint.getLongitude()));
        markerOptions.position(position);
        markerOptions.draggable(true);
        markerOptions.icon(icon);
        markerOptions.title(geoPoint.getTitle());
        markerOptions.snippet(geoPoint.getInfo() + DATE_DEF + getNowDateTime(geoPoint.getInsertDate()));
        Marker newMarker = googleMap.addMarker(markerOptions);
        points.add(newMarker);
        tableOfPreviousPositions.put(newMarker, position);
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
                    + Math.round(Geodesic.WGS84.Inverse(bufferPolygonPoint.latitude,
                    bufferPolygonPoint.longitude, point.latitude,
                    point.longitude).s12) + METERS);
            bufferPolygonPoint = point;
        } else if (pinCounter == 1) {
            bufferPolygonPoint = point;
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
                    .fillColor(getActivity().getResources().getColor(R.color.gray_light)).geodesic(true);
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
        newValues.put(COLUMN_NAME_DELETE, SQLITE_TRUE);
        getActivity().getContentResolver().update(
                CONTENT_URI,
                newValues,
                ACCOUNT_FILTER + LAT_LONG_FILTER,
                new String[]{accountName,
                        String.valueOf(marker.getPosition().latitude),
                        String.valueOf(marker.getPosition().longitude)});
    }

    private void openInputDialog(final Marker marker) {
        DialogFragment newFragment = EditPointInfoDialog.create(marker.getTitle(), marker.getSnippet().split(DATE_DEF, 2)[0], marker.getPosition());
        newFragment.show(getFragmentManager(), EditPointInfoDialog.TAG);
    }

    private String getNowDateTime(long ms) {
        return new DateTime(ms).toString("dd-MMM-yy hh:mm:ss aa ZZ");
    }
}
