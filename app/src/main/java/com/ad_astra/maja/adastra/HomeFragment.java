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
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.time.Period;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/*
 * initalize new day()
 * 207: u intentu poslat nes da naglasis da treba ucitat stare podatke i updateat samo blabla -> ime navike ne smin minjat!
 *
 * Bodove dobivam svaki put kad nešto napravim!! i to npr 25*(a+b)
 *
 * */

public class HomeFragment extends Fragment {

    User user;
    String userID;
    String activeWeekDate;
    String todaysDate;
    String activeWeekDay; //Date I'm currently modifying
    String todaysWeekDay;
    ArrayList<String> weekDays = new ArrayList<>();

    TextView progressText;
    ProgressBar progressBar;
    LinearLayout habitHolder;
    LinearLayout progressHolder;
    Button addHabitBtn;
    TabLayout dayManager;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    Map<String, HabitInfo> habitsData = new HashMap<>();
    Set<String> toDo = new HashSet<>();
    int habitsDone = 0;
    long dayTime = 86400; // 24 * 60 * 60

    Context context;
    AddHabit addHabit;
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
        progressBar = (ProgressBar) homeFragment.findViewById(R.id.HF_circBar);
        habitHolder = (LinearLayout) homeFragment.findViewById(R.id.HF_habitHolder);
        progressHolder = (LinearLayout) homeFragment.findViewById(R.id.HF_progressHolder);
        addHabitBtn = (Button) homeFragment.findViewById(R.id.HF_add);
        progressText = (TextView) homeFragment.findViewById(R.id.HF_progressTxt);

        //Initialize Authentication and Firestore
        user = new User();
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userID = mAuth.getCurrentUser().getUid();

        //Shared Preferences: used for accessing today's habit data quickly
        context = getContext();
        addHabit = new AddHabit();
        sharedPref = context.getSharedPreferences(userID, Context.MODE_PRIVATE);

        getHabitData();
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
                if (tab.getPosition() > getDayIndex(todaysWeekDay)) {
                    Objects.requireNonNull(dayManager.getTabAt(getDayIndex(todaysWeekDay))).select();
                    return;
                }

                activeWeekDay = weekDays.get(tab.getPosition());
                cal.setTime(cDay);
                cal.add(Calendar.DATE, -(getDayIndex(todaysWeekDay)-getDayIndex(activeWeekDay)));
                activeWeekDate = df.format(cal.getTime());

                Map<String, Object> dateInt = new HashMap<>();
                dateInt.put("dateInt", addHabit.getMidnight(getDayIndex(activeWeekDay)-getDayIndex(todaysWeekDay)));

