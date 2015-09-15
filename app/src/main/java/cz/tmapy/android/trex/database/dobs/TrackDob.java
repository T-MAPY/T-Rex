package cz.tmapy.android.trex.database.dobs;

/**
 * Created by kasvo on 15.9.2015.
 */
public class TrackDob {
    private Long id;
    private Long startTime;
    private Double startLat;
    private Double startLon;
    private String startAddress;
    private Long finishTime;
    private Double finishLat;
    private Double finishLon;
    private String finishAddress;
    private Float distance;
    private Float maxSpeed;
    private Float aveSpeed;
    private Double minAlt;
    private Double maxAlt;
    private Float elevDiffUp;
    private Float elevDiffDown;
    private String note;
    private Long updateTime;

    public TrackDob(){};

    public TrackDob(Long startTime, Double startLat, Double startLon, String startAddress, Long finishTime, Double finishLat, Double finishLon, String finishAddress, Float distance, Float maxSpeed, Float aveSpeed, Double minAlt, Double maxAlt, Float elevDiffUp, Float elevDiffDown, String note) {
        this.startTime = startTime;
        this.startLat = startLat;
        this.startLon = startLon;
        this.startAddress = startAddress;
        this.finishTime = finishTime;
        this.finishLat = finishLat;
        this.finishLon = finishLon;
        this.finishAddress = finishAddress;
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

    public Double getStartLat() {
        return startLat;
    }

    public void setStartLat(Double startLat) {
        this.startLat = startLat;
    }

    public Double getStartLon() {
        return startLon;
    }

    public void setStartLon(Double startLon) {
        this.startLon = startLon;
    }

    public String getStartAddress() {
        return startAddress;
    }

    public void setStartAddress(String startAddress) {
        this.startAddress = startAddress;
    }

    public Long getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Long finishTime) {
        this.finishTime = finishTime;
    }

    public Double getFinishLat() {
        return finishLat;
    }

    public void setFinishLat(Double finishLat) {
        this.finishLat = finishLat;
    }

    public Double getFinishLon() {
        return finishLon;
    }

    public void setFinishLon(Double finishLon) {
        this.finishLon = finishLon;
    }

    public String getFinishAddress() {
        return finishAddress;
    }

    public void setFinishAddress(String finishAddress) {
        this.finishAddress = finishAddress;
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

    public Float getElevDiffUp() {
        return elevDiffUp;
    }

    public void setElevDiffUp(Float elevDiffUp) {
        this.elevDiffUp = elevDiffUp;
    }

    public Float getElevDiffDown() {
        return elevDiffDown;
    }

    public void setElevDiffDown(Float elevDiffDown) {
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
