package com.dogar.geodesic.eventbus.event;

import com.google.android.gms.maps.model.LatLng;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by lester on 16.05.15.
 */
@Data
@AllArgsConstructor(suppressConstructorProperties = true)
public class MoveMapCameraEvent {
    private LatLng point;
}
