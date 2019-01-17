package com.ad_astra.maja.adastra;


import android.content.Context;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class HabitInfo {
    public String name;
    public String desc;
    public int goal;
    public String trigger;
    public String replacement;

    public Context context;

    public int week = 0;
    public int done = 0;
    public String startDay;
    public boolean skipped = false;

    public HabitInfo() {};

    public HabitInfo(Context context, String hName, String hDesc, int hGoal, String hTrigger, String hReplacement, String sDay) {
        context = context;
        name = hName;
        desc = hDesc;
        goal = hGoal;
        trigger = hTrigger;
        replacement = hReplacement;
        startDay = sDay;
    }

    public void saveToFile(String fileName) {
        try {
            FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(this);
            os.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public HabitInfo loadFromFile(String fileName) {
        HabitInfo habitInfo = new HabitInfo();
        try {
            FileInputStream fis = context.openFileInput(fileName);
            ObjectInputStream is = new ObjectInputStream(fis);
            habitInfo = (HabitInfo) is.readObject();
            is.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return habitInfo;
    }
}
