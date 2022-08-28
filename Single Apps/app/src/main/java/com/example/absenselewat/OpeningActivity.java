package com.example.absenselewat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.appcompat.app.AppCompatActivity;

public class OpeningActivity extends AppCompatActivity {

    FirebaseAuth auth;
    FirebaseDatabase firebaseData;
    DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.auth = FirebaseAuth.getInstance();
        this.firebaseData = FirebaseDatabase.getInstance();
        this.databaseRef = this.firebaseData.getReference();

        FirebaseUser currUser = this.auth.getCurrentUser();

        setContentView(R.layout.activity_splash_screen);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            updateUI(currUser);
            finish();
        }, 3000);
    }

    public void updateUI(FirebaseUser user){
        if(user != null){
            startActivity(new Intent(this, AbsenActivity.class));
        } else {
            startActivity(new Intent(this, LoginActivity.class));
            Toast.makeText(this,"Silakan login menggunakan nomor HP dan username Anda.",Toast.LENGTH_LONG).show();
        }
        finish();
    }

}
