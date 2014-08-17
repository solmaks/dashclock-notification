package net.solapps.dashclock.notification.service;

import com.google.android.apps.dashclock.api.ExtensionData;

public interface IExtensionDataFactory<T> {

    public enum Error {
        NOTIFICATION_ACCESS,
        EXTENSION_ACCESS
    }

    ExtensionData createFrom(T model);

    ExtensionData createFrom(Error error);
}