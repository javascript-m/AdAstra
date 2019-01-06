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
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

/***
 * unutar registerUser dodati novi dokument za usera u collection Users
 */

public class SignUp extends AppCompatActivity {

    EditText emailIn, passIn;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();

        emailIn = (EditText)findViewById(R.id.SU_email);
        passIn = (EditText) findViewById(R.id.SU_password);
    }

    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.SU_submitBtn:
                registerUser();
                break;
        }
    }

    private void registerUser() {
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

        mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    finish();
                    FirebaseUser user = mAuth.getCurrentUser();

                    Intent intent = new Intent(SignUp.this, MyProfile.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //In case user presses 'back'
                    startActivity(intent);
                } else {
                    if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                        Toast.makeText(SignUp.this, "You are alreaddy registered", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SignUp.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
}
