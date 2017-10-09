package com.example.mikko.sensorproject.interfaces;

import com.google.maps.model.Distance;
import com.google.maps.model.Duration;

public interface UpdateInfoListener {
    void newInfo(String destination, Duration duration, Distance distance);
}