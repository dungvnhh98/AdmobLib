package com.dungvnhh98.percas.studio.admoblib.model

import androidx.lifecycle.MutableLiveData
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd

open class RewardInterHolder(var ads: String) {
    var rewardInterAd: RewardedInterstitialAd? = null
    val mutable: MutableLiveData<RewardedInterstitialAd> = MutableLiveData(null)
    var isLoading = false
}