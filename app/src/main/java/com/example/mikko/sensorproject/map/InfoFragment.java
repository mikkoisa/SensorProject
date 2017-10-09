package com.example.mikko.sensorproject.map;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mikko.R;
import com.example.mikko.sensorproject.interfaces.UpdateInfoListener;
import com.google.maps.model.Duration;

public class InfoFragment extends Fragment implements UpdateInfoListener {


    ImageView i,next;
    InfoListeners onXClickListener;
    TextView destination;
    TextView duration;

    public InfoFragment() {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (getParentFragment() instanceof InfoListeners) {
            Log.i("inio", "joo on!!");
            onXClickListener = (InfoListeners) getParentFragment();
        } else{
            Log.i("inio", "juuh opk"+String.valueOf(getParentFragment()));
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

    }



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_info, container, false);

        destination = (TextView) v.findViewById(R.id.destination);
        duration = (TextView) v.findViewById(R.id.duration);

        i = (ImageView) v.findViewById(R.id.mister_x);
        i.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                onXClickListener.onXClick();
            }
        });

        next = (ImageView) v.findViewById(R.id.altroute);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onXClickListener.onNextClick();
            }
        });

        return v;

    }

    @Override
    public void newInfo(String destination, Duration duration, com.google.maps.model.Distance distance) {
        this.destination.setText("Destination: "+destination);
        this.duration.setText(duration.humanReadable+", "+distance.humanReadable);
    }


    public interface InfoListeners {
        void onXClick();
        void onNextClick();
    }
}
