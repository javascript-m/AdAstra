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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/***
 * is date valid (b date)
 *
 */

public class EditUserInfo extends AppCompatActivity {

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    EditText fNameE;
    EditText lNameE;
    EditText bDateE;

    private static final String TAG = "MyActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user_info);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        fNameE = (EditText)findViewById(R.id.EUI_fName);
        lNameE = (EditText)findViewById(R.id.EUI_lName);
        bDateE = (EditText)findViewById(R.id.EUI_bDate);
    }

    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.EUI_submitBtn:
                updateUserInfo();
                finish();
                startActivity(new Intent(EditUserInfo.this, MyProfile.class));
                break;
        }
    }

    private void updateUserInfo() {
        String fName = fNameE.getText().toString().trim();
        String lName = lNameE.getText().toString().trim();
        String bDate = bDateE.getText().toString().trim();

        //User input validation
        if (fName.isEmpty()) {
            fNameE.setError("First name is required.");
            fNameE.requestFocus();
            return;
        }
        if (lName.isEmpty()) {
            lNameE.setError("Last name is required.");
            lNameE.requestFocus();
            return;
        }
        if (bDate.isEmpty()) {
            bDateE.setError("Birth date is required.");
            bDateE.requestFocus();
            return;
        }

        User user = new User(mAuth.getCurrentUser().getUid(), fName, lName, bDate);
        db.collection("users").document(user.userID).set(user);
    }
}
