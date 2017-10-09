package com.example.mikko.sensorproject;

/**
 * Created by buckfast on 8.10.2017.
 */

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
