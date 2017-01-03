package jp.hmproject.ams_service;

import java.util.Date;

/**
 * Created by hm on 1/2/2017.
 */
public class AMS_Data {
    protected long aid;
    protected int flag;
    protected int event;
    protected float accuracy;
    protected double latitude;
    protected double longitude;
    protected Date generation;
    protected Date registration;
    protected String user;
    protected long lid;
    protected long sid;
    protected long oid;
    protected String remarks;
    protected int twopeople;
    protected int dailysubcon;
    protected Date sending;
    protected Date revise;

    public AMS_Data() {
    }

    public void setTraceData(float accuracy,double latitude, double longitude,
                             Date generation, Date registration) {
        this.flag = 0;
        this.event = 4;
        this.accuracy = accuracy;
        this.latitude = latitude;
        this.longitude = longitude;
        this.generation = generation;
        this.registration = registration;
    }
}
