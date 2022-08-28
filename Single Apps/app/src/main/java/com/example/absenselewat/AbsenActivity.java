package com.example.absenselewat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

//Aplikasi akan menampilkan longitude dan latitude secara langsung
//saat aplikasi dibuka (real time).
//Koordinat yang dicari menunjukkan lokasi tepatnya perangkat,
//sehingga perangkat memiliki batasan lokasi untuk mengaktifkan tombol (button).
//Menggunakan API dari Google (PlayServices) untuk mengakses latitude dan longitude.
//Jarak antar titik absen dan titik perangkat ditentukan oleh method distanceTo().
//Data latitude dan longitude titik absen serta radius absen disimpan di dalam database.
//Database yang digunakan adalah Firebase.
//Sumber: https://www.geeksforgeeks.org/how-to-get-user-location-in-android/

public class AbsenActivity extends AppCompatActivity implements OnClickListener {

    FusedLocationProviderClient locProvider;
    TextView latitudeText, longitudeText, usernameShow;
    EditText latitudePoint, longitudePoint;
    Button absen, assignPoint, logout, maps;
    FirebaseAuth auth;
    FirebaseDatabase firebaseData;
    DatabaseReference databaseRef;
    double latitudeDatabase, longitudeDatabase, rangeDatabase;
    boolean masuk;
    int PERMISSION_ID = 44;
    String clockIn = "ABSEN MASUK";
    String clockOut = "ABSEN PULANG";

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_absen);

        this.auth = FirebaseAuth.getInstance();
        this.firebaseData = FirebaseDatabase.getInstance();
        this.databaseRef = this.firebaseData.getReference();

        this.latitudeText = findViewById(R.id.latitude);
        this.longitudeText = findViewById(R.id.longitude);
        this.latitudePoint = findViewById(R.id.latitudepoint);
        this.longitudePoint = findViewById(R.id.longitudepoint);
        this.usernameShow = findViewById(R.id.user_username);
        this.assignPoint = findViewById(R.id.assign);
        this.absen = findViewById(R.id.absen);
        this.logout = findViewById(R.id.logout_button);
        this.maps = findViewById(R.id.maps_button);
        this.assignPoint.setOnClickListener(this);
        this.absen.setOnClickListener(this);
        this.logout.setOnClickListener(this);
        this.maps.setOnClickListener(this);

        this.getUsernameToPage();
        this.getAbsenStatus();
        this.getLongLatRange();

        this.locProvider = LocationServices.getFusedLocationProviderClient(this);
        this.getLastLocation();
