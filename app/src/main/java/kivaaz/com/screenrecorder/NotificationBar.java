package kivaaz.com.screenrecorder;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * Created by Muguntan on 10/6/2017.
 */

public class NotificationBar extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainActivity mainActivity = new MainActivity();
        mainActivity.setRecording(false);
        mainActivity.actionBtnReload();
        finish();
    }
}
