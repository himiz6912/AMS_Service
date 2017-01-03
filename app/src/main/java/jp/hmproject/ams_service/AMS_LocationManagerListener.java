package jp.hmproject.ams_service;

import java.util.EventListener;

/**
 * Created by hm on 1/2/2017.
 */
public interface AMS_LocationManagerListener extends EventListener {
    public void changeLocationData();
    public void locationServiceConnected();
}
