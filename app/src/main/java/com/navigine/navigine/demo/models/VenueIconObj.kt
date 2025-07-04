package com.navigine.navigine.demo.models

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import java.util.Objects

class VenueIconObj : Parcelable {
    var activated: Boolean = false
    var imageDrawable: Int = -1
        private set
    var categoryName: String? = null
        private set
    @JvmField
    var isActivated: Boolean = false


    constructor(imageDrawable: Int, categoryName: String?) {
        this.imageDrawable = imageDrawable
        this.categoryName = categoryName
    }

    protected constructor(`in`: Parcel) {
        imageDrawable = `in`.readInt()
        categoryName = `in`.readString()
        isActivated = `in`.readByte().toInt() != 0
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val venueIconObj = o as VenueIconObj
        return categoryName == venueIconObj.categoryName
    }

    override fun hashCode(): Int {
        return Objects.hash(categoryName)
    }

    override fun toString(): String {
        return "VenueIconObj{" +
                "categoryName='" + categoryName + '\'' +
                ", isActivated=" + isActivated +
                '}'
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(imageDrawable)
        dest.writeString(categoryName)
        dest.writeByte((if (isActivated) 1 else 0).toByte())
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @JvmField
        val CREATOR: Creator<VenueIconObj?> = object : Creator<VenueIconObj?> {
            override fun createFromParcel(`in`: Parcel): VenueIconObj {
                return VenueIconObj(`in`)
            }

            override fun newArray(size: Int): Array<VenueIconObj?> {
                return arrayOfNulls<VenueIconObj>(size)
            }
        }
    }
}
