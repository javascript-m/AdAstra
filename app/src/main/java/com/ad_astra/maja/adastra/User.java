package com.ad_astra.maja.adastra;

import java.util.ArrayList;

public class User {
    public int lvl;
    public int exp;
    public int pDays;

    public String username;
    public String imgUrl;

    public ArrayList<String> habitList = new ArrayList<String>();

    public User() {}

    public User(int uLvl, int uExp, int uPdays, String usname, String url) {
        lvl = uLvl;
        exp = uExp;
        pDays = uPdays;
        username = usname;
        imgUrl = url;
    }
}
