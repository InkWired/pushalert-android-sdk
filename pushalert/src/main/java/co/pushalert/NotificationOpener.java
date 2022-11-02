package co.pushalert;

/**
 * Notification Opener helper interface to set your own handler
 * @author Mohit Kuldeep
 * @since 02-11-2022
 */
public interface NotificationOpener {

    void notificationOpened(PANotificationOpened paNotificationOpened);
}
