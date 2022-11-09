package co.pushalert;

import android.util.Log;

/**
 * Handles logging system
 * @author Mohit Kuldeep
 * @since 02-11-2022
 */
class LogM {

    static final String TAG = "PALogs";
    private static boolean ENABLE_DEBUG = false;

    /**
     * To enable or disable logs
     * @param enable the boolean whether to print logs or not.
     */
    static void enableDebug(boolean enable){
        ENABLE_DEBUG = enable;
    }

    /**
     * To print error logs with custom tag
     * @param tag Tag value for the log
     * @param message Message to print in the log
     */
    static void e(String tag, String message){
        if(ENABLE_DEBUG) {
            Log.e(tag, message);
        }
    }

    /**
     * To print error logs with tag "PALogs"
     * @param message Message to print in the log
     */
    static void e(String message){
        try {
            if (ENABLE_DEBUG) {
                Log.e(TAG, message);
            }
        }catch (Exception ignored){

        }
    }

    /**
     * To print debug logs with custom tag
     * @param tag Tag value for the log
     * @param message Message to print in the log
     */
    static void d(String tag, String message){
        if(ENABLE_DEBUG) {
            Log.d(tag, message);
        }
    }

    /**
     * To print debug logs with tag "PALogs"
     * @param message Message to print in the log
     */
    static void d(String message){
        if(ENABLE_DEBUG) {
            Log.d(TAG, message);
        }
    }

    /**
     * To print info logs with custom tag
     * @param tag Tag value for the log
     * @param message Message to print in the log
     */
    static void i(String tag, String message){
        if(ENABLE_DEBUG) {
            Log.i(tag, message);
        }
    }

    /**
     * To print info logs with tag "PALogs"
     * @param message Message to print in the log
     */
    static void i(String message){
        if(ENABLE_DEBUG) {
            Log.i(TAG, message);
        }
    }

    /**
     * To print warning logs with custom tag
     * @param tag Tag value for the log
     * @param message Message to print in the log
     */
    static void w(String tag, String message){
        if(ENABLE_DEBUG) {
            Log.w(tag, message);
        }
    }

    /**
     * To print warning logs with tag "PALogs"
     * @param message Message to print in the log
     */
    static void w(String message){
        if(ENABLE_DEBUG) {
            Log.w(TAG, message);
        }
    }
}
