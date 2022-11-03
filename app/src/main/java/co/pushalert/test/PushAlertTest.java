package co.pushalert.test;

import android.app.Application;
import android.os.Build;
import android.util.Log;

import co.pushalert.PASubscribe;
import co.pushalert.PushAlert;

public class PushAlertTest extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        //PushAlert Initialization
        PushAlert.enableDebug(true);
        PushAlert.init("wfvet5v6-2caef9-9ae0", getApplicationContext())
                .enableFirebaseEventReporting(true)
                .setOnSubscribeListener(new PASubscribe() {
                    @Override
                    public void onSubscribe(String s) {
                        Log.e("MyLogs", "PushAlert Subscribe ID - " + s);
                    }
                });

        if (Build.VERSION.SDK_INT >= 33){ //Permission required from Android 13 and above
            PushAlert.setOptInMode(PushAlert.PAOptInMode.TWO_STEP);
        }
        else{
            PushAlert.setOptInMode(PushAlert.PAOptInMode.AUTO);
        }
    }
}
