package com.ad_astra.maja.adastra;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Set;

/* Unutar validate inputs (update)
Mozes obrisat onaj dolje add success listener
treba na UI dodat hReplacement i dodat ChooseHabit actitvity sa intentom ispred
* */

public class AddHabit extends AppCompatActivity {

    ImageView hImg;
    EditText hName, hDesc, hTrigger, hReplacement;
    SeekBar hGoal;

    String sName, sDesc, sTrigger, sReplacement;
    int iGoal;

    User user;
    String userID;

    Context context;
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    Set<String> hList;
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_habit);

        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        context = (Context) getApplicationContext();
        sharedPref = context.getSharedPreferences(userID, Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        hImg = (ImageView)findViewById(R.id.AAH_img);
        hName = (EditText)findViewById(R.id.AAH_name);
        hGoal = (SeekBar)findViewById(R.id.AAH_goal);
        hDesc = (EditText)findViewById(R.id.AAH_desc);
        hTrigger = (EditText)findViewById(R.id.AAH_trigger);
        //hReplacement = (EditText)findViewById(R.id.AAH_replacement);
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
        sName = hName.getText().toString().trim();
        sDesc = hDesc.getText().toString().trim();
        sTrigger = hTrigger.getText().toString().trim();
        sReplacement = "replacement";
        iGoal = 2;
        //Get GOAL and REPLACEMENT VALUE

        if (sName.isEmpty()) {
            hName.setError("Name is required.");
            hName.requestFocus();
            return false;
        }
        if (user.habitList.contains(sName)) {
            hName.setError("This habit already exists");
            hName.requestFocus();
            return false;
        }
        return true;
    }

    //Create habitInfo file and add habit name to userInfo file
    private void submitHabitChanges() {
        final HabitInfo habitInfo = new HabitInfo(context, sName, sDesc, iGoal, sTrigger, sReplacement);

        //Update Shared Preferences and add habit to current habit list
        /*hList = (Set<String>) sharedPref.getStringSet("habitList", new HashSet<String>());
        hList.add(sName);
        editor.putStringSet("habitList", hList);
        editor.commit();*/

        //Put it online in case user changes phone
        db.collection("users").document(userID).collection("habits").document(sName).set(habitInfo)
        .addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    //Toast.makeText(AddHabit.this, "Success", Toast.LENGTH_LONG).show();
                }
                else {
                    //Toast.makeText(AddHabit.this, "Fail", Toast.LENGTH_LONG).show();
                }
            }
        });

        user.habitList.add(sName);
        db.collection("users").document(userID).set(user);
    }
}
