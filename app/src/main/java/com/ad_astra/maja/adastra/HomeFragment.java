package com.ad_astra.maja.adastra;


import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.text.Layout;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

/* 207: u intentu poslat nes da naglasis da treba ucitat stare podatke i updateat samo blabla -> ime navike ne smin minjat!
 * Bodove dobivam svaki put kad nešto napravim!! i to npr 25*(a+b)
 * */

public class HomeFragment extends Fragment {

    private final String TAG = "HOME FRAGMENT";

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
    FirebaseStorage storage;
    StorageReference storageReference;

    Map<String, HabitInfo> habitsData = new HashMap<>();
    Set<String> toDo = new HashSet<>();
    Set<String> curState;
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
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        db = FirebaseFirestore.getInstance();
        userID = mAuth.getCurrentUser().getUid();

        setRealtimeUpdates();

        //Shared Preferences: used for accessing today's habit data quickly
        context = getContext();
        addHabit = new AddHabit();
        sharedPref = context.getSharedPreferences(userID, Context.MODE_PRIVATE);

        curState = sharedPref.getStringSet(activeWeekDay, new HashSet<String>());

        /*editor = sharedPref.edit();
        editor.clear();
        editor.apply();*/

        dayManager = (TabLayout) homeFragment.findViewById(R.id.HF_week);

        addHabitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), AddHabit.class);
                String message = "add habit";
                intent.putExtra(EXTRA_MESSAGE, message);
                startActivity(intent);
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
        for (Map.Entry<String, HabitInfo> hp : habitsData.entrySet()) {
            uploadDataOnline(hp.getKey());
        }
    }

    public void uploadDataOnline(String hName) {
        try {
            db.collection("users").document(userID).collection("habits").document(hName).set(habitsData.get(hName), SetOptions.merge());
        } catch (Exception e) {
            Log.d(TAG, "Failed to upload data online");
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
        getHabitData();
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
                                initializeNewDay();
                            }
                        }
                    }
                });
    }

    private void arrangeButtonStyle(View btn, String state) {
        LayerDrawable layerList = (LayerDrawable) btn.getBackground();

        Objects.requireNonNull(layerList).mutate();
        layerList.findDrawableByLayerId(R.id.habitButtonDone).setAlpha(0);
        layerList.findDrawableByLayerId(R.id.habitButtonSkipped).setAlpha(0);

        switch (state) {
            case "true":
                layerList.findDrawableByLayerId(R.id.habitButtonDone).setAlpha(255);
                break;
            case "false":
                break;
            case "skipped":
                layerList.findDrawableByLayerId(R.id.habitButtonSkipped).setAlpha(255);
                break;
            case "completed":
                break;
        }

        layerList.invalidateSelf();
        btn.setBackground(layerList);
    }

    private void hButtonChangeState(View v) {
        String hName = (String) v.getTag();
        Map<String, Object> setData = new HashMap<>();
        HabitInfo hInfo = habitsData.get(hName);

        String state = "false";

        if (curState.contains("T"+hName)) {
            curState.remove("T"+hName);
            curState.add("F"+hName);
            state = "false";
            hInfo.done--;
            if (toDo.contains(hName)) habitsDone--;
        } else if (curState.contains("F"+hName)) {
            curState.remove("F"+hName);
            curState.add("T"+hName);
            state = "true";
            hInfo.done++;
            if (toDo.contains(hName)) habitsDone++;
        }

        updateUpperText(hName, habitHolder.indexOfChild(v));
        updateProgressBar();
        arrangeButtonStyle(v, state);

        editor = sharedPref.edit();
        editor.putStringSet(activeWeekDay, curState);
        editor.apply();
        setData.put(hName, state);
        try {
            db.collection("users").document(userID).collection("events").document(activeWeekDate).set(setData, SetOptions.merge());
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
    }

    private void hButtonSkip(View v) {
        try {
            String hName = v.getTag().toString();
            HabitInfo hInfo = habitsData.get(hName);
            uploadDataOnline(hInfo.name);

            habitsData.put(hName, hInfo);

            if (curState.contains("S"+hName)) {
                toDo.add(hName);

                curState.remove("S"+hName);
                curState.add("F"+hName);

                arrangeButtonStyle(v, "false");
            } else {
                toDo.remove(hName);

                curState.remove("F"+hName);
                curState.add("S"+hName);

                arrangeButtonStyle(v, "skipped");
            }
            updateProgressBar();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hButtonEditInfo(View v) {
        Intent intent = new Intent(getContext(), AddHabit.class);
        String message = v.getTag().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    public LayerDrawable circleBtn(Drawable drawMe, boolean isText) {
        LayerDrawable layerList = (LayerDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.habit_btn, null);
        Objects.requireNonNull(layerList).mutate();
        if (isText) layerList.setLayerGravity(0, Gravity.CENTER);
        layerList.findDrawableByLayerId(R.id.habitButtonDone).setAlpha(0);
        layerList.findDrawableByLayerId(R.id.habitButtonSkipped).setAlpha(0);
        layerList.setDrawable(0, drawMe);
        layerList.invalidateSelf();

        return layerList;
    }

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
                                                            Log.d(TAG, "Parameter is not defined");
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
                                        db.collection("users").document(userID).collection("habits").document(hInfo.name).set(hInfo, SetOptions.merge());
                                    } //Otherwise it's still the same week

                                    habitsData.put(hInfo.name, hInfo);
                                } catch (Exception e) {
                                    Log.d(TAG, e.toString());
                                }
                            }
                            createHomeScreen();
                        } else {
                            Log.d(TAG, "Error reading document");
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
        uploadDataOnline(hName);
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

    private boolean needsToBeDone(String hName) {
        HabitInfo hInfo = habitsData.get(hName);
        if (curState.contains("S"+hName)) return false;
        else if(curState.contains("T"+hName) && hInfo.done > HabitPlan.plan[hInfo.goal-1][hInfo.week]) return false;
        else if(curState.contains("F"+hName) && hInfo.done >= HabitPlan.plan[hInfo.goal-1][hInfo.week]) return false;
        return true;
    }

    private void createHomeScreen() {
        int cnt = 0;
        habitsDone = 0;
        toDo = new HashSet<>();

        if (curState.isEmpty()) {
            for (final String hName : user.habitList)
                curState.add("F"+hName);
        }

        for (final String hName : user.habitList) {
            final String state;
            if (curState.contains("T"+hName))
                state = "true";
            else if (curState.contains("F"+hName))
                state = "false";
            else
                state = "skipped";

            final View btnView = habitHolder.getChildAt(2*cnt);
            btnView.setVisibility(View.VISIBLE);
            btnView.setTag(hName);

            if (!habitsData.isEmpty()) {
                updateUpperText(hName, 2*cnt);
                //If the habit is not skipped and finished for this week, add to to_do set
                if (needsToBeDone(hName)) {
                    if (state.equals("true")) habitsDone++;
                    toDo.add(hName);
                }

                try {
                    String imgUrl = habitsData.get(hName).imgUriS;
                    if (!imgUrl.isEmpty() && !imgUrl.equals("textImage")) {
                        StorageReference httpsReference = storage.getReferenceFromUrl(imgUrl);

                        final long ONE_MEGABYTE = 1024 * 1024;
                        httpsReference.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                            @Override
                            public void onSuccess(byte[] bytes) {
                                Drawable habitIcon = new BitmapDrawable(getResources(), BitmapFactory.decodeByteArray(bytes, 0, bytes.length));

                                try {
                                    LayerDrawable layerList = circleBtn(habitIcon, false);
                                    btnView.setBackground(layerList);
                                    arrangeButtonStyle(btnView, state);
                                } catch (Exception e) {
                                    Log.d(TAG, "Habit icon display failed.");
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                Log.d(TAG, "Error loading habit icon.");
                            }
                        });
                    } else {
                        TextDrawable textDrawable = new TextDrawable(getContext());
                        textDrawable.addCustomStyle(hName);
                        LayerDrawable layerList = circleBtn(textDrawable, true);
                        btnView.setBackground(layerList);
                        arrangeButtonStyle(btnView, state);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Adding habit icon error.");
                }

                btnView.setOnTouchListener(new View.OnTouchListener() {
                    private GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onSingleTapConfirmed(MotionEvent e) {
                            if (!curState.contains("S"+hName))
                                hButtonChangeState(btnView);
                            return super.onSingleTapConfirmed(e);
                        }
                        @Override
                        public boolean onDoubleTap(MotionEvent e) {
                            if (!curState.contains("T"+hName))
                                hButtonSkip(btnView);
                            return super.onDoubleTap(e);
                        }
                        @Override
                        public void onLongPress(MotionEvent e) {
                            hButtonEditInfo(btnView);
                            super.onLongPress(e);
                        }
                    });

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        gestureDetector.onTouchEvent(event);
                        return true;
                    }
                });
            }

            habitHolder.getChildAt(2*cnt+1).setVisibility(View.VISIBLE);
            cnt++;
        }
        updateProgressBar();
    }

    private void updateHomeScreen() {
        curState = sharedPref.getStringSet(activeWeekDay, new HashSet<String>());
        int cnt = 0;
        for (final String hName : user.habitList) {
            final String state;
            if (curState.contains("T"+hName))
                state = "true";
            else if (curState.contains("F"+hName))
                state = "false";
            else {
                curState.add("S"+hName);
                state = "skipped";
            }

            final View btnView = habitHolder.getChildAt(2*cnt);

            arrangeButtonStyle(btnView, state);

            cnt++;
        }
        updateProgressBar();
    }
}
