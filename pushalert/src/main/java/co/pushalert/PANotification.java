package co.pushalert;

import org.json.JSONObject;

/**
 * Notification object
 * @author Mohit Kuldeep
 * @since 02-11-2022
 */
public class PANotification {
    private int id, group_id=-1,ref_id;
    private String short_title, content, image, url, icon, channel, sound_res, led_color, accent_color, small_icon_res, group_key, lock_screen_visibility, priority;
    private int local_notf_id=-1, sent_time = 0, template_id=0;
    private String short_title_attr, content_attr, url_attr, template, header_text;

    private String action1_title, action1_url, action1_icon_res, action1_id;
    private String action2_title, action2_url, action2_icon_res, action2_id;
    private String action3_title, action3_url, action3_icon_res, action3_id;
    private String action1_title_attr, action1_url_attr;
    private String action2_title_attr, action2_url_attr;
    private String action3_title_attr, action3_url_attr;
    private int type;
    private JSONObject extraData;


    /**
     * Constructor with all values
     * @param id notification id
     * @param short_title notification short title
     * @param content notification content message
     * @param url notification url
     * @param icon notification large icon
     * @param largeImage notification large image
     * @param sent_time notification actual sent time
     * @param channel notification channel id
     * @param sound_res notification sound resource name
     * @param led_color notification led color in hex.
     * @param lock_screen_visibility notification screen visibility
     * @param accent_color notification accent color in hex
     * @param small_icon_res notification small icon
     * @param group_key notification group key
     * @param group_id notification group id
     * @param local_notf_id notification old id
     * @param priority notification priority
     * @param action1_title notification action 1 title
     * @param action1_url notification action 1 url
     * @param action1_icon_res notification action icon resource name
     * @param action2_title notification action 2 title
     * @param action2_url notification action 2 url
     * @param action3_title notification action 3 title
     * @param action2_icon_res notification action 2 icon resource name
*    * @param action3_url notification action 3 url
*    * @param action3_icon_res notification action 3 icon resource name
     * @param short_title_attr notification short title with attributes
     * @param content_attr notification content message with attributes
     * @param type notification type
     */
    PANotification(String id, String short_title, String content, String url, String icon, String largeImage, String sent_time,
                          String channel, String sound_res, String led_color, String lock_screen_visibility, String accent_color,
                          String small_icon_res, String group_key, String group_id, String local_notf_id, String priority,
                          String action1_title, String action1_url, String action1_icon_res, String action1_id,
                          String action2_title, String action2_url, String action2_icon_res, String action2_id,
                          String action3_title, String action3_url, String action3_icon_res, String action3_id,
                          String short_title_attr, String content_attr, String url_attr,
                          String action1_title_attr, String action1_url_attr,
                          String action2_title_attr, String action2_url_attr,
                          String action3_title_attr, String action3_url_attr,
                          String type, String extraData, String ref_id, String template_id, String template, String header_text){
        this.id = Integer.parseInt(id);
        this.short_title = short_title;
        this.content = content;
        this.url = url;
        this.icon = icon;
        this.image = largeImage;
        this.channel = channel;
        this.sound_res = sound_res;
        this.led_color = led_color;
        this.lock_screen_visibility = lock_screen_visibility;
        this.accent_color = accent_color;
        this.small_icon_res = small_icon_res;
        this.group_key = group_key;
        if(local_notf_id!=null) {
            try{
                this.local_notf_id = Integer.parseInt(local_notf_id);
            }
            catch (Exception ignored){}
        }
        if(group_id!=null) {
            try{
                this.group_id = Integer.parseInt(group_id);
            }
            catch (Exception ignored){}
        }
        this.priority = priority;
        this.action1_title = action1_title;
        this.action1_url = action1_url;
        this.action1_icon_res = action1_icon_res;
        this.action1_id = action1_id;
        this.action2_title = action2_title;
        this.action2_url = action2_url;
        this.action2_icon_res = action2_icon_res;
        this.action2_id = action2_id;
        this.action3_title = action3_title;
        this.action3_url = action3_url;
        this.action3_icon_res = action3_icon_res;
        this.action3_id = action3_id;

        if(sent_time!=null) {
            try{
                this.sent_time = Integer.parseInt(sent_time);
            }
            catch (Exception ignored){}
        }

        this.short_title_attr = short_title_attr;
        this.content_attr = content_attr;
        this.url_attr = url_attr;
        this.action1_title_attr = action1_title_attr;
        this.action1_url_attr = action1_url_attr;
        this.action2_title_attr = action2_title_attr;
        this.action2_url_attr = action2_url_attr;
        this.action3_title_attr = action3_title_attr;
        this.action3_url_attr = action3_url_attr;


        if(type!=null) {
            try{
                this.type = Integer.parseInt(type);
            }
            catch (Exception e){
                this.type = 0;
            }
        }

        try{
            if(extraData!=null) {
                this.extraData = new JSONObject(extraData);
            }
            else{
                this.extraData = new JSONObject();
            }
        }
        catch (Exception e){
            this.extraData = new JSONObject();
        }

        this.ref_id = Integer.parseInt(ref_id);

        if(template_id!=null) {
            try{
                this.template_id = Integer.parseInt(template_id);
                this.template = template;
            }
            catch (Exception ignored){}
        }

        this.header_text = header_text;
    }


    /**
     *
     * @return notification content message
     */
    public String getContent() {
        return content;
    }

