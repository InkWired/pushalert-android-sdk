package co.pushalert;

/**
 * Helper class to customize optin dialog
 * @author Mohit Kuldeep
 * @since 02-11-2022
 */
public abstract class TwoStepHelper {
    protected String title, subTitle, acceptBtn, rejectBtn;

    protected  int
            bgColor = R.color.pa_optin_bg_color,
            titleTextColor = R.color.pa_optin_title_color,
            subTitleTextColor = R.color.pa_optin_subtitle_color,
            acceptBtnTextColor = R.color.pa_optin_accept_btn_color,
            acceptBtnBgColor = R.color.pa_optin_accept_btn_bg_color,
            rejectBtnTextColor = R.color.pa_optin_reject_btn_color,
            rejectBtnBgColor = R.color.pa_optin_reject_btn_bg_color;

    protected int iconDrawable = R.drawable.pushalert_get_notified,
            imageDrawable = R.drawable.pushalert_notifications_preview;

    public abstract void setValues();

    public int getBgColor() {
        return bgColor;
    }

    public String getTitle() {
        return title;
    }

    public int getTitleTextColor() {
        return titleTextColor;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public int getSubTitleTextColor() {
        return subTitleTextColor;
    }

    public String getAcceptBtn() {
        return acceptBtn;
    }

    public int getAcceptBtnTextColor() {
        return acceptBtnTextColor;
    }

    public int getAcceptBtnBgColor() {
        return acceptBtnBgColor;
    }

    public String getRejectBtn() {
        return rejectBtn;
    }

    public int getRejectBtnTextColor() {
        return rejectBtnTextColor;
    }

    public int getRejectBtnBgColor() {
        return rejectBtnBgColor;
    }

    public int getIconDrawable() {
        return iconDrawable;
    }

    public int getImageDrawable() {
        return imageDrawable;
    }
}
