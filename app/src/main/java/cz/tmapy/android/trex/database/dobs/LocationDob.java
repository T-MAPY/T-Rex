package cz.tmapy.android.trex.database.dobs;

import android.location.Location;
import android.view.animation.PathInterpolator;

/**
 * Created by kasvo on 8.9.2015.
 */
public class LocationDob {
    private Long id;
    private Long gpsTime; //miliseconds
    private Double lat;
    private Double lon;
    private Double alt;
    private Float speed;
    private Float bearing;
    private Float accuracy;
    private String address;
    private Float distanceToStart;
    private Float bearingToStart;
    private Long duration;
    private String serverResponse;
    private Long updateTime;

    public LocationDob(){};

    public LocationDob(Location location) {
        this.gpsTime = location.getTime();
        this.lat = location.getLatitude();
        this.lon = location.getLongitude();
        this.alt = location.getAltitude();
        this.speed = location.getSpeed();
        this.bearing = location.getBearing();
        this.accuracy = location.getAccuracy();
    }

    public LocationDob(Long id, Long gpsTime, Double lat, Double lon, Double alt, Float speed, Float bearing, Float accuracy, String address, Float distanceToStart, Float bearingToStart, Long duration, String serverResponse, Long updateTime) {
        this.id = id;
        this.gpsTime = gpsTime;
        this.lat = lat;
        this.lon = lon;
        this.alt = alt;
        this.speed = speed;
        this.bearing = bearing;
        this.accuracy = accuracy;

        this.address = address;
        this.distanceToStart = distanceToStart;
        this.bearingToStart = bearingToStart;
        this.duration = duration;

        this.serverResponse = serverResponse;
        this.updateTime = updateTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGpsTime() {
        return gpsTime;
    }

    public void setGpsTime(Long gpsTime) {
        this.gpsTime = gpsTime;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public Double getAlt() {
        return alt;
    }

    public void setAlt(Double alt) {
        this.alt = alt;
    }

    public Float getSpeed() {
        return speed;
    }

    public void setSpeed(Float speed) {
        this.speed = speed;
    }

    public Float getBearing() {
        return bearing;
    }

    public void setBearing(Float bearing) {
        this.bearing = bearing;
    }

    public Float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Float accuracy) {
        this.accuracy = accuracy;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Float getDistanceToStart() {
        return distanceToStart;
    }

    public void setDistanceToStart(Float distanceToStart) {
        this.distanceToStart = distanceToStart;
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

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }
}
