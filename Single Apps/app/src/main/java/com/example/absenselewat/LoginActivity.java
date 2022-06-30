package com.example.absenselewat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity implements OnClickListener {

    Button login;
    FirebaseAuth auth;
    FirebaseDatabase databaseInstance;
    DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        this.auth = FirebaseAuth.getInstance();
        this.databaseInstance = FirebaseDatabase.getInstance();

        if (this.auth.getCurrentUser() != null) {
            startActivity(new Intent(this, AbsenActivity.class));
        }

        setContentView(R.layout.activity_login);

        this.login = findViewById(R.id.login_button);
        this.login.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        startActivity(new Intent(this, AbsenActivity.class));
    }
}
