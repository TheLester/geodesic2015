package com.dogar.geodesic.eventbus.event;

/**
 * Created by lester on 11.05.15.
 */

public class MapTypeChangedEvent {
    private int mapType;

    public MapTypeChangedEvent(int mapType) {
        this.mapType = mapType;
    }

    public int getMapType() {
        return mapType;
    }
}
