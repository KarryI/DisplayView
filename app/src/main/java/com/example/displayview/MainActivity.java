package com.example.displayview;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.example.displayview.model.PopupMessage;
import com.example.displayview.model.PopupMessageRequest;
import com.example.displayview.view.PopupStackLayout;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private String TAG = MainActivity.class.getSimpleName();
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private PopupStackLayout mPopupRoot;
    private ViewGroup mHelpLayout;
    private boolean isShow = false;
    private View mRootLayout;
    LayoutInflater layoutInflater;

    private int count = 5;
    String[] mPermissionString={
            Manifest.permission.SYSTEM_ALERT_WINDOW
    };

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0:
                    add();
                    break;
                case 1:
                    dimiss();
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initView();
        checkPermissions();
    }

    private void checkPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(checkSelfPermission(Manifest.permission.SYSTEM_ALERT_WINDOW) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(mPermissionString, 0);
            }
        }
    }

    private void initView() {
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.gravity = Gravity.CENTER;

        layoutParams.flags = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        layoutParams.windowAnimations = R.style.jeejio_popup_anim;
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
    }

    public void add(View view){
        count = 5;
        mHandler.sendEmptyMessage(0);
//        mHandler.sendEmptyMessageDelayed(1,5000);
    }

    public void add() {
        if(!isShow){
            Log.e(TAG,"windowManager.addView");
            layoutInflater = LayoutInflater.from(this);
            mRootLayout = layoutInflater.inflate(R.layout.jeejio_popup_layout, null);
            mPopupRoot = mRootLayout.findViewById(R.id.jeejio_popup_stack);
            mHelpLayout = mRootLayout.findViewById(R.id.jeejio_popup_help_layout);
            mHelpLayout.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
            mRootLayout.findViewById(R.id.jeejio_popup_know_bt).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mHelpLayout.setVisibility(View.GONE);
                }
            });
            mPopupRoot.setOnPageChangeListener(new PopupStackLayout.OnPageChangeListener() {
                @Override
                public void onAllFinish(List<PopupMessageRequest> list) {
                    Log.d(TAG,"onEmpty:"+list.size());
                    windowManager.removeView(mRootLayout);
                    isShow = false;
                    mPopupRoot.removeOnPageChangeListener();
                    mPopupRoot = null;
                    mRootLayout = null;
                }

                @Override
                public void onStart(View view) {
                    Log.d(TAG,"onStart:");
                }

                @Override
                public void onEnd(View view) {
                    Log.d(TAG,"onEnd:");
                }

                @Override
                public void onShowHelp() {
                    mHelpLayout.setVisibility(View.VISIBLE);
                }
            });
            windowManager.addView(mRootLayout,layoutParams);
            isShow = true;
        }else{
            Log.e(TAG,"showing");
        }
        //DataBean dataBean =new DataBean(DataBean.MessageType.VIDEO, "直播 1","rtmp://playflow.jeejio.com/ZBJ_20201125120616/stream_ZBJ_20201125120616?auth_key=1606794683-0-0-73fbb47b3716563fed51344e19b18908");
        //DataBean dataBean1 =new DataBean(DataBean.MessageType.IMAGE, "图片1","http://pic1.win4000.com/pic/a/fc/c8171434244.jpg");

        PopupMessage popupMessage = new PopupMessage.Builder(MainActivity.this)
                .setContentTitle("直播 1")
//                .setContentPath("/sdcard/test.mp4")
//                .setContentPath("https://stream7.iqilu.com/10339/upload_transcode/202002/18/20200218025702PSiVKDB5ap.mp4")
//                .setContentPath("https://qa-video-files.oss-cn-beijing.aliyuncs.com/qa/202001042058/resourcebase_video/2020-09-07/ef44d78e4f7f4451977c995a61a5d24d.mp4")
                .setContentPath("https://qacontentstoreimgs.jeejio.com/qa/202001042058/resourcebase/2020-10-26/5a8a960695cb49d2ba373ebf7db9f0e7.GIF")
                .setContentType(PopupMessage.POPUP_MESSAGE_PICTURE).build();
//                .setContentPath("https://qacontentstoreimgs.jeejio.com/qa/202001042058/resourcebase/2020-08-05/6f242bcd764f43b2b95146a4938b506c.gif")
//                .setContentType(PopupMessage.POPUP_MESSAGE_PICTURE).build();


        mPopupRoot.addData(new PopupMessageRequest(getPackageName(),popupMessage,null,null));
    }

    private void dimiss(){
        windowManager.removeView(mRootLayout);
        isShow = false;
        mPopupRoot.removeOnPageChangeListener();
        mPopupRoot = null;
        mRootLayout = null;
        mHelpLayout = null;
        layoutInflater = null;
    }
}