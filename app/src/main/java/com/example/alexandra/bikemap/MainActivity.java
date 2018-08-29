package com.example.alexandra.bikemap;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.example.alexandra.bikemap.fragment.ManualFragment;
import com.example.alexandra.bikemap.fragment.MapFragment;
import com.example.alexandra.bikemap.fragment.RouteFragment;
import com.example.alexandra.bikemap.fragment.SelectCityFragment;
import com.google.android.gms.maps.model.LatLng;

public class MainActivity extends AppCompatActivity
        implements
        NavigationView.OnNavigationItemSelectedListener,
        RouteAction.OnRouteInteractionListener,
        SelectCityFragment.OnSelectCityFragmentListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //контейнер верхнего уровня для выдвижных окон
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        //Панель инструментов
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        //назначить toolbar как actionbar
        setSupportActionBar(toolbar);

        //связать toolbar и drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close){
            public void onDrawerOpened(View drawerView) {
                hideKeyboard();
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        //создание панели навигации
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        if (savedInstanceState == null) {
            Fragment fragment = null;
            Class fragmentClass;

            //создание фрагмента с картой
            fragmentClass = MapFragment.class;
            try {
                fragment = (Fragment) fragmentClass.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }

            //открытие фрагмента с картой
            FragmentManager fragmentManager = getSupportFragmentManager();
            if (fragment != null) {
                fragmentManager.beginTransaction().replace(R.id.flContent, fragment).commit();
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();
        Fragment fragment = null;
        Class fragmentClass = null;
        if (id == R.id.nav_route) {
            fragmentClass = RouteFragment.class;
        } else if (id == R.id.nav_map) {
            fragmentClass = MapFragment.class;
        } else if (id == R.id.nav_select_city) {
            fragmentClass = SelectCityFragment.class;
        }
        else if (id == R.id.nav_manual) {
            fragmentClass = ManualFragment.class;
        }
        try {
            if (fragmentClass != null) {
                fragment = (Fragment) fragmentClass.newInstance();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragment != null) {
            fragmentManager.beginTransaction().replace(R.id.flContent, fragment).commit();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        return true;
    }



    @Override
    public void onRouteInteraction(LatLng originLatLng, LatLng destLatLng) {

        MapFragment fragment = null;

        Bundle args = new Bundle();
        Bundle origin = new Bundle();
        Bundle desc = new Bundle();

        origin.putDouble("lat", originLatLng.latitude);
        origin.putDouble("lng", originLatLng.longitude);

        desc.putDouble("lat", destLatLng.latitude);
        desc.putDouble("lng", destLatLng.longitude);

        args.putBundle("origin", origin);
        args.putBundle("dest", desc);

        Class fragmentClass = MapFragment.class;
        try {
            fragment = (MapFragment) fragmentClass.newInstance();
            fragment.setArguments(args);

        } catch (Exception e) {
            e.printStackTrace();
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragment != null) {
            fragmentManager.beginTransaction().replace(R.id.flContent, fragment).commit();
        }
        hideKeyboard();

    }

    @Override
    public void onSelectCityFragmentInteraction() {
        MapFragment fragment = null;
        Class fragmentClass = MapFragment.class;
        try {
            fragment = (MapFragment) fragmentClass.newInstance();

        } catch (Exception e) {
            e.printStackTrace();
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragment != null) {
            fragmentManager.beginTransaction().replace(R.id.flContent, fragment).commit();
        }
        hideKeyboard();
    }



    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

}
