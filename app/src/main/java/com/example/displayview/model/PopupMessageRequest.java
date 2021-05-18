package com.example.displayview.model;

import android.os.Binder;
import android.os.IBinder;
import android.os.Messenger;

public class PopupMessageRequest{
    private PopupMessage mPopupMessage;
    private Messenger mMessenger;
    private IBinder mBinder;
    private String mPackageName;

    public PopupMessageRequest(String pkgName, PopupMessage popupMessage, Messenger messenger, IBinder binder){
        this.mPackageName = pkgName;
        this.mPopupMessage = popupMessage;
        this.mMessenger  = messenger;
        this.mBinder = binder;
    }


    public PopupMessage getPopupMessage() {
        return mPopupMessage;
    }

    public void setPopupMessage(PopupMessage mPopupMessage) {
        this.mPopupMessage = mPopupMessage;
    }

    public Messenger getMessenger() {
        return mMessenger;
    }

    public void setMessenger(Messenger mMessenger) {
        this.mMessenger = mMessenger;
    }

    public IBinder getBinder() {
        return mBinder;
    }

    public void setBinder(Binder mBinder) {
        this.mBinder = mBinder;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public void setPackageName(String mPackageName) {
        this.mPackageName = mPackageName;
    }

}
