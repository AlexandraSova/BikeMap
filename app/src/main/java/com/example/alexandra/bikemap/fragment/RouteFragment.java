package com.example.alexandra.bikemap.fragment;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;

import com.example.alexandra.bikemap.PlaceAutoComplete;
import com.example.alexandra.bikemap.R;
import com.example.alexandra.bikemap.RouteAction;
import com.example.alexandra.bikemap.parser.LocationNameToLatLngParser;
import com.example.alexandra.bikemap.urlBuilder.UrlBuilderForPlacesInCity;
import com.google.android.gms.maps.model.LatLng;


public class RouteFragment extends Fragment {
    private Context context;
    private String start = null;
    private String finish = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_route, container, false);
        AutoCompleteTextView startTextView = view.findViewById(R.id.start_place);
        AutoCompleteTextView finishTextView = view.findViewById(R.id.finish_place);

        PlaceAutoComplete.OnAutocompleteInteractionListener startListener =
                new PlaceAutoComplete.OnAutocompleteInteractionListener() {
                    @Override
                    public void onAutocompleteInteraction(String locationName) {
                        start = locationName;
                        getRoute();
                    }
                };
        PlaceAutoComplete.OnAutocompleteInteractionListener finishListener =
                new PlaceAutoComplete.OnAutocompleteInteractionListener() {
                    @Override
                    public void onAutocompleteInteraction(String locationName) {
                        finish = locationName;
                        getRoute();
                    }
                };

        PlaceAutoComplete startPlaceAutoComplete = new PlaceAutoComplete();
        PlaceAutoComplete finishPlaceAutoComplete = new PlaceAutoComplete();

        startPlaceAutoComplete.
                setPlaceAutoComplete(context, startTextView, startListener,
                        new UrlBuilderForPlacesInCity(context), (ListView) view.findViewById(R.id.placesList));
        finishPlaceAutoComplete.
                setPlaceAutoComplete(context, finishTextView, finishListener,
                        new UrlBuilderForPlacesInCity(context), (ListView) view.findViewById(R.id.placesList));

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = getActivity();
    }

    private void getRoute() {
        if (start != null && finish != null) {
            LocationNameToLatLngParser parser = new LocationNameToLatLngParser(context);
            LatLng startPlace = parser.parse(start);
            LatLng finishPlace = parser.parse(finish);
            RouteAction routeAction = new RouteAction(startPlace, finishPlace, context);
            routeAction.getRoute();
        }
    }
}
