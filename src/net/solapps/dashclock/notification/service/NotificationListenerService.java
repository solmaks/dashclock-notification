package net.solapps.dashclock.notification.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.service.notification.StatusBarNotification;

public class NotificationListenerService extends android.service.notification.NotificationListenerService implements Callback {

    private static final int MSG_BOUND = 1;
    private static final int MSG_UNBOUND = 2;
    private static final int MSG_POSTED = 3;
    private static final int MSG_REMOVED = 4;

    private boolean mBound;
    private Handler mHandler;
    private NotificationListenerClient mClient;
    private List<StatusBarNotification> mSbns;
    private String mPackage;

    @Override
    public void onCreate() {
        super.onCreate();
        mPackage = getPackageFilter();
        mHandler = new Handler(this);
        mSbns = new ArrayList<StatusBarNotification>(2);
        mClient = NotificationListenerClient.instance();
        mClient.setService(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        IBinder binder = super.onBind(intent);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_BOUND), 500);
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mHandler.removeMessages(MSG_BOUND);
        mHandler.obtainMessage(MSG_UNBOUND).sendToTarget();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        mClient.setService(null);
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (checkPackage(sbn)) {
            mHandler.obtainMessage(MSG_POSTED, sbn).sendToTarget();
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (checkPackage(sbn)) {
            mHandler.obtainMessage(MSG_REMOVED, sbn).sendToTarget();
        }
    }

    @Override
    public StatusBarNotification[] getActiveNotifications() {
        StatusBarNotification[] sbns = super.getActiveNotifications();
        return sbns != null ? sbns : new StatusBarNotification[0];
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
        case MSG_BOUND:
            handleBound();
            return true;
        case MSG_UNBOUND:
            handleUnBound();
            return true;
        case MSG_POSTED:
            handlePosted((StatusBarNotification) msg.obj);
            return true;
        case MSG_REMOVED:
            handleRemoved((StatusBarNotification) msg.obj);
            return true;
        default:
            return false;
        }
    }

    void requestRefresh() {
        if (mBound) {
            mClient.onNotificationsUpdated(mSbns);
        } else {
            mClient.onAccessChanged(false);
        }
    }

    private void handleRemoved(StatusBarNotification sbn) {
        if (mBound) {
            for (Iterator<StatusBarNotification> it = mSbns.iterator(); it.hasNext();) {
                if (sbn.getId() == it.next().getId()) {
                    it.remove();
                    break;
                }
            }
            mClient.onNotificationsUpdated(mSbns);
        }
    }

    private void handlePosted(StatusBarNotification sbn) {
        if (mBound) {
            boolean set = false;
            for (ListIterator<StatusBarNotification> it = mSbns.listIterator(); it.hasNext();) {
                if (sbn.getId() == it.next().getId()) {
                    it.set(sbn);
                    set = true;
                    break;
                }
            }
            if (!set) {
                mSbns.add(sbn);
            }
            mClient.onNotificationsUpdated(mSbns);
        }
    }

    private void handleBound() {
        mBound = true;
        mSbns.clear();
        try {
            StatusBarNotification[] sbns = getActiveNotifications();
            for (StatusBarNotification sbn : sbns) {
                if (checkPackage(sbn)) {
                    mSbns.add(sbn);
                }
            }
        } catch (Exception e) {
            // Ignored
        }
        mClient.onAccessChanged(true);
    }

    private void handleUnBound() {
        mBound = false;
        mSbns.clear();
        mClient.onAccessChanged(false);
    }

    private String getPackageFilter() {
        try {
            ComponentName name = new ComponentName(this, getClass());
            ServiceInfo info = getPackageManager().getServiceInfo(name, PackageManager.GET_META_DATA);
            return info.metaData.getString("package");
        } catch (NameNotFoundException e) {
        }
        throw new IllegalStateException();
    }

    private boolean checkPackage(StatusBarNotification sbn) {
        return mPackage.equals(sbn.getPackageName());
    }
}