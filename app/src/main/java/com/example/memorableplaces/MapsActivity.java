package com.example.memorableplaces;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    LocationManager locationManager;
    LocationListener locationListener;
    Location lastLocation;
    List<Address> addresses;
    Boolean start = false;


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                Location lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


    }

    public void centerOnMapZoom(Location location , String title){
        if(location != null) {
            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(currentLocation).title("Current Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);

        Intent intent=getIntent();
        if(intent.getIntExtra("places",-1) == -1) {
            start = true;

            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    //centerOnMapZoom(location,"Your Location");
                }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle) {

                }

                @Override
                public void onProviderEnabled(String s) {

                }

                @Override
                public void onProviderDisabled(String s) {

                }
            };

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                centerOnMapZoom(lastLocation,"Current Location");
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }else{
            start = false;
            Location placeLocation = new Location(LocationManager.NETWORK_PROVIDER);
            placeLocation.setLatitude(MainActivity.locations.get(intent.getIntExtra("places",0)).latitude);
            placeLocation.setLongitude(MainActivity.locations.get(intent.getIntExtra("places",0)).longitude);

            centerOnMapZoom(placeLocation,MainActivity.places.get(intent.getIntExtra("places",0)));
        }

    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        if (start) {

            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
            String address = "";
            try {
                addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                if (addresses != null && addresses.size() > 0) {
                    if (addresses.get(0).getThoroughfare() != null) {
                        if (addresses.get(0).getSubThoroughfare() != null) {
                            address += addresses.get(0).getSubThoroughfare() + " ";
                        }
                        address += addresses.get(0).getThoroughfare();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (address.equals("")) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd-MM-yyyy");
                address += sdf.format(new Date());
            }
            mMap.addMarker(new MarkerOptions().position(latLng).title(address));

            MainActivity.places.add(address);
            MainActivity.locations.add(latLng);
            MainActivity.arrayAdapter.notifyDataSetChanged();

            SharedPreferences sharedPreferences = this.getSharedPreferences("com.example.memorableplaces",Context.MODE_PRIVATE);

            try {

                ArrayList<String> latitude = new ArrayList<String>();
                ArrayList<String> longitude = new ArrayList<String>();

                for(LatLng coord : MainActivity.locations){
                    latitude.add(Double.toString(coord.latitude));
                    longitude.add(Double.toString(coord.longitude));
                }

                sharedPreferences.edit().putString("places", ObjectSerializer.serialize(MainActivity.places)).apply();
                sharedPreferences.edit().putString("lats", ObjectSerializer.serialize(latitude)).apply();
                sharedPreferences.edit().putString("lons", ObjectSerializer.serialize(longitude)).apply();


            }catch (Exception e){
                e.printStackTrace();
            }
            Toast.makeText(getApplicationContext(), "Location Saved!", Toast.LENGTH_SHORT).show();
        }
    }
}
