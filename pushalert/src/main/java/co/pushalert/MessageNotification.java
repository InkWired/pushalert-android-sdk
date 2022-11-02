package co.pushalert;

import static co.pushalert.PushAlert.NOTIFICATION_CHANNEL;
import static co.pushalert.PushAlert.SUBSCRIBER_ID_PREF;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for handling new message notifications.
 * @author Mohit Kuldeep
 * @since 02-11-2022
 */
public class MessageNotification {
    /**
     * The unique identifier for this type of notification.
     */
    static final String NOTIFICATION_TAG = "NewNotification";

    /**
     * Initializes notification
     * @param context context of activity
     * @param notification notification instance
     */
    static void sendReceivedReport(final Context context,
                              PANotification notification){
        new receivedNotificationReport(context, notification).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Shows the notification, or updates a previously shown notification of
     * @see #cancel(Context, String, int)
     */
    static void notifyInit(final Context context,
                                  PANotification notification){
        new receiveNotification(context, notification).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    static void notify(final Context context,
                              PANotification notification, Bitmap largeIcon, Bitmap largeImage, String uid) {

        SharedPreferences sharedPrefs = Helper.getSharedPreferences(context);

        int pendingIntentFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlag = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        }
        else {
            pendingIntentFlag = PendingIntent.FLAG_UPDATE_CURRENT;
        }


        String ticker = notification.getContent();
        String title = notification.getShortTitle();
        String ticker_attr = notification.getContentAttr();
        String title_attr = notification.getShortTitleAttr();
        String url = notification.getUrl();
        String url_attr = notification.getUrlAttr();

        String action1_url = notification.getAction1Url();
        String action1_url_attr = notification.getAction1UrlAttr();
        String action1_title = notification.getAction1Title();
        String action1_title_attr = notification.getAction1TitleAttr();

        String action2_title = notification.getAction2Title();
        String action2_title_attr = notification.getAction2TitleAttr();
        String action2_url = notification.getAction2Url();
        String action2_url_attr = notification.getAction2UrlAttr();

        String action3_title = notification.getAction3Title();
        String action3_title_attr = notification.getAction3TitleAttr();
        String action3_url = notification.getAction3Url();
        String action3_url_attr = notification.getAction3UrlAttr();

        String channel = notification.getChannel();
        channel = (channel==null || channel.compareToIgnoreCase("")==0)?NOTIFICATION_CHANNEL:channel;

        Map<String, String> attr_text = new HashMap<String, String>();
        Map<String, String> no_attr_text = new HashMap<String, String>();
        if(ticker_attr!=null) {
            //ticker = Helper.processAttributes(context, ticker_attr, ticker);
            attr_text.put("ticker", ticker_attr);
            no_attr_text.put("ticker", ticker);
        }
        if(title_attr!=null) {
            //title = Helper.processAttributes(context, title_attr, title);
            attr_text.put("title", title_attr);
            no_attr_text.put("title", title);
        }
        if(url_attr!=null){
            //url = Helper.processAttributes(context, url_attr, url);
            attr_text.put("url", url_attr);
            no_attr_text.put("url", url);
        }

        if(action1_title_attr!=null){
            //action1_title = Helper.processAttributes(context, action1_title_attr, action1_title);
            attr_text.put("action1_title", action1_title_attr);
            no_attr_text.put("action1_title", action1_title);
        }
        if(action1_url_attr!=null){
            //action1_url = Helper.processAttributes(context, action1_url_attr, action1_url);
            attr_text.put("action1_url", action1_url_attr);
            no_attr_text.put("action1_url", action1_url);
        }

        if(action2_title_attr!=null){
            //action2_title = Helper.processAttributes(context, action2_title_attr, action2_title);
            attr_text.put("action2_title", action2_title_attr);
            no_attr_text.put("action2_title", action2_title);
        }
        if(action2_url_attr!=null){
            //action2_url = Helper.processAttributes(context, action2_url_attr, action2_url);
            attr_text.put("action2_url", action2_url_attr);
            no_attr_text.put("action2_url", action2_url);
        }

        if(action3_title_attr!=null){
            //action3_title = Helper.processAttributes(context, action3_title_attr, action3_title);
            attr_text.put("action3_title", action3_title_attr);
            no_attr_text.put("action3_title", action3_title);
        }
        if(action3_url_attr!=null){
            //action3_url = Helper.processAttributes(context, action3_url_attr, action3_url);
            attr_text.put("action3_url", action3_url_attr);
            no_attr_text.put("action3_url", action3_url);
        }

        Map<String, String> process_attr_text = Helper.checkAndConvert2Attributes(context, attr_text, no_attr_text);
        if(!process_attr_text.containsKey("no_attr")) { //Need improvement here
            for (Map.Entry<String, String> entry : process_attr_text.entrySet()) {
                if (entry.getKey().compareToIgnoreCase("ticker") == 0) {
                    ticker = entry.getValue();
                } else if (entry.getKey().compareToIgnoreCase("title") == 0) {
                    title = entry.getValue();
                } else if (entry.getKey().compareToIgnoreCase("url") == 0) {
                    url = entry.getValue();
                } else if (entry.getKey().compareToIgnoreCase("action1_title") == 0) {
                    action1_title = entry.getValue();
                } else if (entry.getKey().compareToIgnoreCase("action1_url") == 0) {
                    action1_url = entry.getValue();
                } else if (entry.getKey().compareToIgnoreCase("action2_title") == 0) {
                    action2_title = entry.getValue();
                } else if (entry.getKey().compareToIgnoreCase("action2_url") == 0) {
                    action2_url = entry.getValue();
                } else if (entry.getKey().compareToIgnoreCase("action3_title") == 0) {
                    action3_title = entry.getValue();
                } else if (entry.getKey().compareToIgnoreCase("action3_url") == 0) {
                    action3_url = entry.getValue();
                }
            }
        }

        //For abandoned cart or others
        if(notification.getType()==501){
            String abandoned_cart_data = sharedPrefs.getString(PushAlert.ABANDONED_CART_DATA, "{}");

            try {
                JSONObject attributes = new JSONObject(abandoned_cart_data);
                String new_title = Helper.processJSONAttributes(title_attr, attributes);
                String new_ticker = Helper.processJSONAttributes(ticker_attr, attributes);

                if(new_title.compareTo("-1")!=0 && new_ticker.compareTo("-1")!=0){
                    title = new_title;
                    ticker = new_ticker;
                }

                if(attributes.has("cart_url")){
                    url = attributes.getString("cart_url");
                }

                if(attributes.has("checkout_url")){
                    action1_url = attributes.getString("checkout_url");
                }
            } catch (Exception e) {

                LogM.e("Error while setting attribute in abandoned cart: " + e.getMessage());
            }

        }
        else if(notification.getType()==32 || notification.getType()==33){
            String product_alert_data = sharedPrefs.getString(PushAlert.PRODUCT_ALERT_DATA, "{}");

            try {
                JSONObject attributes = new JSONObject(product_alert_data);
                String new_title = Helper.processJSONAttributes(title, attributes);
                String new_ticker = Helper.processJSONAttributes(ticker, attributes);

                if(new_title.compareTo("-1")!=0 && new_ticker.compareTo("-1")!=0){
                    title = new_title;
                    ticker = new_ticker;
                }
            } catch (Exception e) {

                LogM.e("Error while setting attribute in abandoned cart: " + e.getMessage());
            }
        }


        String content_text = ticker;

        Intent intent;
        intent = new Intent(context, NotificationHandler.class);

        //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("url", url);
        intent.putExtra("extraData", notification.getExtraData().toString());

        intent.putExtra("notification_id", notification.getId());
        intent.putExtra("campaign", (notification.getTemplateId()>0)?(notification.getTemplate() + " ("+notification.getTemplateId()+")"):"None");
        intent.putExtra("is_action", false);
        intent.putExtra("uid", uid);
        intent.putExtra("type", notification.getType());
        intent.putExtra("eid", 0);
        intent.putExtra("clicked_on", 0);
        intent.putExtra("group_id", notification.getGroupId());
        intent.putExtra("action_id", "main");
        //intent.setData(Uri.parse(notification.getUrl()));

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channel)

                .setContentTitle(title)
                .setContentText(content_text)
                .setTicker(ticker)

                .setWhen(notification.getSentTime()*1000L)

                // Set the pending intent to be initiated when the user touches
                // the notification.
                .setContentIntent(
                        PendingIntent.getActivity(
                                context,
                                notification.getId(),
                                intent,
                                pendingIntentFlag))
                //.setOnlyAlertOnce(PushAlert.isOnlyAlertOnce())
                // Automatically dismiss the notification when it is touched.*/
                .setAutoCancel(true);


        String ledColor = notification.getLEDColor();

        boolean ledColorCheck = !(ledColor== null
                || (ledColor.compareToIgnoreCase("off")==0)
                /*|| (ledColor.compareToIgnoreCase("default")==0)*/
                || (ledColor.compareToIgnoreCase("")==0));
        if(ledColor!=null && ledColor.compareToIgnoreCase("")!=0 &&
                ledColor.compareToIgnoreCase("off")!=0 &&
                ledColor.compareToIgnoreCase("default")!=0){
            builder.setLights(Color.parseColor(ledColor), 200, 200);
        }

        Uri sound_res = Settings.System.DEFAULT_NOTIFICATION_URI;
        if(notification.getSoundRes()!=null && notification.getSoundRes().compareToIgnoreCase("default")!=0){
            try {
                int tmp_sound_res = context.getResources().getIdentifier(notification.getSoundRes(), "raw", context.getPackageName());
                if(tmp_sound_res!=0) {
                    sound_res = Uri.parse("android.resource://" + context.getPackageName() + "/" + tmp_sound_res);
                }
            }
            catch (Exception e){
                LogM.e("Error while getting sound: " + e.getMessage());
                e.printStackTrace();
            }
        }
        builder.setSound(sound_res);

        int visibility = NotificationCompat.VISIBILITY_PUBLIC;
        if(notification.getLockScreenVisibility()!=null) {
            if (notification.getLockScreenVisibility().equalsIgnoreCase("private")){
                visibility = NotificationCompat.VISIBILITY_PRIVATE;
            }
            else if (notification.getLockScreenVisibility().equalsIgnoreCase("secret")){
                visibility = NotificationCompat.VISIBILITY_SECRET;
            }
        }
        builder.setVisibility(visibility);

        int small_icon = PushAlert.getDefaultSmallIcon(context);
        if(notification.getSmallIconRes()!=null){
            try {
                int tmp_small_icon = context.getResources().getIdentifier(notification.getSmallIconRes(), "drawable", context.getPackageName());
                if(tmp_small_icon!=0){
                    small_icon = tmp_small_icon;
                }
            }
            catch (Exception e){
                LogM.e("Error while getting small icon: " + e.getMessage());
            }
        }
        builder.setSmallIcon(small_icon);

        LogM.e("PushAlert.getDefaultAccentColor() - " + PushAlert.getDefaultAccentColor(context));
        //int accent_color = ContextCompat.getColor(context, PushAlert.getDefaultAccentColor());
        int accent_color = PushAlert.getDefaultAccentColor(context);
        if(notification.getAccentColor()!=null){
            try {
                accent_color = Color.parseColor(notification.getAccentColor());
            }
            catch (Exception e){
                LogM.e("Error while getting accent color: " + e.getMessage());
            }
        }
        builder.setColor(accent_color);

        if(largeIcon!=null){
            builder.setLargeIcon(largeIcon);
        }

        if(largeImage!=null){
            builder.setStyle(
                    new NotificationCompat.BigPictureStyle().bigPicture(largeImage)
                            .setSummaryText(content_text)
                            .setBigContentTitle(title)
            );
        }
        else{
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(content_text));
        }

