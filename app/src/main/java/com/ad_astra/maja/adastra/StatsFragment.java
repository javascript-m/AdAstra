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
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */


public class StatsFragment extends Fragment {

    private final String TAG = "STATS FRAGMENT";

    GraphView graph;

    final SimpleDateFormat df = new SimpleDateFormat("dd/MMM/yyyy");
    final int dayInSec = 24*60*60;

    final int count = 31;
    final DataPoint[] values = new DataPoint[count];

    private BarGraphSeries<DataPoint> mSeries;

    AddHabit addHabit;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    String userID;

    Calendar calendar;
    Date day, bDay, eDay; //For label generation
    long startDay, endDay, midDay; //For firestore query

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View statsFragment = inflater.inflate(R.layout.fragment_stats, container, false);

        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        graph = (GraphView) statsFragment.findViewById(R.id.SF_graph);

        chartOptions();

        //TODO: ACTIVATE CODE BELOW
        /*
        //get the spinner from the xml.
        Spinner dropdown = statsFragment.findViewById(R.id.spinner1);
        //create a list of items for the spinner.
        String[] items = new String[]{"1", "2", "three"};
        //create an adapter to describe how the items are displayed, adapters are used in several places in android.
        //There are multiple variations of this, but this is the basic variant.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        //set the spinners adapter to the previously created one.
        dropdown.setAdapter(adapter);
        */

        return statsFragment;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            try {
                generateData();
            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }
        }
        else {
            return;
        }
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
}
