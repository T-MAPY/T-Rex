package cz.tmapy.android.trex.service;

import android.location.Location;

/**
 * Created by kasvo on 11.9.2015.
 */
public class LocationWrapper {
    private Location location = null;
    private String address = null;
    private String serverResponse = null;

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getServerResponse() {
        return serverResponse;
    }

    public void setServerResponse(String serverResponse) {
        this.serverResponse = serverResponse;
    }
}
