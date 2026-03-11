package com.nachozgames.xperiaplaykeypadinjection.Service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.nachozgames.xperiaplaykeypadinjection.Injection.DeviceDetector;
import com.nachozgames.xperiaplaykeypadinjection.Injection.KeypadZeusWriter;
import com.nachozgames.xperiaplaykeypadinjection.Util.Toasts;

import java.io.IOException;

public class InjectionService extends Service {
    private static final String TAG = "InjectionService";
    
    private KeypadZeusWriter keypadZeusWriter;
    private String detectedDevicePath;
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service created");
        Toasts.init(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isRunning) {
            Log.i(TAG, "Service already running");
            return START_STICKY;
        }
        
        Log.i(TAG, "Service starting...");
        
        // Detect the keypad-zeus device
        detectedDevicePath = DeviceDetector.findKeypadZeus();
        
        if (detectedDevicePath == null) {
            Log.e(TAG, "keypad-zeus device not found on this device!");
            Toasts.show("ERROR: keypad-zeus not found in /dev/input/");
            stopSelf();
            return START_NOT_STICKY;
        }
        
        Log.i(TAG, "Detected keypad-zeus at: " + detectedDevicePath);
        Toasts.show("keypad-zeus detected: " + detectedDevicePath);
        
        try {
            keypadZeusWriter = new KeypadZeusWriter();
            keypadZeusWriter.start(detectedDevicePath);
            isRunning = true;
            Log.i(TAG, "Injection service started successfully");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to start writer on " + detectedDevicePath + ": " + e.getMessage());
            Toasts.show("Failed to open " + detectedDevicePath);
            stopSelf();
            return START_NOT_STICKY;
        }
        
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        if (keypadZeusWriter != null) {
            try {
                keypadZeusWriter.close();
                Log.i(TAG, "KeypadZeusWriter closed");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        isRunning = false;
        Log.i(TAG, "Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
