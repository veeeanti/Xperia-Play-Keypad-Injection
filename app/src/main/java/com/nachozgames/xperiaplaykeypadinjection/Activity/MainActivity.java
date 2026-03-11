package com.nachozgames.xperiaplaykeypadinjection.Activity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.nachozgames.xperiaplaykeypadinjection.Injection.DeviceDetector;
import com.nachozgames.xperiaplaykeypadinjection.Injection.KeypadZeusWriter;
import com.nachozgames.xperiaplaykeypadinjection.R;
import com.nachozgames.xperiaplaykeypadinjection.Util.Toasts;

import java.io.IOException;


public class MainActivity extends Activity
{
    private static final String TAG = "XperiaPlay";

    private KeypadZeusWriter keypadZeusWriter;
    private String detectedDevicePath;

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toasts.init(this);
        keypadZeusWriter = new KeypadZeusWriter();

        // --- Dynamic device detection (replaces hardcoded /dev/input/event5) ---
        detectedDevicePath = DeviceDetector.findKeypadZeus();

        if (detectedDevicePath == null)
        {
            Log.e(TAG, "keypad-zeus device not found on this device!");
            Toasts.show("ERROR: keypad-zeus not found in /dev/input/");
            disableButtons();
            return;
        }

        Log.i(TAG, "Detected keypad-zeus at: " + detectedDevicePath);
        Toasts.show("keypad-zeus detected: " + detectedDevicePath);

        try
        {
            keypadZeusWriter.start(detectedDevicePath);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.e(TAG, "Failed to start writer on " + detectedDevicePath
                       + ": " + e.getMessage());
            Toasts.show("Failed to open " + detectedDevicePath);
            disableButtons();
        }
    }

    @Override protected void onStart()
    {
        super.onStart();
    }

    @Override protected void onStop()
    {
        super.onStop();

        try
        {
            keypadZeusWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onButtonOpen(View v)
    {
        try{
            keypadZeusWriter.switchEvent(0, 0);
            keypadZeusWriter.mscEvent(0x3, 0);
            keypadZeusWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void onButtonClose(View v)
    {
        try{
            keypadZeusWriter.switchEvent(0, 1);
            keypadZeusWriter.mscEvent(0x3, 1);
            keypadZeusWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void disableButtons()
    {
        Button btnOpen = findViewById(R.id.btn_sync);
        Button btnClose = findViewById(R.id.btn_sync2);
        if (btnOpen != null) btnOpen.setEnabled(false);
        if (btnClose != null) btnClose.setEnabled(false);
    }
}