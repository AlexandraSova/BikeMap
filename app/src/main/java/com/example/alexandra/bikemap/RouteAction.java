package com.example.alexandra.bikemap;

import android.content.Context;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

public class RouteAction {
    private final LatLng startPlace;
    private final LatLng finishPlace;
    private OnRouteInteractionListener mListener;
    private Context context;

    public RouteAction(LatLng start, LatLng finish, Context listener)
    {
        startPlace=start;
        finishPlace=finish;
        context = listener;
        mListener = (OnRouteInteractionListener)listener;
    }

    public interface OnRouteInteractionListener {
        void onRouteInteraction(LatLng orign, LatLng desc);
    }

    public void getRoute()
    {
        if (startPlace != null & finishPlace != null) {
            mListener.onRouteInteraction(startPlace, finishPlace);
        } else {
            Toast toast = Toast.makeText(context.getApplicationContext(),
                    "Слишком много запросов (геокодирование). Попробуйте еще раз.", Toast.LENGTH_SHORT);
            toast.show();
        }
    }
}
