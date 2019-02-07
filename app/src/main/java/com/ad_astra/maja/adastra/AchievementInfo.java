package com.ad_astra.maja.adastra;

public class AchievementInfo {
    String ID;
    String title;
    String description;
    String url;

    public static String[][] builtIn = new String[][]{
            { "Cassiopeia", "Cassiopeia", "Be active for 10 days in a row.", "" },
            { "Orion", "Orion", "Joined a group.", "https://firebasestorage.googleapis.com/v0/b/ad-astra-19408.appspot.com/o/achievements%2FOrion.jpg?alt=media&token=4c49de6d-43c4-40d8-b72b-53214ca75134" },
            { "Ursa Minor", "Ursa Minor", "Get five votes on a post.", "" }
    };

    public AchievementInfo() {}

    public AchievementInfo(String cID, String cTitle, String cDesc, String cUrl) {
        ID = cID;
        title = cTitle;
        description = cDesc;
        url = cUrl;
    }

    public AchievementInfo(String[] values) {
        ID = values[0];
        title = values[1];
        description = values[2];
        url = values[3];
    }
}
