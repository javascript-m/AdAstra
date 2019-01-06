package com.ad_astra.maja.adastra;

import java.util.ArrayList;

public class User {
    public String userID;
    public String fName;
    public String lName;
    public String bDate;

    public ArrayList<String> habitList = new ArrayList<String>();

    public User() {}

    public User(String uID, String fN, String lN, String bD) {
        userID = uID;
        fName = fN;
        lName = lN;
        bDate = bD;
    }
}
