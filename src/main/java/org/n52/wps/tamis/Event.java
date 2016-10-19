package org.n52.wps.tamis;

import org.joda.time.DateTime;

public class Event {

    private String date;
    private String time;
    private String value;
    private String flag;
    private DateTime dateTime;
    
    public String getDate() {
        if(date == null){
            breakUpDateTime();
        }
        return date;
    }
    public Event setDate(String date) {
        this.date = date;
        return this;
    }
    public String getTime() {
        if(time == null){
            breakUpDateTime();
        }
        return time;
    }
    public Event setTime(String time) {
        this.time = time;
        return this;
    }
    public String getValue() {
        return value;
    }
    public Event setValue(String value) {
        this.value = value;
        return this;
    }
    public String getFlag() {
        return flag;
    }
    public Event setFlag(String flag) {
        this.flag = flag;
        return this;
    }
    public DateTime getDateTime() {
        return dateTime;
    }
    public Event setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
        return this;
    }
    
    private void breakUpDateTime(){
        if(dateTime != null){
            date = dateTime.toString(TimeUtils.DATE_FORMATTER);
            time = dateTime.toString(TimeUtils.TIME_FORMATTER);
        }
    }
}
