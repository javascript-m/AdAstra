package com.ad_astra.maja.adastra;


import android.content.Context;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class HabitInfo {
    public String sName;
    public String sDesc;
    public int iGoal;
    public String sTrigger;
    public String sReplacement;

    public Context context;
    //HABIT PLAN?

    public HabitInfo() {};

    public HabitInfo(Context context, String hName, String hDesc, int hGoal, String hTrigger, String hReplacement) {
        context = context;
        sName = hName;
        sDesc = hDesc;
        iGoal = hGoal;
        sTrigger = hTrigger;
        sReplacement = hReplacement;
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
