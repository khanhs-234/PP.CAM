package com.khanhs.ppcam;

import android.os.Environment;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StorageHelper {
    public static final String FOLDER_NAME = "PP_DAT";

    public static File getPresetDirectory() {
        File[] roots = new File[] {
            new File("/mnt/sdcard/" + FOLDER_NAME),
            new File("/media/0/" + FOLDER_NAME),
            new File(Environment.getExternalStorageDirectory(), FOLDER_NAME)
        };
        for (File dir : roots) {
            if (dir.exists() && dir.isDirectory()) return dir;
        }
        // Try creating on primary external storage
        File primary = new File("/mnt/sdcard/" + FOLDER_NAME);
        if (primary.mkdirs() || primary.exists()) return primary;
        File ext = new File(Environment.getExternalStorageDirectory(), FOLDER_NAME);
        ext.mkdirs();
        return ext;
    }

    public static List<PPPreset> loadPresets() {
        List<PPPreset> list = new ArrayList<PPPreset>();
        
        // 1. Always add the Built-in Default Preset first
        PPPreset defaultPreset = PPPreset.createDefaultCinestill50D();
        list.add(defaultPreset);

        // 2. Scan SD card
        File dir = getPresetDirectory();
        if (dir != null && dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                List<File> datFiles = new ArrayList<File>();
                for (File f : files) {
                    if (f.isFile() && f.getName().toUpperCase().endsWith(".DAT")) {
                        datFiles.add(f);
                    }
                }
                // Sort by name
                Collections.sort(datFiles, new Comparator<File>() {
                    public int compare(File a, File b) {
                        return a.getName().compareToIgnoreCase(b.getName());
                    }
                });

                for (File f : datFiles) {
                    PPPreset p = PPPreset.loadFromFile(f);
                    if (p != null) {
                        list.add(p);
                    }
                }
            }
        }
        return list;
    }

    /**
     * Generates the next sequential custom filename: CUS000.DAT, CUS001.DAT...
     */
    public static String getNextCustomFileName(File dir) {
        if (dir == null) return "CUS000.DAT";
        for (int i = 0; i < 1000; i++) {
            String name = String.format("CUS%03d.DAT", i);
            File f = new File(dir, name);
            if (!f.exists()) {
                return name;
            }
        }
        return "CUS999.DAT";
    }

    public static boolean saveCustomPreset(PPPreset preset) {
        File dir = getPresetDirectory();
        if (dir == null) return false;
        String fileName = getNextCustomFileName(dir);
        File target = new File(dir, fileName);
        preset.filmType = PPPreset.TYPE_BODY_CUSTOM;
        return preset.saveToFile(target);
    }

    public static boolean deletePreset(PPPreset preset) {
        if (preset.filePath == null) return false;
        File f = new File(preset.filePath);
        if (f.exists()) {
            return f.delete();
        }
        return false;
    }

    public static boolean updatePresetFile(PPPreset preset) {
        if (preset.filePath == null) return false;
        File f = new File(preset.filePath);
        return preset.saveToFile(f);
    }
}
