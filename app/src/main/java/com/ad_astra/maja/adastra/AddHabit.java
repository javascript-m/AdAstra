package com.ad_astra.maja.adastra;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;
import java.util.ArrayList;

/* Srediti url slike
* Unutar validate inputs pazi da nema bise od odredjenog broja navika
* */

public class AddHabit extends AppCompatActivity {

    EditText hName;

    String Sname;
    User user;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_habit);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        hName = (EditText)findViewById(R.id.AAH_name);
    }

    @Override
    protected void onStart() {
        super.onStart();
        db.collection("users").document(mAuth.getCurrentUser().getUid())
        .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    user = task.getResult().toObject(User.class);
                }
            }
        });
    }

    public void onClick(View v) {
        switch (v.getId()){
            case R.id.AAH_submit:
                if (validateInputs()) {
                    submitHabitChanges();
                    finish();
                    startActivity(new Intent(AddHabit.this, HomeScreen.class));
                }
                break;
        }
    }

    private boolean validateInputs() {
        Sname = hName.getText().toString().trim();

        if (Sname.isEmpty()) {
            hName.setError("Name is required.");
            hName.requestFocus();
            return false;
        }
        if (user.habitList.contains(Sname)) {
            hName.setError("This habit already exists.");
            hName.requestFocus();
            return false;
        }
        return true;
    }

    //Create habitInfo file and add habit name to userInfo file
    private void submitHabitChanges() {
        //final HabitInfo hInfo = new HabitInfo(Sname, Sdesc,"blablaImgUrl", Ipr1, Ipr2, Ipr3);
        //db.collection("users").document(user.userID).collection("habits").document(hInfo.hName).set(hInfo);

        user.habitList.add(Sname);
        db.collection("users").document(user.userID).set(user);

    }
}
