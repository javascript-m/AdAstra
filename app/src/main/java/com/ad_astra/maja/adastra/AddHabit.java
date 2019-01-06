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

    EditText hName, hDesc;
    ProgressBar pr1, pr2, pr3;

    String Sname, Sdesc;
    int Ipr1, Ipr2, Ipr3;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_habit);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        hName = (EditText)findViewById(R.id.AAH_name);
        hDesc = (EditText)findViewById(R.id.AAH_desc);
        pr1 = (ProgressBar)findViewById(R.id.AAH_p1S);
        pr2 = (ProgressBar)findViewById(R.id.AAH_p2S);
        pr3 = (ProgressBar)findViewById(R.id.AAH_p3S);
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
        Sdesc = hDesc.getText().toString().trim();
        Ipr1 = pr1.getProgress();
        Ipr2 = pr2.getProgress();
        Ipr3 = pr3.getProgress();

        if (Sname.isEmpty()) {
            hName.setError("Name is required.");
            hName.requestFocus();
            return false;
        }
        if (Sdesc.isEmpty()) {
            hDesc.setError("Description is required");
            hDesc.requestFocus();
            return false;
        }
        return true;
    }

    private void submitHabitChanges() {
        final HabitInfo hInfo = new HabitInfo(Sname, Sdesc,"blablaImgUrl", Ipr1, Ipr2, Ipr3);
        db.collection("users").document(mAuth.getCurrentUser().getUid()).collection("habits").document(hInfo.hName).set(hInfo);

        //Add habit to user's habitList
        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    User user = task.getResult().toObject(User.class);
                    user.habitList.add(Sname);

                    db.collection("users").document(mAuth.getCurrentUser().getUid()).set(user);
                }
            }
        });

    }
}
