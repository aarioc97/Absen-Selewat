package com.example.absenselewat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

//Aplikasi akan menampilkan longitude dan latitude secara langsung
//saat aplikasi dibuka (real time).
//Koordinat yang dicari menunjukkan lokasi tepatnya perangkat,
//sehingga perangkat memiliki batasan lokasi untuk mengaktifkan tombol (button).
//Menggunakan API dari Google (PlayServices) untuk mengakses latitude dan longitude.
//Sumber: https://www.geeksforgeeks.org/how-to-get-user-location-in-android/

//Penonaktifan tombol absen masih salah!

public class MainActivity extends AppCompatActivity implements OnClickListener {

    FusedLocationProviderClient locProvider;
    TextView latitudeText, longitudeText;
    EditText latitudePoint, longitudePoint;
    Button absen, assignPoint;
    int PERMISSION_ID = 44;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.latitudeText = findViewById(R.id.latitude);
        this.longitudeText = findViewById(R.id.longitude);
        this.latitudePoint = findViewById(R.id.latitudepoint);
        this.longitudePoint = findViewById(R.id.longitudepoint);
        this.assignPoint = findViewById(R.id.assign);
        this.absen = findViewById(R.id.absen);
        this.assignPoint.setOnClickListener(this);
        this.absen.setOnClickListener(this);

        this.locProvider = LocationServices.getFusedLocationProviderClient(this);
        this.getLastLocation();
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation(){
        if(this.checkPermissions()){
            if(this.isLocationEnabled()){
                this.locProvider.getLastLocation().addOnCompleteListener(task -> {
                    Location loc = task.getResult();
                    if(loc == null){
                        requestNewLocationData();
                    }
                    else {
                        if (loc.getLatitude() <= (Double.parseDouble(this.latitudePoint.getText().toString()) - 0.0001) || loc.getLatitude() >= (Double.parseDouble(this.latitudePoint.getText().toString()) + 0.0001)){
                            if (loc.getLongitude() <= (Double.parseDouble(this.longitudePoint.getText().toString()) - 0.0001) || loc.getLongitude() >= (Double.parseDouble(this.longitudePoint.getText().toString()) + 0.0001)){
                                this.absen.setEnabled(false);
                                Toast.makeText(this, "Anda tidak berada di area absensi.", Toast.LENGTH_LONG).show();
                            }
                            else{
                                this.absen.setEnabled(true);
                                Toast.makeText(this, "Silakan melakukan absensi.", Toast.LENGTH_LONG).show();
                            }
                        }
                        String lati = Double.toString(loc.getLatitude());
                        String longi = Double.toString(loc.getLongitude());
                        latitudeText.setText(lati);
                        longitudeText.setText(longi);
                    }
                });
            }
            else{
                Toast.makeText(this, "Mohon nyalakan layanan lokasi (GPS atau data seluler).", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        }
        else {
            requestPermissions();
        }
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData(){
        LocationRequest locReq = LocationRequest.create();
        locReq.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
        locReq.setInterval(5);
        locReq.setFastestInterval(0);
        locReq.setNumUpdates(1);

        this.locProvider = LocationServices.getFusedLocationProviderClient(this);
        this.locProvider.requestLocationUpdates(locReq, callback, Looper.myLooper());
    }

    private final LocationCallback callback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            Location loc = locationResult.getLastLocation();
            if(loc != null){
                String lati = Double.toString(loc.getLatitude());
                String longi = Double.toString(loc.getLongitude());
                latitudeText.setText(lati);
                longitudeText.setText(longi);
            }
        }
    };

    private boolean checkPermissions(){
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
    }

    private boolean isLocationEnabled(){
        LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    // If everything is alright then
    @Override
    public void
    onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkPermissions()) {
            getLastLocation();
        }
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.absen) {
            Toast.makeText(this, "Sudah masuk absen!", Toast.LENGTH_SHORT).show();
        }
        else if(view.getId() == R.id.assign){
            try{
                this.locProvider = LocationServices.getFusedLocationProviderClient(this);
                this.getLastLocation();
            }
            catch (Exception e){
                Toast.makeText(this, "Menggunakan titik absen default.", Toast.LENGTH_SHORT).show();
                String latDef = Double.toString(-6.9449);
                String longDef = Double.toString(107.6719);
                this.latitudePoint.setText(latDef);
                this.longitudePoint.setText(longDef);
            }
        }
    }
}
