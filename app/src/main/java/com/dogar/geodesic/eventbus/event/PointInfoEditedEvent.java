package com.dogar.geodesic.eventbus.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(suppressConstructorProperties = true)
public class PointInfoEditedEvent {
    private String pointTitle;
    private String pointInfo;
}