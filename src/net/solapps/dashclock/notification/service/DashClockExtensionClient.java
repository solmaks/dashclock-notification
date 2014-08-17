package net.solapps.dashclock.notification.service;

import java.util.List;

import net.solapps.dashclock.notification.service.IExtensionDataFactory.Error;
import net.solapps.dashclock.notification.service.IModelFactory.IModel;
import net.solapps.dashclock.notification.service.NotificationListenerClient.ICallback;
import android.service.notification.StatusBarNotification;

import com.google.android.apps.dashclock.api.ExtensionData;

public class DashClockExtensionClient<T extends IModel> {

    private static DashClockExtensionClient<?> sInstance;

    private final IModelFactory<T> mModelFactory;
    private final IExtensionDataFactory<T> mExtensionDataFactory;
    private final NotificationListenerClient mNotificationClient;

    private DashClockExtension mService;
    private T mModel;

    public DashClockExtensionClient(NotificationListenerClient notificationClient, IModelFactory<T> modelFactory,
            IExtensionDataFactory<T> extensionDataFactory) {
        mNotificationClient = notificationClient;
        mModelFactory = modelFactory;
        mExtensionDataFactory = extensionDataFactory;
        mNotificationClient.setCallback(new ICallback() {
            @Override
            public void onNotificationsUpdated(List<StatusBarNotification> sbns) {
                if (mService != null) {
                    if (mService.isWorldReadable() || mService.isAuthorizedHost()) {
                        mModel = mModelFactory.createFrom(sbns);
                        ExtensionData data = mExtensionDataFactory.createFrom(mModel);
                        mService.publishData(data);
                    } else {
                        ExtensionData data = mExtensionDataFactory.createFrom(Error.EXTENSION_ACCESS);
                        mService.publishData(data);
                    }
                }
            }

            @Override
            public void onAccessChanged(boolean granted) {
                if (mService != null) {
                    if (granted) {
                        mNotificationClient.requestRefresh();
                        return;
                    }
                    ExtensionData data = mExtensionDataFactory.createFrom(Error.NOTIFICATION_ACCESS);
                    mService.publishData(data);
                }
            }
        });
    }

    public static void init(DashClockExtensionClient<?> client) {
        if (sInstance != null) {
            throw new IllegalStateException();
        }
        sInstance = client;
    }

    public static DashClockExtensionClient<?> getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException();
        }
        return sInstance;
    }

    public IModel getCurrent() {
        return mModel;
    }

    void setService(DashClockExtension service) {
        mService = service;
    }

    void onUpdateRequested() {
        mNotificationClient.requestRefresh();
    }
}