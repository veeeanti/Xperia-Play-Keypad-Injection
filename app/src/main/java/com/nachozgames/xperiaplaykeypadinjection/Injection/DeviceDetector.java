package com.nachozgames.xperiaplaykeypadinjection.Injection;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;

/**
 * DeviceDetector
 *  - Scans Linux input devices to find the correct event node for
 *    the Xperia Play's keypad-zeus (slide mechanism).
 *  - Strategy 1: Parse /proc/bus/input/devices (no root needed)
 *  - Strategy 2: Fallback via 'getevent -S' (root needed)
 *  - Returns null if not found.
 */
public final class DeviceDetector {

    private static final String TAG = "DeviceDetector";
    private static final String TARGET_DEVICE_NAME = "keypad-zeus";

    private DeviceDetector() {}

    /**
     * Finds the /dev/input/eventX path for keypad-zeus.
     * @return e.g. "/dev/input/event5", or null if not found.
     */
    public static String findKeypadZeus() {
        // Strategy 1: /proc/bus/input/devices (readable without root)
        String path = findFromProcDevices();
        if (path != null) {
            Log.i(TAG, "Found via /proc/bus/input/devices: " + path);
            return path;
        }

        // Strategy 2: getevent -S (needs root)
        path = findFromGetevent();
        if (path != null) {
            Log.i(TAG, "Found via getevent: " + path);
            return path;
        }

        Log.e(TAG, "keypad-zeus device NOT found by any method");
        return null;
    }

    /**
     * Parses /proc/bus/input/devices.
     *
     * Device blocks look like:
     *   I: Bus=0019 Vendor=0001 Product=0001 Version=0100
     *   N: Name="keypad-zeus"
     *   P: Phys=keypad-zeus/input0
     *   S: Sysfs=/devices/platform/keypad-zeus/input/input5
     *   U: Uniq=
     *   H: Handlers=kbd event5
     *   B: ...
     *
     * We find the block where N: contains "keypad-zeus",
     * then extract "eventX" from the H: line.
     */
    private static String findFromProcDevices() {
        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            new java.io.FileInputStream("/proc/bus/input/devices")));

            String line;
            boolean inTargetBlock = false;

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    inTargetBlock = false;
                    continue;
                }

                if (line.startsWith("N:") &&
                    line.toLowerCase().contains(TARGET_DEVICE_NAME)) {
                    inTargetBlock = true;
                    Log.d(TAG, "Found target block: " + line);
                    continue;
                }

                if (inTargetBlock && line.startsWith("H:")) {
                    String eventNode = extractEventNode(line);
                    if (eventNode != null) {
                        br.close();
                        return "/dev/input/" + eventNode;
                    }
                }
            }
            br.close();
        } catch (Exception e) {
            Log.w(TAG, "/proc/bus/input/devices parse failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Fallback: run `getevent -S` via su.
     *
     * Output format:
     *   add device 5: /dev/input/event5
     *     name:     "keypad-zeus"
     */
    private static String findFromGetevent() {
        Process proc = null;
        try {
            proc = new ProcessBuilder("su", "-c", "getevent -S")
                    .redirectErrorStream(true)
                    .start();

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()));

            String line;
            String lastDevicePath = null;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("add device") &&
                    line.contains("/dev/input/event")) {
                    int idx = line.indexOf("/dev/input/event");
                    if (idx >= 0) {
                        lastDevicePath = line.substring(idx).trim();
                    }
                }

                if (line.startsWith("name:") &&
                    line.toLowerCase().contains(TARGET_DEVICE_NAME)) {
                    if (lastDevicePath != null) {
                        br.close();
                        return lastDevicePath;
                    }
                }
            }
            br.close();
        } catch (Exception e) {
            Log.w(TAG, "getevent parse failed: " + e.getMessage());
        } finally {
            if (proc != null) proc.destroy();
        }
        return null;
    }

    /** Extracts "eventX" from "H: Handlers=kbd event5" */
    private static String extractEventNode(String handlersLine) {
        String[] parts = handlersLine.split("\\s+");
        for (String part : parts) {
            if (part.startsWith("event")) {
                return part;
            }
        }
        return null;
    }

    /** Checks if the detected device node exists on the filesystem */
    public static boolean deviceNodeExists(String path) {
        if (path == null) return false;
        if (new File(path).exists()) return true;

        try {
            Process p = new ProcessBuilder("su", "-c",
                    "test -e " + path + " && echo YES")
                    .redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            String result = br.readLine();
            br.close();
            p.destroy();
            return "YES".equals(result != null ? result.trim() : "");
        } catch (Exception e) {
            return false;
        }
    }
}