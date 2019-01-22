package com.ad_astra.maja.adastra;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/* Unutar validate inputs (update)
GOAL TREBA ISCITAVAT PROGRESS BAR
Mozes obrisat onaj dolje add success listener
treba na UI dodat hReplacement i dodat ChooseHabit actitvity sa intentom ispred
* */

public class AddHabit extends AppCompatActivity {

    ImageView hImg;
    EditText hName, hDesc, hTrigger, hReplacement;
    SeekBar hGoal;

    String name, desc, trigger, replacement;
    int goal;

    User user;
    String userID;

    Context context;
    FirebaseAuth mAuth;
    FirebaseFirestore db;

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

    public long getMidnight(int day) {
        Calendar date = new GregorianCalendar();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        date.add(Calendar.DAY_OF_MONTH, day);

        return date.getTime().getTime()/1000;
    }

    private boolean validateInputs() {
        name = hName.getText().toString().trim();
        desc = hDesc.getText().toString().trim();
        trigger = hTrigger.getText().toString().trim();
        replacement = "replacement";
        goal = 2;
        //Get GOAL and REPLACEMENT VALUE

        //KAD SPREMAS SVE TO LOWERCASE I STIRP(TRIM)

        if (name.isEmpty()) {
            hName.setError("Name is required.");
            hName.requestFocus();
            return false;
        }
        if (user.habitList.contains(name)) {
            hName.setError("This habit already exists");
            hName.requestFocus();
            return false;
        }
        return true;
    }

    //Create habitInfo file and add habit name to userInfo file
    private void submitHabitChanges() {
        final HabitInfo habitInfo = new HabitInfo(context, name, desc, goal, trigger, replacement, getMidnight(0));
        db.collection("users").document(userID).collection("habits").document(name).set(habitInfo);

        user.habitList.add(name);
        db.collection("users").document(userID).set(user);
    }
}