//        this.autoClockOutOOR();
//        Toast.makeText(AbsenActivity.this, "Status absensi: " + masuk, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("MissingPermission")
    public void getLastLocation(){
        if(this.checkPermissions()){
            if(this.isLocationEnabled()){
                this.locProvider.getLastLocation().addOnCompleteListener(task -> {
                    Location loc = task.getResult();
                    if(loc == null){
                        requestNewLocationData();
                    }

                    Location poinAbsen = new Location("poinAbsen");
                    Location poinCurr = new Location("poinCurr");

                    String lati = Double.toString(Objects.requireNonNull(loc).getLatitude());
                    String longi = Double.toString(loc.getLongitude());
                    latitudeText.setText(lati);
                    longitudeText.setText(longi);

                    poinAbsen.setLatitude(latitudeDatabase);
                    poinAbsen.setLongitude(longitudeDatabase);
                    poinCurr.setLatitude(loc.getLatitude());
                    poinCurr.setLongitude(loc.getLongitude());
                    double distance = poinAbsen.distanceTo(poinCurr);
                    //                        Toast.makeText(AbsenActivity.this, "Anda tidak berada di area absensi.", Toast.LENGTH_LONG).show();
                    //                        Toast.makeText(AbsenActivity.this, "Silakan melakukan absensi.", Toast.LENGTH_LONG).show();
                    absen.setEnabled(!(distance > rangeDatabase));
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
            this.doAbsensi();
            this.locProvider = LocationServices.getFusedLocationProviderClient(this);
            this.getLastLocation();
        }
        else if(view.getId() == R.id.assign){
            try{
                this.databaseRef.child("poin_absen").child("latitude").setValue(Double.parseDouble(this.latitudePoint.getText().toString()));
                this.databaseRef.child("poin_absen").child("longitude").setValue(Double.parseDouble(this.longitudePoint.getText().toString()));
                Toast.makeText(this, "Titik absen terbaru ditambahkan.", Toast.LENGTH_LONG).show();
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
        else if(view.getId() == R.id.logout_button){
            new AlertDialog.Builder(this)
                    .setTitle("Log Out")
                    .setMessage("Apakah Anda yakin ingin Log Out dari aplikasi?")
                    .setPositiveButton("Ya", (dialogInterface, i) -> {
                        auth.signOut();
                        startActivity(new Intent(AbsenActivity.this, LoginActivity.class));
                        finish();
                    })
                    .setNegativeButton("Tidak", null)
                    .show();
        }
        else if(view.getId() == R.id.maps_button){
            startActivity(new Intent(this, OpenStreetMapsActivity.class));
            finish();
        }
    }

    private void getUsernameToPage(){
//        this.usernameShow.setText(this.databaseRef.child("user").child(Objects.requireNonNull(this.auth.getCurrentUser()).getUid()).get().toString());
        this.databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String uName = snapshot.child("user").child(Objects.requireNonNull(auth.getCurrentUser()).getUid()).child("username").getValue(String.class);
                usernameShow.setText(uName);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AbsenActivity.this, "Gagal mengambil data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getAbsenStatus(){
        this.databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                masuk = snapshot.child("user").child(Objects.requireNonNull(auth.getCurrentUser()).getUid()).child("absensi").getValue(Boolean.class);
                if(masuk){
                    absen.setText(clockOut);
                } else {
                    absen.setText(clockIn);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AbsenActivity.this, "Status absensi pengguna tidak diketahui.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setAbsenStatus(boolean status){
        this.databaseRef.child("user").child(Objects.requireNonNull(this.auth.getCurrentUser()).getUid()).child("absensi").setValue(status);
        this.getAbsenStatus();
    }

    private void getLongLatRange(){
        this.databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                latitudeDatabase = snapshot.child("poin_absen").child("latitude").getValue(Double.class);
                longitudeDatabase = snapshot.child("poin_absen").child("longitude").getValue(Double.class);
                rangeDatabase = snapshot.child("poin_absen").child("range").getValue(Double.class);

                String latData = Double.toString(latitudeDatabase);
                String longData = Double.toString(longitudeDatabase);
                latitudePoint.setText(latData);
                longitudePoint.setText(longData);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AbsenActivity.this, "Tidak dapat mengambil titik absen.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void doAbsensi(){
        if(!this.masuk){
            this.setAbsenStatus(true);
            Toast.makeText(this, "Berhasil melakukan absen masuk (clock in)!", Toast.LENGTH_SHORT).show();
        }
        else{
            new AlertDialog.Builder(this)
                .setTitle("Clock Out / Absen Pulang")
                .setMessage("Apakah Anda yakin ingin absen pulang sekarang?")
                .setPositiveButton("Ya", (dialogInterface, i) -> {
                    this.setAbsenStatus(false);
                    Toast.makeText(this, "Berhasil melakukan absen pulang (clock out)!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Tidak", null)
                .show();
        }
    }

//    private void autoClockOutOOR(){
//        this.getAbsenStatus();
//        if(this.masuk && !this.absen.isEnabled()){
//            this.setAbsenStatus(false);
//            Toast.makeText(this, "Otomatis clock out karena keluar dari lokasi.", Toast.LENGTH_SHORT).show();
//        }
//        else if(!this.masuk){
//            Toast.makeText(this, "Status absensi masih false.", Toast.LENGTH_SHORT).show();
//        }
//    }
}