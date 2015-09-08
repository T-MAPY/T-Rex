package cz.tmapy.android.trex.database.dobs;

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
    private String serverResponse;
    private Long updateTime;

    public LocationDob() {
    }

    ;

    public LocationDob(Long id, Long gpsTime, Double lat, Double lon, Double alt, Float speed, Float bearing, String serverResponse, Long updateTime) {
        this.id = id;
        this.gpsTime = gpsTime;
        this.lat = lat;
        this.lon = lon;
        this.alt = alt;
        this.speed = speed;
        this.bearing = bearing;
        this.serverResponse = serverResponse;
        this.updateTime = updateTime;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setGpsTime(Long gpsTime) {
        this.gpsTime = gpsTime;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public void setAlt(Double alt) {
        this.alt = alt;
    }

    public void setSpeed(Float speed) {
        this.speed = speed;
    }

    public void setBearing(Float bearing) {
        this.bearing = bearing;
    }

    public void setServerResponse(String serverResponse) {
        this.serverResponse = serverResponse;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    public Long getId() {
        return id;
    }

    public Long getGpsTime() {
        return gpsTime;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLon() {
        return lon;
    }

    public Double getAlt() {
        return alt;
    }

    public Float getSpeed() {
        return speed;
    }

    public Float getBearing() {
        return bearing;
    }

    public String getServerResponse() {
        return serverResponse;
    }

    public Long getUpdateTime() {
        return updateTime;
    }
}
