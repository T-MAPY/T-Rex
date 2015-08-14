package cz.tmapy.android.trex;

import android.app.Application;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

/**
 * Created by Kamil on 13. 8. 2015.
 */
@ReportsCrashes(
        mailTo = "kasvo@tmapy.cz",
        mode = ReportingInteractionMode.DIALOG,
        resDialogTitle = R.string.crash_dialog_title,
        resDialogText = R.string.crash_toast_text,
        resDialogIcon = android.R.drawable.ic_dialog_alert,
        customReportContent = {
                ReportField.APP_VERSION_CODE,
                ReportField.APP_VERSION_NAME,
                ReportField.ANDROID_VERSION,
                ReportField.PHONE_MODEL,
                ReportField.CUSTOM_DATA,
                ReportField.STACK_TRACE,
                ReportField.LOGCAT}
)
public class TRexApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }
}
