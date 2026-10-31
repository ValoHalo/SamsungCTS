/* Copyright (c) 2021 RikkaW. MIT License; see assets/SHIZUKU-LICENSE.txt.
 * Adapted from Shizuku-API provider: keep the wire-format class name. */
package moe.shizuku.api;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Keep;

@Keep
public final class BinderContainer implements Parcelable {
    public IBinder binder;
    public BinderContainer(IBinder binder) { this.binder = binder; }
    private BinderContainer(Parcel source) { binder = source.readStrongBinder(); }
    @Override public int describeContents() { return 0; }
    @Override public void writeToParcel(Parcel dest, int flags) { dest.writeStrongBinder(binder); }
    public static final Creator<BinderContainer> CREATOR = new Creator<BinderContainer>() {
        @Override public BinderContainer createFromParcel(Parcel source) { return new BinderContainer(source); }
        @Override public BinderContainer[] newArray(int size) { return new BinderContainer[size]; }
    };
}
