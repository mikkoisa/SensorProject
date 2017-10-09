package com.example.mikko.sensorproject.autocomplete;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class PredThread implements Runnable {

    private Handler handler;
    private URL urli;
    private String mess;

    public PredThread(Handler uihandler, URL url ) {
        handler = uihandler;
        urli = url;
    }

    @Override
    public void run() {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        String json;

        try {

            urlConnection = (HttpURLConnection) urli.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                mess = "ei toimi 1";
            }
            assert inputStream != null;
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }

            if (buffer.length() == 0) {
                //stream was empty
                mess = "ei toimi 2";
            }
            json = buffer.toString();

            mess = json;

        } catch (IOException e) {
            Log.e("predictions", "error ", e);
            Log.i("workiiko" , "ew");
            mess = "ei toimi 3";

        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e("predictions", "error", e);
                }
            }

            Message msg = handler.obtainMessage();
            msg.obj = mess;
            msg.what = 0;
            handler.sendMessage(msg);


        }
    }
}
