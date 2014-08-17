package net.solapps.dashclock.notification.service;

import java.util.List;

import android.service.notification.StatusBarNotification;

public class NotificationListenerClient {

    public interface ICallback {

        void onNotificationsUpdated(List<StatusBarNotification> sbns);

        void onAccessChanged(boolean granted);
    }

    private static NotificationListenerClient sInstance;
    private NotificationListenerService mService;
    private ICallback mCallback;

    public static void init(NotificationListenerClient client) {
        if (sInstance != null) {
            throw new IllegalStateException();
        }
        sInstance = client;
    }

    public static NotificationListenerClient instance() {
        if (sInstance == null) {
            throw new IllegalStateException();
        }
        return sInstance;
    }

    public void requestRefresh() {
        if (mService != null) {
            mService.requestRefresh();
        } else {
            mCallback.onAccessChanged(false);
        }
    }

    public void setCallback(ICallback callback) {
        mCallback = callback;
    }

    void setService(NotificationListenerService service) {
        mService = service;
    }

    void onNotificationsUpdated(List<StatusBarNotification> sbns) {
        if (mCallback != null) {
            mCallback.onNotificationsUpdated(sbns);
        }
    }

    void onAccessChanged(boolean granted) {
        if (mCallback != null) {
            mCallback.onAccessChanged(granted);
        }
    }
}