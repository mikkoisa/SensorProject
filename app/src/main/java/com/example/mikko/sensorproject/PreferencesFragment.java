package com.example.mikko.sensorproject;

import android.content.Context;
import android.os.Bundle;


import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.mikko.R;

public class PreferencesFragment extends PreferenceFragment {

    private ActionBar actionbar;

    public PreferencesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        addPreferencesFromResource(R.xml.preferences);
    }





    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        actionbar = ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        assert view != null;
        view.setBackgroundColor(getResources().getColor(R.color.colorPrimary));

        return view;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
       // menu.findItem(R.id.searchbar).setVisible(false);

        actionbar.getCustomView().findViewById(R.id.searchbar).setVisibility(View.GONE);
        actionbar.getCustomView().findViewById(R.id.titlebar).setVisibility(View.VISIBLE);
        menu.findItem(R.id.action_manage).setVisible(false);
        menu.findItem(R.id.action_info).setVisible(false);

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu2, menu);

    }
}
