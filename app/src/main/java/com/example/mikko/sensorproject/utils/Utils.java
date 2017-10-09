package com.example.mikko.sensorproject.utils;


public final  class Utils {

    public static String removeCountry(String string) {
        if (string.length() > 0) {
            int lastComma = string.lastIndexOf(",");
            if (lastComma != -1) {
                return string.substring(0, lastComma);

            }
        }
        return null;
    }
}