        int priority = NotificationCompat.PRIORITY_DEFAULT;
        if(notification.getPriority()!=null){
            String importance = notification.getPriority();
            if(importance.compareToIgnoreCase("urgent")==0){
                priority = NotificationCompat.PRIORITY_HIGH;
            }
            else if(importance.compareToIgnoreCase("high")==0){
                priority = NotificationCompat.PRIORITY_DEFAULT;
            }
            else if(importance.compareToIgnoreCase("medium")==0){
                priority = NotificationCompat.PRIORITY_LOW;
            }
            else if(importance.compareToIgnoreCase("low")==0){
                priority = NotificationCompat.PRIORITY_MIN;
            }
        }
        builder.setPriority(priority);

        if (notification.getHeaderText()!=null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setSubText(notification.getHeaderText());
        }

        if(action1_title!=null && action1_url!=null){
            int action1_icon = -1;
            if(notification.getAction1IconRes()!=null){
                try {
                    int tmp_action1_icon = context.getResources().getIdentifier(notification.getAction1IconRes(), "drawable", context.getPackageName());
                    if(tmp_action1_icon!=0){
                        action1_icon = tmp_action1_icon;
                    }
                }
                catch (Exception ignored){

                }
            }


            Intent action1_intent = new Intent(context, NotificationHandler.class);
            action1_intent.setAction(notification.getId() + "_1");
            //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            action1_intent.putExtra("url", action1_url);
            action1_intent.putExtra("extraData", notification.getExtraData().toString());
            action1_intent.putExtra("notification_id", notification.getId());
            action1_intent.putExtra("campaign", (notification.getTemplateId()>0)?(notification.getTemplate() + " ("+notification.getTemplateId()+")"):"None");
            action1_intent.putExtra("is_action", true);
            action1_intent.putExtra("uid", uid);
            action1_intent.putExtra("clicked_on", 1);
            action1_intent.putExtra("group_id", notification.getGroupId());
            action1_intent.putExtra("action_id", notification.getAction1Id());

            builder.addAction(
                    action1_icon,
                    action1_title,
                    PendingIntent.getActivity(
                            context,
                            notification.getId(),
                            action1_intent,
                            pendingIntentFlag));
        }

