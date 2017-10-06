package kivaaz.com.screenrecorder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE = 1000;
    private static final int RECORD_AUDIO = 0;
    private int ScreenDensity;
    Button Start;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private static final int DISPLAY_WIDTH  = 720;
    private static final int DISPLAY_HEIGHT  = 1280;
    private VirtualDisplay virtualDisplay;
    private MediaProjectionCallback mediaProjCallback;

    public boolean isRecording() {
        return isRecording;
    }

    public void setRecording(boolean recording) {
        isRecording = recording;
    }

    private boolean isRecording = false;
    private MediaRecorder mediaRecorder;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_PERMISSION_KEY = 1;

    NotificationManager notificationManager ;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0,90);
        ORIENTATIONS.append(Surface.ROTATION_90,0);
        ORIENTATIONS.append(Surface.ROTATION_180,270);
        ORIENTATIONS.append(Surface.ROTATION_270,180);
    }

    @SuppressLint("NewApi")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE){
            Log.e(TAG,"UNKNOWN REQUEST CODE: "+ requestCode);
            return;
        }
        if (resultCode != RESULT_OK){
            Toast.makeText(this,"SCREEN CAST PERMSISSON DENIED",Toast.LENGTH_SHORT).show();
            isRecording = false;
            actionBtnReload();
            return;
        }

        mediaProjCallback = new MediaProjectionCallback();
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode,data);
        mediaProjection.registerCallback(mediaProjCallback,null);
        virtualDisplay = createVirtualDisplay();
        mediaRecorder.start();
        isRecording = true;
        actionBtnReload();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_PERMISSION_KEY:{
                if(grantResults.length > 0 && (grantResults[0]+grantResults[1]+grantResults[3]) == PackageManager.PERMISSION_GRANTED){
                    onToggleScreenShare();
                }else{
                    isRecording = false;
                    actionBtnReload();
                    Snackbar.make(findViewById(android.R.id.content),"ENABLE MICROPHONE AND STORAGE PERMISSIONS",Snackbar.LENGTH_LONG).setAction("ENABLE", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.addCategory(Intent.CATEGORY_DEFAULT);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                            startActivity(intent);
                        }
                    }).show();
                }
                return;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO};

        if(!Function.hasPermissions(getBaseContext(),PERMISSIONS)){
            ActivityCompat.requestPermissions(this,PERMISSIONS,REQUEST_PERMISSION_KEY);
        }

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        ScreenDensity = metrics.densityDpi;

        mediaRecorder = new MediaRecorder();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        Start = (Button) findViewById(R.id.start);
        Start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onToggleScreenShare();
            }
        });
    }

    private void onToggleScreenShare() {
        if(!isRecording){
            initRecorder();
            shareScreen();
        }else {
            mediaRecorder.stop();
            mediaRecorder.reset();
            stopScreenSharing();
        }
    }

    private void initRecorder() {
        try {

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setOutputFile(Environment.getExternalStorageDirectory() + "/video.mp4");
            mediaRecorder.setVideoSize(DISPLAY_WIDTH,DISPLAY_HEIGHT);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setVideoEncodingBitRate(512*1000);
            mediaRecorder.setVideoFrameRate(30);
            mediaRecorder.setVideoEncodingBitRate(3000000);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = ORIENTATIONS.get(rotation + 90);
            mediaRecorder.setOrientationHint(orientation);
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("NewApi")
    private void shareScreen() {
        if(mediaProjection == null){
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),REQUEST_CODE);
            return;
        }
        virtualDisplay = createVirtualDisplay();
        mediaRecorder.start();
        isRecording = true;
        actionBtnReload();
    }

    @SuppressLint("NewApi")
    private VirtualDisplay createVirtualDisplay() {
        return mediaProjection.createVirtualDisplay(TAG,DISPLAY_WIDTH,DISPLAY_HEIGHT,ScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,mediaRecorder.getSurface(),null,null);
    }


    @SuppressLint("NewApi")
    private class MediaProjectionCallback extends MediaProjection.Callback{

        @Override
        public void onStop() {

            if (isRecording){
                isRecording = false;
                actionBtnReload();
                mediaRecorder.stop();
                mediaRecorder.reset();


            }
            
            mediaProjection = null;
            stopScreenSharing();
        }
    }

    @SuppressLint("NewApi")
    private void stopScreenSharing() {
        if(virtualDisplay == null)
            return;
        virtualDisplay.release();
        destroyMediaProjection();
        isRecording = false;
        actionBtnReload();
    }

    @SuppressLint("NewApi")
    private void destroyMediaProjection() {
        if(mediaProjection != null){
            mediaProjection.unregisterCallback(mediaProjCallback);
            mediaProjection.stop();
            mediaProjection = null;
        }
        Log.i(TAG,"MEDIA PROJECTION STOPPED");
    }

    public void actionBtnReload() {
        if(isRecording){
            Start.setText("Stop");

        }else{
            Start.setText("Start");
            Snackbar.make(findViewById(android.R.id.content),"Wanna See the Recorded video ?",Snackbar.LENGTH_LONG).setAction("YES", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent vdoPlayer = new Intent(Intent.ACTION_VIEW);
                    vdoPlayer.setDataAndType(Uri.parse(Environment.getExternalStorageDirectory() + "/video.mp4"),"video/*");
                    startActivity(Intent.createChooser(vdoPlayer,"Choose a Video Player"));
                }
            }).show();
        }
        
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyMediaProjection();
    }

    @Override
    public void onBackPressed() {
        if (isRecording){
            Snackbar.make(findViewById(android.R.id.content),"STOP RECORDING AND EXIT ?",Snackbar.LENGTH_LONG).setAction("STOP", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mediaRecorder.stop();
                    mediaRecorder.reset();
                    Log.v(TAG,"RECORDING STOPPED");
                    stopScreenSharing();
                    finish();
                }
            }).show();
        }else
            finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        CreateNotification();

    }



    public void CreateNotification(){
        Intent NotifyIntent = new Intent(this,MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,(int) System.currentTimeMillis(),NotifyIntent,0);

        Notification noti = new NotificationCompat.Builder(this).setContentTitle("RECORDING")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.stop)
                .build();

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        noti.flags |= Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify(0,noti);
    }
}
