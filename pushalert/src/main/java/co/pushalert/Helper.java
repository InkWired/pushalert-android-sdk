package co.pushalert;

import static co.pushalert.PushAlert.appId;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;

import com.google.firebase.FirebaseOptions;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

/**
 * Helper Class
 * @author Mohit Kuldeep
 * @since 02-11-2022
 */
class Helper {
    static String PREFERENCE_NAME = "_PA_PREFERENCE_";
    static String PREFERENCE_LAST_NOTIFICATION_RECEIVED = "PA_LAST_NOTIFICATION_RECEIVED";
    static String PREFERENCE_LAST_NOTIFICATION_CLICKED = "PA_LAST_NOTIFICATION_CLICKED";
    static String EVENT_NOTIFICATION_RECEIVED = "pa_notification_received";
    static String EVENT_NOTIFICATION_CLICKED = "pa_notification_clicked";
    static String EVENT_NOTIFICATION_IMPACT = "pa_notification_impact";

    static final long impact_time = 120000L;

    static String getPostDataString(JSONObject params) throws Exception {

        StringBuilder result = new StringBuilder();
        boolean first = true;

        Iterator<String> itr = params.keys();

        while(itr.hasNext()){

            String key= itr.next();
            Object value = params.get(key);

            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(key, "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(value.toString(), "UTF-8"));

        }
        return result.toString();
    }

    static String connectWithPushAlert(String url, JSONObject finalParams, boolean authorization){
        try {
            String finalData = getPostDataString(finalParams);

            byte[] finalParamsByte;
            finalParamsByte = finalData.getBytes(StandardCharsets.UTF_8);

            URL urlObj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            if(authorization){
                conn.setRequestProperty("Authorization",  "pushalert_id="+ appId);
            }
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("charset", "utf-8");
            conn.setRequestProperty("Content-Length", Integer.toString(finalParamsByte.length));

            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);


            DataOutputStream dataOutputStream = new DataOutputStream(conn.getOutputStream());
            dataOutputStream.write(finalParamsByte);

            dataOutputStream.flush();
            dataOutputStream.close();

            int responseCode=conn.getResponseCode();
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                StringBuffer result = new StringBuffer();
                String line;
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                String encrypted_result = result.toString();
                if (encrypted_result.compareToIgnoreCase("")==0) {
                    return  null;
                }
                else {
                    return result.toString();
                }
            }
            else {
                return null;
            }

        }
        catch (Exception e) {
            LogM.e("Not able to connect with PushAlert: " + e.getMessage());
        }
        return null;
    }

    static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = null;
        if (connectivityManager != null) {
            activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        }
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    static String processJSONAttributes(String msg, JSONObject attributes){


        Pattern pattern = Pattern.compile("\\{\\{(.*?)\\}\\}");
        Matcher matcher = pattern.matcher(msg);

        while (matcher.find()) {
            if(attributes.has(matcher.group(1))){
                try {
                    msg = msg.replace(matcher.group(), attributes.getString(matcher.group(1)));
                } catch (Exception e) {
                    LogM.e("Not able to process JSON Attributes: " + e.getMessage() );
                    return "-1";
                }
            }
            else {
                return "-1";
            }
        }

        return  msg;
    }

    /**
     * To process attributes in a given string
     */
    static String processAttributes(Context context, String msg, String msg_no_attr){
        if(msg==null || msg.equals("") || msg.compareToIgnoreCase("")==0){
            return msg_no_attr;
        }

        SharedPreferences sharedPrefs = Helper.getSharedPreferences(context);

        Pattern pattern;
        pattern = Pattern.compile("\\{\\{(.*?)\\}\\}");
        Matcher matcher = pattern.matcher(msg);
        boolean completed = true;

        while (matcher.find()) {
            String replacer = sharedPrefs.getString("pa_attr_"+matcher.group(1), null);
            if(replacer!=null) {
                msg = msg.replace(matcher.group(), replacer);
            }
            else{
                completed = false;
                break;
            }
        }

        if(completed) {
            return msg;
        }
        else{
            return  msg_no_attr;
        }
    }

    static Map<String, String> checkAndConvert2Attributes(Context context, Map<String, String> attr_text, Map<String, String> no_attr_text){
        Map<String, String> map = new HashMap<>();
        boolean full_completed = true;
        SharedPreferences sharedPrefs = Helper.getSharedPreferences(context);
        Pattern pattern;
        pattern = Pattern.compile("\\{\\{(.*?)\\}\\}");

        for (Map.Entry<String,String> entry : attr_text.entrySet()){
            System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
            String msg = attr_text.get(entry.getKey());
            String msg_no_attr = no_attr_text.get(entry.getKey());

            if(msg==null || msg.equals("") || msg.compareToIgnoreCase("")==0){
                map.put(entry.getKey(), msg_no_attr);
                continue;
            }

            Matcher matcher = pattern.matcher(msg);
            boolean completed = true;

            while (matcher.find()) {
                String replacer = sharedPrefs.getString("pa_attr_"+matcher.group(1), null);
                if(replacer!=null) {
                    msg = msg.replace(matcher.group(), replacer);
                }
                else{
                    completed = false;
                    break;
                }
            }

            if(completed) {
                map.put(entry.getKey(), msg);
            }
            else{
                full_completed = false;
                break;
            }
        }

        if(!full_completed){
            map.put("no_attr", "1");
            return no_attr_text;
        }
        else{
            return map;
        }
    }

    static void firebaseAnalyticsLogEvent(Context context, String event, String notificiation_id, String campaign){
        if(getPreference(context, PushAlert.ENABLE_FIREBASE_ANALYTICS, false)){
            try {
                FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);

                Bundle bundle = new Bundle();
                bundle.putString("notification_id", notificiation_id);
                bundle.putString("campaign", campaign);
                bundle.putString("source", "PushAlert");
                bundle.putString("medium", "notification");
                mFirebaseAnalytics.logEvent(event, bundle);
            }
            catch (Exception e){
                LogM.e("Error while logging firebase event: " + e.getMessage());
            }
        }
    }

    static boolean isAppInBackground(Context context) {
        boolean isInBackground = true;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String activeProcess : processInfo.pkgList) {
                        if (activeProcess.equals(context.getPackageName())) {
                            isInBackground = false;
                        }
                    }
                }
            }
        } else {
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            if (componentInfo.getPackageName().equals(context.getPackageName())) {
                isInBackground = false;
            }
        }

        return isInBackground;
    }

    static void saveLastReceivedNotificationInfo(Context context, String notification_id, String campaign){

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("notification_id", notification_id);
            jsonObject.put("campaign", campaign);
            jsonObject.put("time", System.currentTimeMillis());
            setPreference(context, PREFERENCE_LAST_NOTIFICATION_RECEIVED, jsonObject.toString());

        } catch (Exception e) {
            LogM.e("Error while saving last received notification info: " + e.getMessage());
        }

    }

    static JSONObject getLastReceivedNotificationInfo(Context context){

        try {
            String last_notification_info = getPreference(context, PREFERENCE_LAST_NOTIFICATION_RECEIVED, null);
            if(last_notification_info!=null) {
                return new JSONObject(last_notification_info);
            }
            else{
                return null;
            }

        } catch (Exception e) {
            LogM.e("Error while getting last received notification info: " + e.getMessage());
        }

        return  null;
    }

    static void saveLastClickedNotificationInfo(Context context, String notification_id, String campaign){

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("notification_id", notification_id);
            jsonObject.put("campaign", campaign);
            jsonObject.put("time", System.currentTimeMillis());
            setPreference(context, PREFERENCE_LAST_NOTIFICATION_CLICKED, jsonObject.toString());

        } catch (Exception e) {
            LogM.e("Error while saving last received notification info: " + e.getMessage());
        }

    }

    static JSONObject getLastClickedNotificationInfo(Context context){

        try {
            String last_notification_info = getPreference(context, PREFERENCE_LAST_NOTIFICATION_CLICKED, null);
            if(last_notification_info!=null) {
                return new JSONObject(last_notification_info);
            }
            else{
                return null;
            }

        } catch (Exception e) {
            LogM.e("Error while getting last received notification info: " + e.getMessage());
        }

        return  null;
    }

    static void removeLastClickedNotificationInfo(Context context){
        removePreference(context, PREFERENCE_LAST_NOTIFICATION_CLICKED);
    }

    static void checkForNotificationImpact(Context context){
        if(getPreference(context, PushAlert.ENABLE_FIREBASE_ANALYTICS, false)) {
            try {
                JSONObject lastNotificationInfo = getLastReceivedNotificationInfo(context);
                if (lastNotificationInfo != null) {
                    if (lastNotificationInfo.getLong("time") >= System.currentTimeMillis() - impact_time) {
                        Helper.firebaseAnalyticsLogEvent(context, Helper.EVENT_NOTIFICATION_IMPACT, lastNotificationInfo.getString("notification_id"), lastNotificationInfo.getString("campaign"));
                    }
                    //removePreference(context, PREFERENCE_LAST_NOTIFICATION_RECEIVED);
                }
            } catch (Exception e) {
                LogM.e("Error while check for notification impact: " + e.getMessage());
            }
        }
    }

    //Preference Management
    static SharedPreferences getSharedPreferences(Context context){
        String[] preference_postfix;
        String appId = getAppId(context);
        if(appId!=null) {
            preference_postfix = appId.split("-");
        }
        else{
            preference_postfix = new String[1];
            preference_postfix[0] = "";
            LogM.i("App ID not specified");
        }

        return context.getSharedPreferences(PREFERENCE_NAME+preference_postfix[0], Context.MODE_PRIVATE);
    }

    static int getPreference(Context context, String key, int default_value){
        SharedPreferences prefs = getSharedPreferences(context);
        return prefs.getInt(key, default_value);
    }

    static String getPreference(Context context, String key, String default_value){
        SharedPreferences prefs = getSharedPreferences(context);
        return prefs.getString(key, default_value);
    }

    static boolean getPreference(Context context, String key, boolean default_value){
        SharedPreferences prefs = getSharedPreferences(context);
        return prefs.getBoolean(key, default_value);
    }

    static void setPreference(Context context, String key, int value){
        SharedPreferences prefs = getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    static void setPreference(Context context, String key, String value){
        SharedPreferences prefs = getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    static void setPreference(Context context, String key, boolean value){
        SharedPreferences prefs = getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    static void removePreference(Context context, String key){
        SharedPreferences prefs = getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(key);
        editor.apply();
    }

    static void saveAppId(Context context, String appId){
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PushAlert.APP_ID_PREF, appId);
        editor.apply();
    }

    static String getAppId(Context context){
        if(appId!=null){
            return appId;
        }
        else {
            SharedPreferences prefs = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
            return prefs.getString(PushAlert.APP_ID_PREF, null);
        }
    }

    static void processNotification(Context context, Map<String, String> map, boolean sendReceivedReport){
        boolean isAppInBg = Helper.isAppInBackground(context);
        //Client preference
        boolean showNotificationSettings = (isAppInBg || (PushAlert.mInAppBehaviour == PushAlert.PAInAppBehaviour.NOTIFICATION));

        PANotification notification = new PANotification(
                map.get("id"), map.get("short_title"), map.get("content"), map.get("url"), map.get("icon"), map.get("image"), map.get("sent_time"),
                map.get("channel"), map.get("sound_res"), map.get("led_color"), map.get("lock_screen_visibility"), map.get("accent_color"),
                map.get("small_icon_res"), map.get("group_key"), map.get("group_id"), map.get("local_notf_id"), map.get("priority"),
                map.get("action1_title"), map.get("action1_url"), map.get("action1_icon_res"), map.get("action1_id"),
                map.get("action2_title"), map.get("action2_url"), map.get("action2_icon_res"), map.get("action2_id"),
                map.get("action3_title"), map.get("action3_url"), map.get("action3_icon_res"), map.get("action3_id"),
                map.get("short_title_attr"), map.get("content_attr"), map.get("url_attr"),
                map.get("action1_title_attr"), map.get("action1_url_attr"),
                map.get("action2_title_attr"), map.get("action2_url_attr"),
                map.get("action3_title_attr"), map.get("action3_url_attr"),
                map.get("type"), map.get("extraData"), map.get("ref_id"),
                map.get("template_id"), map.get("template"), map.get("header_text")
        );

        String bgData = null;
        if(map.containsKey("background_data")){
            bgData = map.get("background_data");
        }
        boolean isBgDataNotification = (bgData!=null && bgData.equalsIgnoreCase("1"));

        PushAlert.removeAlertPrefs(context, map.get("ls_id"));

        NotificationReceiver notificationReceiver = PushAlert.getNotificationReceiver();
        boolean showNotification = true;
        if(notificationReceiver!=null){
            notificationReceiver.putPANotification(notification);
            showNotification = !notificationReceiver.notificationReceived();
            notification = notificationReceiver.getPANotification();
        }

        if(showNotification && !isBgDataNotification && showNotificationSettings){
            MessageNotification.notifyInit(context, notification);
        }

        String nid = null;
        if(map.containsKey("id")){
            nid = map.get("id");
        }
        if(nid!=null && nid.compareTo("-1")!=0) {
            if(sendReceivedReport) {
                MessageNotification.sendReceivedReport(context, notification);
            }
            //FirebaseAnalytics
            Helper.firebaseAnalyticsLogEvent(context, Helper.EVENT_NOTIFICATION_RECEIVED, map.get("id"), map.containsKey("template_id") ? (map.get("template") + " (" + map.get("template_id") + ")") : "None");
            Helper.saveLastReceivedNotificationInfo(context, map.get("id"), map.containsKey("template_id") ? (map.get("template") + " (" + map.get("template_id") + ")") : "None");
        }
    }

    static Map<String, String> toMap(JSONObject jsonObj)  throws JSONException {
        Map<String, String> map = new HashMap<>();
        Iterator<String> keys = jsonObj.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            String value = jsonObj.getString(key);
            map.put(key, value);
        }
        return map;
    }

    static void openAppNativeNotificationSettings(final Activity context) {
        if (context == null) {
            return;
        }
        final Intent i = new Intent();
        i.setAction("android.settings.APP_NOTIFICATION_SETTINGS");

        //Android 5 to 7
        i.putExtra("app_package", context.getPackageName());
        i.putExtra("app_uid", context.getApplicationInfo().uid);

        //Android 8 and above
        i.putExtra("android.provider.extra.APP_PACKAGE", context.getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(i);
    }

    @SuppressLint("AnnotateVersionCheck")
    static boolean isAndroid13AndAbove(){
        return (Build.VERSION.SDK_INT >= 33);
    }

    static void setSubscriptionStatus(Context context, int status){
        setPreference(context, PushAlert.SUBSCRIPTION_STATUS_PREF, status);
    }

    static int getSubscriptionStatus(Context context){
        return getPreference(context, PushAlert.SUBSCRIPTION_STATUS_PREF, PushAlert.PA_SUBS_STATUS_DEFAULT);
    }

    static String getFBAPIKey(Context context) {
        try {
            String api_key = FirebaseOptions.fromResource(context).getApiKey();
            if (api_key != null) {
                return api_key;
            }
        }
        catch (Exception e){
            e.printStackTrace();
            return "";
        }
        return "";
    }

    static String getFBAppID(Context context) {
        try {
            String app_id = FirebaseOptions.fromResource(context).getApplicationId();
            if (app_id!=null) {
                return app_id;
            }
        }
        catch (Exception e){
            return "";
        }
        return "";
    }

    static String getFBProjectID(Context context){
        try {
            String project_id = FirebaseOptions.fromResource(context).getProjectId();
            if(project_id!=null) {
                return project_id;
            }
        }
        catch (Exception e) {
            return "";
        }
        return "";
    }

}
