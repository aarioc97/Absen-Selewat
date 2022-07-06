package com.example.absenselewat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

//Jika pengguna belum mendaftarkan nomor HP, maka pengguna dapat mendaftarkan nomor HP di page ini.
//Jika pengguna sudah log out, pengguna dapat masuk kembali menggunakan username dan nomor HP.
//Data Username, Nomor HP, dan Nomor IMEI HP akan disimpan di dalam database.
//Sumber: https://www.geeksforgeeks.org/firebase-authentication-with-phone-number-otp-in-android/

public class LoginActivity extends AppCompatActivity implements OnClickListener {

    Button login, getOTP;
    EditText nomorText, usernameText, OTPText;
    String noHP, username, noOTP, noIMEI, verificationId;
    FirebaseAuth auth;
    FirebaseDatabase firebaseData;
    DatabaseReference databaseRef;
    private static final String KEY_VERIFICATION_ID = "key_verification_id";
    private static final int REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        this.auth = FirebaseAuth.getInstance();
        this.firebaseData = FirebaseDatabase.getInstance();
        this.databaseRef = this.firebaseData.getReference();

        FirebaseUser currUser = this.auth.getCurrentUser();
        updateUI(currUser);

        setContentView(R.layout.activity_login);

        this.nomorText = findViewById(R.id.nomor_hp);
        this.usernameText = findViewById(R.id.username);
        this.OTPText = findViewById(R.id.nomor_otp);
        this.login = findViewById(R.id.login_button);
        this.getOTP = findViewById(R.id.get_otp_button);
        this.login.setOnClickListener(this);
        this.getOTP.setOnClickListener(this);

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_CODE);
            return;
        }
        this.noIMEI = telephonyManager.getImei();

        if (verificationId == null && savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState);
        }
    }

    @Override
    public void onClick(View view) {
        this.noHP = this.nomorText.getText().toString();
        this.username = this.usernameText.getText().toString();
        this.noOTP = this.OTPText.getText().toString();

        if(view.getId() == R.id.login_button){
            // validating if the OTP text field is empty or not.
            if (TextUtils.isEmpty(this.noOTP)) {
                // if the OTP text field is empty display
                // a message to user to enter OTP
                Toast.makeText(this, "Mohon masukkan nomor OTP.", Toast.LENGTH_SHORT).show();
            } else {
                // if OTP field is not empty calling
                // method to verify the OTP.
                verifyCode(this.noOTP);
            }
        }
        else if(view.getId() == R.id.get_otp_button){
            if (this.noHP.length() < 10) {
                Toast.makeText(this, "Nomor HP tidak valid.", Toast.LENGTH_SHORT).show();
            } else if(this.username.contains(" ")){
                Toast.makeText(this, "Username tidak dapat mengandung spasi.", Toast.LENGTH_SHORT).show();
            } else if (this.username.isEmpty()) {
                Toast.makeText(this, "Mohon isi Username.", Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(this.noHP)) {
                Toast.makeText(this, "Masukkan nomor HP terlebih dahulu.", Toast.LENGTH_SHORT).show();
            } else {
                String phone = this.noHP;
                sendVerificationCode(phone);
            }
        }
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        // inside this method we are checking if
        // the code entered is correct or not.
        this.auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // if the code is correct and the task is successful
                        // we are sending our user to new activity.
                        Intent i = new Intent(LoginActivity.this, AbsenActivity.class);
                        startActivity(i);
                        this.databaseRef.child("user").child(Objects.requireNonNull(this.auth.getCurrentUser()).getUid()).child("username").setValue(this.username);
                        this.databaseRef.child("user").child(Objects.requireNonNull(this.auth.getCurrentUser()).getUid()).child("nomor_HP").setValue(this.noHP);
                        this.databaseRef.child("user").child(Objects.requireNonNull(this.auth.getCurrentUser()).getUid()).child("nomor_IMEI").setValue(this.noIMEI);
                        this.databaseRef.child("user").child(Objects.requireNonNull(this.auth.getCurrentUser()).getUid()).child("is_admin").setValue(0);
                        finish();
                    } else {
                        // if the code is not correct then we are
                        // displaying an error message to the user.
                        Toast.makeText(LoginActivity.this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void sendVerificationCode(String number) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(this.auth)
                        .setPhoneNumber(number)            // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(mCallBack)           // OnVerificationStateChangedCallbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    // callback method is called on Phone auth provider.
    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks

            // initializing our callbacks for on
            // verification callback method.
            mCallBack = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        // below method is used when
        // OTP is sent from Firebase
        @Override
        public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(s, forceResendingToken);
            // when we receive the OTP it
            // contains a unique id which
            // we are storing in our string
            // which we have already created.
            verificationId = s;
        }
        // this method is called when user
        // receive OTP from Firebase.
        @Override
        public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
            // below line is used for getting OTP code
            // which is sent in phone auth credentials.
            final String code = phoneAuthCredential.getSmsCode();

            // checking if the code
            // is null or not.
            if (code != null) {
                // if the code is not null then
                // we are setting that code to
                // our OTP edittext field.
                OTPText.setText(code);

                // after setting this code
                // to OTP edittext field we
                // are calling our verifycode method.
                verifyCode(code);
            }
        }

        // this method is called when firebase doesn't
        // sends our OTP code due to any error or issue.
        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {
            // displaying error message with firebase exception.
            Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    };

    // below method is use to verify code from Firebase.
    private void verifyCode(String code) {
        // below line is used for getting
        // credentials from our verification id and code.
        if(verificationId != null){
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
            // after getting credential we are
            // calling sign in method.
            signInWithCredential(credential);
        } else {
            Toast.makeText(LoginActivity.this, "Variabel verificationId null!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_VERIFICATION_ID, verificationId);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        verificationId = savedInstanceState.getString(KEY_VERIFICATION_ID);
    }

    public void updateUI(FirebaseUser user){
        if(user != null){
            startActivity(new Intent(this, AbsenActivity.class));
            finish();
        } else {
            Toast.makeText(this,"Silakan login menggunakan nomor HP dan username Anda.",Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Aplikasi sudah mendapatkan izin.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Mohon beri izin untuk aplikasi.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