        if(action2_title!=null && action2_url!=null){
            int action2_icon = -1;
            if(notification.getAction2IconRes()!=null){
                try {
                    int tmp_action2_icon = context.getResources().getIdentifier(notification.getAction2IconRes(), "drawable", context.getPackageName());
                    if(tmp_action2_icon!=0){
                        action2_icon = tmp_action2_icon;
                    }
                }
                catch (Exception ignored){}
            }

            Intent action2_intent = new Intent(context, NotificationHandler.class);
            action2_intent.setAction(notification.getId() + "_2");
            //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            action2_intent.putExtra("url", action2_url);
            action2_intent.putExtra("extraData", notification.getExtraData().toString());
            action2_intent.putExtra("notification_id", notification.getId());
            action2_intent.putExtra("campaign", (notification.getTemplateId()>0)?(notification.getTemplate() + " ("+notification.getTemplateId()+")"):"None");
            action2_intent.putExtra("is_action", true);
            action2_intent.putExtra("uid", uid);
            action2_intent.putExtra("clicked_on", 2);
            action2_intent.putExtra("group_id", notification.getGroupId());
            action2_intent.putExtra("action_id", notification.getAction2Id());

            builder.addAction(
                    action2_icon,
                    action2_title,
                    PendingIntent.getActivity(
                            context,
                            notification.getId(),
                            action2_intent,
                            pendingIntentFlag));
        }

