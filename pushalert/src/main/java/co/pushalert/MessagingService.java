package co.pushalert;


import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.util.Map;

/**
 * Manage cloud messages and determine whether to show notification or not
 * @author Mohit Kuldeep
 * @since 02-11-2022
 */
public class MessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {

            Map<String, String> map = remoteMessage.getData();
            //LogM.d("Data: " + map); //ToDo remove in final version

            String notification_type = map.get("ntype");

            if(notification_type==null || notification_type.compareToIgnoreCase("notification")==0) {
                Helper.processNotification(getApplicationContext(), map, true);
                //notifyMainApp();
            }
            else if(notification_type.compareToIgnoreCase("attributes")==0) {
                try {
                    JSONObject attributes = new JSONObject(map.get("attributes"));
                    PushAlert.addRemoteAttributes(this, attributes);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if(notification_type.compareToIgnoreCase("create_channel")==0) {
                try {
                    JSONObject data = new JSONObject(map.get("data"));
                    PushAlert.createChannel(this, data.getString("channelID"), data.getString("name"), data.getString("desc"), data.getString("importance"), data.getString("ledColor"), data.getString("lockScreenVisibility"),
                            data.getString("badge"), data.getString("sound"), data.getString("vibration_pattern"),  data.getString("group_id"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            /*else if(notification_type.compareToIgnoreCase("edit_channel")==0) {

            }*/
            else if(notification_type.compareToIgnoreCase("delete_channel")==0) {
                try {
                    JSONObject data = new JSONObject(map.get("data"));
                    PushAlert.deleteChannel(this, data.getString("id"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if(notification_type.compareToIgnoreCase("create_group")==0) {
                try {
                    JSONObject data = new JSONObject(map.get("data"));
                    PushAlert.createGroup(this, data.getString("name"), data.getString("id"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if(notification_type.compareToIgnoreCase("delete_group")==0) {
                try {
                    JSONObject data = new JSONObject(map.get("data"));
                    PushAlert.deleteGroup(this, data.getString("id"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if(notification_type.compareToIgnoreCase("update_attribution_time")==0) {
                try {
                    JSONObject data = new JSONObject(map.get("data"));
                    Helper.setAttributionTime(this, data.getLong("attribution_time"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


        }

    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        if(token!=null) {
            if(!PushAlert.tokenAlreadyGenerated) {
                PushAlert.tokenAlreadyGenerated = true;
                PushAlert.checkSubscription(getApplicationContext(), token);
            }

        }
    }
}
