// START CUSTOM IPC MONITOR
package com.android.internal.util;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;

import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Shared singleton helper for IPC Monitoring (ContentProvider & Services).
 * Safe for Zygote and SystemServer usage.
 */
public class IpcMonitorHelper {

    private static final String TAG = "IPC_MONITOR";
    private static final String MONITOR_PKG = "com.example.ipcmonitorclient";
    private static final String MONITOR_ACTION = "com.custom.aosp.IPC_MONITOR_EVENT";

    private static IpcMonitorHelper sInstance;
    private static final Object sLock = new Object();

    private HandlerThread mThread;
    private Handler mHandler;
    
    // Settings Cache
    private volatile boolean mEnabled = false;
    private volatile String mTargetPackages = "*";
    private boolean mObserverRegistered = false;

    // Recursion Guard (для защиты от StackOverflow при чтении Settings)
    private final ThreadLocal<Boolean> mRecursionGuard = new ThreadLocal<Boolean>() {
        @Override protected Boolean initialValue() { return false; }
    };

    // Private Constructor
    private IpcMonitorHelper() {}

    public static IpcMonitorHelper getInstance() {
        if (sInstance == null) {
            synchronized (sLock) {
                if (sInstance == null) {
                    sInstance = new IpcMonitorHelper();
                }
            }
        }
        return sInstance;
    }

    /**
     * Lazy initialization of the background thread.
     */
    private void ensureThreadReady() {
        if (mHandler != null) return;
        synchronized (sLock) {
            if (mHandler == null) {
                try {
                    mThread = new HandlerThread("IpcMonitorShared");
                    mThread.start();
                    mHandler = new Handler(mThread.getLooper());
                } catch (Exception e) {
                    Slog.e(TAG, "Failed to start thread", e);
                }
            }
        }
    }

    /**
     * Initializes Settings observer. Safe to call multiple times.
     */
    public void ensureInitialized(Context context) {
        if (context == null || mObserverRegistered) return;
        
        // Anti-recursion & Deadlock check
        String pkg = context.getPackageName();
        if ("com.android.providers.settings".equals(pkg)) return;

        ensureThreadReady();
        if (mHandler == null) return;

        // Double check locking for registration
        synchronized (sLock) {
            if (mObserverRegistered) return;

            ContentObserver observer = new ContentObserver(mHandler) {
                @Override
                public void onChange(boolean selfChange) {
                    updateSettings(context);
                }
            };

            context.getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor("ipc_monitor_enabled"), false, observer);
            context.getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor("ipc_monitor_targets"), false, observer);

            mHandler.post(() -> updateSettings(context));
            mObserverRegistered = true;
        }
    }

    private void updateSettings(Context context) {
        if (mRecursionGuard.get()) return;
        
        try {
            mRecursionGuard.set(true);
            mEnabled = Settings.Global.getInt(context.getContentResolver(), "ipc_monitor_enabled", 0) == 1;
            mTargetPackages = Settings.Global.getString(context.getContentResolver(), "ipc_monitor_targets");
            if (mTargetPackages == null) mTargetPackages = "*";
        } catch (Exception e) {
            Slog.e(TAG, "Failed to report ContentProvider interaction", e);
        } finally {
            mRecursionGuard.set(false);
        }
    }

    public boolean isMonitored(String callingPkg) {
        if (!mEnabled) return false;
        if (MONITOR_PKG.equals(callingPkg)) return false;
        if ("android".equals(callingPkg)) return false; // Reduce noise
        if (mTargetPackages == null || "*".equals(mTargetPackages)) return true;
        return mTargetPackages.contains(callingPkg);
    }

    /**
     * Main entry point to report an event.
     */
    public void report(Context context, String type, String sender, String receiver, JSONObject payload) {
        if (context == null) return;
        
        // Fast checks before posting to thread
        if (!mEnabled) return;
        
        // Logic check: monitor if EITHER sender OR receiver is targeted
        if (!isMonitored(sender) && !isMonitored(receiver)) return;

        ensureThreadReady();
        if (mHandler != null) {
            mHandler.post(() -> sendBroadcast(context, type, sender, receiver, payload));
        }
    }

    private void sendBroadcast(Context context, String type, String sender, String receiver, JSONObject payload) {
        try {
            Slog.d(TAG, "Hook triggered! Sender: " + sender);
            JSONObject json = new JSONObject();
            json.put("type", type);
            json.put("sender", sender != null ? sender : "unknown");
            json.put("receiver", receiver != null ? receiver : "unknown");
            json.put("payload", payload);
            json.put("timestamp", System.currentTimeMillis());

            Intent intent = new Intent(MONITOR_ACTION);
            intent.setPackage(MONITOR_PKG);
            intent.putExtra("ipc_data", json.toString());
            intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY | Intent.FLAG_RECEIVER_FOREGROUND);

            //context.sendBroadcastAsUser(intent, UserHandle.ALL);
            context.sendBroadcast(intent);
        } catch (Exception e) {
            Slog.e(TAG, "Failed to report ContentProvider interaction", e);
        }
    }
}
// END CUSTOM IPC MONITOR