        if(action3_title!=null && action3_url!=null){
            int action3_icon = -1;
            if(notification.getAction3IconRes()!=null){
                try {
                    int tmp_action3_icon = context.getResources().getIdentifier(notification.getAction3IconRes(), "drawable", context.getPackageName());
                    if(tmp_action3_icon!=0){
                        action3_icon = tmp_action3_icon;
                    }
                }
                catch (Exception ignored){}
            }

            Intent action3_intent = new Intent(context, NotificationHandler.class);
            action3_intent.setAction(notification.getId() + "_3");
            //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            action3_intent.putExtra("url", action3_url);
            action3_intent.putExtra("extraData", notification.getExtraData().toString());
            action3_intent.putExtra("notification_id", notification.getId());
            action3_intent.putExtra("campaign", (notification.getTemplateId()>0)?(notification.getTemplate() + " ("+notification.getTemplateId()+")"):"None");
            action3_intent.putExtra("is_action", true);
            action3_intent.putExtra("uid", uid);
            action3_intent.putExtra("clicked_on", 3);
            action3_intent.putExtra("group_id", notification.getGroupId());
            action3_intent.putExtra("action_id", notification.getAction3Id());

            builder.addAction(
                    action3_icon,
                    action3_title,
                    PendingIntent.getActivity(
                            context,
                            notification.getId(),
                            action3_intent,
                            pendingIntentFlag));
        }


