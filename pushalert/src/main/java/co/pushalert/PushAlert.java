package co.pushalert;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * PushAlert main class to provide all functionality
 * @author Mohit Kuldeep
 * @since 02-11-2022
 */

@SuppressWarnings("unused")
public class PushAlert {
    private static boolean sendingSubsID = false;
    static final String APP_ID_PREF = "PA_APP_ID";
    static final String SUBSCRIBER_ID_PREF = "PA_SUBSCRIBER_ID";
    static final String SUBSCRIPTION_STATUS_PREF = "PA_SUBSCRIPTION_STATUS";
    static final String EXPLICITLY_PERMISSION_DENIED = "PA_EXPLICITLY_PERMISSION_DENIED";
    static final String NOTIFICATION_CHANNEL = "pa_fallback_channel";
    private static final String NOTIFICATION_CHANNEL_TITLE = "Miscellaneous";
    static final String ABANDONED_CART_DATA = "_pa_abandoned_cart";
    static final String PRODUCT_ALERT_DATA = "_pa_product_alert";
    private static final String APP_VERSION = "_pa_app_version";
    public static final String PA_DEFAULT_SMALL_ICON = "pa_default_small_icon";
    public static final String PA_DEFAULT_ACCENT_COLOR = "pa_default_accent_color";
    //public static final String PA_ALERT_ONLY_ONCE = "pa_alert_only_once";

    //private static final String USER_SUBSCRIPTION_STATE = "PA_USER_SUBSCRIPTION_STATE";
    private static final String USER_PRIVACY_CONSENT = "PA_USER_PRIVACY_CONSENT";
    private static final String USER_PRIVACY_CONSENT_REQUIRED = "PA_USER_PRIVACY_CONSENT_REQUIRED";
    private static final String APP_NOTIFICATION_PERMISSION_STATE = "PA_APP_NOTIFICATION_PERMISSION_STATE";
    public static final String ENABLE_FIREBASE_ANALYTICS = "PA_ENABLE_FIREBASE_ANALYTICS";

    @SuppressLint("StaticFieldLeak")
    private static PushAlert.InkWired instance;

    private static NotificationReceiver notificationReceiver = null;
    private static NotificationOpener notificationOpener = null;
    private static TwoStepHelper twoStepHelper = null;
    static String appId = null;
    private  static int open_time;
    static boolean paUnsubscribeWhenNotificationsAreDisabled = false;

    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    static  boolean tokenAlreadyGenerated = false;

    static  LocationCallback mLocationCallback;

    public enum PAInAppBehaviour{
        NONE, NOTIFICATION
    }

    public enum PAOptInMode{
        AUTO, TWO_STEP, MANUAL
    }

    public static int PA_SUBS_STATUS_SUBSCRIBED = 1;
    public static int PA_SUBS_STATUS_UNSUBSCRIBED = -1;
    public static int PA_SUBS_STATUS_DEFAULT = 0;
    public static int PA_SUBS_STATUS_DENIED = -2;

    static PAInAppBehaviour mInAppBehaviour = PAInAppBehaviour.NOTIFICATION;
    static PAOptInMode mOptInMode = PAOptInMode.AUTO;
    static PASubscribe mOnSubscribeListener = null;
    static FirebaseAnalytics mFirebaseAnalytics = null;

    public static boolean fireBaseInitialized =false;
    public static boolean showAskOnAndroid13Above = false;
    public static final int PUSHALERT_PERMISSION_REQUEST_POST_NOTIFICATIONS=1981;
    public static boolean resumedFromAppNotificationSettings = false;

    private PushAlert() {
    }

    /**
     * To initialize PushAlert
     * @param reqAppId  Your app id (get it from Dashboard->App)
     * @param context   Your application context
     */
    public static PushAlert.InkWired init(String reqAppId, final Context context) throws NullPointerException{

        PushAlert.setContext(context);
        PushAlert.setAppId(reqAppId);

        String pushalert_id = appId;
        if(pushalert_id==null){
            throw new NullPointerException("Invalid app id: " +  reqAppId);
        }
        else if(pushalert_id.compareToIgnoreCase("test-0-0")==0){
            throw new NullPointerException("PushAlert ID is not defined.");
        }

        //Default channel for oreo and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL,
                    NOTIFICATION_CHANNEL_TITLE, NotificationManager.IMPORTANCE_DEFAULT);
            final NotificationManager notificationManager=
                    (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        instance = new PushAlert.InkWired(context);
        return instance;
    }

    static void setContext(Context context) {
        mContext = context;
    }

    /**
     *
     * @param reqAppId String application id available at PushAlert Dashboard
     */
    static void setAppId(String reqAppId){
        if (reqAppId == null || reqAppId.isEmpty()) {
            LogM.e("setAppId called with invalid id.");
            return;
        }

        appId = reqAppId;
        Helper.saveAppId(mContext, appId);
    }

    /**
     * To set opt-in mode
     * @param optInMode {@link PAOptInMode}
     */
    public static void setOptInMode(PAOptInMode optInMode){
        PushAlert.mOptInMode = optInMode;
        if(optInMode == PAOptInMode.AUTO || optInMode == PAOptInMode.TWO_STEP){
            getInstance().build(false);
        }
        else if(optInMode == PAOptInMode.MANUAL){
            if(PushAlert.getSubscriberID()!=null){
                getInstance().build(false);
            }
        }
    }

    /**
     * To initiate push notification permission manually
     * @param direct if true one step (either permission or if disabled then notification settings page)
     * @return true if user is already subscribed
     */
    public static boolean requestForPushNotificationPermission(boolean direct){
        if(isUserSubscribed()){
            return true;
        }

        if(direct){
            if (PushAlert.getOSNotificationPermissionState(mContext)) {
                getInstance().build(true);
            }
            else{
                getInstance().requestPermissionActivity(PAOptInMode.MANUAL);
            }
        }
        else {
            if (mOptInMode == PAOptInMode.MANUAL) {
                mOptInMode = PAOptInMode.AUTO;
                getInstance().build(true);
                mOptInMode = PAOptInMode.MANUAL;
            } else {
                getInstance().build(true);
            }
        }

        return  false;
    }

    /**
     * @return PushAlert static instance
     */
    private static PushAlert.InkWired getInstance() throws NullPointerException {
        if(instance==null){
            throw new NullPointerException("PushAlert never initiated.");
        }
        return instance;
    }

