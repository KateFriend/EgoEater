package com.playposse.egoeater.storage;

import android.os.Parcel;
import android.os.Parcelable;

import com.playposse.egoeater.contentprovider.EgoEaterContract;
import com.playposse.egoeater.contentprovider.EgoEaterContract.MatchTable;
import com.playposse.egoeater.util.SmartCursor;

/**
 * A {@link Parcelable} that represents a match with another profile.
 */
public class MatchParcelable implements Parcelable {

    private int localMatchId;
    private long cloudMatchId;
    private boolean isLocked;
    private boolean hasNewMessage;
    private int unreadMessagesCount;
    private ProfileParcelable otherProfile;

    public static final Parcelable.Creator<MatchParcelable> CREATOR =
            new Creator<MatchParcelable>() {
                @Override
                public MatchParcelable createFromParcel(Parcel source) {
                    return new MatchParcelable(source);
                }

                @Override
                public MatchParcelable[] newArray(int size) {
                    return new MatchParcelable[size];
                }
            };

    public MatchParcelable(SmartCursor smartCursor) {
        localMatchId = smartCursor.getInt(0); // Warning: Multiple ID columns!
        cloudMatchId = smartCursor.getLong(MatchTable.MATCH_ID_COLUMN);
        isLocked = smartCursor.getBoolean(MatchTable.IS_LOCKED_COLUMN);
        hasNewMessage = smartCursor.getBoolean(MatchTable.HAS_NEW_MESSAGE);
        unreadMessagesCount = smartCursor.getInt(MatchTable.UNREAD_MESSAGES_COUNT);
        otherProfile = new ProfileParcelable(smartCursor);
    }

    private MatchParcelable(Parcel in) {
        localMatchId = in.readInt();
        cloudMatchId = in.readLong();
        isLocked = in.readInt() > 0;
        hasNewMessage = in.readInt() > 0;
        unreadMessagesCount = in.readInt();
        otherProfile = ProfileParcelable.CREATOR.createFromParcel(in);
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(localMatchId);
        dest.writeLong(cloudMatchId);
        dest.writeInt(isLocked ? 1 : 0);
        dest.writeInt(hasNewMessage ? 1 : 0);
        dest.writeInt(unreadMessagesCount);
        otherProfile.writeToParcel(dest, flags);
    }

    public int getLocalMatchId() {
        return localMatchId;
    }

    public long getCloudMatchId() {
        return cloudMatchId;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public boolean hasNewMessage() {
        return hasNewMessage;
    }

    public int getUnreadMessagesCount() {
        return unreadMessagesCount;
    }

    public ProfileParcelable getOtherProfile() {
        return otherProfile;
    }
}
