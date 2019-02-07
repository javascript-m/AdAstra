package com.ad_astra.maja.adastra;

public class AchievementInfo {
    String ID;
    String title;
    String description;
    String url;

    public AchievementInfo() {}

    public AchievementInfo(String cID, String cTitle, String cDesc, String cUrl) {
        ID = cID;
        title = cTitle;
        description = cDesc;
        url = cUrl;
    }
}
