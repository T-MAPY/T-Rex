package cz.tmapy.android.trex.service;

import android.location.Location;

/**
 * Created by kasvo on 11.9.2015.
 */
public class LocationWrapper {
    private Location location = null;
    private String address = "";
    private Float directDistanceToStart = 0f;
    private Float bearingToStart = 0f;
    private Float estimatedDistance = 0f;
    private Long duration = 0l;
    private String serverResponse = "";

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

    public Float getDirectDistanceToStart() {
        return directDistanceToStart;
    }

    public void setDirectDistanceToStart(Float directDistanceToStart) {
        this.directDistanceToStart = directDistanceToStart;
    }

    public Float getEstimatedDistance() {
        return estimatedDistance;
    }

    public void setEstimatedDistance(Float estimatedDistance) {
        this.estimatedDistance = estimatedDistance;
    }

    public Float getBearingToStart() {
        return bearingToStart;
    }

    public void setBearingToStart(Float bearingToStart) {
        this.bearingToStart = bearingToStart;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public String getServerResponse() {
        return serverResponse;
    }

    public void setServerResponse(String serverResponse) {
        this.serverResponse = serverResponse;
    }
}
