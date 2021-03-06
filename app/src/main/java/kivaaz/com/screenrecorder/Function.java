package kivaaz.com.screenrecorder;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

/**
 * Created by Muguntan on 10/6/2017.
 */

public class Function {
    public static boolean hasPermissions(Context context, String ... permissions){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null){
            for (String permission : permissions){
                if (ActivityCompat.checkSelfPermission(context,permission) != PackageManager.PERMISSION_GRANTED){
                    return false;
                }
            }
        }
        return true;
    }
}
