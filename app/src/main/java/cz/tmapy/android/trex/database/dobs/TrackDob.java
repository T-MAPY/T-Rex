package cz.tmapy.android.trex.database.dobs;

/**
 * Created by kasvo on 15.9.2015.
 */
public class TrackDob {
    private Long id;
    private Long startTime;
    private Double firstLat;
    private Double firstLon;
    private String firstAddress;
    private Long finishTime;
    private Double lastLat;
    private Double lastLon;
    private String lastAddress;
    private Float distance;
    private Float maxSpeed;
    private Float aveSpeed;
    private Double minAlt;
    private Double maxAlt;
    private Double elevDiffUp;
    private Double elevDiffDown;
    private String note;
    private Long updateTime;

    public TrackDob(){};

    public TrackDob(Long startTime, Double firstLat, Double firstLon, String firstAddress, Long finishTime, Double lastLat, Double lastLon, String lastAddress, Float distance, Float maxSpeed, Float aveSpeed, Double minAlt, Double maxAlt, Double elevDiffUp, Double elevDiffDown, String note) {
        this.startTime = startTime;
        this.firstLat = firstLat;
        this.firstLon = firstLon;
        this.firstAddress = firstAddress;
        this.finishTime = finishTime;
        this.lastLat = lastLat;
        this.lastLon = lastLon;
        this.lastAddress = lastAddress;
        this.distance = distance;
        this.maxSpeed = maxSpeed;
        this.aveSpeed = aveSpeed;
        this.minAlt = minAlt;
        this.maxAlt = maxAlt;
        this.elevDiffUp = elevDiffUp;
        this.note = note;
        this.elevDiffDown = elevDiffDown;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Double getFirstLat() {
        return firstLat;
    }

    public void setFirstLat(Double firstLat) {
        this.firstLat = firstLat;
    }

    public Double getFirstLon() {
        return firstLon;
    }

    public void setFirstLon(Double firstLon) {
        this.firstLon = firstLon;
    }

    public String getFirstAddress() {
        return firstAddress;
    }

    public void setFirstAddress(String firstAddress) {
        this.firstAddress = firstAddress;
    }

    public Long getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Long finishTime) {
        this.finishTime = finishTime;
    }

    public Double getLastLat() {
        return lastLat;
    }

    public void setLastLat(Double finishLat) {
        this.lastLat = finishLat;
    }

    public Double getLastLon() {
        return lastLon;
    }

    public void setLastLon(Double lastLon) {
        this.lastLon = lastLon;
    }

    public String getLastAddress() {
        return lastAddress;
    }

    public void setLastAddress(String lastAddress) {
        this.lastAddress = lastAddress;
    }

    public Float getDistance() {
        return distance;
    }

    public void setDistance(Float distance) {
        this.distance = distance;
    }

    public Float getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(Float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public Float getAveSpeed() {
        return aveSpeed;
    }

    public void setAveSpeed(Float aveSpeed) {
        this.aveSpeed = aveSpeed;
    }

    public Double getMinAlt() {
        return minAlt;
    }

    public void setMinAlt(Double minAlt) {
        this.minAlt = minAlt;
    }

    public Double getMaxAlt() {
        return maxAlt;
    }

    public void setMaxAlt(Double maxAlt) {
        this.maxAlt = maxAlt;
    }

    public Double getElevDiffUp() {
        return elevDiffUp;
    }

    public void setElevDiffUp(Double elevDiffUp) {
        this.elevDiffUp = elevDiffUp;
    }

    public Double getElevDiffDown() {
        return elevDiffDown;
    }

    public void setElevDiffDown(Double elevDiffDown) {
        this.elevDiffDown = elevDiffDown;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }
}
