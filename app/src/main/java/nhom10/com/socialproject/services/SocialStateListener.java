/*package nguyenhoangthinh.com.socialproject.services;

public interface SocialStateListener {
    void onMetaChanged(String typeChange, Object object);

    void onNavigateToProFile(String uid);

    void onDarkMode(boolean change);
}*/
package nhom10.com.socialproject.services;

public interface SocialStateListener {
    void onMetaChanged(String type, Object sender);

    void onNavigate(String type, String idType);

    void onDarkMode(boolean change);

    void onRefreshApp();
}
