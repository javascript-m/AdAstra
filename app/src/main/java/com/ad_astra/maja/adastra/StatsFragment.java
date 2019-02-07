package com.ad_astra.maja.adastra;


import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 */


public class StatsFragment extends Fragment {

    private final String TAG = "STATS FRAGMENT";

    View statsFragment;
    GraphView graph;
    private BarGraphSeries<DataPoint> mSeries;

    final SimpleDateFormat df = new SimpleDateFormat("dd/MMM/yyyy");
    final int dayInSec = 24*60*60;

    final int count = 31;
    final DataPoint[] values = new DataPoint[count];

    FirebaseAuth mAuth;
    FirebaseFirestore db;
    String userID;

    TextView week, done, sDay;

    // HabitInfo objects for ALL habits
    Map<String, Object> habit_list = new HashMap<>();

    Calendar calendar;
    Date day, bDay, eDay; //For label generation
    long startDay, endDay, midDay; //For firestore query

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        statsFragment = inflater.inflate(R.layout.fragment_stats, container, false);

        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        graph = (GraphView) statsFragment.findViewById(R.id.SF_graph);

        week = (TextView) statsFragment.findViewById(R.id.SF_week);
        done = (TextView) statsFragment.findViewById(R.id.SF_done);
        sDay = (TextView) statsFragment.findViewById(R.id.SF_sDay);

        chartOptions();
        return statsFragment;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            try {
                generateData();
                getHabitsData();
            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }
        }
        else {
            return;
        }
    }

    private void bottomData() {
        Spinner chooser = statsFragment.findViewById(R.id.SF_choose);
        TextView test = statsFragment.findViewById(R.id.SF_title);

        final List<String> list = new ArrayList<String>();
        for (Map.Entry<String, Object> habit : habit_list.entrySet()) {
            list.add(habit.getKey());
        }
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(Objects.requireNonNull(getContext()), android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chooser.setAdapter(dataAdapter);

        chooser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String hName = list.get(position);

                try {
                    HabitInfo habitInfo = (HabitInfo) habit_list.get(hName);

                    String weekTxt = "Current week: " + Integer.toString(habitInfo.week +1);
                    String doneTxt = "Done this week: " + Integer.toString(habitInfo.done);
                    String sDayTxt = "Start day: " + getStartDay(habitInfo.startDay);

                    week.setText(weekTxt);
                    done.setText(doneTxt);
                    sDay.setText(sDayTxt);
                } catch (Exception e) {
                    Log.d(TAG, e.toString());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {}
        });


    }

    private void chartOptions() {
        // Date 'n' time stuff
        calendar = Calendar.getInstance();
        eDay = calendar.getTime();
        calendar.add(Calendar.DATE, -count);
        bDay = calendar.getTime();
        day = bDay;

        endDay = (int) AddHabit.getMidnight(0);
        startDay = (int) AddHabit.getMidnight(-count);

        // set manual x bounds to have nice steps
        graph.getViewport().setMinX(bDay.getTime());
        graph.getViewport().setMaxX(eDay.getTime());

        // set chart lables
        graph.getGridLabelRenderer().setNumVerticalLabels(5);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getGridLabelRenderer().setHumanRounding(true);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setYAxisBoundsManual(false);

        // set x axis title
        String xTitle = df.format(bDay) + " - " + df.format(eDay);
        graph.getGridLabelRenderer().setHorizontalAxisTitle(xTitle);
    }

    private void drawChart() {
        mSeries = new BarGraphSeries<>(values);
        graph.addSeries(mSeries);

        // styles and colors
        mSeries.setColor(Color.parseColor("#001465"));
    }

    //Get the data for specific habit
    public void generateData() {
        graph.removeAllSeries();
        chartOptions();

        //Initialization
        for (int i=0; i<count; i++) {
            DataPoint v = new DataPoint(day, 0);
            values[i] = v;

            calendar.add(Calendar.DATE, 1);
            day = calendar.getTime();
        }

        CollectionReference colRef = db.collection("users").document(userID).collection("events");
        Query query = colRef.whereGreaterThan("dateInt", startDay).whereLessThanOrEqualTo("dateInt", endDay);

        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    Date x;
                    long y;
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        try {
                            y = (long) document.getData().get("EXP");
                            midDay = (long) document.getData().get("dateInt");
                        } catch (Exception e) {
                            continue;
                        }
                        if (y == 0) continue;

                        x = new Date(midDay*1000);
                        DataPoint v = new DataPoint(x, (double) y);
                        int pos = (int) (midDay-startDay)/dayInSec - 1;
                        values[pos] = v;
                    }
                    drawChart();
                }
            }
        });


    }

    private void getHabitsData() {
        db.collection("users").document(userID).collection("habits").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                HabitInfo habitInfo = document.toObject(HabitInfo.class);
                                habit_list.put(habitInfo.name, habitInfo);
                            }
                            bottomData();
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    private String getStartDay(long d) {
        Date date = new Date(d*1000);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int dayPos = c.get(Calendar.DAY_OF_WEEK);

        HomeFragment homeFragment = new HomeFragment();
        int pos = homeFragment.getCurrentDay(dayPos);

        return HabitPlan.full_week_days[pos];
    }
}
