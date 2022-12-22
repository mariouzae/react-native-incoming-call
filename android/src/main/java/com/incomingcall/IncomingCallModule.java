package com.incomingcall;

import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.app.Activity;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.WindowManager;
import android.content.Context;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

public class IncomingCallModule extends ReactContextBaseJavaModule {
    public static ReactApplicationContext reactContext;
    public static Activity mainActivity;
    private static final String TAG = "RNIncomingCall:IncomingCallModule";
    private static Vibrator vibrator;
    private static Ringtone ringtone;
    private WritableMap headlessExtras;

    public IncomingCallModule(ReactApplicationContext context) {
        super(context);
        reactContext = context;
        mainActivity = getCurrentActivity();
        vibrator = (Vibrator) reactContext.getSystemService(Context.VIBRATOR_SERVICE);
    }

    private Context getAppContext() {
        return this.reactContext.getApplicationContext();
    }

    @Override
    public String getName() {
        return "IncomingCall";
    }

    @ReactMethod
    public void display(String uuid, String name, String avatar, String info, int timeout, boolean ringer) {
        if (UnlockScreenActivity.active) {
            return;
        }

        if (reactContext != null) {
            Bundle bundle = new Bundle();

            bundle.putString("uuid", uuid);
            bundle.putString("name", name);
            bundle.putString("avatar", avatar);
            bundle.putString("info", info);
            bundle.putInt("timeout", timeout);
            bundle.putBoolean("ringer", ringer);

            Intent i = new Intent(reactContext, UnlockScreenActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            i.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED +
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD +
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            i.putExtras(bundle);

            reactContext.startActivity(i);
        }
    }

    @ReactMethod
    public void dismiss() {
        if (UnlockScreenActivity.active) {
            UnlockScreenActivity.getInstance().dismissIncoming();
        }

        return;
    }

    @ReactMethod
    public void backToForeground() {
        Context context = getAppContext();
        String packageName = context.getApplicationContext().getPackageName();
        Intent focusIntent = context.getPackageManager().getLaunchIntentForPackage(packageName).cloneFilter();
        Activity activity = getCurrentActivity();
        boolean isOpened = activity != null;
        Log.d(TAG, "backToForeground, app isOpened ?" + (isOpened ? "true" : "false"));

        if (isOpened) {
            focusIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            activity.startActivity(focusIntent);
        }
    }

    @ReactMethod
    public void openAppFromHeadlessMode(String uuid) {
        Context context = getAppContext();
        String packageName = context.getApplicationContext().getPackageName();
        Intent focusIntent = context.getPackageManager().getLaunchIntentForPackage(packageName).cloneFilter();
        Activity activity = getCurrentActivity();
        boolean isOpened = activity != null;

        if (!isOpened) {
            focusIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

            final WritableMap response = new WritableNativeMap();
            response.putBoolean("isHeadless", true);
            response.putString("uuid", uuid);

            this.headlessExtras = response;

            getReactApplicationContext().startActivity(focusIntent);
        }
    }

    @ReactMethod
    public void getExtrasFromHeadlessMode(Promise promise) {
        if (this.headlessExtras != null) {
            promise.resolve(this.headlessExtras);

            this.headlessExtras = null;

            return;
        }

        promise.resolve(null);
    }

    @ReactMethod
    public static void startRingtone() {
        long[] pattern = {0, 1000, 800};

        vibrator = (Vibrator) reactContext.getSystemService(Context.VIBRATOR_SERVICE);

        int ringerMode = ((AudioManager) reactContext.getSystemService(Context.AUDIO_SERVICE)).getRingerMode();

        if (ringerMode == AudioManager.RINGER_MODE_SILENT) return;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            VibrationEffect vibe = VibrationEffect.createWaveform(pattern, 0);
            vibrator.vibrate(vibe);
        } else {
            vibrator.vibrate(pattern, 0);
        }

        if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) return;

        ringtone = RingtoneManager.getRingtone(reactContext, RingtoneManager.getActualDefaultRingtoneUri(reactContext, RingtoneManager.TYPE_RINGTONE));

        if (ringtone.isPlaying()) return;

        ringtone.play();
    }

    @ReactMethod
    public static void stopRinging() {
        if (vibrator != null) {
            vibrator.cancel();
        }

        int ringerMode = ((AudioManager) reactContext.getSystemService(Context.AUDIO_SERVICE)).getRingerMode();

        if(ringerMode != AudioManager.RINGER_MODE_NORMAL) return;

        ringtone.stop();
    }
}