        int notification_id = notification.getId();
        if(notification.getLocalNotfId()!=-1){
            notification_id = notification.getLocalNotfId();
        }


        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notification.getGroupKey() != null && notification.getChannel() != null) {
                builder.setChannelId(notification.getChannel());
                builder.setGroup(notification.getGroupKey());
            }
        }*/


        if (notification.getGroupKey() != null && notification.getGroupId() != 0) {
            builder.setGroup(notification.getGroupKey());

            NotificationCompat.Builder groupBuilder =
                    new NotificationCompat.Builder(context, channel)
                            .setSmallIcon(small_icon)
                            .setColor(accent_color)
                            .setContentTitle(title)
                            .setContentText(content_text)
                            .setGroupSummary(true)
                            .setAutoCancel(true)
                            .setGroup(notification.getGroupKey());

            Notification notificationFinal = builder.build();
            if(ledColorCheck){
                notificationFinal.flags |= Notification.FLAG_SHOW_LIGHTS;
            }
            notify(context, notificationFinal, NOTIFICATION_TAG, notification_id, groupBuilder, notification.getGroupId());

        } else {
            Notification notificationFinal = builder.build();
            if(ledColorCheck){
                notificationFinal.flags |= Notification.FLAG_SHOW_LIGHTS;
            }
            notify(context, notificationFinal, NOTIFICATION_TAG, notification_id);
        }


    }

    /**
     * To show notification with grouping
     */
    private static void notify(final Context context, final Notification notification, String notificationTag, int notificationID, NotificationCompat.Builder groupBuilder, int group_id) {
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            nm.notify(notificationTag, group_id, groupBuilder.build());
        }
        nm.notify(notificationTag, notificationID, notification);
    }


    /**
     * To show notification without grouping
     */
    private static void notify(final Context context, final Notification notification, String notificationTag, int notificationID) {
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);

        nm.notify(notificationTag, notificationID, notification);
    }

    static void cancel(final Context context, String notificationTag, int notificationID) {
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);

        nm.cancel(notificationTag, notificationID);
    }


    /**
     * Callback the we received the notification
     */
    private static class receiveNotification extends AsyncTask<Void, Void, String> {

        @SuppressLint("StaticFieldLeak")
        Context ctx;
        PANotification notification;
        Bitmap largeIcon = null, largeImage = null;

        receiveNotification(Context context, PANotification notification) {
            super();
            this.ctx = context;
            this.notification = notification;
        }

        @Override
        protected String doInBackground(Void... params) {
            String icon = notification.getIcon();
            if (icon!=null && !icon.equalsIgnoreCase("")) {
                InputStream in;
                try {
                    URL url = new URL(icon);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    in = connection.getInputStream();
                    largeIcon = BitmapFactory.decodeStream(in);

                    connection.disconnect();
                } catch(Exception e){
                    LogM.e("Error getting large icon: " + e.getMessage());
                }
            }

            String image = notification.getImage();
            if(image!=null && !image.equalsIgnoreCase("")) {
                InputStream in;
                try {
                    URL url = new URL(image);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    in = connection.getInputStream();
                    largeImage = BitmapFactory.decodeStream(in);

                    connection.disconnect();
                } catch (Exception e) {
                    LogM.e("Error getting big picture: " + e.getMessage());
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            try {
                MessageNotification.notify(ctx, notification, largeIcon, largeImage, result);
            } catch (Exception e) {
                e.printStackTrace();
                LogM.e("Error while showing notification: "+ e.getMessage());
            }
        }
    }

    private static class receivedNotificationReport extends AsyncTask<Void, Void, String> {
        @SuppressLint("StaticFieldLeak")
        Context ctx;
        PANotification notification;

        receivedNotificationReport(Context context, PANotification notification) {
            super();
            this.ctx = context;
            this.notification = notification;
        }

        @Override
        protected String doInBackground(Void... params) {

            InputStream in;
            try {

                SharedPreferences prefs = Helper.getSharedPreferences(ctx);

                String[] pushalert_info = Helper.getAppId(ctx).split("-");
                String raw_url = "https://androidapi.pushalert.co/deliveredApp.php?" +
                        "user_id=" + pushalert_info[1] +
                        "&domain_id=" + pushalert_info[2] +
                        "&subs_id=" + URLEncoder.encode(prefs.getString(SUBSCRIBER_ID_PREF, null), "UTF-8") +
                        "&sent_time=" + notification.getSentTime() +
                        "&http_user_agent=" + (System.getProperty("http.agent")!=null?URLEncoder.encode(System.getProperty("http.agent"), "UTF-8"):"") +
                        "&notification_id=" + notification.getId() +
                        "&type=" + notification.getType() +
                        "&device=" + (ctx.getResources().getBoolean(R.bool.isTablet)?"tablet":"mobile") +
                        "&os=" + "android" +
                        "&osVer=" + Build.VERSION.RELEASE +
                        "&nref_id=" + notification.getRefId();

                URL url = new URL(raw_url);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                in = connection.getInputStream();

                BufferedReader br = new BufferedReader(new InputStreamReader(
                        in));

                StringBuilder result = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    result.append(line);
                }


                connection.disconnect();

                return null;

            } catch (Exception e) {
                LogM.e("Error in sending receiving report: "+ e.getMessage());
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }
}
