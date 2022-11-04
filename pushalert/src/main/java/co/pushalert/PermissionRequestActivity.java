package co.pushalert;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Optin dialog class, all events relating to users trying to subscribe for Push Notifications are handled here.
 * @author Mohit Kuldeep
 * @since 02-11-2022
 */
public class PermissionRequestActivity extends Activity {
    interface PermissionCallback {
        void onAccept();
        void onReject();
    }
    private static PermissionCallback permissionCallback;
    private boolean openAppNotificationSettings = false;
    private boolean onResumeCalled = false;

    static void registerAsCallback(@NonNull PermissionCallback callback) {
        permissionCallback = callback;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PushAlert.PAOptInMode optInModeEnum = PushAlert.mOptInMode;
        try {
            String optInModeStr = getIntent().getStringExtra("optInModeStr");
            optInModeEnum = PushAlert.PAOptInMode.valueOf(optInModeStr);
        }
        catch (Exception e){
            LogM.e("Empty intent string extra.");
        }

        //LogM.d("OptInMode: " + optInModeEnum.name() + ", mainOptInMode: " + PushAlert.mOptInMode.name()); //Todo Remove

        boolean shouldShowRequestPermission =  Helper.isAndroid13AndAbove() && ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS);
        if (optInModeEnum != PushAlert.PAOptInMode.MANUAL &&
                (optInModeEnum == PushAlert.PAOptInMode.TWO_STEP || shouldShowRequestPermission)) {
            showInMessage();
            if(shouldShowRequestPermission) {
                openAppNotificationSettings = true;
            }
        }
        else{
            if(optInModeEnum == PushAlert.PAOptInMode.MANUAL){
                if(!(Helper.isAndroid13AndAbove() && !shouldShowRequestPermission)) {
                    openAppNotificationSettings = true;
                }
            }
            pushAlertRequestPermission(this);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {

        if (requestCode == PushAlert.PUSHALERT_PERMISSION_REQUEST_POST_NOTIFICATIONS) {
            //LogM.d("PushAlertLogs", "Permission Status - " + (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)); //ToDo Remove
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                if (permissionCallback != null) {
                    permissionCallback.onAccept();
                }
            }
            else{
                PushAlert.setUserPrivacyConsent(false);
            }
            finish();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void pushAlertRequestPermission(Activity activity){
        if(openAppNotificationSettings){
            PushAlert.resumedFromAppNotificationSettings = true;
            LogM.i("Launching native notification settings");
            Helper.openAppNativeNotificationSettings(activity);
        }
        else if(Helper.isAndroid13AndAbove()){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, PushAlert.PUSHALERT_PERMISSION_REQUEST_POST_NOTIFICATIONS);
        }
        openAppNotificationSettings = false;
    }

    public void showInMessage(){
        LayoutInflater factory = LayoutInflater.from(this);
        final View permissionPromptView = factory.inflate(R.layout.pushalert_optin_layout, null);
        final AlertDialog permissionDialog = new AlertDialog.Builder(this, R.style.ThemeAlertDialog).create();
        permissionDialog.setView(permissionPromptView);
        permissionDialog.setCancelable(false);

        TwoStepHelper twoStepHelper = PushAlert.getTwoStepCustomization();
        if(twoStepHelper!=null){
            twoStepHelper.setValues();

            try {
                permissionPromptView.setBackgroundColor(getCompatColor(twoStepHelper.getBgColor()));

                TextView tvTitle = permissionPromptView.findViewById(R.id.title);
                if (twoStepHelper.getTitle() != null) {
                    tvTitle.setText(twoStepHelper.getTitle());
                }
                tvTitle.setTextColor(getCompatColor(twoStepHelper.getTitleTextColor()));

                TextView tvSubTitle = permissionPromptView.findViewById(R.id.subtitle);
                if (twoStepHelper.getSubTitle() != null) {
                    tvSubTitle.setText(twoStepHelper.getSubTitle());
                }
                tvSubTitle.setTextColor(getCompatColor(twoStepHelper.getSubTitleTextColor()));

                TextView acceptBtnText = permissionPromptView.findViewById(R.id.yesText);
                if (twoStepHelper.getAcceptBtn() != null) {
                    acceptBtnText.setText(twoStepHelper.getAcceptBtn());
                }
                acceptBtnText.setTextColor(getCompatColor(twoStepHelper.getAcceptBtnTextColor()));
                LinearLayout acceptBtn = permissionPromptView.findViewById(R.id.yes);
                ((GradientDrawable) acceptBtn.getBackground()).setColor(getCompatColor(twoStepHelper.getAcceptBtnBgColor()));

                TextView rejectBtnText = permissionPromptView.findViewById(R.id.noText);
                if (twoStepHelper.getRejectBtn() != null) {
                    rejectBtnText.setText(twoStepHelper.getRejectBtn());
                }
                rejectBtnText.setTextColor(getCompatColor(twoStepHelper.getRejectBtnTextColor()));
                LinearLayout rejectBtn = permissionPromptView.findViewById(R.id.no);
                ((GradientDrawable) rejectBtn.getBackground()).setColor(getCompatColor(twoStepHelper.getRejectBtnBgColor()));

                ((ImageView) permissionPromptView.findViewById(R.id.icon)).setImageResource(twoStepHelper.getIconDrawable());
                ((ImageView) permissionPromptView.findViewById(R.id.image)).setImageResource(twoStepHelper.getImageDrawable());
            }
            catch (Exception e){
                LogM.e("There is some issue with two step customization.");
            }
        }

        permissionPromptView.findViewById(R.id.yes).setOnClickListener(v -> {
            if (!PushAlert.getOSNotificationPermissionState(this)) {
                if(!Helper.isAndroid13AndAbove()) {
                    openAppNotificationSettings = true;
                }
                pushAlertRequestPermission(PermissionRequestActivity.this);
                permissionDialog.dismiss();
            } else {
                permissionDialog.dismiss();
                if (permissionCallback != null) {
                    permissionCallback.onAccept();
                }
                finish();
            }
        });

        permissionPromptView.findViewById(R.id.no).setOnClickListener(v -> {
            PushAlert.setUserPrivacyConsent(false);
            permissionDialog.dismiss();
            if (permissionCallback != null) {
                permissionCallback.onReject();
            }
            finish();
        });

        permissionDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*LogM.d("onResumedCalled: " + onResumeCalled + ", PushAlert.resumedFromAppNotificationSettings: " +
                PushAlert.resumedFromAppNotificationSettings + " && PushAlert.getOSNotificationPermissionState(): " +
                PushAlert.getOSNotificationPermissionState(this));*/ //ToDo Remove

        if(!onResumeCalled){
            onResumeCalled = true;
            return;
        }

        if (PushAlert.resumedFromAppNotificationSettings) {
            if (PushAlert.getOSNotificationPermissionState(this)) {
                if (permissionCallback != null) {
                    permissionCallback.onAccept();
                }
            }
            PushAlert.resumedFromAppNotificationSettings = false;
            finish();
        }
    }

    public int getCompatColor(int color){
        return ContextCompat.getColor(this, color);
    }
}
