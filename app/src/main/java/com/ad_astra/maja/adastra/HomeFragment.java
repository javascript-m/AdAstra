package com.ad_astra.maja.adastra;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

/* Bodove dobivam svaki put kad nešto napravim!! i to npr 25*(a+b)
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
    String lvl, exp;

    TextView progressText, lvlTxt, expTxt;
    ImageView profilePic;
    ProgressBar progressBar;
    LinearLayout habitHolder, progressHolder;
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
        cal.setTime(cDay);
        cal.add(Calendar.DATE, -(getDayIndex(todaysWeekDay)-getDayIndex(activeWeekDay)));
        activeWeekDate = df.format(cal.getTime());

        //Get all necessary views
        progressBar = (ProgressBar) homeFragment.findViewById(R.id.HF_circBar);
        habitHolder = (LinearLayout) homeFragment.findViewById(R.id.HF_habitHolder);
        progressHolder = (LinearLayout) homeFragment.findViewById(R.id.HF_progressHolder);
        addHabitBtn = (Button) homeFragment.findViewById(R.id.HF_add);
        progressText = (TextView) homeFragment.findViewById(R.id.HF_progressTxt);
        dayManager = (TabLayout) homeFragment.findViewById(R.id.HF_week);
        lvlTxt = (TextView) homeFragment.findViewById(R.id.HF_lvl);
        expTxt = (TextView) homeFragment.findViewById(R.id.HF_exp);
        profilePic = (ImageView) homeFragment.findViewById(R.id.HF_profilePic);

        //Initialize Authentication and Firestore
        user = new User();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        db = FirebaseFirestore.getInstance();
        userID = mAuth.getCurrentUser().getUid();

        //Shared Preferences: used for accessing today's habit data quickly
        context = getContext();
        addHabit = new AddHabit();
        sharedPref = context.getSharedPreferences(userID, Context.MODE_PRIVATE);
        curState = sharedPref.getStringSet(activeWeekDay, new HashSet<String>());

        addHabitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), AddHabit.class);
                String message = "add habit";
                intent.putExtra(EXTRA_MESSAGE, message);
                startActivity(intent);
            }
        });

        /*editor = sharedPref.edit();
        editor.clear().apply();*/

        setRealtimeUpdates();

        dayManager.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() > getDayIndex(todaysWeekDay)) {
                    Objects.requireNonNull(dayManager.getTabAt(getDayIndex(todaysWeekDay))).select();
                    return;
                }

                //For old active week day
                uploadDataOnline(activeWeekDay, activeWeekDate);
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
    public void onPause() {
        super.onPause();
        uploadDataOnline(activeWeekDay, activeWeekDate);
    }

    @Override
    public void onStop() {
        super.onStop();
        uploadDataOnline(activeWeekDay, activeWeekDate);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uploadDataOnline(activeWeekDay, activeWeekDate);
    }

    @Override
    public void onStart() {
        updateHomeScreen();
        super.onStart();
    }

    public void uploadDataOnline(String AWDY, String AWDT) {
        //Events - todaysExp
        int tExp = (int) sharedPref.getInt(AWDY+"EXP", 0);
        Map <String, Object> setData = new HashMap<>();
        setData.put("EXP", tExp);
        db.collection("users").document(userID).collection("events").document(AWDT).set(setData,SetOptions.merge());

        //Update habits data
        for (String hName : user.habitList) {
            try {
                int done = sharedPref.getInt(hName+"D", 0);
                habitsData.get(hName).done = done;
                db.collection("users").document(userID).collection("habits").document(hName).set(habitsData.get(hName), SetOptions.merge());
            } catch (Exception e) {
                Log.d(TAG, "Failed to upload data online");
            }
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
        curState = sharedPref.getStringSet(activeWeekDay, new HashSet<String>());
        if (!lastUpdate.equals(todaysDate)) {
            Toast.makeText(getActivity(), "Updated", Toast.LENGTH_LONG).show();

            editor = sharedPref.edit();
            Set<String> state = new HashSet<String>();
            for (String hName : user.habitList) state.add("F"+hName);

            try {
                editor.remove("lastUpdateDate").apply();
            } catch (Exception e) {
                Log.d(TAG, "Last update date doesn't exist.");
            }

            try {
                editor.remove(todaysWeekDay).apply();
            } catch (Exception e) {
                Log.d(TAG, "No data from the last day.");
            }

            editor.putString("lastUpdateDate", todaysDate);
            editor.putStringSet(todaysWeekDay, state);
            editor.apply();
        }
        getHabitData();
    }

    RequestOptions glideOptions = new RequestOptions().centerCrop();

    //Real-time updates listener (checks if database state has changed)
    public void setRealtimeUpdates() {
        final FirebaseUser fbUser = mAuth.getCurrentUser();
        if (fbUser != null) {
            if (fbUser.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(fbUser.getPhotoUrl())
                        .apply(glideOptions)
                        .into(profilePic);
            }
        }

        db.collection("users").document(userID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            user = task.getResult().toObject(User.class);
                            if (user != null) {
                                lvl = "Lvl. #" + Integer.toString(user.lvl);
                                exp = Integer.toString(user.exp) + "/" + Integer.toString(user.lvl * 50);
                                lvlTxt.setText(lvl);
                                expTxt.setText(exp);
                                initializeNewDay();
                            }
                        }
                    }
                });
    }

    private void addToSharedPref (String hName, boolean stateChanged, int done) {
        editor = sharedPref.edit();
        try {
            editor.remove(activeWeekDay).apply();
            editor.putStringSet(activeWeekDay, curState);
            editor.apply();
        } catch (Exception e) {
            Log.d(TAG, "No data for last "+activeWeekDay);
            editor.putStringSet(activeWeekDay, curState);
            editor.apply();
        }
        if (stateChanged) {
            try {
                editor.remove(hName+"D").apply();
                editor.putInt(hName+"D", done).apply();
            } catch (Exception e) {
                editor.putInt(hName+"D", 0).apply();
            }
        }
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

    private void manageExp(String state, String hName) {
        HabitInfo hInfo = habitsData.get(hName);
        final int newExp;
        if (state.equals("true")) {
            newExp = 20;
        } else {
            newExp = -20;
        }
        user.exp += newExp;

        if (user.exp >= user.lvl * 50) {
            user.exp -= user.lvl * 50;
            user.lvl++;
        }
        else if (user.exp < 0) {
            user.lvl--;
            user.exp = user.lvl * 50 + user.exp;
        }
        lvl = "Lvl. #" + Integer.toString(user.lvl);
        exp = Integer.toString(user.exp) + "/" + Integer.toString(user.lvl * 50);
        lvlTxt.setText(lvl);
        expTxt.setText(exp);

        Map<String, Object> setData = new HashMap<>();
        setData.put("exp", user.exp);
        setData.put("lvl", user.lvl);

        int tExp = (int) sharedPref.getInt(activeWeekDay+"EXP", 0);
        tExp += newExp;

        editor = sharedPref.edit();
        editor.remove(activeWeekDay+"EXP").apply();
        editor.putInt(activeWeekDay+"EXP", tExp).apply();

        try {
            db.collection("users").document(userID).set(setData, SetOptions.merge());
        } catch (Exception e) {
            Toast.makeText(getContext(), e.toString(), Toast.LENGTH_SHORT).show();
        }
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
        //Add/remove points
        manageExp(state, hName);
        addToSharedPref(hName, true, hInfo.done);

        updateUpperText(hName, habitHolder.indexOfChild(v));
        updateProgressBar();
        arrangeButtonStyle(v, state);

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
            addToSharedPref(hName, false, 0);
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
        int hDone = sharedPref.getInt(hName+"D", 0);
        final TextView txt = (TextView) progressHolder.getChildAt(pos);
        final String weekProgress = hDone+"/"+HabitPlan.plan[habitsData.get(hName).goal-1][habitsData.get(hName).week];
        txt.setText(weekProgress);
        progressHolder.getChildAt(pos+1).setVisibility(View.VISIBLE);
    }

    private void updateProgressBar() {
        int value = 0;
        if (toDo.size() != 0) value = habitsDone * 100/toDo.size();

        if(user.habitList.isEmpty()) value = 0;
        else if (toDo.size() == habitsDone || value > 100) value = 100;
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

        progressText.append("\nCHS: "+curState.toString());

        if (curState.isEmpty()) {
            for (final String hName : user.habitList)
                curState.add("F"+hName);
        }

        for (final String hName : user.habitList) {
            final String state;
            if (curState.contains("T"+hName))
                state = "true";
            else if (curState.contains("S"+hName))
                state = "skipped";
            else {
                curState.add("F" + hName);
                state = "false";
            }

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
        progressText.append("\n"+curState.toString());
        int cnt = 0;
        for (final String hName : user.habitList) {
            final String state;
            if (curState.contains("T"+hName))
                state = "true";
            else if (curState.contains("S"+hName))
                state = "skipped";
            else {
                curState.add("F"+hName);
                state = "false";
            }

            final View btnView = habitHolder.getChildAt(2*cnt);
            arrangeButtonStyle(btnView, state);
            cnt++;
        }
        updateProgressBar();
    }
}
