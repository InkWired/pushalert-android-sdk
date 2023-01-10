package co.pushalert;

import static co.pushalert.MessageNotification.NOTIFICATION_TAG;
import static co.pushalert.PushAlert.SUBSCRIBER_ID_PREF;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * To handle notification click
 * @author Mohit Kuldeep
 * @since 02-11-2022
 */

public class NotificationHandler extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        processIntent(this, getIntent());
    }

    public void processIntent(Context context, Intent intent){
        int notificationID = intent.getIntExtra("notification_id", 0);
        int group_id = intent.getIntExtra("group_id", 0);
        boolean clearGroup = false;
        if(intent.getBooleanExtra("is_action", false)){
            if (group_id!=0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    NotificationManager notificationManager = (NotificationManager) context
                            .getSystemService(Context.NOTIFICATION_SERVICE);

                    StatusBarNotification[] statusBarNotifications;
                    if (notificationManager != null) {
                        statusBarNotifications = notificationManager.getActiveNotifications();

                        String groupKey = null;

                        for (StatusBarNotification statusBarNotification : statusBarNotifications) {
                            if (notificationID == statusBarNotification.getId()) {
                                groupKey = statusBarNotification.getGroupKey();
                                break;
                            }
                        }

                        int counter = 0;
                        for (StatusBarNotification statusBarNotification : statusBarNotifications) {
                            if (statusBarNotification.getGroupKey().equals(groupKey)) {
                                counter++;
                            }
                        }

                        if (counter == 2) { //1 for the current notification and 1 for the group notification
                            clearGroup = true;
                        }
                    }
                    else{
                        LogM.i("Notification manager is null");
                    }
                }
                catch (Exception e){
                    LogM.e("Notification Handler error in counting: " + e.getMessage());
                }
            }

            clearNotification(context, notificationID);
            if(clearGroup){
                clearNotification(context, group_id);
            }
            //closeNotificationDrawer(context);
        }

        if(notificationID!=-1) {
            sendClickEvent(context, intent.getStringExtra("uid"), notificationID, intent.getIntExtra("clicked_on", 0), intent.getIntExtra("type", 0), intent.getIntExtra("eid", 0));

            //FirebaseAnalytics
            Helper.firebaseAnalyticsLogEvent(context, Helper.EVENT_NOTIFICATION_CLICKED, notificationID + "", intent.getStringExtra("campaign"));

            Helper.saveLastClickedNotificationInfo(context, notificationID+"", intent.getStringExtra("campaign"));
        }

        openUri(context, intent);
    }

    public void openUri(Context context, Intent intent){
        String url = intent.getStringExtra("url");


        NotificationOpener notificationOpener = PushAlert.getNotificationOpener();
        if(notificationOpener!=null){
            int notificationID = intent.getIntExtra("notification_id", 0);
            JSONObject extraData = new JSONObject();
            try{
                extraData = new JSONObject(intent.getStringExtra("extraData"));
            }
            catch (Exception ignored){

            }

            PANotificationOpened paNotificationOpened = new PANotificationOpened(notificationID, url, intent.getStringExtra("action_id"), extraData);
            notificationOpener.notificationOpened(paNotificationOpened);
        }
        else {
            try {
                Intent action_intent = new Intent(Intent.ACTION_VIEW);
                //action_intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP| Intent.FLAG_ACTIVITY_SINGLE_TOP);
                action_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK );
                action_intent.setData(Uri.parse(url));
                context.startActivity(action_intent);
            }
            catch (Exception e){
                LogM.e("Error while opening the notification uri: " + e.getMessage());
            }
        }
        finish();
    }

    public void sendClickEvent(Context context, String uid, int notificationID, int clicked_on, int type, int eid){
        Helper.connectWithPushAlert("get", new ConnectionHelper(){

            @Override
            public JSONObject getJSONParams() {
                return null;
            }

            @Override
            public String getUrl() {
                try{
                    SharedPreferences prefs = Helper.getSharedPreferences(context);

                    String[] pushalert_info = Helper.getAppId(context).split("-");
                    String raw_url = "https://androidapi.pushalert.co/trackClickedApp.php?" +
                            "uid=" + uid +
                            "&browser=" + "chrome" +
                            "&os=" + "android" +
                            "&osVer=" + Build.VERSION.RELEASE +
                            "&device=" + (context.getResources().getBoolean(R.bool.isTablet)?"tablet":"mobile") +
                            "&user_id=" + pushalert_info[1] +
                            "&domain_id=" + pushalert_info[2] +
                            "&host=" + pushalert_info[0] +
                            "&notification_id=" + notificationID +
                            "&clicked_on=" + clicked_on +
                            "&clicked_time=" + (int)(System.currentTimeMillis() / 1000L) +
                            "&type=" + type +
                            "&eid=" + eid +
                            "&subs_id=" + URLEncoder.encode(prefs.getString(SUBSCRIBER_ID_PREF, null), StandardCharsets.UTF_8.name()) +
                            "&http_user_agent=" + (System.getProperty("http.agent")!=null?URLEncoder.encode(System.getProperty("http.agent"), StandardCharsets.UTF_8.name()):"");

                    if (!Helper.isNetworkAvailable(context)) {
                        Helper.addPendingTask(context, "clickedReport", raw_url);
                        return null;
                    }

                    return raw_url;
                } catch (Exception e) {
                    LogM.e("Error in sending click report: " + e.getMessage());
                }

                return null;
            }

            @Override
            public void postResult(JSONObject reader) {

            }

            @Override
            public void onFailure(String message) {
                Helper.addPendingTask(context, "clickedReport", message);
            }
        },false);
    }

    public void clearNotification(Context context, int notificationID){
        NotificationManagerCompat nMgr = NotificationManagerCompat.from(context);
        nMgr.cancel(NOTIFICATION_TAG, notificationID);
    }

    /*public void closeNotificationDrawer(Context context){
        Intent closeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(closeIntent);
    }*/

    private static void closeNotificationDrawer(Context _context) {
        try {
            @SuppressLint("WrongConstant")
            Object sbservice = _context.getSystemService("statusbar");
            Class<?> statusbarManager;
            statusbarManager = Class.forName("android.app.StatusBarManager");
            Method showsb;
            showsb = statusbarManager.getMethod("collapsePanels");
            showsb.invoke(sbservice);
        } catch (Exception e) {
            LogM.e("Error while closing notification drawer: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
