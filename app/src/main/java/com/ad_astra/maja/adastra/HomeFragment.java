package com.ad_astra.maja.adastra;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Optional.of;

/* uredi gumb i kod if/elsa sredi parametre u objektu koji se dodaje
 * VAZNO: offline mode
 * update online stuff when app closes?
 * line 100
 * 207: u intentu poslat nes da naglasis da treba ucitat stare podatke i updateat samo blabla -> ime navike ne smin minjat!
 * */

public class HomeFragment extends Fragment {

    User user;
    String userID;
    String currentDate;

    TextView prText;
    LinearLayout habitHolder;
    Button addHabitBtn;
    LinearLayout.LayoutParams btnParams;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    Context context;
    Set<String> hList;
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
        addHabitBtn = (Button) homeFragment.findViewById(R.id.HF_add);
        prText = (TextView) homeFragment.findViewById(R.id.HF_progressTxt);

        //Initialize Authentication and Firestore
        user = new User();
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userID = mAuth.getCurrentUser().getUid();

        //Shared Preferences: used for accessing today's habit data quickly
        context = getContext();
        sharedPref = context.getSharedPreferences(userID, Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        setRealtimeUpdates();

        //Get date for event filename
        Date cDay = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
        currentDate = df.format(cDay);

        addHabitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), AddHabit.class));
            }
        });

        // Inflate the layout for this fragment
        return homeFragment;
    }

    private void updateHomeScreen() {
        hList = (Set<String>) sharedPref.getStringSet("habitList", new HashSet<String>());

        //In case user changes phone or reinstalls the app
        if (hList.isEmpty()) hList.addAll(user.habitList);

        StringBuilder rez = new StringBuilder();
        for (String i : hList) {
            rez.append(i);
        }
        if(!rez.toString().equals("")) prText.setText(rez.toString());
        else {
            prText.setText("nema niceg");
        }

        //Loop through sharedPrefList and add habits
        int cnt = 0;
        for (final String hName : hList) {
            final String isDone = sharedPref.getString(hName, "false");

            final View v = habitHolder.getChildAt(2*cnt);
            v.setVisibility(View.VISIBLE);

            if (isDone.equals("true")) v.setBackgroundResource(R.drawable.habit_btn_done);
            else v.setBackgroundResource(R.drawable.habit_btn);

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Map<String, Object> setData = new HashMap<>();
                    String curState = sharedPref.getString(hName, "false");

                    if (curState.equals("true")) curState = "false";
                    else curState = "true";

                    if (curState.equals("true")) v.setBackgroundResource(R.drawable.habit_btn_done);
                    else v.setBackgroundResource(R.drawable.habit_btn);

                    setData.put(hName, curState);
                    editor.putString(hName, curState);
                    editor.commit();

                    db.collection("users").document(userID).collection("events").document(currentDate).set(setData, SetOptions.merge());
                }
            });

            v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    //startActivity(new Intent(getContext(), AddHabit.class));
                    return false;
                }
            });
            habitHolder.getChildAt(2*cnt+1).setVisibility(View.VISIBLE);
            cnt++;
        }
    }

}
