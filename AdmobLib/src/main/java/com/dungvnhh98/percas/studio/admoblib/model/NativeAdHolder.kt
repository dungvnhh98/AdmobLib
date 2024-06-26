package com.dungvnhh98.percas.studio.admoblib.model

import androidx.lifecycle.MutableLiveData
import com.google.android.gms.ads.nativead.NativeAd

open class NativeAdHolder(var ads: String){
    var nativeAd : NativeAd?= null
    var isLoading = false
    var native_mutable: MutableLiveData<NativeAd> = MutableLiveData()
}