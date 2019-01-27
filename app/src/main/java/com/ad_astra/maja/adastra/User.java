package com.ad_astra.maja.adastra;

import java.util.ArrayList;

public class User {
    public int lvl;
    public int exp;
    public int pDays;

    public ArrayList<String> habitList = new ArrayList<String>();

    public User() {}

    public User(int uLvl, int uExp, int uPdays) {
        lvl = uLvl;
        exp = uExp;
        pDays = uPdays;
    }
}
