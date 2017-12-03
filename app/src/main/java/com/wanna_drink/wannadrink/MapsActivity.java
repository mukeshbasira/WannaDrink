package com.wanna_drink.wannadrink;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.wanna_drink.wannadrink.entities.User;
import com.wanna_drink.wannadrink.entities.UserBuilder;
import com.wanna_drink.wannadrink.functional.Consumer;
import com.wanna_drink.wannadrink.functional.RetainFragment;

import java.util.ArrayList;
import java.util.Date;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener, GoogleMap.OnMapClickListener {
    private static final int MY_LOCATION_REQUEST_CODE = 1;
    private GoogleMap mMap;
    ArrayList<User> userList = new ArrayList<>();
    private FusedLocationProviderClient mFusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        if (Build.VERSION.SDK_INT >= 23) {

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);

            }
            else{
                ActivityCompat.requestPermissions(MapsActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_LOCATION_REQUEST_CODE);
            }

        }
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);

        MapsInitializer.initialize(this);

        if(map.isMyLocationEnabled()){
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                sendDataToApi(location.getLatitude(), location.getLongitude());
                            }
                        }
                    });
        }

//        setMarkers();
    }



    private void sendDataToApi(double lat, double lng) {

        SharedPreferences sharedPref = getDefaultSharedPreferences(getApplicationContext());
        String name = sharedPref.getString(getString(R.string.key_name), "");
        String email = sharedPref.getString(getString(R.string.key_email), "");
        String drinkCode = String.valueOf(sharedPref.getInt(getString(R.string.key_drink), 0));
        String hours = String.valueOf(sharedPref.getInt(getString(R.string.key_hours), 0));

        User user = new UserBuilder()
                .addName(name)
                .addEmail(email)
                .addDrink(Drink.getDrink(drinkCode))
                .addHours(hours)
                .addLat(String.valueOf(lat))
                .addLng(String.valueOf(lng))
                .build();

        addUser(user);

    }

    private void addUser(final User user) {
        RetainFragment retainFragment = (RetainFragment) getSupportFragmentManager().findFragmentByTag(getString(R.string.NETWORK_FRAGMENT_TAG));
        if (retainFragment == null) {
            retainFragment = new RetainFragment();
            getSupportFragmentManager().beginTransaction().add(retainFragment, getString(R.string.NETWORK_FRAGMENT_TAG)).commit();
        }

        if(user != null) {

            retainFragment.registerUser(new Consumer<Void>() {

                @Override
                public void apply(Void v) {
                    startActivity(new Intent(MapsActivity.this, MapsActivity.class));
                }

                @Override
                public Object get() {
                    return user;
                }
            });
        }
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MY_LOCATION_REQUEST_CODE) {
            if (permissions.length == 1 &&
                    permissions[0] == android.Manifest.permission.ACCESS_FINE_LOCATION &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            } else {
                // Permission was denied. Display an error message.
            }
        }
    }

    @Override
    public void onMapClick(LatLng point) {
        mMap.addMarker(new MarkerOptions().position(point).title("123123"));

    }

    private void setMarkers() {
        getUsers();

        LatLng sydney = new LatLng(-33.852, 151.211);

        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(sydney)
                .title("mArea")
                .snippet("Snippet").icon(BitmapDescriptorFactory.fromResource(Integer.valueOf(userList.get(0).getFavoriteDrinks()[0].getId()))));

        mMap.addMarker(new MarkerOptions().position(sydney)
                .title("Marker in Sydney"));


        User user;
        LatLng point;
        for (int i = 0; i < userList.size(); i++) {
            user = userList.get(i);
            point = new LatLng(
                    Integer.valueOf(user.getAvailable().getLat()),
                    Integer.valueOf(user.getAvailable().getLat()));
            mMap.addMarker(new MarkerOptions()
                    .position(point)
                    .title(user.getName()));
        }
    }

    private void getUsers() {
        userList.clear();

        User user = new UserBuilder()
                .addName("Donald Duck")
                .addEmail("donald@duck.com")
                .addDrink(Drink.getDrink("0"))
                .addLat("52")
                .addLng("0")
                .addHours("3")
                .build();
        userList.add(user);
        User user2 = new UserBuilder()
                .addName("SnowWhite")
                .addEmail("love@dwarfs.com")
                .addDrink(Drink.getDrink("7"))
                .addLat("53")
                .addLng("0")
                .addHours("7")
                .build();
        userList.add(user2);
        User user3 = new UserBuilder()
                .addName("Snoopy")
                .addEmail("snoop@dog.com")
                .addDrink(Drink.getDrink("12"))
                .addLat("54")
                .addLng("0")
                .addHours("8")
                .build();
        userList.add(user3);
        User user4 = new UserBuilder()
                .addName("Rihanna")
                .addEmail("shine@diamond.com")
                .addDrink(Drink.getDrink("7"))
                .addLat("50")
                .addLng("0")
                .addHours("1")
                .build();
        userList.add(user4);

    }

}