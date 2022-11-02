package co.pushalert;

/**
 * Notification receiver helper class to set your own handler
 * @author Mohit Kuldeep
 * @since 02-11-2022
 */
public abstract class NotificationReceiver {
    public PANotification paNotification;

    /**
     * Handle any changes you want to do at your app end
     * @return boolean. True: If you handled the notification at your end and don't want to show notification. False: If you want to show notification after the modification.
     */
    public abstract  boolean notificationReceived();

    public final PANotification getPANotification(){
        return paNotification;
    }

    public final void putPANotification(PANotification paNotification){
        this.paNotification = paNotification;
    }
}
