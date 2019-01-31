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

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

/***
 * unutar registerUser dodati novi dokument za usera u collection Users
 */

public class SignUp extends AppCompatActivity {

    EditText userIn, emailIn, passIn;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        userIn = (EditText) findViewById(R.id.SU_username);
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
        final String username = userIn.getText().toString().trim();
        String email = emailIn.getText().toString().trim();
        String pass = passIn.getText().toString().trim();

        //User input validation
        if (username.isEmpty()) {
            userIn.setError("Username is required");
            userIn.requestFocus();
            return;
        }
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
                    final FirebaseUser user = mAuth.getCurrentUser();

                    UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                            .setDisplayName(username)
                            .build();

                    try {
                        user.updateProfile(profile).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    String photoUrl = ""; //Url na custom sliku
                                    if (user.getPhotoUrl() != null)
                                        photoUrl = user.getPhotoUrl().toString();

                                    User dbUser = new User(0, 0, 0, user.getDisplayName(), photoUrl);
                                    db.collection("users").document(user.getUid()).set(dbUser);
                                    Intent intent = new Intent(SignUp.this, HomeScreen.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //In case user presses 'back'
                                    startActivity(intent);
                                }
                            }
                        });
                    } catch (Exception e) {
                        Toast.makeText(SignUp.this, e.toString(), Toast.LENGTH_SHORT).show();
                    }


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
