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
import android.widget.TextView;

import com.example.alexandra.bikemap.CurrentCity;
import com.example.alexandra.bikemap.PlaceAutoComplete;
import com.example.alexandra.bikemap.R;
import com.example.alexandra.bikemap.urlBuilder.UrlBuilderForCities;

public class SelectCityFragment extends Fragment {

    private Context context;
    private OnSelectCityFragmentListener mListener;
    private CurrentCity currentCity;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_select_city, container, false);
        TextView selectedCityTextView = view.findViewById(R.id.selected_city);
        currentCity = new CurrentCity(context);
        final String cityName = currentCity.getCity().first;
        if (cityName != null) {
            selectedCityTextView.setText(cityName);
        } else {
            selectedCityTextView.setText("Город не выбран");
        }

        AutoCompleteTextView mAutoCompleteCity = view.findViewById(R.id.select_city);
        PlaceAutoComplete.OnAutocompleteInteractionListener listener =
                new PlaceAutoComplete.OnAutocompleteInteractionListener() {
                    @Override
                    public void onAutocompleteInteraction(String locationName) {
                        currentCity.setCity(locationName);
                        mListener.onSelectCityFragmentInteraction();
                    }
                };

        PlaceAutoComplete placeAutoComplete = new PlaceAutoComplete();
        placeAutoComplete.
                setPlaceAutoComplete(context,mAutoCompleteCity,listener,
                new UrlBuilderForCities(context),(ListView)view.findViewById(R.id.citiesList));

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = getActivity();
        try {
            mListener = (OnSelectCityFragmentListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " должен реализовывать интерфейс OnFragmentInteractionListener");
        }
    }

    public interface OnSelectCityFragmentListener {
        void onSelectCityFragmentInteraction();
    }

}
