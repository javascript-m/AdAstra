package com.ad_astra.maja.adastra;

import java.util.HashMap;
import java.util.Map;

public class hEvent {
    public Map<String, String> eventData = new HashMap<String, String>();

    public hEvent() {};

    public hEvent(Map<String, String> mapca) {
        eventData = mapca;
    }
}
