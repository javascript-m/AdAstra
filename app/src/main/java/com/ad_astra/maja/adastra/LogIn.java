package com.ad_astra.maja.adastra;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LogIn extends AppCompatActivity {

    EditText emailIn, passIn;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        mAuth = FirebaseAuth.getInstance();

        emailIn = (EditText)findViewById(R.id.LI_email);
        passIn = (EditText) findViewById(R.id.LI_password);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.LI_submitBtn:
                logUser();
                break;
        }
    }

    //Check if user is already logged in
    @Override
    protected void onStart() {
        super.onStart();

        if (mAuth.getCurrentUser() != null) {
            finish();
            startActivity(new Intent(LogIn.this, MyProfile.class));
        }
    }

    private void logUser() {
        String email = emailIn.getText().toString().trim();
        String pass = passIn.getText().toString().trim();

        //User input validation
        if (email.isEmpty()) {
            emailIn.setError("Email is required.");
            emailIn.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailIn.setError("Please enter a valid email.");
            emailIn.requestFocus();
            return;
        }
        if (pass.isEmpty()) {
            passIn.setError("Password is required.");
            passIn.requestFocus();
            return;
        }
        if (pass.length() < 6) {
            passIn.setError("Minimum length of password should be 6.");
            passIn.requestFocus();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(LogIn.this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    finish();
                    FirebaseUser user = mAuth.getCurrentUser();

                    Intent intent = new Intent(LogIn.this, MyProfile.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //In case user presses 'back'
                    startActivity(intent);
                } else {
                    Toast.makeText(LogIn.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
