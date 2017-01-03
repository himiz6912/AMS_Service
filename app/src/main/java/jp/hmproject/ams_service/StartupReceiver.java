package jp.hmproject.ams_service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by hm on 12/25/2016.
 */
public class StartupReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent ams_service = new Intent(context,MainService.class);
        context.startService(ams_service);
    }
}
