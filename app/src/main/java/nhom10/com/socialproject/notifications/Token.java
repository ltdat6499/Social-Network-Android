package nhom10.com.socialproject.notifications;

public class Token {
    /* Một fcm token, hay thường được gọi là registrationToken..
    ID do máy chủ GCMconnection cấp cho ứng dụng khách cho phép nhận tin nhắn*/
    private String token;

    public Token(){}

    public Token(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