    /**
     * To set notification content message
     * @param content notification content message
     */
    public void setContent(String content) {
        this.content = content;
    }

    public String getShortTitle() {
        return short_title;
    }

    public void setShortTitle(String short_title){
        this.short_title = short_title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url){
        this.url = url;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getLocalNotfId() {
        return local_notf_id;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getLockScreenVisibility() {
        return lock_screen_visibility;
    }

    public void setLockScreenVisibility(String lock_screen_visibility) {
        this.lock_screen_visibility = lock_screen_visibility;
    }

    public String getAccentColor() {
        return accent_color;
    }

    public void setAccentColor(String accent_color) {
        this.accent_color = accent_color;
    }

    public String getAction1IconRes() {
        return action1_icon_res;
    }

    public void setAction1IconRes(String action1_icon_res) {
        this.action1_icon_res = action1_icon_res;
    }

    public String getAction1Title() {
        return action1_title;
    }

    public void setAction1Title(String action1_title) {
        this.action1_title = action1_title;
    }

    public String getAction1Url() {
        return action1_url;
    }

    public void setAction1Url(String action1_url) {
        this.action1_url = action1_url;
    }

    public String getAction2Title() {
        return action2_title;
    }

    public void setAction2Title(String action2_title) {
        this.action2_title = action2_title;
    }

    public String getAction2Url() {
        return action2_url;
    }

    public void setAction2Url(String action2_url) {
        this.action2_url = action2_url;
    }

    public String getAction2IconRes() {
        return action2_icon_res;
    }

    public void setAction2IconRes(String action2_icon_res) {
        this.action2_icon_res = action2_icon_res;
    }

    public String getAction3Title() {
        return action3_title;
    }

    public void setAction3Title(String action3_title) {
        this.action3_title = action3_title;
    }

    public String getAction3Url() {
        return action3_url;
    }

    public void setAction3Url(String action3_url) {
        this.action3_url = action3_url;
    }

    public String getAction3IconRes() {
        return action3_icon_res;
    }

    public void setAction3IconRes(String action3_icon_res) {
        this.action3_icon_res = action3_icon_res;
    }


    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getGroupKey() {
        return group_key;
    }

    public void setGroupKey(String group_key) {
        this.group_key = group_key;
    }

    public String getLEDColor() {
        return led_color;
    }

    public void setLEDColor(String led_color) {
        this.led_color = led_color;
    }

    public String getSmallIconRes() { return small_icon_res;}

    public void setSmallIconRes(String small_icon_res) {
        this.small_icon_res = small_icon_res;
    }

    public String getSoundRes() {
        return sound_res;
    }

    public void setSoundRes(String sound_res) {
        this.sound_res = sound_res;
    }

    public int getGroupId() {
        return group_id;
    }

    public void setGroupId(int group_id) {
        this.group_id = group_id;
    }

    public int getSentTime() {
        return sent_time;
    }

    public String getShortTitleAttr() {
        return short_title_attr;
    }

    public void setShortTitleAttr(String short_title_attr) {
        this.short_title_attr = short_title_attr;
    }

    public String getContentAttr() {
        return content_attr;
    }

    public void setContentAttr(String content_attr) {
        this.content_attr = content_attr;
    }

    public String getUrlAttr() {
        return url_attr;
    }

    public void setUrlAttr(String url_attr) {
        this.url_attr = url_attr;
    }

    public String getAction1TitleAttr() {
        return action1_title_attr;
    }

    public void setAction1TitleAttr(String action1_title_attr) {
        this.action1_title_attr = action1_title_attr;
    }

    public String getAction1UrlAttr() {
        return action1_url_attr;
    }

    public void setAction1UrlAttr(String action1_url_attr) {
        this.action1_url_attr = action1_url_attr;
    }

    public String getAction2TitleAttr() {
        return action2_title_attr;
    }

    public void setAction2TitleAttr(String action2_title_attr) {
        this.action2_title_attr = action2_title_attr;
    }

    public String getAction2UrlAttr() {
        return action2_url_attr;
    }

    public void setAction2UrlAttr(String action2_url_attr) {
        this.action2_url_attr = action2_url_attr;
    }

    public String getAction3TitleAttr() {
        return action3_title_attr;
    }

    public void setAction3TitleAttr(String action3_title_attr) {
        this.action3_title_attr = action3_title_attr;
    }

    public String getAction3UrlAttr() {
        return action3_url_attr;
    }

    public void setAction3UrlAttr(String action3_url_attr) {
        this.action3_url_attr = action3_url_attr;
    }

    public int getType(){
        return type;
    }

    public JSONObject getExtraData() {
        return extraData;
    }

    public void setExtraData(JSONObject extraData) {
        this.extraData = extraData;
    }

    public int getRefId() {
        return ref_id;
    }

    public void setRefId(int ref_id) {
        this.ref_id = ref_id;
    }

    public String getTemplate() {
        return template;
    }

    public int getTemplateId() {
        return template_id;
    }

    public String getAction1Id() {
        if(action1_id==null){
            return "button1";
        }
        else {
            return action1_id;
        }
    }

    public String getAction2Id() {
        if(action2_id==null){
            return "button2";
        }
        else {
            return action2_id;
        }
    }

    public String getAction3Id() {
        if(action3_id==null){
            return "button3";
        }
        else {
            return action3_id;
        }
    }

    public String getHeaderText() {
        return header_text;
    }

    public void setHeaderText(String header_text) {
        this.header_text = header_text;
    }
}