    /**
     * To add attributes to the subscribers like name, age, user id, email etc
     * @param attributes Key-value pair of attributes
     */
    public static void  addAttributes(Map<String, String> attributes){
        new AddAttributesTask(PushAlert.mContext, attributes).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * To add subscriber to a segment
     * @param seg_id segment id
     */
    public static void  addUserToSegment(int seg_id){
        new AddToSegment(mContext, seg_id).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Remove subscriber from a segment
     * @param seg_id segment id
     */
    public static void  removeUserFromSegment(int seg_id){
        new RemoveFromSegment(mContext, seg_id).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /*/**
     * Set true if you would only like the sound, vibrate and ticker to be played if the notification is not already showing.
     * @param value boolean
     */
    /*public static void setOnlyAlertOnce(boolean value){
        SharedPreferences prefs = Helper.getSharedPreferences(mContext);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PA_ALERT_ONLY_ONCE, value);
        editor.apply();
    }*/


    static int getDefaultSmallIcon(Context context){
        return Helper.getPreference(context, PA_DEFAULT_SMALL_ICON, R.drawable.default_small_icon);
    }

    static int getDefaultAccentColor(Context context){
        return Helper.getPreference(context, PA_DEFAULT_ACCENT_COLOR, NotificationCompat.COLOR_DEFAULT);
    }

    static TwoStepHelper getTwoStepCustomization(){
        return PushAlert.twoStepHelper;
    }

    static void addRemoteAttributes(Context context, JSONObject attributes){
        try {
            SharedPreferences prefs = Helper.getSharedPreferences(context);
            SharedPreferences.Editor editor = prefs.edit();

            Iterator<?> keys = attributes.keys();
            while(keys.hasNext() ) {
                String key = (String)keys.next();
                editor.putString("pa_attr_"+key, attributes.getString(key));
            }

            editor.apply();
            LogM.i("Added remote attributes");
        } catch (Exception e) {
            LogM.e("Error while adding remote attribute: " + e.getMessage());
        }
    }

    static void createChannel(Context context, String channelID, String name, String desc, String importance, String ledColor, String lockScreenVisibility,
                                     String showBadge, String sound, String vibration_pattern, String group_id){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance_int = NotificationManager.IMPORTANCE_DEFAULT;
            if(importance.compareToIgnoreCase("urgent")==0){
                importance_int = NotificationManager.IMPORTANCE_HIGH;
            }
            else if(importance.compareToIgnoreCase("high")==0){
                //default
            }
            else if(importance.compareToIgnoreCase("medium")==0){
                importance_int = NotificationManager.IMPORTANCE_LOW;
            }
            else if(importance.compareToIgnoreCase("low")==0){
                importance_int = NotificationManager.IMPORTANCE_MIN;
            }

            NotificationChannel channel = new NotificationChannel(channelID,
                    name, importance_int);
            channel.setDescription(desc);

            if(ledColor.compareToIgnoreCase("off")==0){
                channel.enableLights(false);
            }
            else if(ledColor.compareToIgnoreCase("default")==0){
                //default
            }
            else if(ledColor.compareToIgnoreCase("")!=0){
                channel.enableLights(true);
                channel.setLightColor(Color.parseColor(ledColor));
            }

            if (lockScreenVisibility.equalsIgnoreCase("private")){
                channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            }
            else if (lockScreenVisibility.equalsIgnoreCase("secret")){
                channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            }
            else if (lockScreenVisibility.equalsIgnoreCase("public")){
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            }

            if(showBadge.compareToIgnoreCase("true")==0) {
                channel.setShowBadge(true);
            }
            else if(showBadge.compareToIgnoreCase("false")==0){
                channel.setShowBadge(false);
            }

            if(sound.compareToIgnoreCase("off")==0){
                channel.setSound(null, null);
            }
            else if(sound.compareToIgnoreCase("default")==0){
                //default
            }
            else if(sound.compareToIgnoreCase("")!=0){
                try {
                    int tmpSoundRes = context.getResources().getIdentifier(sound, "raw", context.getPackageName());
                    if(tmpSoundRes!=0) {
                        Uri sound_res = Uri.parse("android.resource://" + context.getPackageName() + "/" + tmpSoundRes);
                        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                .build();
                        channel.setSound(sound_res, audioAttributes);
                    }
                }
                catch (Exception e){
                    LogM.e("Error while setting channel sound res: " + e.getMessage());
                }

            }

            if(vibration_pattern.compareToIgnoreCase("off")==0) {
                channel.setVibrationPattern(null);
            }
            else if(vibration_pattern.compareToIgnoreCase("default")==0) {
                //do nothing, user default
            }
            else if(vibration_pattern.compareToIgnoreCase("")!=0) {
                String[] vibs = vibration_pattern.split(" ");
                long[] data = new long[vibs.length];
                for (int i = 0; i < vibs.length; i++) {
                    data[i] = Long.parseLong(vibs[i]);
                }
                channel.setVibrationPattern(data);
            }

            if(group_id.compareToIgnoreCase("")!=0) {
                channel.setGroup(group_id);
            }


            final NotificationManager notificationManager=
                    (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                LogM.i("Created a channel - " + channelID);
            }
        }
    }

    static void deleteChannel(Context context, String id){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (mNotificationManager != null) {
                mNotificationManager.deleteNotificationChannel(id);
                LogM.i("Deleted a channel - " + id);
            }
        }
    }

    static void createGroup(Context context, String name, String id){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannelGroup(new NotificationChannelGroup(id, name));
                LogM.i("Created a group - " + id);
            }
        }
    }

    static void deleteGroup(Context context, String id){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (mNotificationManager != null) {
                mNotificationManager.deleteNotificationChannelGroup(id);
                LogM.i("Deleted a group - " + id);
            }
        }
    }

    /**
     * Add a abandoned cart
     * @param action action can be update or delete
     * @param data key-value pair map includes cart url, checkout url, total items, product name, image or any additional data
     */
    public static void processAbandonedCart(String action, Map<String, String> data){
        new AbandonedCart(mContext, action, data).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * To enable or disable logging
     * @param enable true to enable
     */
    public static void enableDebug(boolean enable){
        LogM.enableDebug(enable);
    }

    /**
     * Add out of stock alert
     * @param product_id product ID
     * @param variant_id  variant ID, if not variant set 0
     * @param price price of the product
     * @param extras key-value map with product title and url
     */
    public static void addOutOfStockAlert(int product_id, int variant_id, double price, Map<String, String> extras){
        new ProductAlert(mContext, "oos", "add", product_id, variant_id, price, extras).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * To check whether user set an alert for out of stock for the given product
     * @param product_id product ID
     * @param variant_id variant ID, if not variant set 0
     * @return true if an alert set.
     */
    public static boolean isOutOfStockEnabled(int product_id, int variant_id){
        return Helper.getPreference(mContext, "pushalert_oos_" + product_id + "_" + variant_id, false);
    }

    /**
     * To remove out of stock alert
     * @param product_id product ID
     * @param variant_id variant ID, if not variant set 0
     */
    public static void removeOutOfStockAlert(int product_id, int variant_id){
        new ProductAlert(mContext, "oos", "remove", product_id, variant_id, 0, null).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Add price drop alert
     * @param product_id product ID
     * @param variant_id  variant ID, if not variant set 0
     * @param price price of the product
     * @param extras key-value map with product title and url
     */
    public static void addPriceDropAlert(int product_id, int variant_id, double price, Map<String, String> extras){
        new ProductAlert(mContext, "price_drop", "add", product_id, variant_id, price, extras).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * To check whether user set an alert for price drop for the given product
     * @param product_id product ID
     * @param variant_id variant ID, if not variant set 0
     * @return true if an alert set.
     */
    public static boolean isPriceDropEnabled(int product_id, int variant_id){
        return Helper.getPreference(mContext, "pushalert_price_drop_" + product_id + "_" + variant_id, false);
    }

    /**
     * To remove price drop alert
     * @param product_id product ID
     * @param variant_id variant ID, if not variant set 0
     */
    public static void removePriceDropAlert(int product_id, int variant_id){
        new ProductAlert(mContext, "price_drop", "remove", product_id, variant_id, 0, null).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static void initAppUsageAnalytics(){
        open_time = (int)(System.currentTimeMillis()/1000);
    }

    private static void appUsageAnalytics(){
        int exit_time = (int)(System.currentTimeMillis()/1000);
        new AppUsage(mContext, open_time,exit_time).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static void reportAnalytics(String url, String title){
        new Analytics(mContext, url, title).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private  static void reportAnalytics(String analyticsJSONStr, int conversionReceivedNotificationId){
        /*if(conversionNotificationId==-1){
            conversionNotificationId = getConversionNotificationId(); //Checking if there is another click or attribution
        }*/
        int conversionNotificationId = -1;
        int conversionClickedNotificationId = getConversionClickedNotificationId();

        int direct =0;
        if(conversionClickedNotificationId!=-1){
            conversionNotificationId = conversionClickedNotificationId;
            direct = 1;
        }
        else if(conversionReceivedNotificationId!=-1){
            conversionNotificationId = conversionReceivedNotificationId;
        }

        //Reset
        Helper.removeLastClickedNotificationInfo(mContext);

        new Analytics(mContext, analyticsJSONStr, conversionNotificationId, direct).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    static int getConversionReceivedNotificationId(){
        try {
            JSONObject lastNotificationInfo = Helper.getLastReceivedNotificationInfo(mContext);
            if (lastNotificationInfo != null) {
                if (lastNotificationInfo.getLong("time") >= System.currentTimeMillis() - 3600000L) { //ToDo
                    return lastNotificationInfo.getInt("notification_id");
                }
            }
        }
        catch (Exception e){
            LogM.e("Error while reporting conversion last notification info: " + e.getMessage());
        }

        return  -1;
    }

    static int getConversionClickedNotificationId(){
        try {
            JSONObject lastNotificationInfo = Helper.getLastClickedNotificationInfo(mContext);
            if (lastNotificationInfo != null) {
                return lastNotificationInfo.getInt("notification_id");
            }
        }
        catch (Exception e){
            LogM.e("Error while reporting conversion clicked last notification info: " + e.getMessage());
        }

        return  -1;
    }

    /*//Session conversion is different that we need to report everytime if there is notification id
    static void reportConversion(int notification_id, String conversion_name, double conversion_value){
         new ReportConversion(mContext, notification_id, conversion_name, conversion_value).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }*/

    /**
     * Report conversion with value (like purchase)
     * @param conversion_name conversion name
     * @param conversion_value conversion value
     */
    public static void reportConversionWithValue(String conversion_name, double conversion_value){
        int conversionNotificationId = -1;

        int conversionReceivedNotificationId = getConversionReceivedNotificationId();
        int conversionClickedNotificationId = getConversionClickedNotificationId();

        int direct =0;
        if(conversionClickedNotificationId!=-1){
            conversionNotificationId = conversionClickedNotificationId;
            direct = 1;
        }
        else if(conversionReceivedNotificationId!=-1){
            conversionNotificationId = conversionReceivedNotificationId;
        }

        if(conversionNotificationId>0) {
            new ReportConversion(mContext, conversionNotificationId, conversion_name, conversion_value, direct).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    /**
     * Report conversion only (not with value like register or sign-up)
     * @param conversion_name conversion name
     */
    public static void reportConversion(String conversion_name){
        reportConversionWithValue(conversion_name, 0.0);
    }

    /**
     * To associate a subscriber with a particular your logic ID
     * @param id String associating id
     */
    public static void  associateID(String id){
        Map<String, String> attributes = new HashMap<>();
        attributes.put("_assoc_id", id);
        new AddAttributesTask(mContext, attributes).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    /**
     * To set email of the push subscriber
     * @param email String Email
     */
    public static void setEmail(String email){
        Map<String, String> map = new HashMap<>();
        map.put("pa_email", email);
        PushAlert.addAttributes(map);
    }

    /**
     * To set age of the push subscriber
     * @param age int Age
     */
    public static void setAge(int age){
        Map<String, String> map = new HashMap<>();
        map.put("pa_age", String.valueOf(age));
        PushAlert.addAttributes(map);
    }

    /**
     * To set gender of the push subscriber
     * @param gender String Gender
     */
    public static void setGender(String gender){
        Map<String, String> map = new HashMap<>();
        map.put("pa_gender", gender);
        PushAlert.addAttributes(map);
    }

    /**
     * To set first name of the push subscriber
     * @param firstName String First Name
     */
    public static void setFirstName(String firstName){
        Map<String, String> map = new HashMap<>();
        map.put("pa_first_name", firstName);
        PushAlert.addAttributes(map);
    }

    /**
     * To set last name of the push subscriber
     * @param lastName String Last Name
     */
    public static void setLastName(String lastName){
        Map<String, String> map = new HashMap<>();
        map.put("pa_last_name", lastName);
        PushAlert.addAttributes(map);
    }

    /**
     * To set phone number of the push subscriber
     * @param phoneNum String Phone Number in required format
     */
    public static void setPhoneNum(String phoneNum){
        Map<String, String> map = new HashMap<>();
        map.put("pa_phone_num", phoneNum);
        PushAlert.addAttributes(map);
    }


    /**
     * Whether require privacy consent is enabled or not
     * @return boolean
     */
    public static boolean getRequiresPrivacyConsent(){
        if(Helper.isAndroid13AndAbove()){
            return true;
        }
        else {
            return Helper.getPreference(mContext, USER_PRIVACY_CONSENT_REQUIRED, false);
        }
    }

    /**
     * If privacy consent is required then call this method  with user consent to enable notifications
     * @param privacyConsent boolean
     */
    public static void setUserPrivacyConsent(boolean privacyConsent){
        try {
            Helper.setPreference(mContext, USER_PRIVACY_CONSENT, privacyConsent);
            if(privacyConsent){
                getInstance().build(true);
            }
            else{
                Helper.setSubscriptionStatus(mContext, PushAlert.PA_SUBS_STATUS_DENIED);
            }
        }
        catch (Exception ignored){
        }
    }

    /**
     * Get whether user allowed to get notifications or not.
     * @return boolean
     */
    public static boolean getUserPrivacyConsent(){
        if(getRequiresPrivacyConsent()) {
            return Helper.getPreference(mContext, USER_PRIVACY_CONSENT, false);
        }
        else{
            return true;
        }
    }

    static boolean getOSNotificationPermissionState(Context context){
        try {
            return NotificationManagerCompat.from(context).areNotificationsEnabled();
        } catch (Exception e) {
            LogM.e("Error in getting OS notification permission state: " + e.getMessage());
        }

        return true;
    }

    /**
     * To check whether user disabled notification or not.
     * @return boolean true or false
     */
    public static boolean isNotificationDisabled(){
        if(getUserPrivacyConsent()) {
            return !Helper.getPreference(mContext, APP_NOTIFICATION_PERMISSION_STATE, true);
        }
        else{
            return true;
        }
    }

    /**
     * To disable notification for the subscriber. This won't unsubscribe user from PushAlert. Call this method with ture to opt out users from receiving notifications.
     * @param disable boolean
     */
    public static void disableNotification(boolean disable){
        if(getUserPrivacyConsent()) {
            new SetAppNotificationPermissionStateTask(PushAlert.mContext, !disable).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    /**
     * Check whether user subscribed or not.
     * @return boolean
     */
    public static boolean isUserSubscribed(){
        return (getSubscriberID()!=null && getOSNotificationPermissionState(mContext));
    }

    //Check for version change
    private static void setAppVersionInit(Context context){
        try {
            int version_stored = Helper.getPreference(context, APP_VERSION, 0);
            int current_version = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionCode;
            if (version_stored != current_version) {
                Helper.setPreference(mContext, APP_VERSION, current_version);

                setAppVersion(String.valueOf(current_version));
            }
        }
        catch (Exception ignored){
        }
    }

    private static void setAppVersion(String appVersion){
        Map<String, String> map = new HashMap<>();
        map.put("pa_app_version", appVersion);
        PushAlert.addAttributes(map);
    }

    static void removeAlertPrefs(Context context, String ls_id){
        if(ls_id==null || ls_id.compareToIgnoreCase("")==0){
            return;
        }
        Helper.removePreference(context, ls_id);
    }

    static void subscribe(String subs_id){
        new ManageSubscriberTask(mContext, "subscribe").executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, subs_id);
    }

    static void checkSubscription(Context context, String subs_id){
        new ManageSubscriberTask(context, "check").executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, subs_id);
    }

    static void unsubscribe(Context context){
        String subs_id = Helper.getPreference(context, SUBSCRIBER_ID_PREF, null);
        if(subs_id!=null) {
            new ManageSubscriberTask(context, "unsubscribe").executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, subs_id);
        }
    }

    /**
     * Events are user interactions with content on your page. These are independent of app load and allow you to track the behavior of visitors. Examples of custom events can be a download, link click, scroll to a particular part of page, video play, ad-click and so on.
 interacted with (e.g. 'Video').
     * @param eventAction The type of interaction (e.g. 'play','pause','stopped').
     * @param eventLabel Used for categorizing events. (e.g. 'Launch Campaign').
     * @param eventValue A numeric value associated with the event (e.g. 2017).
     */
    public static void triggerEvent(String eventCategory, String eventAction, String eventLabel, int eventValue){
        new TriggerEvents(mContext, eventCategory, eventAction, eventLabel, eventValue).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void triggerEvent(String eventCategory, String eventAction, String eventLabel){
        new TriggerEvents(mContext, eventCategory, eventAction, eventLabel, 0).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void triggerEvent(String eventCategory, String eventAction){
        new TriggerEvents(mContext, eventCategory, eventAction, "", 0).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    static class ManageSubscriberTask extends AsyncTask<String, Void, JSONObject> {
        boolean isConnected = false;
        String action;
        @SuppressLint("StaticFieldLeak")
        Context context;
        SharedPreferences prefs;

        ManageSubscriberTask(Context context, String action) {
            this.action = action;
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            isConnected = Helper.isNetworkAvailable(context);
            prefs = Helper.getSharedPreferences(context);
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                if (!isConnected) {
                    return null;
                }

                while (sendingSubsID) {
                    Thread.sleep(500);
                }
                sendingSubsID = true;
                String reg_id = params[0];

                String[] pushalert_info = Helper.getAppId(mContext).split("-");


                //String uri = "https://"+pushalert_info[0]+".pushalert.co/"+action+"/"+reg_id;
                String uri = "https://androidapps.pushalert.co/"+action+"/"+reg_id;

                JSONObject postDataParams = new JSONObject();
                try {
                    WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                    Display display = null;
                    if (wm != null) {
                        display = wm.getDefaultDisplay();
                    }
                    DisplayMetrics metrics = new DisplayMetrics();
                    if (display != null) {
                        display.getMetrics(metrics);
                    }

                    postDataParams.put("action=", action);
                    postDataParams.put("pa_id", pushalert_info[1]);
                    postDataParams.put("domain_id", pushalert_info[2]);
                    postDataParams.put("host", pushalert_info[0]);
                    postDataParams.put("endpoint", reg_id);
                    postDataParams.put("type", context.getResources().getBoolean(R.bool.isTablet)?"tablet":"mobile");
                    postDataParams.put("browser", "chrome");
                    postDataParams.put("browserVer", "1");
                    postDataParams.put("browserMajor", "1.0");
                    postDataParams.put("os", "android");
                    postDataParams.put("osVer", Build.VERSION.RELEASE);
                    postDataParams.put("resoln_width", metrics.widthPixels+"");
                    postDataParams.put("resoln_height", metrics.heightPixels+"");
                    postDataParams.put("color_depth", "-1");
                    postDataParams.put("language", Locale.getDefault().getLanguage());
                    postDataParams.put("engine", "na");
                    postDataParams.put("userAgent", (System.getProperty("http.agent")!=null? URLEncoder.encode(System.getProperty("http.agent"), "UTF-8"):""));
                    postDataParams.put("endpoint_url", "https://fcm.googleapis.com/fcm/send/");
                    postDataParams.put("subs_info", "{}");
                    postDataParams.put("referrer", "na");
                    postDataParams.put("subscription_url", "na");
                    postDataParams.put("app_type", "android");

                } catch (Exception ignored) {

                }

                String encrypted_result = Helper.connectWithPushAlert(uri, postDataParams, true);
                if (encrypted_result==null) {
                    return null;
                } else {
                    return new JSONObject(encrypted_result);
                }

            } catch (Exception e) {
                LogM.e("Error in manage subscribe task: " + action + " - " + e.getMessage());
            }
            return null;
        }

        @SuppressLint("ApplySharedPref")
        @Override
        protected void onPostExecute(JSONObject reader) {
            PushAlert.tokenAlreadyGenerated = true;
            if (reader != null) {
                try {
                    if (reader.has("status") && reader.getBoolean("status")) {
                        LogM.i("Added/Updated subscriber successfully - " + action);

                        SharedPreferences.Editor editor = prefs.edit();
                        if(action.compareToIgnoreCase("unsubscribe")==0){
                            editor.remove(SUBSCRIBER_ID_PREF);
                            //editor.putBoolean(USER_SUBSCRIPTION_STATE, false);
                            editor.putInt(SUBSCRIPTION_STATUS_PREF, PA_SUBS_STATUS_UNSUBSCRIBED);

                            editor.apply();
                        }
                        else{
                            editor.putString(SUBSCRIBER_ID_PREF, reader.getString("subs_id"));
                            //editor.putBoolean(USER_SUBSCRIPTION_STATE, true);
                            editor.putInt(SUBSCRIPTION_STATUS_PREF, PA_SUBS_STATUS_SUBSCRIBED);

                            editor.commit(); //commit is required
                            sendingSubsID = false;

                            if(mOnSubscribeListener!=null) {
                                mOnSubscribeListener.onSubscribe(reader.getString("subs_id"));
                            }

                            if(reader.has("groups")){
                                JSONArray groups = reader.getJSONArray("groups");
                                for(int i=0;i<groups.length();i++){
                                    JSONObject data = groups.getJSONObject(i);
                                    PushAlert.createGroup(context, data.getString("name"), data.getString("id"));
                                }
                            }

                            if(reader.has("channels")){
                                JSONArray channels = reader.getJSONArray("channels");
                                for(int i=0;i<channels.length();i++){
                                    JSONObject data = channels.getJSONObject(i);
                                    PushAlert.createChannel(context, data.getString("channelID"), data.getString("name"), data.getString("desc"), data.getString("importance"), data.getString("ledColor"), data.getString("lockScreenVisibility"),
                                            data.getString("badge"), data.getString("sound"), data.getString("vibration_pattern"),  data.getString("group_id"));
                                }
                            }

                            //Welcome Notification ToDo
                            if(action.compareToIgnoreCase("subscribe")==0){
                                if(reader.has("welcome_enable") && reader.getBoolean("welcome_enable")){
                                    if(reader.has("welcome_data")){
                                        JSONObject welcome_data = new JSONObject(reader.getString("welcome_data"));
                                        LogM.e("welcome_data - " + welcome_data); //Todo remove
                                        Helper.processNotification(context, Helper.toMap(welcome_data), false);
                                    }
                                }
                            }
                        }

                        setAppVersionInit(context);

                    } else {
                        LogM.i("Issue while managing subscriber");
                    }

                } catch (Exception e) {
                    LogM.e("Error post manage subscriber task: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            sendingSubsID = false;
        }
    }

    static class AddAttributesTask extends AsyncTask<String, Void, JSONObject> {
        boolean isConnected = false;
        @SuppressLint("StaticFieldLeak")
        Context context;
        SharedPreferences prefs;
        Map<String, String> attributes;

        AddAttributesTask(Context context, Map<String, String> attributes) {
            this.context = context;
            this.attributes = attributes;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            isConnected = Helper.isNetworkAvailable(context);
            prefs = Helper.getSharedPreferences(context);
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                if (!isConnected) {
                    return null;
                }

                while (sendingSubsID) {
                    Thread.sleep(500);
                }


                String uri = "https://androidapi.pushalert.co/app/v1/attribute/put";
                JSONObject postDataParams = new JSONObject();
                try {
                    postDataParams.put("subscriber", prefs.getString(SUBSCRIBER_ID_PREF, null));
                    JSONObject jsonAttributes = new JSONObject(attributes);
                    postDataParams.put("attributes", jsonAttributes.toString());

                } catch (Exception ignored) {

                }

                String encrypted_result = Helper.connectWithPushAlert(uri, postDataParams, true);
                if (encrypted_result==null) {
                    return null;
                } else {
                    return new JSONObject(encrypted_result);
                }

            } catch (Exception e) {
                LogM.e("Error in add attribute: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject reader) {
            if (reader != null) {
                try {
                    if (reader.has("success") && reader.getBoolean("success")) {
                        SharedPreferences.Editor editor = prefs.edit();
                        //JSONArray jsonArray = attributes.toJSONArray();
                        for (Map.Entry<String, String> entry : attributes.entrySet()) {
                            String key = entry.getKey();
                            String value = entry.getValue();
                            editor.putString("pa_attr_"+key, value);
                        }

                        editor.apply();
                    } else {
                        LogM.i("Issue while adding attributes.");
                    }

                } catch (Exception e) {
                    LogM.e("Error in post adding attribute: "  + e.getMessage());
                }
            }
        }
    }

    static class SetAppNotificationPermissionStateTask extends AsyncTask<String, Void, JSONObject> {
        boolean isConnected = false;
        @SuppressLint("StaticFieldLeak")
        Context context;
        boolean subscriptionState;
        SharedPreferences prefs;

        SetAppNotificationPermissionStateTask(Context context, boolean subscriptionState) {
            this.context = context;
            this.subscriptionState = subscriptionState;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            isConnected = Helper.isNetworkAvailable(context);
            prefs = Helper.getSharedPreferences(context);
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                if (!isConnected) {
                    return null;
                }

                while (sendingSubsID) {
                    Thread.sleep(500);
                }


                String uri = "https://androidapi.pushalert.co/app/v1/subscriptionState";

                JSONObject postDataParams = new JSONObject();
                try {
                    postDataParams.put("subscriber", prefs.getString(SUBSCRIBER_ID_PREF, null));
                    postDataParams.put("is_active", subscriptionState?1:0);

                } catch (Exception ignored) {

                }

                String encrypted_result = Helper.connectWithPushAlert(uri, postDataParams, true);
                if (encrypted_result==null) {
                    return null;
                } else {
                    return new JSONObject(encrypted_result);
                }

            } catch (Exception e) {
                LogM.e("Error in setAppNotificationPermissionState: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject reader) {
            if (reader != null) {
                try {
                    if (reader.has("success") && reader.getBoolean("success")) {
                        Helper.setPreference(context, APP_NOTIFICATION_PERMISSION_STATE, subscriptionState);
                    } else {
                        LogM.i("Issue while updating user subscription state.");
                    }

                } catch (Exception e) {
                    LogM.e("Error in post setAppNotificationPermissionState: " + e.getMessage());
                }
            }
        }
    }

    static class AddToSegment extends AsyncTask<String, Void, JSONObject> {
        boolean isConnected = false;
        @SuppressLint("StaticFieldLeak")
        Context context;
        SharedPreferences prefs;
        int seg_id;

        AddToSegment(Context context, int seg_id) {
            this.context = context;
            this.seg_id = seg_id;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            isConnected = Helper.isNetworkAvailable(context);
            prefs = Helper.getSharedPreferences(context);
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                if (!isConnected) {
                    return null;
                }

                while (sendingSubsID) {
                    Thread.sleep(500);
                }


                String uri = "https://androidapi.pushalert.co/app/v1/segment/"+seg_id+"/add";

                JSONObject postDataParams = new JSONObject();
                try {
                    postDataParams.put("subscribers", "[\""+prefs.getString(SUBSCRIBER_ID_PREF, null)+"\"]");
                } catch (Exception ignored) {

                }

                String encrypted_result = Helper.connectWithPushAlert(uri, postDataParams, true);
                if (encrypted_result==null) {
                    return null;
                } else {
                    return new JSONObject(encrypted_result);
                }

            } catch (Exception e) {
                LogM.e("Error in Add to Segment: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject reader) {
            if (reader != null) {
                try {
                    if (reader.has("success") && reader.getBoolean("success")) {
                        LogM.i("User added to segment successfully");
                        //}
                    } else {
                        LogM.i("Issue while adding subscriber to segment");
                    }

                } catch (Exception e) {
                    LogM.e("Error in post Add to Segment: " + e.getMessage());
                }
            }
        }
    }

    static class RemoveFromSegment extends AsyncTask<String, Void, JSONObject> {
        boolean isConnected = false;
        @SuppressLint("StaticFieldLeak")
        Context context;
        SharedPreferences prefs;
        int seg_id;

        RemoveFromSegment(Context context, int seg_id) {
            this.context = context;
            this.seg_id = seg_id;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            isConnected = Helper.isNetworkAvailable(context);
            prefs = Helper.getSharedPreferences(context);
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                if (!isConnected) {
                    return null;
                }

                while (sendingSubsID) {
                    Thread.sleep(500);
                }


                String uri = "https://androidapi.pushalert.co/app/v1/segment/"+seg_id+"/remove";
                URL url = new URL(uri);

                JSONObject postDataParams = new JSONObject();
                try {
                    postDataParams.put("subscribers", "[\""+prefs.getString(SUBSCRIBER_ID_PREF, null)+"\"]");
                } catch (Exception ignored) {

                }

                String encrypted_result = Helper.connectWithPushAlert(uri, postDataParams, true);
                if (encrypted_result==null) {
                    return null;
                } else {
                    return new JSONObject(encrypted_result);
                }

            } catch (Exception e) {
                LogM.e("Error in remove from Segment: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject reader) {
            if (reader != null) {
                try {
                    if (reader.has("success") && reader.getBoolean("success")) {
                        LogM.i( "Subscriber removed from the segment.");
                    } else {
                        LogM.i( "Issue while removing subscriber to segment");
                    }

                } catch (Exception e) {
                    LogM.e("Error in post remove from Segment: " + e.getMessage());
                }
            }
        }
    }

    static class AbandonedCart extends AsyncTask<String, Void, JSONObject> {
        boolean isConnected = false;
        @SuppressLint("StaticFieldLeak")
        Context context;
        SharedPreferences prefs;
        String action;
        Map<String, String> data;

        AbandonedCart(Context context, String action, Map<String, String> data) {
            this.context = context;
            this.action = action;
            this.data = data;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            isConnected = Helper.isNetworkAvailable(context);
            prefs = Helper.getSharedPreferences(context);
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                if (!isConnected) {
                    return null;
                }

                while (sendingSubsID) {
                    Thread.sleep(500);
                }


                String uri = "https://androidapi.pushalert.co/app/v1/abandonedCart";
                if(action.compareToIgnoreCase("delete")==0){
                    uri = "https://androidapi.pushalert.co/app/v1/abandonedCart/delete";
                }

                JSONObject postDataParams = new JSONObject();
                try {
                    postDataParams.put("subscriber", prefs.getString(SUBSCRIBER_ID_PREF, null));
                    if(data!=null) {
                        JSONObject jsonAttributes = new JSONObject(data);
                        postDataParams.put("extra_info", jsonAttributes.toString());
                    }
                } catch (Exception ignored) {

                }

                String encrypted_result = Helper.connectWithPushAlert(uri, postDataParams, true);
                if (encrypted_result==null) {
                    return null;
                } else {
                    return new JSONObject(encrypted_result);
                }

            } catch (Exception e) {
                LogM.e("Error in Abandoned Cart: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject reader) {
            SharedPreferences.Editor editor = prefs.edit();
            if(data!=null){
                JSONObject jsonAttributes = new JSONObject(data);
                editor.putString(ABANDONED_CART_DATA, jsonAttributes.toString());
            }
            editor.apply();


            if (reader != null) {
                try {
                    if (reader.has("success") && reader.getBoolean("success")) {
                        LogM.i( "Abandoned action performed successfully: "+action);
                    } else {
                        LogM.i("There was some issue while processing abandoned cart.");
                    }

                } catch (Exception e) {
                    LogM.e("Error in post Abandoned Cart: " + e.getMessage());
                }
            }
        }
    }

    static class ProductAlert extends AsyncTask<String, Void, JSONObject> {
        boolean isConnected = false;
        @SuppressLint("StaticFieldLeak")
        Context context;
        SharedPreferences prefs;
        String action; //add or remove
        String type;
        Map<String, String> extras;
        int product_id, variant_id;
        double price;

        ProductAlert(Context context, String type, String action, int product_id, int variant_id, double price, Map<String, String> extras) {
            this.context = context;
            this.type = type;
            this.action = action;
            this.product_id = product_id;
            this.variant_id = variant_id;
            this.price = price*100;
            this.extras = extras;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            isConnected = Helper.isNetworkAvailable(context);
            prefs = Helper.getSharedPreferences(context);
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                if (!isConnected) {
                    return null;
                }

                while (sendingSubsID) {
                    Thread.sleep(500);
                }


                String uri = "https://androidapi.pushalert.co/app/v1/productAlert";

                JSONObject postDataParams = new JSONObject();
                try {
                    postDataParams.put("subscriber", prefs.getString(SUBSCRIBER_ID_PREF, null));

                    postDataParams.put("product_id", String.valueOf(product_id));
                    postDataParams.put("variant_id", String.valueOf(variant_id));
                    postDataParams.put("price", String.valueOf(price));
                    postDataParams.put("type", type);
                    postDataParams.put("alert_action", action);

                    if(extras!=null) {
                        JSONObject jsonAttributes = new JSONObject(extras);
                        postDataParams.put("extras", jsonAttributes.toString());
                    }
                } catch (Exception ignored) {

                }

                String encrypted_result = Helper.connectWithPushAlert(uri, postDataParams, true);
                if (encrypted_result==null) {
                    return null;
                } else {
                    return new JSONObject(encrypted_result);
                }
            } catch (Exception e) {
                LogM.e("Error in Product Alert: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject reader) {
            if (reader != null) {
                try {
                    if (reader.has("success") && reader.getBoolean("success")) {
                        SharedPreferences.Editor editor = prefs.edit();

                        if(extras!=null){
                            JSONObject jsonAttributes = new JSONObject(extras);
                            editor.putString(PRODUCT_ALERT_DATA, jsonAttributes.toString());
                        }

                        if(type.compareToIgnoreCase("oos")==0) {
                            if (action.compareToIgnoreCase("add")==0) {
                                editor.putBoolean("pushalert_oos_" + product_id + "_" + variant_id, true);
                            } else {
                                editor.remove("pushalert_oos_" + product_id + "_" + variant_id);
                            }
                        }
                        else if(type.compareToIgnoreCase("price_drop")==0) {
                            if (action.compareToIgnoreCase("add")==0) {
                                editor.putBoolean("pushalert_price_drop_" + product_id + "_" + variant_id, true);
                            } else {
                                editor.remove("pushalert_price_drop_" + product_id + "_" + variant_id);
                            }
                        }

                        editor.apply();

                        //}
                    } else {
                        LogM.i("There was some issue while processing product alerts");
                    }

                } catch (Exception e) {
                    LogM.e("Error in post Product Alert: " + e.getMessage());
                }
            }
        }
    }

    static class LocationUpdate extends AsyncTask<String, Void, JSONObject> {
        boolean isConnected = false;
        @SuppressLint("StaticFieldLeak")
        Context context;
        SharedPreferences prefs;
        String city, region, country, country_code;
        double latitude, longitude;

        LocationUpdate(Context context, String city, String region, String country, String country_code, double latitude, double longitude) {
            this.context = context;
            this.city = city;
            this.region = region;
            this.country = country;
            this.country_code = country_code;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            isConnected = Helper.isNetworkAvailable(context);
            prefs = Helper.getSharedPreferences(context);
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                if (!isConnected) {
                    return null;
                }

                while (sendingSubsID) {
                    Thread.sleep(500);
                }


                String uri = "https://androidapi.pushalert.co/app/v1/updateLoc";
                JSONObject postDataParams = new JSONObject();
                try {
                    postDataParams.put("subscriber", prefs.getString(SUBSCRIBER_ID_PREF, null));

                    postDataParams.put("city", city);
                    postDataParams.put("region", region);
                    postDataParams.put("country", country);
                    postDataParams.put("country_code", country_code);

                    DecimalFormat df = new DecimalFormat("#.######");
                    postDataParams.put("latitude", df.format(latitude));
                    postDataParams.put("longitude", df.format(longitude));
                } catch (Exception ignored) {

                }

                String encrypted_result = Helper.connectWithPushAlert(uri, postDataParams, true);
                if (encrypted_result==null) {
                    return null;
                } else {
                    return new JSONObject(encrypted_result);
                }
            } catch (Exception e) {
                LogM.e("Error in Location Update: " + e.getMessage());
                //e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject reader) {
            if (reader != null) {
                try {
                    if (reader.has("success") && reader.getBoolean("success")) {
                        LogM.i("Info updated successfully");
                    } else {
                        LogM.i("There was some issue while updating info");
                    }

                } catch (Exception e) {
                    LogM.e("Error in post Location Update: " + e.getMessage());
                }
            }
        }
    }

    static class AppUsage extends AsyncTask<String, Void, JSONObject> {
        boolean isConnected = false;
        @SuppressLint("StaticFieldLeak")
        Context context;
        SharedPreferences prefs;
        int open_time, exit_time;

        AppUsage(Context context, int open_time, int exit_time) {
            this.context = context;
            this.open_time = open_time;
            this.exit_time = exit_time;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            isConnected = Helper.isNetworkAvailable(context);
            prefs = Helper.getSharedPreferences(context);
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                if (!isConnected) {
                    return null;
                }

                while (sendingSubsID) {
                    Thread.sleep(500);
                }


                String uri = "https://androidapi.pushalert.co/app/v1/appUsage";

                JSONObject postDataParams = new JSONObject();
                try {
                    postDataParams.put("subscriber", prefs.getString(SUBSCRIBER_ID_PREF, null));

                    postDataParams.put("open_time", open_time);
                    postDataParams.put("exit_time", exit_time);
                } catch (Exception ignored) {

                }

                String encrypted_result = Helper.connectWithPushAlert(uri, postDataParams, true);
                if (encrypted_result==null) {
                    return null;
                } else {
                    return new JSONObject(encrypted_result);
                }
            } catch (Exception e) {
                LogM.e("Error in App Usage: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject reader) {
            if (reader != null) {
                try {
                    if (reader.has("success") && reader.getBoolean("success")) {
                        LogM.i("App usage updated successfully");
                    } else {
                        LogM.i("There was some issue while updating app usage");
                    }

                } catch (Exception e) {
                    LogM.e("Error in post App Usage: " + e.getMessage());
                }
            }
        }
    }

    static class ReportConversion extends AsyncTask<String, Void, JSONObject> {
        boolean isConnected = false;
        @SuppressLint("StaticFieldLeak")
        Context context;
        SharedPreferences prefs;
        String conversion_name;
        double conversion_value;
        int notification_id;
        int direct;

        ReportConversion(Context context, int notification_id, String conversion_name, double conversion_value, int direct) {
            this.context = context;
            this.conversion_name = conversion_name;
            this.conversion_value = conversion_value;
            this.notification_id = notification_id;
            this.direct = direct;

            LogM.i("Reporting conversion "+notification_id+" - " + conversion_name + "=" + conversion_value);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            isConnected = Helper.isNetworkAvailable(context);
            prefs = Helper.getSharedPreferences(context);
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                if (!isConnected) {
                    return null;
                }

                while (sendingSubsID) {
                    Thread.sleep(500);
                }


                String uri = "https://androidapi.pushalert.co/app/v1/conversion";
                JSONObject postDataParams = new JSONObject();
                try {
                    postDataParams.put("subscriber", prefs.getString(SUBSCRIBER_ID_PREF, null));

                    postDataParams.put("conversion_name", conversion_name);
                    postDataParams.put("notification_id", notification_id);

                    DecimalFormat df = new DecimalFormat("#.##");
                    postDataParams.put("conversion_value", df.format(conversion_value));
                    postDataParams.put("conversion_direct", direct);
                } catch (Exception ignored) {

                }

                String encrypted_result = Helper.connectWithPushAlert(uri, postDataParams, true);
                if (encrypted_result==null) {
                    return null;
                } else {
                    return new JSONObject(encrypted_result);
                }
            } catch (Exception e) {
                LogM.e("Error in Conversion Reporting: " + e.getMessage());
                //e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject reader) {
            if (reader != null) {
                try {
                    if (reader.has("success") && reader.getBoolean("success")) {
                        LogM.i("Conversion reported successfully");
                    } else {
                        LogM.i("There was some issue while updating info");
                    }

                } catch (Exception e) {
                    LogM.e("Error in post Conversion Reporting: " + e.getMessage());
                }
            }
        }
    }

    static class Analytics extends AsyncTask<String, Void, JSONObject> {
        boolean isConnected = false;
        @SuppressLint("StaticFieldLeak")
        Context context;
        SharedPreferences prefs;
        String mUrl, mTitle;
        String analyticsJSONStr;
        int conversionNotificationId = -1; //For Session Conversion
        int direct = 0;

        Analytics(Context context, String url, String title) {
            this.context = context;
            this.mUrl = url;
            this.mTitle = title;
        }

        Analytics(Context context, String analyticsJSONStr) {
            this.context = context;
            this.analyticsJSONStr = analyticsJSONStr;
        }

        Analytics(Context context, String analyticsJSONStr, int conversionNotificationId, int direct) {
            this.context = context;
            this.analyticsJSONStr = analyticsJSONStr;
            this.conversionNotificationId = conversionNotificationId;
            this.direct = direct;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            isConnected = Helper.isNetworkAvailable(context);
            prefs = Helper.getSharedPreferences(context);
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                if (!isConnected) {
                    return null;
                }

                while (sendingSubsID) {
                    Thread.sleep(500);
                }


                String uri = "https://androidapi.pushalert.co/analyticsApp";

                JSONObject postDataParams = new JSONObject();
                try {
                    postDataParams.put("subscriber", prefs.getString(SUBSCRIBER_ID_PREF, null));

                    //postDataParams.put("url", mUrl);
                    //postDataParams.put("title", mTitle);
                    postDataParams.put("analytics", analyticsJSONStr);
                    postDataParams.put("conversion_notification_id", conversionNotificationId);
                    postDataParams.put("conversion_direct", direct);
                } catch (Exception ignored) {

                }

                String encrypted_result = Helper.connectWithPushAlert(uri, postDataParams, true);
                if (encrypted_result==null) {
                    return null;
                } else {
                    return new JSONObject(encrypted_result);
                }
            } catch (Exception e) {
                LogM.e("Error in sending Analytics: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject reader) {
            if (reader != null) {
                try {
                    if (reader.has("success") && reader.getBoolean("success")) {
                        LogM.i("App analytics updated successfully");
                    } else {
                        LogM.i("There was some issue while updating app analytics");
                    }

                } catch (Exception e) {
                    LogM.e("Error in post sending Analytics: " + e.getMessage());
                }
            }
        }
    }

    static class TriggerEvents extends AsyncTask<String, Void, JSONObject> {
        boolean isConnected = false;
        @SuppressLint("StaticFieldLeak")
        Context context;
        SharedPreferences prefs;
        String eventCategory, eventAction, eventLabel;
        int eventValue;

        TriggerEvents(Context context, String eventCategory, String eventAction, String eventLabel, int eventValue) {
            this.context = context;
            this.eventCategory = eventCategory;
            this.eventAction = eventAction;
            this.eventLabel = eventLabel;
            this.eventValue = eventValue;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            isConnected = Helper.isNetworkAvailable(context);
            prefs = Helper.getSharedPreferences(context);
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                if (!isConnected) {
                    return null;
                }

                while (sendingSubsID) {
                    Thread.sleep(500);
                }


                String uri = "https://androidapi.pushalert.co/app/v1/track/event";

                JSONObject postDataParams = new JSONObject();
                try {
                    postDataParams.put("subscriber", prefs.getString(SUBSCRIBER_ID_PREF, null));

                    postDataParams.put("eventCategory", eventCategory);
                    postDataParams.put("eventAction", eventAction);
                    postDataParams.put("eventLabel", eventLabel);
                    postDataParams.put("eventValue", eventValue);
                } catch (Exception ignored) {

                }

                String encrypted_result = Helper.connectWithPushAlert(uri, postDataParams, true);
                if (encrypted_result==null) {
                    return null;
                } else {
                    return new JSONObject(encrypted_result);
                }
            } catch (Exception e) {
                LogM.e("Error in customEvent: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject reader) {
            if (reader != null) {
                try {
                    if (reader.has("success") && reader.getBoolean("success")) {
                        LogM.i("Event registered successfully");
                    } else {
                        LogM.i("There was some issue while registering event");
                    }

                } catch (Exception e) {
                    LogM.e("Error in post customEvent: " + e.getMessage());
                }
            }

        }
    }

    static NotificationReceiver getNotificationReceiver(){
        return PushAlert.notificationReceiver;
    }

    static NotificationOpener getNotificationOpener(){
        return PushAlert.notificationOpener;
    }

    /**
     * To get subscriber ID
     * @return null if not subscribed otherwise a string
     */
    public static String getSubscriberID(){
        return Helper.getPreference(mContext, SUBSCRIBER_ID_PREF, null);
    }

    /**
     * To get subscriber ID
     * @return null if not subscribed otherwise a string
     */
    /*public static boolean checkUserExplicitlyPermissionDenied(){
        return Helper.getPreference(mContext, EXPLICITLY_PERMISSION_DENIED, false);
    }*/

    /*
    public static void setUserExplicitlyPermissionDenied(boolean value){
        Helper.setPreference(mContext, EXPLICITLY_PERMISSION_DENIED, value);
    }
    */

    private static boolean  canUserSubscribe(Context context){
        if(paUnsubscribeWhenNotificationsAreDisabled){
            if(Helper.isAndroid13AndAbove()) {
                boolean osNotificationPermissionState = getOSNotificationPermissionState(mContext);
                if(osNotificationPermissionState){
                    return  true;
                }
                else {
                    return !ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.POST_NOTIFICATIONS);
                }
            }
            else{
                return getOSNotificationPermissionState(mContext);
            }
        }
        else{
            return  true;
        }
    }

    public static class InkWired{
        Context mContext;

        /**
         * The desired interval for location updates. Inexact. Updates may be more or less frequent.
         */
        private static final long UPDATE_INTERVAL = 60*1000;//1800 * 1000;

        /**
         * The fastest rate for active location updates. Updates will never be more frequent
         * than this value, but they may be less frequent.
         */
        private static final long FASTEST_UPDATE_INTERVAL = UPDATE_INTERVAL / 2;

        /**
         * The max time before batched results are delivered by location services. Results may be
         * delivered sooner than this interval.
         */
        private static final long MAX_WAIT_TIME = (UPDATE_INTERVAL * 15)/10;

        private static final float SMALLEST_DISPLACEMENT = 100f;

        private static boolean isLifecycleCallbacksAttached = false;
        private static int conversionReceivedNotificationId = -1;
        private static int conversionClickedNotificationId = -1;


        private  InkWired(Context mContext){
            this.mContext = mContext;
        }

        public void build(boolean byPassCheck){
            String currSubsID = PushAlert.getSubscriberID();
            LogM.i("PushAlert Subscriber ID: " + currSubsID);

            boolean condition1 = PushAlert.getUserPrivacyConsent();
            boolean condition2 = (currSubsID != null);
            boolean condition3 = (Helper.getSubscriptionStatus(mContext) == PA_SUBS_STATUS_DEFAULT);

            LogM.d("Condition1: " + condition1 + ", condition2: " + condition2 + ", condition3: " + condition3 + ", canUserSubscribe: " + canUserSubscribe(mContext)); //ToDo Remove

            if(!Helper.isAndroid13AndAbove() && PushAlert.mOptInMode == PAOptInMode.AUTO && (!condition1 || byPassCheck)){
                PushAlert.mOptInMode = PAOptInMode.TWO_STEP;
            }

            if (condition1 || condition2 || condition3 || byPassCheck) {

                if(byPassCheck || canUserSubscribe(mContext)) {
                    Log.e("PushAlertSDK", "Checking Android Version - " + Build.VERSION.SDK_INT);

                    if(currSubsID!=null && !PushAlert.getOSNotificationPermissionState(mContext) && !byPassCheck){
                        return; //User disabled notification by choice, respect privacy
                    }
                    else if ((currSubsID==null || (!PushAlert.getOSNotificationPermissionState(mContext) && byPassCheck)) &&
                                (Helper.isAndroid13AndAbove() || mOptInMode == PAOptInMode.TWO_STEP) && !PushAlert.getOSNotificationPermissionState(mContext)) {
                        Log.e("PushAlertSDK", "Two Step Opt In Mode - " + (mOptInMode == PAOptInMode.TWO_STEP));
                        requestPermissionActivity(mOptInMode);
                        return;
                    }


                    LogM.e("PushAlertSDK", "Initializing firebase app - " + Helper.getFBAPIKey(mContext) +", " + Helper.getFBAppID(mContext) + ", " + Helper.getFBProjectID(mContext));
                    FirebaseApp.initializeApp(mContext);
                    fireBaseInitialized = true;


                    try {
                        if (currSubsID == null) {

                            FirebaseMessaging.getInstance().getToken()
                                    .addOnCompleteListener(new OnCompleteListener<String>() {
                                        @Override
                                        public void onComplete(@NonNull Task<String> task) {
                                            if (!task.isSuccessful()) {
                                                LogM.w("Fetching FCM registration token failed: " + task.getException());
                                                return;
                                            }

                                            // Get new FCM registration token
                                            String token = task.getResult();

                                            if (!tokenAlreadyGenerated) {
                                                tokenAlreadyGenerated = true;
                                                PushAlert.subscribe(token);
                                            }
                                        }
                                    });
                        } else {
                            setAppVersionInit(mContext);
                        }
                    } catch (Exception e) {
                        LogM.e("Error while initiating: " + e.getMessage());
                    }


                    open_time = (int) (System.currentTimeMillis() / 1000);
                    PushAlert.InkWired.registerActivityLifeCycleListener((Application) mContext);
                }
                else{
                    PushAlert.unsubscribe(mContext);
                }
            }

        }

        private void requestPermissionActivity(PAOptInMode optInMode){
            PermissionRequestActivity.registerAsCallback(new PermissionRequestActivity.PermissionCallback(){
                @Override
                public void onAccept() {
                    Log.e("PushAlertSDK", "Permission Accepted");
                    PushAlert.setUserPrivacyConsent(true); //because we're taking consent
                    //build();
                }

                @Override
                public void onReject() {
                    Log.e("PushAlertSDK", "Permission rejected");
                }
            });

            Intent intent = new Intent(mContext, PermissionRequestActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("optInModeStr", optInMode.name());
            mContext.startActivity(intent);
        }

        /**
         * To set your own notification receiver, you can modify color, text etc or can use extra data as per your need. Even can stop notification to display.
         * @param notificationReceiver {@link NotificationReceiver}
         * @return PushAlert Instance
         */
        public PushAlert.InkWired setNotificationReceiver(NotificationReceiver notificationReceiver){
            PushAlert.notificationReceiver = notificationReceiver;
            return getInstance();
        }

        /**
         * To set your notification click handler, you can open your app using extra data or url inside your app.
         * @param notificationOpener {@link NotificationOpener}
         * @return PushAlert Instance
         */
        public PushAlert.InkWired setNotificationOpener(NotificationOpener notificationOpener){
            PushAlert.notificationOpener = notificationOpener;
            return getInstance();
        }

        /**
         * To set your listener when user subscribes, this will help you to associate subscription id with your database
         * @param onSubscribeListener {@link PASubscribe}
         * @return PushAlert Instance
         */
        public PushAlert.InkWired setOnSubscribeListener(PASubscribe onSubscribeListener){
            mOnSubscribeListener = onSubscribeListener;
            return getInstance();
        }

        /**
         * If notifications are disabled for your app, unsubscribe the user.
         * Happens when your app users go to Settings -&lt; Apps and turn off notifications or
         * they long press your notifications and select "block notifications". This is false by default.
         * @param unsubscribe (true or false)
         * @return PushAlert instance
         */
        public PushAlert.InkWired unsubscribeWhenNotificationsAreDisabled(boolean unsubscribe){
            paUnsubscribeWhenNotificationsAreDisabled = unsubscribe;
            return getInstance();
        }

        /**
         * To set whether privacy consent is required or not to enable notifications. If required then you need to setUserPrivacyConsent with user consent.
         * @param consentRequired (true or false)
         * @return PushAlert instance
         */
        public PushAlert.InkWired setRequiresPrivacyConsent(boolean consentRequired){
            try {
                Helper.setPreference(mContext, USER_PRIVACY_CONSENT_REQUIRED, consentRequired);
            }
            catch (Exception ignored){
            }

            return getInstance();
        }

        /**
         * To enable location sharing, requires ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission. You can use this information to target user based on longitude and latitude
         * @param enable boolean.
         * @return PushAlert Instance
         */
        public PushAlert.InkWired enableLocationSharing(boolean enable){
            PushAlert.InkWired pushAlertIW = PushAlert.getInstance();
            if(enable){
                final Context context = pushAlertIW.mContext;
                if ((ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                        || (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)){

                    mLocationCallback = new LocationCallback() {
                        @Override
                        public void onLocationResult(@NonNull LocationResult locationResult) {
                            try {
                                List<Location> locations = locationResult.getLocations();

                                for (Location location : locations) {
                                    Geocoder gcd = new Geocoder(mContext, Locale.getDefault());
                                    List<Address> addresses;

                                    addresses = gcd.getFromLocation(location.getLatitude(),
                                            location.getLongitude(), 1);
                                    if (addresses.size() > 0) {
                                        Address address = addresses.get(0);
                                        new PushAlert.LocationUpdate(mContext, address.getLocality(), address.getAdminArea(),
                                                address.getCountryName(), address.getCountryCode(), address.getLatitude(), address.getLongitude())
                                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                    }

                                }
                            } catch (Exception e) {
                                LogM.e("Error Service Location Servicing: " + e.getMessage());
                            }

                        }
                    };

                    buildGoogleApiClient();
                }
            }


            return getInstance();
        }

        /**
         * To set default small icon
         * @param drawable int. Drawable resource int
         * @return PushAlert instance
         */
        public PushAlert.InkWired setDefaultSmallIcon(int drawable){
            Helper.setPreference(mContext, PA_DEFAULT_SMALL_ICON, drawable);

            return getInstance();
        }

        /**
         * To set notification default accent color.
         * @param color int.  Color resource int
         * @return  PushAlert instance
         */
        public PushAlert.InkWired setDefaultAccentColor(int color){
            Helper.setPreference(mContext, PA_DEFAULT_ACCENT_COLOR, color);

            return getInstance();
        }

        /**
         * To enable or disable firebase events reporting
         * @param enable boolean. Set true to enable firebase events reporting
         * @return PushAlert instance
         */
        public PushAlert.InkWired enableFirebaseEventReporting(boolean enable){
            Helper.setPreference(mContext, ENABLE_FIREBASE_ANALYTICS, enable);
            return getInstance();
        }

        /**
         * To customize 2 step optin dialog
         * @param twoStepHelper you can change title, subTitle, acceptBtn, rejectBtn,
         *                      bgColor, titleTextColor, subTitleTextColor, acceptBtnTextColor, acceptBtnBgColor,
         *                      rejectBtnTextColor, rejectBtnBgColor, iconDrawable, imageDrawable
         * @return PushAlert instance
         */
        public PushAlert.InkWired customizeTwoStep(TwoStepHelper twoStepHelper){
            PushAlert.twoStepHelper = twoStepHelper;
            return getInstance();
        }

        /**
         * To set in app behaviour, when your app is in focus
         * @param inAppBehaviour {@link PAInAppBehaviour} None - Don't show notification, Notification - Default behaviour
         * @return PushAlert Instance
         */
        public PushAlert.InkWired setInAppNotificationBehaviour(PAInAppBehaviour inAppBehaviour){
            PushAlert.mInAppBehaviour = inAppBehaviour;

            return getInstance();
        }

        private void buildGoogleApiClient() {
            LocationRequest mLocationRequest = LocationRequest.create()
                    .setInterval(UPDATE_INTERVAL)
                    .setFastestInterval(FASTEST_UPDATE_INTERVAL)
                    .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                    .setMaxWaitTime(MAX_WAIT_TIME)
                    .setSmallestDisplacement(SMALLEST_DISPLACEMENT);

            try {
                FusedLocationProviderClient mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mContext);

                mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
                mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());

            } catch (SecurityException e) {
                LogM.e("Location update error: " + e.getMessage());
            }

        }

        static void registerActivityLifeCycleListener(Application app) {

            if(!isLifecycleCallbacksAttached) {
                isLifecycleCallbacksAttached = true;
                app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                    private int myStarted;
                    private JSONArray analyticsJSON = new JSONArray();

                    @Override
                    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

                    }

                    @Override
                    public void onActivityStarted(Activity activity) {
                        if (myStarted == 0) {
                            initAppUsageAnalytics();

                            //Check notification conversion
                            conversionReceivedNotificationId = getConversionReceivedNotificationId();
                        }
                        myStarted++;
                    }

                    @Override
                    public void onActivityResumed(Activity activity) {
                        try {
                            analyticsJSON.put(
                                    (new JSONObject())
                                            .put("url", activity.getClass().getCanonicalName())
                                            .put("title", activity.getClass().getSimpleName())
                                            .put("time", System.currentTimeMillis() / 1000L));
                        } catch (Exception ignored) {

                        }

                        Helper.checkForNotificationImpact(activity.getApplicationContext());
                    }

                    @Override
                    public void onActivityPaused(Activity activity) {
                        try {
                            int index = analyticsJSON.length()-1;
                            JSONObject jsonObject = (JSONObject) analyticsJSON.get(index);
                            long start_time = jsonObject.getLong("time");
                            long time_spent = (System.currentTimeMillis() / 1000L) - start_time;
                            analyticsJSON.put(index,
                                    (new JSONObject())
                                            .put("url", jsonObject.getString("url"))
                                            .put("title", jsonObject.getString("title"))
                                            .put("time", start_time)
                                            .put("time_spent", time_spent));
                        } catch (Exception ignored) {

                        }
                    }

                    @Override
                    public void onActivityStopped(Activity activity) {

                        myStarted--;
                        if (myStarted == 0) {
                            appUsageAnalytics();

                            try {
                                reportAnalytics(analyticsJSON.toString(), conversionReceivedNotificationId);
                            } catch (Exception e) {
                                LogM.e("Error while reporting analytics: " + e.getMessage());
                            }
                            analyticsJSON = new JSONArray();
                        }
                    }

                    @Override
                    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

                    }

                    @Override
                    public void onActivityDestroyed(Activity activity) {

                    }


                });




            }
        }
    }


}
