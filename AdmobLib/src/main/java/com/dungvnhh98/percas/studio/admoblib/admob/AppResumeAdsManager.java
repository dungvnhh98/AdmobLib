package com.dungvnhh98.percas.studio.admoblib.admob;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.dungvnhh98.percas.studio.admoblib.R;
import com.google.android.gms.ads.AdActivity;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class AppResumeAdsManager implements Application.ActivityLifecycleCallbacks, LifecycleObserver {
    private static final String TAG = "TAG === ADS ON RESUME";
    private static final String AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921";
    @SuppressLint("StaticFieldLeak")
    private static volatile AppResumeAdsManager instance;

    private AppOpenAd appOpenAd = null;
    private boolean isLoadingAd = false;
    boolean isShowingAd = false;
    private Activity currentActivity;
    private Application mApplication;
    private AdRequest mAdRequest;
    private String adsOnResumeId;
    private long loadTime = 0;
    private final List<Class> disableResumeList;
    private Dialog dialogFullScreen;
    private boolean isInitialized = false;
    public boolean isAppResumeEnabled =true;

    public AppResumeAdsManager() {
        disableResumeList = new ArrayList<>();
    }

    public static synchronized AppResumeAdsManager getInstance() {
        if (instance == null) {
            instance = new AppResumeAdsManager();
        }
        return instance;
    }

    public void init(Application application, String appOnresmeAdsId) {
        this.mApplication = application;
        this.mAdRequest = new AdRequest.Builder().setHttpTimeoutMillis(5000).build();
        this.adsOnResumeId = AdmobManager.INSTANCE.isTestAdmob() ? AD_UNIT_ID : appOnresmeAdsId;
        this.mApplication.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        if (!isAdAvailable()) {
            loadAd(this.mApplication);
        }
        isInitialized = true;
    }
    public boolean isInitialized() {
        return isInitialized;
    }
    private void loadAd(Context context) {
        //check trạng thái trước khi load ads
        if (isLoadingAd || isAdAvailable()) {
            return;
        }
        isLoadingAd = true;
        AppOpenAd.load(
                context, adsOnResumeId, mAdRequest,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd ad) {
                        // khi load thành công ads
                        Log.d(TAG, "Ad was loaded.");
                        appOpenAd = ad;
                        isLoadingAd = false;

                        loadTime = new Date().getTime();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // khi load fail
                        Log.d(TAG, loadAdError.getMessage());
                        isLoadingAd = false;
                    }
                });
    }

    private boolean wasLoadTimeLessThanNHoursAgo(long loadTime) {
        long dateDifference = (new Date()).getTime() - loadTime;
        long numMilliSecondsPerHour = 3600000;
        return (dateDifference < (numMilliSecondsPerHour * (long) 4));
    }

    private boolean isAdAvailable() {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(loadTime);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    protected void onMoveToForeground() {
        // Show the ad (if available) when the app moves to foreground.
        Log.d(TAG, "onresume");
        if (currentActivity == null) {
            return;
        }
        if (AdmobManager.INSTANCE.isOverlayAdShowing()) {
            return;
        }
        if (!AdmobManager.INSTANCE.isEnableAd()) {
            return;
        }
        if (!isAppResumeEnabled) {
            Log.d(TAG, "isAppResumeEnabled");
            return;
        }
//        if (AdmobManager.INSTANCE.dialog != null && AdmobManager.INSTANCE.dialog.isShowing()) AdmobManager.INSTANCE.dialog.dismiss();

        for (Class activity : disableResumeList) {
            if (activity.getName().equals(currentActivity.getClass().getName())) {
                Log.d(TAG, "onStart: activity is disabled");
                return;
            }
        }
        showAdIfAvailable();
    }

    public void showAdIfAvailable() {
        if (!ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            Log.d("===Onresume", "STARTED");
            return;
        }
        Log.d("===Onresume", "FullScreenContentCallback");
        if (!isShowingAd && isAdAvailable()) {
            FullScreenContentCallback callback =
                    new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            isLoadingAd = false;
                            Log.d(TAG, "onAdShowedFullScreenContent: Dismiss");
                            try {
                                dialogFullScreen.dismiss();
                                dialogFullScreen = null;
                            } catch (Exception ignored) {
                            }
                            appOpenAd = null;
                            isShowingAd = false;
                            loadAd(mApplication);
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                            isLoadingAd = false;
                            Log.d(TAG, "onAdShowedFullScreenContent: Show false");
                            try {
                                dialogFullScreen.dismiss();
                                dialogFullScreen = null;
                            } catch (Exception ignored) {

                            }
                            loadAd(mApplication);
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            Log.d(TAG, "onAdShowedFullScreenContent: Show");
                            isShowingAd = true;
                            appOpenAd = null;
                        }
                    };
            showAdsResume(callback);

        } else {
            Log.d(TAG, "Ad is not ready");
            loadAd(mApplication);
        }
    }

    private void showAdsResume(final FullScreenContentCallback callback) {
        if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            new Handler().postDelayed(() -> {
                if (appOpenAd != null) {
                    appOpenAd.setFullScreenContentCallback(callback);
                    if (currentActivity != null) {
                        showDialog(currentActivity);
                        appOpenAd.show(currentActivity);
                    }
                }
            }, 100);
        }
    }

    public void showDialog(Context context) {
        dialogFullScreen = new Dialog(context);
        dialogFullScreen.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogFullScreen.setContentView(R.layout.dialog_full_screen_onresume);
        dialogFullScreen.setCancelable(false);
        Objects.requireNonNull(dialogFullScreen.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        dialogFullScreen.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        try {
            if (!currentActivity.isFinishing() && dialogFullScreen != null && !dialogFullScreen.isShowing()) {
                dialogFullScreen.show();
            }
        } catch (Exception ignored) {
        }
    }

    public void disableAppResumeWithActivity(Class activityClass) {
        Log.d(TAG, "disableAppResumeWithActivity: " + activityClass.getName());
        disableResumeList.add(activityClass);
    }

    public void enableAppResumeWithActivity(Class activityClass) {
        Log.d(TAG, "enableAppResumeWithActivity: " + activityClass.getName());
        disableResumeList.remove(activityClass);
    }
    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        Log.d("===ADS", activity.getClass() + "|" + AdActivity.class);
        if (activity.getClass() == AdActivity.class) {
            Log.d("===ADS", "Back");
            return;
        }
        currentActivity = activity;
        Log.d("===ADS", "Running");
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        if (activity.getClass() == AdActivity.class) {
            return;
        }
        if (!isShowingAd) {
            currentActivity = activity;
        }
        if (!activity.getClass().getName().equals(AdActivity.class.getName())) {
            loadAd(currentActivity);
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (activity.getClass() == AdActivity.class){
            return;
        }
        currentActivity = null;
        if (dialogFullScreen != null && dialogFullScreen.isShowing()){
            dialogFullScreen.dismiss();
        }
    }
}