                db.collection("users").document(userID).collection("events").document(activeWeekDate).set(dateInt, SetOptions.merge());

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        uploadDataOnline();
    }

    public void uploadDataOnline() {
        for (Map.Entry<String, HabitInfo> hp : habitsData.entrySet()) {
            db.collection("users").document(userID).collection("habits").document(hp.getKey()).set(hp.getValue(), SetOptions.merge());
        }
    }

    public static String getWeekDay(int day) {
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
            HabitInfo hInfo = habitsData.get(hName);

            String newVal;
            Set<String> state = new HashSet<>(sharedPref.getStringSet(activeWeekDay, new HashSet<String>()));

            if (state.contains("T" + hName)) {
                state.remove("T" + hName);
                state.add("F" + hName);
                newVal = "false";
                habitsDone--;
                hInfo.done--;
            } else {
                if (state.contains("F" + hName)) state.remove("F" + hName);
                state.add("T" + hName);
                newVal = "true";
                habitsDone++;
                hInfo.done++;
            }

            updateUpperText(hName, habitHolder.indexOfChild(v));

            if (toDo.contains(hName)) updateProgressBar();

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

    private void getHabitData() {
        db.collection("users").document(userID).collection("habits")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                final HabitInfo hInfo = document.toObject(HabitInfo.class);
                                try {
                                    final long endDay = (hInfo.startDay + 7*dayTime);
                                    long toDay = addHabit.getMidnight(0);

                                    if (toDay >= endDay && toDay-endDay < 7*dayTime) { //New week in habit has started
                                        CollectionReference colRef = db.collection("users").document(userID).collection("events");
                                        Query query = colRef.whereGreaterThanOrEqualTo("dateInt", hInfo.startDay).whereLessThan("dateInt", endDay);

                                        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    int tasksDone = 0;
                                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                                        try {
                                                            if (document.get(hInfo.name).equals("true")) {
                                                                tasksDone++;
                                                            }
                                                        } catch (Exception e) {
                                                            Log.d("HOME FRAGMENT", "Parameter is not defined");
                                                        }
                                                    }

                                                    //User has finished last week's plan
                                                    if (tasksDone >= HabitPlan.plan[hInfo.goal-1][hInfo.week]) {
                                                        hInfo.week += 1;
                                                        hInfo.done -= tasksDone;
                                                        hInfo.skipped = false;
                                                        hInfo.startDay = endDay;

                                                        if (hInfo.week >= 10) {
                                                            //User has mastered the habit
                                                        }

                                                        db.collection("users").document(userID).collection("habits").document(hInfo.name).set(hInfo, SetOptions.merge());
                                                    }
                                                }
                                            }
                                        });
                                    } else if (toDay-endDay > 7*dayTime) { //User has skipped a week or more
                                        if (hInfo.week > 0)
                                            hInfo.week--;
                                        hInfo.done = 0;
                                        hInfo.startDay = toDay;
                                        hInfo.skipped = false;
                                        db.collection("users").document(userID).collection("habits").document(hInfo.name).set(hInfo, SetOptions.merge());
                                    } //Otherwise it's still the same week

                                    habitsData.put(hInfo.name, hInfo);
                                    updateHomeScreen();
                                } catch (Exception e) {
                                    Log.d("HOME FRAGMENT", e.toString());
                                }
                            }
                        } else {
                            Log.d("HOME FRAGMENT", "Error reading document");
                        }
                    }
                });
    }

    private void updateUpperText(String hName, int pos) {
        HabitInfo tmp = habitsData.get(hName);
        if (tmp != null) {
            final TextView txt = (TextView) progressHolder.getChildAt(pos);
            final String weekProgress = tmp.done+"/"+HabitPlan.plan[tmp.goal-1][tmp.week];
            txt.setText(weekProgress);
            txt.setTag(hName+"txt");

            txt.setVisibility(View.VISIBLE);
            progressHolder.getChildAt(pos+1).setVisibility(View.VISIBLE);
        };
        uploadDataOnline();
    }

    private void updateProgressBar() {
        int value = 0;
        if (toDo.size() != 0) value = habitsDone * 100/toDo.size();
        if (toDo.size() == habitsDone || value > 100) value = 100;
        else if (habitsDone == 0 || value < 0) value = 0;

        String valueTxt = value + "%";

        progressBar.setProgress(value);
        progressText.setText(valueTxt);
    }

    private boolean needsToBeDone(String hName, String isDone) {
        HabitInfo hInfo = habitsData.get(hName);
        if (hInfo.skipped) return false;
        else if(isDone.equals("true") && hInfo.done > HabitPlan.plan[hInfo.goal-1][hInfo.week]) return false;
        else if(isDone.equals("false") && hInfo.done >= HabitPlan.plan[hInfo.goal-1][hInfo.week]) return false;
        return true;
    }

    private void updateHomeScreen() {
        Set<String> curState = sharedPref.getStringSet(activeWeekDay, new HashSet<String>());
        int cnt = 0;
        habitsDone = 0;
        toDo = new HashSet<>();
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

            if (!habitsData.isEmpty()) {
                updateUpperText(hName, 2*cnt);
                //If the habit is not skipped and finished for this week, add to to_do set
                if (needsToBeDone(hName, isDone)) {
                    if (isDone.equals("true")) habitsDone++;
                    toDo.add(hName);
                }
                v.setOnClickListener(btnListener);
                //v.setOnLongClickListener();
            }

            if (isDone.equals("true")) v.setBackgroundResource(R.drawable.habit_btn_done);
            else v.setBackgroundResource(R.drawable.habit_btn);

            habitHolder.getChildAt(2*cnt+1).setVisibility(View.VISIBLE);
            cnt++;
        }
        //updateProgressBar();
    }
}
