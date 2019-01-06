package com.ad_astra.maja.adastra;

public class HabitInfo {
    public String hName;
    public String hDesc;
    public String imgUrl;

    public int par1;
    public int par2;
    public int par3;

    public int PRIORITY = 1;

    public HabitInfo() {};

    public HabitInfo(String n, String d, String url, int p1, int p2, int p3) {
        hName = n;
        hDesc = d;
        imgUrl = url;
        par1 = p1;
        par2 = p2;
        par3 = p3;
    }

    public int calculatePriority(int p1, int p2, int p3) {
        PRIORITY = p1 + p2 + p3;
        return PRIORITY;
    }
}
