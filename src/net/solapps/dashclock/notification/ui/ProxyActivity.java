package net.solapps.dashclock.notification.ui;

import net.solapps.dashclock.notification.service.DashClockExtensionClient;
import net.solapps.dashclock.notification.service.IModelFactory.IModel;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;

public class ProxyActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DashClockExtensionClient<?> client = DashClockExtensionClient.getInstance();
        IModel model = client.getCurrent();
        if (model != null) {
            boolean sent = false;
            PendingIntent contentIntent = model.contentIntent();
            if (contentIntent != null) {
                try {
                    contentIntent.send();
                    sent = true;
                } catch (CanceledException e) {
                    // pending intent has been cancelled, try launcher intent
                }
            }
            if (!sent) {
                try {
                    startActivity(getLaunchIntent(model));
                } catch (NameNotFoundException e) {
                    // launcher intent did not work quite well, there is nothing we can do about it
                }
            }
        }
        finish();
    }

    private Intent getLaunchIntent(IModel model) throws NameNotFoundException {
        Intent intent = getPackageManager().getLaunchIntentForPackage(model.packageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        return intent;
    }
}