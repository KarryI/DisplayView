package com.example.displayview.model;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.CharacterStyle;
import android.text.style.RelativeSizeSpan;
import android.text.style.TextAppearanceSpan;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

public class PopupMessage implements Parcelable {

    private static final String TAG = PopupMessage.class.getSimpleName();


    public Bundle extras = new Bundle();

    /**
     * @hide
     */
    public int popupId;

    /**
     * Maximum length of CharSequences accepted by Builder and friends.
     */
    private static final int MAX_CHARSEQUENCE_LENGTH = 5 * 1024;

    /**
     * {@link #extras} key: this is the title of the message,
     * as supplied to {@link Builder#setContentTitle(CharSequence)}.
     */
    public static final String EXTRA_TITLE = "popup.message.title";
    /**
     * {@link #extras} key: this is the address of the message,
     * as supplied to {@link Builder#setContentPath(String)}.
     */
    public static final String EXTRA_PATH = "popup.message.path";
    /**
     * {@link #extras} key: this is the type of the message,
     * as supplied to {@link Builder#setContentType(int)}.
     */
    public static final String EXTRA_TYPE = "popup.message.type";
    /**
     * @hide
     */
    public static final String EXTRA_BUILDER_APPLICATION_INFO = "popup.message.appInfo";
    /**
     * Picture type messages
     */
    public static final int POPUP_MESSAGE_PICTURE = 1;
    /**
     * Video type messages
     */
    public static final int POPUP_MESSAGE_VIDEO = 2;
    /**
     * Live type messages
     */
    public static final int POPUP_MESSAGE_LIVE = 3;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            POPUP_MESSAGE_PICTURE,
            POPUP_MESSAGE_VIDEO,
            POPUP_MESSAGE_LIVE,
    })
    public @interface MessageType{}

    public PopupMessage(){

    }

    /**
     * Unflatten the message from a parcel.
     */
    @SuppressWarnings("unchecked")
    public PopupMessage(Parcel parcel) {
        // IMPORTANT: Add unmarshaling code in readFromParcel as the pending
        // intents in extras are always written as the last entry.
        readFromParcelImpl(parcel);
        // Must be read last!

    }

    private void readFromParcelImpl(Parcel parcel){
        //extras = Bundle.setDefusable(parcel.readBundle(), true); // may be null
        extras = parcel.readBundle();
        popupId = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeBundle(extras);
        parcel.writeInt(popupId);
    }

    /**
     * Parcelable.Creator that instantiates Notification objects
     */
    public static final Creator<PopupMessage> CREATOR
            = new Creator<PopupMessage>()
    {
        public PopupMessage createFromParcel(Parcel parcel)
        {
            return new PopupMessage(parcel);
        }

        public PopupMessage[] newArray(int size)
        {
            return new PopupMessage[size];
        }
    };

    /**
     * @hide
     */
    public static void addFieldsFromContext(Context context, PopupMessage popupMessage) {
        addFieldsFromContext(context.getApplicationInfo(), popupMessage);
    }

    /**
     * @hide
     */
    public static void addFieldsFromContext(ApplicationInfo ai, PopupMessage popupMessage) {
        popupMessage.extras.putParcelable(EXTRA_BUILDER_APPLICATION_INFO, ai);
    }

    public static class Builder {
        private Context mContext;
        private PopupMessage mN;
        private Bundle mUserExtras = new Bundle();

        public Builder(Context context) {
            mContext = context;
            mN = new PopupMessage();
        }

        /**
         * Set the message title
         */
        public Builder setContentTitle(CharSequence title) {
            mN.extras.putCharSequence(EXTRA_TITLE, safeCharSequence(title));

            return this;
        }

        /**
         * Set a message path. This is a convenience method for calling {@link #setContentPath(String)}.
         *
         * Equivalent to calling {@link #setContentPath(String) setContentPath(uri.toString())}.
         *
         * @see #setContentPath(String)
         */
        public Builder setContentPath(@NonNull Uri uri) {
            this.setContentPath(uri.toString());
            return this;
        }

        /**
         * Set a message path.
         *
         * @see #setContentPath(Uri)
         */
        public Builder setContentPath(@NonNull String text) {
            mN.extras.putString(EXTRA_PATH, text);
            return this;
        }

        /**
         * Set the message Type
         * @param type the content type values, one of
         *     {@link PopupMessage#POPUP_MESSAGE_PICTURE},
         *     {@link PopupMessage#POPUP_MESSAGE_VIDEO},
         *     {@link PopupMessage#POPUP_MESSAGE_LIVE}.
         */
        public Builder setContentType(@NonNull @MessageType int type) {
            mN.extras.putInt(EXTRA_TYPE, type);
            return this;
        }

        public PopupMessage build() {
            if((mN.extras.getString(EXTRA_PATH) == null)){
                throw new NullPointerException("Path not set, invalid message.(call setContentPath(String path))");
            }
            PopupMessage.addFieldsFromContext(mContext, mN);
            return mN;
        }

    }

    /**
     * Make sure this CharSequence is safe to put into a bundle, which basically
     * means it had better not be some custom Parcelable implementation.
     * @hide
     */
    public static CharSequence safeCharSequence(CharSequence cs) {
        if (cs == null) return cs;
        if (cs.length() > MAX_CHARSEQUENCE_LENGTH) {
            cs = cs.subSequence(0, MAX_CHARSEQUENCE_LENGTH);
        }
        if (cs instanceof Parcelable) {
            Log.e(TAG, "warning: " + cs.getClass().getCanonicalName()
                    + " instance is a custom Parcelable and not allowed in Notification");
            return cs.toString();
        }
        return removeTextSizeSpans(cs);
    }

    private static CharSequence removeTextSizeSpans(CharSequence charSequence) {
        if (charSequence instanceof Spanned) {
            Spanned ss = (Spanned) charSequence;
            Object[] spans = ss.getSpans(0, ss.length(), Object.class);
            SpannableStringBuilder builder = new SpannableStringBuilder(ss.toString());
            for (Object span : spans) {
                Object resultSpan = span;
                if (resultSpan instanceof CharacterStyle) {
                    resultSpan = ((CharacterStyle) span).getUnderlying();
                }
                if (resultSpan instanceof TextAppearanceSpan) {
                    TextAppearanceSpan originalSpan = (TextAppearanceSpan) resultSpan;
                    resultSpan = new TextAppearanceSpan(
                            originalSpan.getFamily(),
                            originalSpan.getTextStyle(),
                            -1,
                            originalSpan.getTextColor(),
                            originalSpan.getLinkTextColor());
                } else if (resultSpan instanceof RelativeSizeSpan
                        || resultSpan instanceof AbsoluteSizeSpan) {
                    continue;
                } else {
                    resultSpan = span;
                }
                builder.setSpan(resultSpan, ss.getSpanStart(span), ss.getSpanEnd(span),
                        ss.getSpanFlags(span));
            }
            return builder;
        }
        return charSequence;
    }

    public CharSequence getContentTitle(){
        return extras.getCharSequence(EXTRA_TITLE);
    }

    public String getContentPath(){
        return extras.getString(EXTRA_PATH);
    }

    public int getContentType(){
        return extras.getInt(EXTRA_TYPE);
    }

    @NonNull
    @Override
    public String toString() {
        return "PopupMessage[ Id: "+ popupId +", Title: "+getContentTitle()+", Path: "+getContentPath()+", Type: "+getContentType()+" ]";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PopupMessage)) return false;
        PopupMessage that = (PopupMessage) obj;
        boolean res = (that.popupId == this.popupId) &&
                (that.getContentType() == this.getContentType());
        if(this.getContentTitle() != null){
            res = res && (this.getContentTitle().equals(that.getContentTitle()));
        }
        if(this.getContentPath() != null){
            res = res && (this.getContentPath().equals(that.getContentPath()));
        }
        return res;
    }

    @Override
    public int hashCode() {
        int code = Objects.hash(popupId, getContentType(), getContentTitle(), getContentPath());
        return code;
    }
}
