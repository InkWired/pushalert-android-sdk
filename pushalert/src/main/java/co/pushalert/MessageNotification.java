package co.pushalert;

import static co.pushalert.PushAlert.NOTIFICATION_CHANNEL;
import static co.pushalert.PushAlert.SUBSCRIBER_ID_PREF;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

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
        Helper.connectWithPushAlert("get", new ConnectionHelper() {
            @Override
            public JSONObject getJSONParams() {
                return null;
            }

            @Override
            public String getUrl() {
                try {
                    SharedPreferences prefs = Helper.getSharedPreferences(context);

                    String[] pushalert_info = Helper.getAppId(context).split("-");
                    String raw_url = "https://androidapi.pushalert.co/deliveredApp.php?" +
                            "user_id=" + pushalert_info[1] +
                            "&domain_id=" + pushalert_info[2] +
                            "&subs_id=" + URLEncoder.encode(prefs.getString(SUBSCRIBER_ID_PREF, null), StandardCharsets.UTF_8.name()) +
                            "&sent_time=" + notification.getSentTime() +
                            "&delivered_time=" + (int)(System.currentTimeMillis() / 1000L) +
                            "&http_user_agent=" + (System.getProperty("http.agent")!=null?URLEncoder.encode(System.getProperty("http.agent"), StandardCharsets.UTF_8.name()):"") +
                            "&notification_id=" + notification.getId() +
                            "&type=" + notification.getType() +
                            "&device=" + (context.getResources().getBoolean(R.bool.isTablet)?"tablet":"mobile") +
                            "&os=" + "android" +
                            "&osVer=" + Build.VERSION.RELEASE +
                            "&nref_id=" + notification.getRefId();

                    if (!Helper.isNetworkAvailable(context)) {
                        Helper.addPendingTask(context, "receivedReport", raw_url);
                        return null;
                    }

                    return raw_url;
                } catch (Exception e) {
                    LogM.e("Error in sending receiving report: "+ e.getMessage());
                }

                return null;
            }

            @Override
            public void postResult(JSONObject result) {

            }

            @Override
            public void onFailure(String message) {
                Helper.addPendingTask(context, "receivedReport", message);
            }
        }, false);
    }

    /**
     * Shows the notification, or updates a previously shown notification of
     * @see #cancel(Context, String, int)
     */
    static void notifyInit(final Context context,
                                  PANotification notification){
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {

                Bitmap largeIcon = null, largeImage = null;

                String icon = notification.getIcon();
                if (icon!=null && icon.toLowerCase().startsWith("https://")) {
                    largeIcon = Helper.getBitmapFromURL(context, icon);
                }

                String image = notification.getImage();
                if(image!=null && image.toLowerCase().startsWith("https://")) {
                    largeImage = Helper.getBitmapFromURL(context, image);
                }

                MessageNotification.notify(context, notification, largeIcon, largeImage, null);

            }
        });
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager=
                (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null && notificationManager.getNotificationChannel(channel)==null){
                channel = NOTIFICATION_CHANNEL;
            }
        }

        Map<String, String> attr_text = new HashMap<>();
        Map<String, String> no_attr_text = new HashMap<>();
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
            JSONObject extraData = notification.getExtraData();
            if(extraData.has("ls_id")){
                try {
                    String product_alert_data = sharedPrefs.getString(PushAlert.PRODUCT_ALERT_DATA + extraData.getString("ls_id"), "{}");

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

        }


        String content_text = ticker;

        Intent intent;
        intent = new Intent(context, NotificationHandler.class);

        //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("url", url);
        intent.putExtra("extraData", notification.getExtraData().toString());

        intent.putExtra("notification_id", notification.getId());
        intent.putExtra("campaign", (notification.getCampaignId()>0)?(notification.getCampaign() + " ("+notification.getCampaignId()+")"):"None");
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


        int template_id = notification.getTemplateId();
        boolean customNotification = template_id>0;
        RemoteViews collapsedView = null, expandedView = null;
        if(customNotification){
            if(template_id==2) {
                collapsedView = new RemoteViews(context.getPackageName(), R.layout.notification_template2);
                expandedView = new RemoteViews(context.getPackageName(), R.layout.notification_template2_expand);
            }
            else{
                collapsedView = new RemoteViews(context.getPackageName(), R.layout.notification_template1);
                expandedView = new RemoteViews(context.getPackageName(), R.layout.notification_template1_expand);

                if(Build.VERSION.SDK_INT<31){
                    builder.setShowWhen(false);
                    expandedView.setViewVisibility(R.id.notification_large_icon, View.VISIBLE);
                    expandedView.setViewVisibility(R.id.notification_time, View.VISIBLE);
                }
            }

            collapsedView.setTextViewText(R.id.notification_message, content_text);
            expandedView.setTextViewText(R.id.notification_message, content_text);


            Date date = new Date(notification.getSentTime()*1000L);
            //DateFormat dateFormat = new SimpleDateFormat("hh:mm a");
            DateFormat dateFormat = SimpleDateFormat
                    .getTimeInstance(SimpleDateFormat.SHORT);
            collapsedView.setTextViewText(R.id.notification_time, dateFormat.format(date));
            expandedView.setTextViewText(R.id.notification_time, dateFormat.format(date));
        }

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

        int accent_color = ContextCompat.getColor(context, PushAlert.getDefaultAccentColor(context));
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
            largeIcon = Helper.getRoundedCornerBitmap(largeIcon, 10);
            if(customNotification){
                collapsedView.setImageViewBitmap(R.id.notification_large_icon, largeIcon);
                expandedView.setImageViewBitmap(R.id.notification_large_icon, largeIcon);
            }
            else{
                builder.setLargeIcon(largeIcon);
            }
        }
        else{
            String largeIconRes = notification.getIcon();
            if(largeIconRes!=null && !largeIconRes.equals("")){
                try {
                    int tmp_large_icon = context.getResources().getIdentifier(largeIconRes, "drawable", context.getPackageName());
                    Bitmap b = Helper.getBitmap(context, tmp_large_icon);
                    if(customNotification){
                        b = Helper.getRoundedCornerBitmap(b, 10);
                        collapsedView.setImageViewBitmap(R.id.notification_large_icon, b);
                        expandedView.setImageViewBitmap(R.id.notification_large_icon, b);
                    }
                    else{

                        builder.setLargeIcon(b);
                    }
                }
                catch (Exception e){
                    LogM.e("Error while getting large icon res: " + e.getMessage());
                }
            }
            else{

                if(customNotification) {
                    collapsedView.setViewVisibility(R.id.notification_large_icon, View.GONE);
                    expandedView.setViewVisibility(R.id.notification_large_icon, View.GONE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (template_id == 1) {
                            collapsedView.setViewLayoutMargin(R.id.notification_message, RemoteViews.MARGIN_END, 0, TypedValue.COMPLEX_UNIT_DIP);
                        }
                    }
                }

            }
        }

        if(largeImage!=null){
            if(customNotification) {
                collapsedView.setImageViewBitmap(R.id.notification_large_image, largeImage);
                expandedView.setImageViewBitmap(R.id.notification_large_image, largeImage);
            }
            else{
                builder.setStyle(
                        new NotificationCompat.BigPictureStyle().bigPicture(largeImage)
                                .setSummaryText(content_text)
                                .setBigContentTitle(title)
                );
            }
        }
        else{
            String largeImageRes = notification.getImage();
            if(largeImageRes!=null && !largeImageRes.equals("")){
                try {
                    int tmp_large_image = context.getResources().getIdentifier(largeImageRes, "drawable", context.getPackageName());

                    if(customNotification) {
                        collapsedView.setImageViewResource(R.id.notification_large_image, tmp_large_image);
                        expandedView.setImageViewResource(R.id.notification_large_image, tmp_large_image);
                    }
                    else{
                        Bitmap b = Helper.getBitmap(context, tmp_large_image);

                        builder.setStyle(
                                new NotificationCompat.BigPictureStyle().bigPicture(b)
                                        .setSummaryText(content_text)
                                        .setBigContentTitle(title)
                        );
                    }
                }
                catch (Exception e){
                    builder.setStyle(new NotificationCompat.BigTextStyle().bigText(content_text));
                    LogM.e("Error while getting large image res: " + e.getMessage());
                }
            }
            else {
                builder.setStyle(new NotificationCompat.BigTextStyle().bigText(content_text));
            }
        }

        int priority = NotificationCompat.PRIORITY_DEFAULT;
        if(notification.getPriority()!=null){
            String importance = notification.getPriority();
            if(importance.compareToIgnoreCase("urgent")==0){
                priority = NotificationCompat.PRIORITY_MAX;
            }
            else if(importance.compareToIgnoreCase("high")==0){
                priority = NotificationCompat.PRIORITY_HIGH;
            }
            else if(importance.compareToIgnoreCase("medium")==0){
                priority = NotificationCompat.PRIORITY_DEFAULT;
            }
            else if(importance.compareToIgnoreCase("low")==0){
                priority = NotificationCompat.PRIORITY_LOW;
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
            action1_intent.putExtra("campaign", (notification.getCampaignId()>0)?(notification.getCampaign() + " ("+notification.getCampaignId()+")"):"None");
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
            action2_intent.putExtra("campaign", (notification.getCampaignId()>0)?(notification.getCampaign() + " ("+notification.getCampaignId()+")"):"None");
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
            action3_intent.putExtra("campaign", (notification.getCampaignId()>0)?(notification.getCampaign() + " ("+notification.getCampaignId()+")"):"None");
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

        if(customNotification){
            if (Build.VERSION.SDK_INT >= 31) {
                builder.setStyle(new NotificationCompat.DecoratedCustomViewStyle());
            }

            builder.setCustomContentView(collapsedView);
            builder.setCustomBigContentView(expandedView);
            builder.setCustomHeadsUpContentView(collapsedView);
        }


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
    @SuppressLint("MissingPermission")
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
    @SuppressLint("MissingPermission")
    private static void notify(final Context context, final Notification notification, String notificationTag, int notificationID) {
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);

        nm.notify(notificationTag, notificationID, notification);
    }

    static void cancel(final Context context, String notificationTag, int notificationID) {
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);

        nm.cancel(notificationTag, notificationID);
    }
}
