package com.example.displayview.view;

import android.content.Context;
import android.graphics.ImageDecoder;
import android.graphics.drawable.AnimatedImageDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.displayview.R;
import com.example.displayview.model.PopupMessage;
import com.example.displayview.model.PopupMessageRequest;
import com.example.displayview.utils.Constants;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import tv.danmaku.ijk.media.player.IMediaPlayer;

public class PopupCardView extends FrameLayout implements Handler.Callback,
        IMediaPlayer.OnErrorListener, IMediaPlayer.OnPreparedListener, IMediaPlayer.OnInfoListener {
    private String TAG = PopupCardView.class.getSimpleName();

    private final int HANDLER_UPDATE_TIME = 0;
    private final int HANDLER_START_SHOW = 1;
    private final int HANDLER_END_SHOW = 2;

    private int mMaxTime = 10; //default 10s
    private int mErrTime = 3; //s
    private int mCurTime = 0;
    private final int TIME_BOUND = 20;
    private ImageView mImageView;
    private IjkVideoView mVideoView;
    private View loadingLayout;
    private ImageView loadingGifView;
    private View errLayout;

    private PopupMessageRequest mDataBean;
    private OnShowListener mShowListener;
    private PopupCardView mView;
    private volatile Boolean onPrepared = false;
    private TextView mTitle;
    private TextView mTimerView;
    private ImageView mTitleIcon;
    private Handler mHandler;
    private Timer mTimer;
    private TimerTask mTimerTask;
    private Context mContext;
    private AnimatedImageDrawable mAnimatedImageDrawable;

    private enum STATUS{
        LOADING,
        ERROR,
        PICTURE_SUCCESS,
        VIDEO_SUCCESS
    }

    public PopupCardView(Context context){
        this(context, null);
    }

    public PopupCardView(Context context, AttributeSet attrs) {
        this(context,  attrs, 0);
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    public PopupCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    public void init(Context context) {
        if(Constants.DEBUG){
            Log.e(TAG,"init");
        }
        mContext = context;
        View root = inflate(context, R.layout.jeejio_popup_view, this);
        mTitle = root.findViewById(R.id.jeejio_popup_title);
        mTimerView = root.findViewById(R.id.jeejio_popup_timer);
        mImageView = root.findViewById(R.id.jeejio_popup_image);
        mVideoView = root.findViewById(R.id.jeejio_popup_video);
        mTitleIcon = root.findViewById(R.id.jeejio_popup_icon);
        loadingLayout = root.findViewById(R.id.jeejio_popup_loading_layout);
        loadingGifView = root.findViewById(R.id.jeejio_popup_loading_gifview);
        errLayout = root.findViewById(R.id.jeejio_popup_error_layout);

        mView = this;
        mHandler = new Handler(this);
        try {
            mAnimatedImageDrawable = (AnimatedImageDrawable) ImageDecoder.decodeDrawable(ImageDecoder.createSource(getResources(),R.drawable.jeejio_popup_loading_pro));
            loadingGifView.setImageDrawable(mAnimatedImageDrawable);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void bind(PopupMessageRequest data) {  //view加载数据
        if (data == null) {
            return;
        }
        mDataBean = data;
        if (mDataBean.getPopupMessage().getContentType() == PopupMessage.POPUP_MESSAGE_PICTURE){
            Log.e(TAG,"图片");
            mTitleIcon.setImageResource(R.drawable.jeejio_popup_picture);
            mTitle.setText(String.format(getResources().getString(R.string.jeejio_popup_str_picture),
                    mDataBean.getPopupMessage().getContentTitle()));
            updateView(STATUS.LOADING);
        } else if (mDataBean.getPopupMessage().getContentType()== PopupMessage.POPUP_MESSAGE_VIDEO){
            Log.e(TAG,"视频");
            mTitleIcon.setImageResource(R.drawable.jeejio_popup_video);
            mTitle.setText(String.format(getResources().getString(R.string.jeejio_popup_str_video),
                    mDataBean.getPopupMessage().getContentTitle()));
            updateView(STATUS.LOADING);
        } else if (mDataBean.getPopupMessage().getContentType() == PopupMessage.POPUP_MESSAGE_LIVE){
            Log.e(TAG,"直播");
            mTitleIcon.setImageResource(R.drawable.jeejio_popup_live);
            mTitle.setText(String.format(getResources().getString(R.string.jeejio_popup_str_live),
                    mDataBean.getPopupMessage().getContentTitle()));
            updateView(STATUS.LOADING);
        }else {
            Log.e(TAG,"未知");
            updateView(STATUS.ERROR);
        }
    }

    public PopupMessageRequest getData() {
        return mDataBean;
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch (msg.what){
            case HANDLER_UPDATE_TIME:
                updateTime();
                if(mCurTime == 0){
                    mHandler.sendEmptyMessage(HANDLER_END_SHOW);
                }
                mCurTime--;
                break;
            case HANDLER_START_SHOW:
                if(mShowListener != null){
                    mShowListener.onStart(this);
                }
                break;
            case HANDLER_END_SHOW:
                if(mShowListener != null){
                    mShowListener.onEnd(this);
                }
                break;
        }
        return false;
    }

    private void updateTime(){
        if(mCurTime < 0){
            return;
        }

        mTimerView.setText(String.format("%ss",mCurTime));
        invalidate();
    }

    public interface OnShowListener {
        void onStart(View view);
        void onEnd(View view);
    }

    public void setOnShowListener(OnShowListener listener) {
        this.mShowListener = listener;
    }

    public void play(){
        if(Constants.DEBUG){
            Log.d(TAG,"begin show");
        }
        mHandler.sendEmptyMessage(HANDLER_START_SHOW);
        String filePath = mDataBean.getPopupMessage().getContentPath();
        Log.d(TAG,"show: "+filePath);
        if(TextUtils.isEmpty(filePath)){
            startTimer(mMaxTime);
            updateView(STATUS.ERROR);
        } else {
            if (mDataBean.getPopupMessage().getContentType() == PopupMessage.POPUP_MESSAGE_VIDEO
                || mDataBean.getPopupMessage().getContentType() == PopupMessage.POPUP_MESSAGE_LIVE) {
                mVideoView.setVideoPath(mDataBean.getPopupMessage().getContentPath());
                mVideoView.setOnErrorListener(this);
                mVideoView.setOnInfoListener(this);
                mVideoView.setOnPreparedListener(this);
            } else if (mDataBean.getPopupMessage().getContentType() == PopupMessage.POPUP_MESSAGE_PICTURE) {
                Glide.with(getContext())
                        .load(filePath)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .listener(new RequestListener<String, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                                Log.e(TAG,"Glide onException: "+e);
                                updateView(STATUS.ERROR);
                                startTimer(mErrTime);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                                Log.e(TAG,"Glide onResourceReady");
                                updateView(STATUS.PICTURE_SUCCESS);
                                startTimer(mMaxTime);
                                return false;
                            }
                        })
                        .into(mImageView);
            } else{
                startTimer(mMaxTime);
                updateView(STATUS.ERROR);
            }
        }
    }

    private void startTimer(int maxTime){
        if(mTimer != null){
            stopTimer();
        }
        mCurTime = maxTime;
        mTimer = new Timer();
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                mHandler.sendEmptyMessage(HANDLER_UPDATE_TIME);
            }
        };
        mTimer.schedule(mTimerTask,0,1000);
        mTimerView.setVisibility(VISIBLE);
    }

    private void stopTimer(){
        if(mTimerTask != null){
            mTimerTask.cancel();
            mTimerTask = null;
        }
        if(mTimer != null){
            mTimer.cancel();
            mTimer = null;
        }
    }

    public void release(){
        stopTimer();
        mHandler.removeMessages(HANDLER_UPDATE_TIME);
        if(mDataBean.getPopupMessage().getContentType() == PopupMessage.POPUP_MESSAGE_VIDEO
            || mDataBean.getPopupMessage().getContentType() == PopupMessage.POPUP_MESSAGE_LIVE) {
            mVideoView.stopPlayback();
        }else if (mDataBean.getPopupMessage().getContentType() == PopupMessage.POPUP_MESSAGE_PICTURE){
            Glide.clear(mImageView);
        }
        if(mAnimatedImageDrawable != null){
            mAnimatedImageDrawable.stop();
        }

    }

    private void updateView(STATUS  status){
        switch (status){
            case LOADING:
                loadingLayout.setVisibility(VISIBLE);
                mImageView.setVisibility(INVISIBLE);
                mVideoView.setVisibility(INVISIBLE);
                errLayout.setVisibility(INVISIBLE);
                if(mAnimatedImageDrawable != null){
                    mAnimatedImageDrawable.start();
                }
                break;
            case ERROR:
                errLayout.setVisibility(VISIBLE);
                mImageView.setVisibility(INVISIBLE);
                mVideoView.setVisibility(INVISIBLE);
                loadingLayout.setVisibility(INVISIBLE);
                if(mAnimatedImageDrawable != null){
                    mAnimatedImageDrawable.stop();
                }
                break;
            case VIDEO_SUCCESS:
                mVideoView.setVisibility(VISIBLE);
                mImageView.setVisibility(INVISIBLE);
                loadingLayout.setVisibility(INVISIBLE);
                errLayout.setVisibility(INVISIBLE);
                if(mAnimatedImageDrawable != null){
                    mAnimatedImageDrawable.stop();
                }
                break;
            case PICTURE_SUCCESS:
                mImageView.setVisibility(VISIBLE);
                mVideoView.setVisibility(INVISIBLE);
                loadingLayout.setVisibility(INVISIBLE);
                errLayout.setVisibility(INVISIBLE);
                if(mAnimatedImageDrawable != null){
                    mAnimatedImageDrawable.stop();
                }
                break;
        }
    }


    @Override
    public boolean onError(IMediaPlayer iMediaPlayer, int i, int i1) {
        Log.e(TAG,"onError");
        updateView(STATUS.ERROR);
        startTimer(mErrTime);
        return true;
    }

    @Override
    public void onPrepared(IMediaPlayer iMediaPlayer) {
        Log.d(TAG,"onPrepared");
        if (mVideoView.canSeekBackward()) {
            int total_time = mVideoView.getDuration() / 1000; //总时长(单位：秒)
            Log.d(TAG,"video_time: "+total_time+"s");
            if (total_time >= TIME_BOUND) {
                mVideoView.seekTo(10 * 1000);//从第10秒开始播放
            }else if((total_time < mMaxTime) && (total_time > 0)){
                mMaxTime = total_time;
            }
        }
        iMediaPlayer.start();
    }

    @Override
    public boolean onInfo(IMediaPlayer iMediaPlayer, int i, int i1) {
        if(i == IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START){
            updateView(STATUS.VIDEO_SUCCESS);
            startTimer(mMaxTime);
        }
        return false;
    }
}
