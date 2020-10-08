package nhom10.com.socialproject.models;

import java.io.Serializable;

public class Room implements Serializable {
    private String roomId, user1, user2;

    public Room(){}

    public Room(String roomId, String user1, String user2) {
        this.roomId = roomId;
        this.user1 = user1;
        this.user2 = user2;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getUser1() {
        return user1;
    }

    public void setUser1(String user1) {
        this.user1 = user1;
    }

    public String getUser2() {
        return user2;
    }

    public void setUser2(String user2) {
        this.user2 = user2;
    }
}
