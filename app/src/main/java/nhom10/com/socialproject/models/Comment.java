package nhom10.com.socialproject.models;

import java.io.Serializable;

public class Comment implements Serializable {
    private String pId, cContent, cTime, uid;

    public Comment(){ }

    public Comment(String pId, String cContent, String cTime, String uid) {
        this.pId = pId;
        this.cContent = cContent;
        this.cTime = cTime;
        this.uid = uid;
    }

    public String getpId() {
        return pId;
    }

    public void setpId(String pId) {
        this.pId = pId;
    }

    public String getcContent() {
        return cContent;
    }

    public void setcContent(String cContent) {
        this.cContent = cContent;
    }

    public String getcTime() {
        return cTime;
    }

    public void setcTime(String cTime) {
        this.cTime = cTime;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
