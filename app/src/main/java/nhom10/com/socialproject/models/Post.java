package nhom10.com.socialproject.models;

import java.io.Serializable;

public class Post implements Serializable {
    private String pId, pStatus, pImage, pTime, pMode, pLike, pComment, uid;

    public Post() {
    }

    public Post(String pId, String pStatus, String pImage, String pTime, String pMode, String pLike, String pComment, String uid) {
        this.pId = pId;
        this.pStatus = pStatus;
        this.pImage = pImage;
        this.pTime = pTime;
        this.pMode = pMode;
        this.pLike = pLike;
        this.pComment = pComment;
        this.uid = uid;
    }

    public String getpId() {
        return pId;
    }

    public void setpId(String pId) {
        this.pId = pId;
    }

    public String getpStatus() {
        return pStatus;
    }

    public void setpStatus(String pStatus) {
        this.pStatus = pStatus;
    }

    public String getpImage() {
        return pImage;
    }

    public void setpImage(String pImage) {
        this.pImage = pImage;
    }

    public String getpTime() {
        return pTime;
    }

    public void setpTime(String pTime) {
        this.pTime = pTime;
    }

    public String getpMode() {
        return pMode;
    }

    public void setpMode(String pMode) {
        this.pMode = pMode;
    }

    public String getpLike() {
        return pLike;
    }

    public void setpLike(String pLike) {
        this.pLike = pLike;
    }

    public String getpComment() {
        return pComment;
    }

    public void setpComment(String pComment) {
        this.pComment = pComment;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
