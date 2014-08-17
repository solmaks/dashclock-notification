package net.solapps.dashclock.notification.service;

import java.util.List;

import net.solapps.dashclock.notification.service.IModelFactory.IModel;
import android.app.PendingIntent;
import android.service.notification.StatusBarNotification;

public interface IModelFactory<T extends IModel> {

    public interface IModel {

        String packageName();

        PendingIntent contentIntent();
    }

    T createFrom(List<StatusBarNotification> sbns);
}
