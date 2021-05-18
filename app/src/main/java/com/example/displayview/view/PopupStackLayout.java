package com.example.displayview.view;

import android.animation.Animator;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.example.displayview.R;
import com.example.displayview.model.PopupMessageRequest;
import com.example.displayview.utils.Constants;
import com.example.displayview.utils.DensityUtil;

import java.util.ArrayList;
import java.util.List;

public class PopupStackLayout extends FrameLayout implements View.OnTouchListener,
        PopupCardView.OnShowListener, Handler.Callback {

    private String TAG = PopupStackLayout.class.getSimpleName();
    private ViewGroup.LayoutParams cardParams = null;
    private LayoutParams helpViewParams = null;
    public static final int DURATIONTIME = 300;
    private static final int STACK_SIZE = 2;
    private int index = 0;
    private PopupCardView tc;

    private List<PopupMessageRequest> mList = new ArrayList<>();
    private List<PopupMessageRequest> mAlreadyShows = new ArrayList<>();

    private float downX;
    private float downY;
    private float newX;
    private float newY;
    private float dX;
    private float dY;
    private float downViewX;
    private float downViewY;
    private float newViewX;
    private float newViewY;
    private float leftBoundary;
    private float topBoundary;
    private int screenWidth;
    private int screenHeight;
    private boolean moveParent = false;
    private boolean moveX = false;
    private boolean moveY = false;
    private volatile boolean isOnTouch;
    private Handler mHandler;
    private final long END_DELAY_TIME = 1000;
    private final int EVENT_DELAY_END = 0;
    private final int EVENT_DISPATCH_SELECTED = 1;
    private final int EVENT_DISPATCH_EMPTY = 2;
    private final int EVENT_DISPATCH_START = 3;
    private final int EVENT_DISPATCH_END = 4;
    private PopupStackLayout mLayout;

    public Object cardLock = new Object();

    private OnPageChangeListener mListener;
    private volatile boolean isEmpty = true;

    public PopupStackLayout(Context context) {
        this(context, null);
    }

    public PopupStackLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PopupStackLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context) {
        mHandler = new Handler(this);
        mLayout = this;
        screenWidth = DensityUtil.getScreenWidth(context);
        screenHeight = DensityUtil.getScreenHeight(context);

        cardParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        leftBoundary = screenWidth / 3;
        topBoundary = screenHeight / 5;

        int help_width = getResources().getDimensionPixelSize(R.dimen.jeejio_popup_help_width);
        int help_height = getResources().getDimensionPixelSize(R.dimen.jeejio_popup_help_height);
        helpViewParams = new LayoutParams(help_width,help_height, Gravity.TOP | Gravity.END);
        helpViewParams.rightMargin = getResources().getDimensionPixelSize(R.dimen.jeejio_popup_help_right);
        helpViewParams.topMargin = getResources().getDimensionPixelSize(R.dimen.jeejio_popup_help_top);
        addHelpView();
    }

    private void addHelpView(){
        if(Constants.DEBUG){
            Log.d(TAG,"addHelpView");
        }
        ImageView imageView = new ImageView(getContext());
        imageView.setImageResource(R.drawable.jeejio_popup_help);
        addView(imageView,-1,helpViewParams);
        imageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener != null){
                    mListener.onShowHelp();
                }
            }
        });
    }

    private void addCard(final PopupMessageRequest dataBean) {
        if(Constants.DEBUG){
            Log.e(TAG,"addCard");
        }
        if(getChildCount() == 0){
            addHelpView();
        }
        tc = new PopupCardView(getContext());
        tc.bind(dataBean);
        tc.setOnTouchListener(this);
        tc.setOnShowListener(this);
        addView(tc, 0, cardParams);
        isEmpty = false;
    }

    public void setDatas(List<PopupMessageRequest> list) {
        if (list == null) {
            return;
        }
        synchronized (mList){
            this.mList.addAll(list);
        }
        for (int i = index; index < i + STACK_SIZE; index++) {
            addCard(mList.get(index));
        }
    }

    public void addData(PopupMessageRequest dataBean){
        mList.add(dataBean);
        if(getCardViewCount() < STACK_SIZE){
            addCard(dataBean);
            index++;
        }
    }


    public int getTopCardIndex(){
        return getChildCount() - 2;
    }

    public int getCardViewCount(){
        return getChildCount() - 1;//减去help view
    }

    public void onLoad() {
        for (int i = index; index < i + (STACK_SIZE - 1); index++) {
            if (index == mList.size()) {
                return;
            }
            addCard(mList.get(index));
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mHandler.sendMessage(mHandler.obtainMessage(EVENT_DISPATCH_SELECTED,getChildAt(getTopCardIndex())));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mList.clear();
        isOnTouch = false;
        this.clearAnimation();
        index = 0;
        this.removeAllViews();
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public boolean onTouch(final View view, MotionEvent motionEvent) {
        PopupStackLayout popupStackLayout = ((PopupStackLayout) view.getParent());
        View topCard = popupStackLayout.getChildAt(popupStackLayout.getTopCardIndex());
        if (topCard.equals(view)) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isOnTouch = true;
                    downX = motionEvent.getX();
                    downY = motionEvent.getY();
                    downViewX = view.getX();
                    downViewY = view.getY();
                    view.clearAnimation();
                    if(Constants.DEBUG){
                        Log.d(TAG, "onTouch: downX:"+downX);
                    }
                    return true;
                case MotionEvent.ACTION_MOVE:
                    newX = motionEvent.getX();
                    newY = motionEvent.getY();
                    newViewX = view.getX();
                    newViewY = view.getY();
                    dX = newX - downX; //手指移动距离
                    dY = newY - downY;
                    if(dY == dX && dY == 0){
                        return true;
                    }
                    if(mLayout.getCardViewCount() > 1){
                        if(Math.abs(dY) > Math.abs(dX)){
                            if(newViewX <= downViewX){
                                if(moveX){
                                    float posX = newViewX + dX;
                                    view.setX(posX > downViewX ? downViewX : posX);
                                }else{
                                    moveY = true;
                                    moveParent = true;
                                    float posY = ((PopupStackLayout) view.getParent()).getY() + dY;
                                    ((PopupStackLayout) view.getParent()).setY(posY > downViewY ? downViewY : posY);
                                }
                            }
                        }else {
                            if(newViewX <= downViewX) {
                                if (moveY) {
                                    float posY = ((PopupStackLayout) view.getParent()).getY() + dY;
                                    ((PopupStackLayout) view.getParent()).setY(posY > downViewY ? downViewY : posY);
                                } else {
                                    moveX = true;
                                    float posX = newViewX + dX;
                                    view.setX(posX > downViewX ? downViewX : posX);
                                }
                            }
                        }
                    }else{
                        moveParent = true;
                        if(Math.abs(dY) > Math.abs(dX)){
                            if(newViewY <= downViewY){//只允许上滑
                                if(moveX){
                                    float posX = ((PopupStackLayout) view.getParent()).getX() + dX;
                                    ((PopupStackLayout) view.getParent()).setX(posX > downViewX ? downViewX : posX);
                                }else{
                                    moveY = true;
                                    float posY = ((PopupStackLayout) view.getParent()).getY() + dY;
                                    ((PopupStackLayout) view.getParent()).setY(posY > downViewY ? downViewY : posY);
                                }
                            }
                        }else{
                            if(newViewY <= downViewY){
                                if(moveY){
                                    float posY = ((PopupStackLayout) view.getParent()).getY() + dY;
                                    ((PopupStackLayout) view.getParent()).setY(posY > downViewY ? downViewY : posY);
                                }else{
                                    moveX = true;
                                    float posX = ((PopupStackLayout) view.getParent()).getX() + dX;
                                    ((PopupStackLayout) view.getParent()).setX(posX > downViewX ? downViewX : posX);
                                }
                            }
                        }
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if(moveParent){
                        if(moveX){
                            if (isBeyondLeftBoundary(((PopupStackLayout) view.getParent()))) {
                                mHandler.removeMessages(EVENT_DELAY_END);
                                removeCardWithAnim(((PopupStackLayout) view.getParent()), -(screenWidth * 2), 0, true);
                            } else {
                                resetCard(((PopupStackLayout) view.getParent()));
                            }
                        }else{
                            if (isBeyondTopBoundary(((PopupStackLayout) view.getParent()))) {
                                mHandler.removeMessages(EVENT_DELAY_END);
                                removeCardWithAnim(((PopupStackLayout) view.getParent()), 0, -(screenHeight * 2), true);
                            } else {
                                resetCard(((PopupStackLayout) view.getParent()));
                            }
                        }

                    }else{
                        if (isBeyondLeftBoundary(view)) {
                            mHandler.removeMessages(EVENT_DELAY_END);
                            removeCardWithAnim(view, -(screenWidth * 2), 0,false);
                        } else {
                            resetCard(view);
                        }
                    }
                    isOnTouch = false;
                    moveParent = false;
                    moveX = false;
                    moveY = false;
                    return true;
                default:
                    return super.onTouchEvent(motionEvent);
            }
        }
        return super.onTouchEvent(motionEvent);
    }

    private boolean isBeyondLeftBoundary(View view) {
        return (view.getX() + (view.getWidth() / 2) < leftBoundary);
    }

    private boolean isBeyondTopBoundary(View view) {
        return (view.getY() + (view.getHeight() / 2) < topBoundary);
    }

    private void removeCardWithAnim(final View view, int xPos, int yPos, final boolean isParent) {
        if(Constants.DEBUG){
            Log.d(TAG,"removeCard: isParent="+isParent);
        }
        if(isParent){
            isEmpty = true;
        }
        view.animate()
                .x(xPos) //x轴移动距离
                .y(yPos) //y轴移动距离
                .setInterpolator(new AccelerateInterpolator())  //插值器   在动画开始的地方速率改变比较慢，然后开始加速
                .setDuration(DURATIONTIME) //移动距离
                .setListener(new Animator.AnimatorListener() { //监听
                    @Override
                    public void onAnimationStart(Animator animator) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {//移出后回调
                        if(isParent){
                            releaseAll(view);
                        }else{
                            releaseLastAndShowNext(view);
                        }

                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {

                    }
                });
    }


    private void resetCard(final View view) {
        view.animate()
                .x(0) //x轴移动
                .y(0) //y轴移动
                .rotation(0) //循环次数
                .setInterpolator(new OvershootInterpolator()) //插值器, 向前甩一定值后再回到原来位置
                .setDuration(DURATIONTIME);
    }

    private void releaseLastAndShowNext(View view){
        ViewGroup viewGroup = (ViewGroup) view.getParent();
        if (viewGroup != null) {
            ((PopupCardView)view).release();
            mHandler.sendMessage(mHandler.obtainMessage(EVENT_DISPATCH_END, view));
            viewGroup.removeView(view);
            if(getCardViewCount()== 0){
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_DISPATCH_EMPTY, view));
                return;
            }else{
                if(viewGroup instanceof PopupStackLayout){
                    int count = ((PopupStackLayout)viewGroup).getCardViewCount();
                    if (count < STACK_SIZE ) {
                        onLoad();
                    }
                }
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_DISPATCH_SELECTED,getChildAt(getTopCardIndex())));
            }
        }
    }

    private void releaseAll(View view){
        if(view instanceof PopupStackLayout){
            View topCardView = ((PopupStackLayout)view).getChildAt(((PopupStackLayout)view).getTopCardIndex());
            if(topCardView instanceof PopupCardView){
                ((PopupCardView)topCardView).release();
            }
            mHandler.sendMessage(mHandler.obtainMessage(EVENT_DISPATCH_END, topCardView));
        }
        mHandler.sendMessage(mHandler.obtainMessage(EVENT_DISPATCH_EMPTY));
    }

    @Override
    public void onStart(View view) {
        mHandler.sendMessage(mHandler.obtainMessage(EVENT_DISPATCH_START,view));
    }

    @Override
    public void onEnd(View view) {
        if(isOnTouch){
            mHandler.sendMessageDelayed(mHandler.obtainMessage(EVENT_DELAY_END,view), END_DELAY_TIME);
        }else{
            synchronized (cardLock) {
                if (getCardViewCount() == 1) {
                    removeCardWithAnim((View) view.getParent(), -(screenWidth * 2), 0, true);
                } else {
                    removeCardWithAnim(view, -(screenWidth * 2), 0, false);
                }
            }
        }
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch (msg.what){
            case EVENT_DELAY_END:
                onEnd((View) msg.obj);
                break;
            case EVENT_DISPATCH_SELECTED:
                dispatchOnPageSelected((View) msg.obj);
                break;
            case EVENT_DISPATCH_EMPTY:
                dispatchOnEmpty();
                break;
            case EVENT_DISPATCH_START:
                dispatchOnStart((View) msg.obj);
                break;
            case EVENT_DISPATCH_END:
                dispatchOnEnd((View) msg.obj);
                break;
        }
        return false;
    }

    private void dispatchOnPageSelected(View view) {
        if(view != null){
            if(view instanceof PopupCardView){
                ((PopupCardView) view).play();
            }
        }
    }

    private void dispatchOnEmpty(){
        if (mListener != null) {
            for(PopupMessageRequest data: mAlreadyShows){
                mList.remove(data);
            }
            mListener.onAllFinish(mList);

        }
    }

    private void dispatchOnStart(View view){
        if(view != null){
            if (mListener != null) {
                mListener.onStart(view);
            }
            mAlreadyShows.add((((PopupCardView) view).getData()));
        }
    }

    private void dispatchOnEnd(View view){
        if(view != null) {
            if (mListener != null) {
                mListener.onEnd(view);
            }
        }
    }

    /**
     * Callback interface for responding to changing state of the selected page.
     */
    public interface OnPageChangeListener {
        /**
         * @param unShowList the not shown page.
         */
        void onAllFinish(List<PopupMessageRequest> unShowList);

        void onStart(View view);

        void onEnd(View view);

        void onShowHelp();
    }

    /**
     * Set a listener that will be invoked whenever the page changes or is incrementally
     * scrolled. See {@link OnPageChangeListener}.
     * @param listener listener to add
     */
    public void setOnPageChangeListener(@NonNull OnPageChangeListener listener) {
        if (mListener != null) {
            return;
        }
        mListener = listener;
    }

    /**
     * Remove a listener that was previously added via
     * {@link #setOnPageChangeListener(OnPageChangeListener)}.
     */
    public void removeOnPageChangeListener() {
        mListener = null;
    }

    public boolean isEmpty(){
        return isEmpty;
    }

    public void cancel(){
        releaseAll(mLayout);
    }
}
