package com.example.alexandra.bikemap;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Pair;
import android.widget.Toast;

import com.example.alexandra.bikemap.parser.LocationNameToLatLngParser;
import com.google.android.gms.maps.model.LatLng;

import static android.content.Context.MODE_PRIVATE;

public class CurrentCity {

    private SharedPreferences sharedPreferences;
    private Context context;

    public CurrentCity(Context context) {
        this.context = context;
    }

    public void setCity(String city_name) {

        LocationNameToLatLngParser parser = new LocationNameToLatLngParser(context);
        LatLng city_latLng = parser.parse(city_name);
        if(city_latLng!=null) {
            sharedPreferences = ((MainActivity) context).getPreferences(MODE_PRIVATE);
            SharedPreferences.Editor edit = sharedPreferences.edit();
            edit.putString("current_city_name", city_name);
            Double lat = city_latLng.latitude;
            Double lng = city_latLng.longitude;
            String string_latLng = lat.toString() + "," + lng.toString();
            edit.putString("current_city_latlng", string_latLng);
            edit.commit();
            showMessage("Выбран город " + city_name);
        }
        else
        {
            showMessage("Ошибка сети");
        }
    }

    public Pair<String, LatLng> getCity() {

        LatLng city_latLng = null;
        String city_name = null;
        sharedPreferences = ((MainActivity) context).getPreferences(MODE_PRIVATE);

        String city = sharedPreferences.getString("current_city_name", "");
        if (city.length() > 0) {
            city_name = city;
        }
        String string_latLng = sharedPreferences.getString("current_city_latlng", "");
        if (string_latLng.length() > 0) {
            String[] array_latLng = string_latLng.split(",");
            city_latLng = new LatLng(Double.parseDouble(array_latLng[0]), Double.parseDouble(array_latLng[1]));
        }
        return new Pair<>(city_name, city_latLng);
    }

    private void showMessage(String text) {
        Toast toast = Toast.makeText(context.getApplicationContext(),
                text, Toast.LENGTH_SHORT);
        toast.show();
    }

}
