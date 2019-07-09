package study.hskim.whereru.model;

import java.util.HashMap;

public class LocationAuth {

    private String id;
    public HashMap<String, Boolean> denyLocationList = new HashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
