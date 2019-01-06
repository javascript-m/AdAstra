package com.ad_astra.maja.adastra;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Layout;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;

import org.w3c.dom.Document;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/* uredi gumb i kod if/elsa sredi parametre u objektu koji se dodaje
* VAZNO: offline mode
* line 100
* 207: u intentu poslat nes da naglasis da treba ucitat stare podatke i updateat samo blabla -> ime navike ne smin minjat!
* */

public class HomeScreen extends AppCompatActivity {

    private static final String TAG = "HOME SCREEN";
    TextView curUser;
    LinearLayout habitHolder;

    User user;
    String userID;
    String currentDate;

    Context context;
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        Toolbar toolbar = findViewById(R.id.HS_toolbar);
        setSupportActionBar(toolbar);

        user = new User();
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userID = mAuth.getCurrentUser().getUid();

        //Used for accessing today's habit data quickly
        context = getApplicationContext();
        sharedPref = context.getSharedPreferences(getString(R.string.my_habits), Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        curUser = (TextView)findViewById(R.id.HS_curUser);
        habitHolder = (LinearLayout)findViewById(R.id.HS_habitHolder);

        //Get date for event filename
        Date cDay = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
        currentDate = df.format(cDay);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.HS_add: //&& button is not disabled
                if (user.habitList.size() >= 4) {
                    //Make button disabled

                }
                finish();
                startActivity(new Intent(HomeScreen.this, AddHabit.class));
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        //Real-time updates listener (checks if database state has changed)
        db.collection("users").document(userID)
        .addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    return;
                }
                if (snapshot != null && snapshot.exists()) {
                    user = snapshot.toObject(User.class);
                    if (user != null)
                        updateHomeScreen();
                }
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuLogout:
                FirebaseAuth.getInstance().signOut();
                finish();
                startActivity(new Intent(HomeScreen.this, MainActivity.class));
                break;
        }
        return true;
    }

    private void arrangeButtonStyle(String val, Button btnID) {
        if (val.equals("true")) {
            btnID.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.habitDone));
        } else {
            btnID.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.habitUndone));
        }
    }

    //Update home screen every time a change in database occurs
    private void updateHomeScreen() {
        //Update user name
        if (user.fName != null) curUser.setText(user.fName);

        //Put habit views inside habit holder and set design
        habitHolder.removeAllViews();
        for (int i=0; i<user.habitList.size(); i++) {
            final String hName = user.habitList.get(i);
            final String pref_hName = userID+"_"+hName;
            final Button hBtn = new Button(this);
            hBtn.setText(hName);

            //If that habit is already done for today, edit button style
            Map<String, Object> setData =  new HashMap<>();
            String isDone = sharedPref.getString(pref_hName, "false");
            setData.put(hName, isDone);
            arrangeButtonStyle(isDone, hBtn);
            db.collection("users").document(userID).collection("events").document(currentDate).set(setData, SetOptions.merge());

            //If a long click occurs, change habit's value (done/undone)
            hBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Map<String, Object> setData = new HashMap<>();
                    String currentState = sharedPref.getString(pref_hName, "false");

                    if (currentState.equals("true")) currentState = "false";
                    else currentState = "true";
                    setData.put(hName, currentState);
                    editor.putString(pref_hName, currentState);
                    arrangeButtonStyle(currentState, hBtn);
                    editor.commit();

                    db.collection("users").document(userID).collection("events").document(currentDate).set(setData, SetOptions.merge());
                }
            });


            hBtn.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    finish();
                    startActivity(new Intent(HomeScreen.this, AddHabit.class));
                    return false;
                }
            });



            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            habitHolder.addView(hBtn, params);
        }
    }
}
