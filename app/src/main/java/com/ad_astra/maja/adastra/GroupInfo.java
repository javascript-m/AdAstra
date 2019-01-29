package com.ad_astra.maja.adastra;

import java.util.ArrayList;

public class GroupInfo {
    public String name;
    public String admin;
    public String adminID;
    public String imgUrl;
    public String groupID;

    public GroupInfo() {}

    public GroupInfo(String n, String a, String aID, String url, String gID) {
        this.name = n;
        this.admin = a;
        this.adminID = aID;
        this.imgUrl = url;
        this.groupID = gID;
    }
}
