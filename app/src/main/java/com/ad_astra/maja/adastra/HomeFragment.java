package com.ad_astra.maja.adastra;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.ThrowOnExtraProperties;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/* update online stuff when app closes?
osiguraj da nmg kliknut na datume poslije
 * line 100
 * 207: u intentu poslat nes da naglasis da treba ucitat stare podatke i updateat samo blabla -> ime navike ne smin minjat!
 * */

public class HomeFragment extends Fragment {

    User user;
    String userID;
    String activeWeekDate;
    String todaysDate;
    String activeWeekDay; //Date I'm currently modifying
    String todaysWeekDay;
    ArrayList<String> weekDays = new ArrayList<>();

    TextView prText;
    LinearLayout habitHolder;
    Button addHabitBtn;
    TabLayout dayManager;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    Context context;
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View homeFragment = inflater.inflate(R.layout.fragment_home, container, false);

        //Date and time stuff
        weekDays.add("MONDAY"); weekDays.add("TUESDAY"); weekDays.add("WEDNESDAY"); weekDays.add("THURSDAY");
        weekDays.add("FRIDAY"); weekDays.add("SATURDAY"); weekDays.add("SUNDAY");

        final Date cDay = Calendar.getInstance().getTime();
        final SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
        todaysDate = df.format(cDay);
        int TWD = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        todaysWeekDay = getWeekDay(TWD);
        activeWeekDay = todaysWeekDay;
        final Calendar cal = Calendar.getInstance();

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

        setRealtimeUpdates();

        dayManager = (TabLayout) homeFragment.findViewById(R.id.HF_week);

        addHabitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), AddHabit.class));
            }
        });

        dayManager.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                activeWeekDay = weekDays.get(tab.getPosition());
                cal.setTime(cDay);
                cal.add(Calendar.DATE, -(getDayIndex(todaysWeekDay)-getDayIndex(activeWeekDay)));
                activeWeekDate = df.format(cal.getTime());
                updateHomeScreen();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        dayManager.getTabAt(getDayIndex(todaysWeekDay)).select();

        // Inflate the layout for this fragment
        return homeFragment;
    }

    private String getWeekDay(int day) {
        switch (day) {
            case Calendar.MONDAY:
                return "MONDAY";
            case Calendar.TUESDAY:
                return "TUESDAY";
            case Calendar.WEDNESDAY:
                return "WEDNESDAY";
            case Calendar.THURSDAY:
                return "THURSDAY";
            case Calendar.FRIDAY:
                return "FRIDAY";
            case Calendar.SATURDAY:
                return "SATURDAY";
            case Calendar.SUNDAY:
                return "SUNDAY";
        }
        return null;
    }

    private int getDayIndex(String day) {
        for (int i=0; i<weekDays.size(); i++)
            if (weekDays.get(i).equals(day)) return i;
        return 0;
    }

    private void initializeNewDay() {
        String lastUpdate = sharedPref.getString("lastUpdateDate", todaysDate);
        if (!lastUpdate.equals(todaysDate)) {
            Toast.makeText(getActivity(), "Updated", Toast.LENGTH_LONG).show();
            editor = sharedPref.edit();
            //#hList = (Set<String>) sharedPref.getStringSet("habitList", new HashSet<String>());
            Set<String> state = new HashSet<String>();
            for (String hName : user.habitList) state.add("F"+hName);

            editor.putString("lastUpdateDate", todaysDate);
            editor.remove(todaysWeekDay);
            editor.putStringSet(todaysWeekDay, state);
            editor.apply();
        }
    }

    //Real-time updates listener (checks if database state has changed)
    public void setRealtimeUpdates() {
        db.collection("users").document(userID)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            return;
                        }
                        if (snapshot != null && snapshot.exists()) {
                            user = snapshot.toObject(User.class);
                            if (user != null) {
                                updateHomeScreen();
                                initializeNewDay();
                            }
                        }
                    }
                });
    }

    //Change state listener
    public View.OnClickListener btnListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String hName = (String) v.getTag();
            Map<String, Object> setData = new HashMap<>();

            String newVal;
            Set<String> state = new HashSet<>(sharedPref.getStringSet(activeWeekDay, new HashSet<String>()));

            if (state.contains("T" + hName)) {
                state.remove("T" + hName);
                state.add("F" + hName);
                newVal = "false";
            } else {
                if (state.contains("F" + hName)) state.remove("F" + hName);
                state.add("T" + hName);
                newVal = "true";
            }

            if (newVal.equals("true")) v.setBackgroundResource(R.drawable.habit_btn_done);
            else v.setBackgroundResource(R.drawable.habit_btn);

            editor = sharedPref.edit();
            editor.putStringSet(activeWeekDay, state);
            editor.apply();

            setData.put(hName, newVal);
            try {
                db.collection("users").document(userID).collection("events").document(activeWeekDate).set(setData, SetOptions.merge());
            } catch (Exception e) {
                Log.d("HOME FRAGMENT", e.toString());
            }
        }
    };

    private void updateHomeScreen() {
        //Add Habits
        int cnt = 0;
        Set<String> curState = sharedPref.getStringSet(activeWeekDay, new HashSet<String>());
        for (String hName : user.habitList) {
            final String isDone;
            if (curState.contains("T"+hName)) isDone = "true";
            else {
                curState.add("F"+hName);
                isDone = "false";
            }

            final View v = habitHolder.getChildAt(2*cnt);
            v.setVisibility(View.VISIBLE);
            v.setTag(hName);
            v.setOnClickListener(btnListener);
            //v.setOnLongClickListener();

            if (isDone.equals("true")) v.setBackgroundResource(R.drawable.habit_btn_done);
            else v.setBackgroundResource(R.drawable.habit_btn);

            habitHolder.getChildAt(2*cnt+1).setVisibility(View.VISIBLE);
            cnt++;
        }
    }
}
