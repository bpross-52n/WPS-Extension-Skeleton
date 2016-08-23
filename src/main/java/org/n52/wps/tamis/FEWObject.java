package org.n52.wps.tamis;

import java.util.List;

public class FEWObject {

    private String type;
    
    private String locationId;

    private String parameterId;

    private String timeStepUnit;

    private String timeStepMultiplier;

    private String startDate;

    private String startTime;

    private String endDate;

    private String endTime;

    private String missVal;

    private String stationName = "Bevertalsperre";

    private String units;

    private List<Event> eventList;

    public FEWObject(){
        
    }

    public String getType() {
        return type;
    }

    public FEWObject setType(String type) {
        this.type = type;
        return this;
    }

    public String getLocationId() {
        return locationId;
    }

    public FEWObject setLocationId(String locationId) {
        this.locationId = locationId;
        return this;
    }

    public String getParameterId() {
        return parameterId;
    }

    public FEWObject setParameterId(String parameterId) {
        this.parameterId = parameterId;
        return this;
    }

    public String getTimeStepUnit() {
        return timeStepUnit;
    }

    public FEWObject setTimeStepUnit(String timeStepUnit) {
        this.timeStepUnit = timeStepUnit;
        return this;
    }

    public String getTimeStepMultiplier() {
        return timeStepMultiplier;
    }

    public FEWObject setTimeStepMultiplier(String timeStepMultiplier) {
        this.timeStepMultiplier = timeStepMultiplier;
        return this;
    }

    public String getStartDate() {
        return startDate;
    }

    public FEWObject setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public String getStartTime() {
        return startTime;
    }

    public FEWObject setStartTime(String startTime) {
        this.startTime = startTime;
        return this;
    }

    public String getEndDate() {
        return endDate;
    }

    public FEWObject setEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }

    public String getEndTime() {
        return endTime;
    }

    public FEWObject setEndTime(String endTime) {
        this.endTime = endTime;
        return this;
    }

    public String getMissVal() {
        return missVal;
    }

    public FEWObject setMissVal(String missVal) {
        this.missVal = missVal;
        return this;
    }

    public String getStationName() {
        return stationName;
    }

    public FEWObject setStationName(String stationName) {
        this.stationName = stationName;
        return this;
    }

    public String getUnits() {
        return units;
    }

    public FEWObject setUnits(String units) {
        this.units = units;
        return this;
    }

    public List<Event> getEventList() {
        return eventList;
    }

    public FEWObject setEventList(List<Event> eventList) {
        this.eventList = eventList;
        return this;
    }

    
    
}
