package net.solapps.dashclock.notification.service;

import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.os.Binder;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.preference.PreferenceManager;

import com.google.android.apps.dashclock.api.ExtensionData;

public class DashClockExtension extends com.google.android.apps.dashclock.api.DashClockExtension implements Callback,
        OnSharedPreferenceChangeListener {

    private static final String WORLD_READABLE_SETTING_KEY = "worldReadableSettingKey";

    private static final int MSG_INITIALIZED = 1;
    private static final int MSG_UPDATE_REQUESTED = 2;

    private volatile int mHostUid;

    private Handler mHandler;
    private SharedPreferences mPrefs;
    private String mWorldReadableSettingKey;
    private DashClockExtensionClient<?> mClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(this);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        mClient = DashClockExtensionClient.getInstance();
    }

    @Override
    public void onDestroy() {
        mClient.setService(null);
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    protected void onInitialize(boolean isReconnect) {
        mHostUid = Binder.getCallingUid();
        mHandler.obtainMessage(MSG_INITIALIZED).sendToTarget();
    }

    @Override
    protected void onUpdateData(int reason) {
        mHandler.obtainMessage(MSG_UPDATE_REQUESTED).sendToTarget();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
        case MSG_INITIALIZED:
            mClient.setService(this);
            return true;
        case MSG_UPDATE_REQUESTED:
            mClient.onUpdateRequested();
            return true;
        default:
            return false;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        mClient.onUpdateRequested();
    }

    void publishData(ExtensionData data) {
        publishUpdate(data);
    }

    boolean isWorldReadable() {
        if (mWorldReadableSettingKey == null) {
            String key = null;
            PackageManager pm = getPackageManager();
            try {
                ServiceInfo si = pm.getServiceInfo(new ComponentName(this, getClass()), PackageManager.GET_META_DATA);
                if (si != null && si.metaData != null) {
                    key = si.metaData.getString(WORLD_READABLE_SETTING_KEY);
                }
            } catch (NameNotFoundException ignored) {
            }

            if (key == null) {
                throw new IllegalStateException("Required service meta-data not found");
            }
            mWorldReadableSettingKey = key;
        }

        return mPrefs.getBoolean(mWorldReadableSettingKey, false);
    }

    boolean isAuthorizedHost() {
        PackageManager pm = getPackageManager();
        String[] packages = pm.getPackagesForUid(mHostUid);
        if (packages != null && packages.length > 0) {
            try {
                PackageInfo pi = pm.getPackageInfo(packages[0], PackageManager.GET_SIGNATURES);
                if (pi.signatures != null && pi.signatures.length == 1 && DASHCLOCK_SIGNATURE.equals(pi.signatures[0])) {
                    return true;
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return false;
    }

    /**
     * The signature of the official DashClock app (net.nurik.roman.dashclock).
     */
    private static final Signature DASHCLOCK_SIGNATURE = new Signature(""
            + "308203523082023aa00302010202044c1132a9300d06092a864886f70d0101050500306b310b30090603"
            + "550406130255533110300e06035504081307556e6b6e6f776e3110300e06035504071307556e6b6e6f77"
            + "6e3110300e060355040a1307556e6b6e6f776e3110300e060355040b1307556e6b6e6f776e3114301206"
            + "03550403130b526f6d616e204e7572696b301e170d3130303631303138343435375a170d333731303236"
            + "3138343435375a306b310b30090603550406130255533110300e06035504081307556e6b6e6f776e3110"
            + "300e06035504071307556e6b6e6f776e3110300e060355040a1307556e6b6e6f776e3110300e06035504"
            + "0b1307556e6b6e6f776e311430120603550403130b526f6d616e204e7572696b30820122300d06092a86"
            + "4886f70d01010105000382010f003082010a02820101008906222723a4b30dca6f0702b041e6f361e38e"
            + "35105ec530bf43f4f1786737fefe6ccfa3b038a3700ea685dd185112a0a8f96327d3373de28e05859a87"
            + "bde82372baed5618082121d6946e4affbdfb6771abb782147d58a2323518b34efcce144ec3e45fb2556e"
            + "ba1c40b42ccbcc1266c9469b5447edf09d5cf8e2ed62cfb3bd902e47f48a11a815a635c3879c882eae92"
            + "3c7f73bfba4039b7c19930617e3326fa163b924eda398bacc0d6ef8643a32223ce1d767734e866553ad5"
            + "0d11fb22ac3a15ba021a6a3904a95ed65f54142256cb0db90038dd55adfeeb18d3ffb085c4380817268f"
            + "039119ecbdfca843e4b82209947fd88470b3d8c76fc15878fbc4f10203010001300d06092a864886f70d"
            + "0101050500038201010047063efdd5011adb69cca6461a57443fef59243f85e5727ec0d67513bb04b650"
            + "b1144fc1f54e09789c278171c52b9305a7265cafc13b89d91eb37ddce34a5c1f17c8c36f86c957c4e9ca"
            + "cc19e6822e0a5711f2cfba2c5913ba582ab69485548b13072bc736310b9da85a716d0418e6449450ceda"
            + "dfc1c897f93ed6189cfa0a02b893125bd4b1c4e4dd50c1ad33e221120b8488841763a3361817081e7691"
            + "1e76d3adcf94b23c758ceb955f9fdf8ef4a8351fc279867a25729f081b511209e96dfa8520225b810072"
            + "de5e8eefc1a6cc22f46857e2cc4fd1a1eaac76054f34352b63c9d53691515b42cc771f195343e61397cb"
            + "7b04ada2a627410d29c214976d13");
}