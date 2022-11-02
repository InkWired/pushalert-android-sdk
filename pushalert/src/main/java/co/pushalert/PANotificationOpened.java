package co.pushalert;

import org.json.JSONObject;

/**
 * Information returned when notification opened.
 * @author Mohit Kuldeep
 * @since 02-11-2022
 */

public class PANotificationOpened {
    private final int notification_id;
    private final JSONObject extraData;
    private final String url;
    private final String action_id;

    /**
     * Constructor
     * @param notification_id id of the notification
     * @param url url of the notification
     * @param extraData key-value pair of extra data
     * @param action_id action_id as specificed while sending notification, action_id main for main notification
     */
    PANotificationOpened(int notification_id, String url, String action_id, JSONObject extraData){
        this.notification_id = notification_id;
        this.url = url;
        this.extraData = extraData;
        this.action_id = action_id;
    }

    /**
     * To get notification id
     * @return notification id
     */
    public int getNotificationId() {
        return notification_id;
    }

    /**
     * To get key-value pair of extra data
     * @return extra data in json format
     */
    public JSONObject getExtraData() {
        return extraData;
    }

    /**
     * To get url of the notification
     * @return url of the notification
     */
    public String getUrl() {
        return url;
    }

    /**
     * To get action id
     * @return string
     */
    public String getActionId() {
        return action_id;
    }
}
