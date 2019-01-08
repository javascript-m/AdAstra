package com.ad_astra.maja.adastra;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;

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

public class HomeFragment extends Fragment {

    User user;
    String userID;
    String currentDate;

    LinearLayout habitHolder;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    Context context;
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    public HomeFragment() {
        // Required empty public constructor
    }

    public void setRealtimeUpdates() {
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View homeFragment = inflater.inflate(R.layout.fragment_home, container, false);

        //Get all necessary views
        habitHolder = (LinearLayout) homeFragment.findViewById(R.id.HF_habitHolder);

        //Initialize Authentication and Firestore
        user = new User();
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userID = mAuth.getCurrentUser().getUid();

        //Shared Preferences: used for accessing today's habit data quickly
        context = getContext();
        sharedPref = context.getSharedPreferences(getString(R.string.my_habits), Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        setRealtimeUpdates();

        //Get date for event filename
        Date cDay = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
        currentDate = df.format(cDay);

        // Inflate the layout for this fragment
        return homeFragment;
    }

    private void arrangeButtonStyle(String val, Button btnID) {
        if (val.equals("true")) {
            btnID.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.habitDone));
        } else {
            btnID.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.habitUndone));
        }
    }

    private void updateHomeScreen() {
        habitHolder.removeAllViews();
        //Put habit views inside habit holder and set design
        for (int i=0; i<user.habitList.size(); i++) {
            final String hName = user.habitList.get(i);
            final String pref_hName = userID+"_"+hName;
            final Button hBtn = new Button(getContext()); //!!
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
                    //startActivity(new Intent(getContext(), AddHabit.class));
                    return false;
                }
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            habitHolder.addView(hBtn, params);
        }
    }

}
