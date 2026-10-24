package com.khanhs.ppcam;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * 64-Byte binary Picture Profile preset container for Sony A7 Mark II (ILCE-7M2).
 */
public class PPPreset {
    public static final byte[] MAGIC = new byte[] { 'P', 'P', 'D', 'T' };
    public static final int FILE_SIZE = 64;

    // Film Type Identifiers
    public static final int TYPE_KODAK = 0x01;
    public static final int TYPE_FUJI = 0x02;
    public static final int TYPE_CINESTILL = 0x03;
    public static final int TYPE_MONOCHROME = 0x04;
    public static final int TYPE_VINTAGE = 0x05;
    public static final int TYPE_BODY_CUSTOM = 0xAA;

    // Metadata
    public String name = "UNTITLED";
    public int filmType = TYPE_BODY_CUSTOM;
    public boolean isFavorite = false;
    public int targetSlot = 7; // PP7
    public String filePath = null;

    // Picture Profile Core (25 parameters)
    public int blackLevel = 0;          // -15 .. +15
    public int gamma = 5;               // 5 = Still, 6..9 = Cine1..4
    public int blackGammaRange = 1;     // 0=Wide, 1=Middle, 2=Narrow
    public int blackGammaLevel = 0;     // -7 .. +7
    public int kneeMode = 0;            // 0=Auto, 1=Manual
    public int kneeAutoMax = 0;         // 0..4 (90%..100%)
    public int kneeAutoSens = 0;        // 0=High, 1=Mid, 2=Low
    public int kneeManualPoint = 0;     // 0..12 (75%..105%)
    public int kneeManualSlope = 0;     // -5 .. +5
    public int colorMode = 6;           // 3=Pro, 5=Movie, 6=Still, 7=Cinema...
    public int saturation = 0;          // -32 .. +32
    public int colorPhase = 0;          // -7 .. +7
    public int colorDepthR = 0;         // -7 .. +7
    public int colorDepthG = 0;
    public int colorDepthB = 0;
    public int colorDepthC = 0;
    public int colorDepthM = 0;
    public int colorDepthY = 0;
    public int detailLevel = 0;         // -7 .. +7
    public int detailAdjustMode = 0;    // 0=Auto, 1=Manual
    public int detailVhBalance = 0;     // -2 .. +2
    public int detailBwBalance = 0;     // 0..4 (Type 1..5)
    public int detailLimit = 0;         // 0..7
    public int detailCrispening = 0;    // 0..7
    public int detailHilight = 0;       // 0..4

    // White Balance & Exposure
    public int wbMode = 14;             // 1=Auto, 14=Kelvin
    public int wbKelvin = 55;           // Kelvin / 100 (55 = 5500K)
    public int wbAbShift = 0;           // -7 (Blue) .. +7 (Amber)
    public int wbGmShift = 0;           // -7 (Green) .. +7 (Magenta)
    public int exposureBias = 0;        // 1/3 EV steps (0=0EV, 2=+0.7EV...)
    public int droMode = 1;             // 0=Off, 1=Auto, 2..6=Lv1..5

    public PPPreset() {}

    /**
     * Creates a deep copy of this preset.
     */
    public PPPreset copy() {
        PPPreset p = new PPPreset();
        p.name = this.name;
        p.filmType = this.filmType;
        p.isFavorite = this.isFavorite;
        p.targetSlot = this.targetSlot;
        p.filePath = this.filePath;

        p.blackLevel = this.blackLevel;
        p.gamma = this.gamma;
        p.blackGammaRange = this.blackGammaRange;
        p.blackGammaLevel = this.blackGammaLevel;
        p.kneeMode = this.kneeMode;
        p.kneeAutoMax = this.kneeAutoMax;
        p.kneeAutoSens = this.kneeAutoSens;
        p.kneeManualPoint = this.kneeManualPoint;
        p.kneeManualSlope = this.kneeManualSlope;
        p.colorMode = this.colorMode;
        p.saturation = this.saturation;
        p.colorPhase = this.colorPhase;
        p.colorDepthR = this.colorDepthR;
        p.colorDepthG = this.colorDepthG;
        p.colorDepthB = this.colorDepthB;
        p.colorDepthC = this.colorDepthC;
        p.colorDepthM = this.colorDepthM;
        p.colorDepthY = this.colorDepthY;
        p.detailLevel = this.detailLevel;
        p.detailAdjustMode = this.detailAdjustMode;
        p.detailVhBalance = this.detailVhBalance;
        p.detailBwBalance = this.detailBwBalance;
        p.detailLimit = this.detailLimit;
        p.detailCrispening = this.detailCrispening;
        p.detailHilight = this.detailHilight;

        p.wbMode = this.wbMode;
        p.wbKelvin = this.wbKelvin;
        p.wbAbShift = this.wbAbShift;
        p.wbGmShift = this.wbGmShift;
        p.exposureBias = this.exposureBias;
        p.droMode = this.droMode;
        return p;
    }

    /**
     * Default built-in recipe: Blue Velvet / Cinestill 50D
     */
    public static PPPreset createDefaultCinestill50D() {
        PPPreset p = new PPPreset();
        p.name = "CINESTILL 50D";
        p.filmType = TYPE_CINESTILL;
        p.isFavorite = false;
        p.targetSlot = 7;

        p.blackLevel = 0;
        p.gamma = 5;                // Still
        p.blackGammaRange = 1;     // Middle
        p.blackGammaLevel = 7;     // +7
        p.kneeMode = 1;            // Manual
        p.kneeManualPoint = 0;     // 75%
        p.kneeManualSlope = 4;     // +4
        p.colorMode = 6;           // Still
        p.saturation = 10;         // +10
        p.colorPhase = -3;         // -3
        p.colorDepthR = 4;
        p.colorDepthG = 5;
        p.colorDepthB = -2;
        p.colorDepthC = 3;
        p.colorDepthM = 5;
        p.colorDepthY = 2;
        p.detailLevel = 0;
        p.detailAdjustMode = 1;    // Manual
        p.detailVhBalance = 2;     // +2
        p.detailBwBalance = 2;     // Type 3
        p.detailLimit = 7;
        p.detailCrispening = 7;
        p.detailHilight = 4;

        p.wbMode = 14;             // Kelvin
        p.wbKelvin = 74;           // 7400K
        p.wbAbShift = -4;          // B4
        p.wbGmShift = 2;           // M1.5 (rounded to M2)
        p.exposureBias = 0;
        p.droMode = 1;             // Auto
        return p;
    }

    public static int calcCrc16(byte[] data, int len) {
        int crc = 0xFFFF;
        for (int i = 0; i < len; i++) {
            crc ^= ((data[i] & 0xFF) << 8);
            for (int b = 0; b < 8; b++) {
                if ((crc & 0x8000) != 0) {
                    crc = ((crc << 1) ^ 0x1021) & 0xFFFF;
                } else {
                    crc = (crc << 1) & 0xFFFF;
                }
            }
        }
        return crc;
    }

    public byte[] toBytes() {
        byte[] buf = new byte[FILE_SIZE];
        Arrays.fill(buf, (byte) 0);

        // Header: 0x00..0x03 Magic
        System.arraycopy(MAGIC, 0, buf, 0, 4);
        buf[4] = 0x01; // Version
        buf[5] = (byte) filmType;
        buf[6] = (byte) (isFavorite ? 1 : 0);

        // Name (20 bytes max)
        byte[] nameBytes = name.getBytes(Charset.forName("US-ASCII"));
        int copyLen = Math.min(nameBytes.length, 19);
        System.arraycopy(nameBytes, 0, buf, 7, copyLen);
        buf[27] = (byte) targetSlot;

        // PP Core (25 bytes)
        buf[28] = (byte) blackLevel;
        buf[29] = (byte) gamma;
        buf[30] = (byte) blackGammaRange;
        buf[31] = (byte) blackGammaLevel;
        buf[32] = (byte) kneeMode;
        buf[33] = (byte) kneeAutoMax;
        buf[34] = (byte) kneeAutoSens;
        buf[35] = (byte) kneeManualPoint;
        buf[36] = (byte) kneeManualSlope;
        buf[37] = (byte) colorMode;
        buf[38] = (byte) saturation;
        buf[39] = (byte) colorPhase;
        buf[40] = (byte) colorDepthR;
        buf[41] = (byte) colorDepthG;
        buf[42] = (byte) colorDepthB;
        buf[43] = (byte) colorDepthC;
        buf[44] = (byte) colorDepthM;
        buf[45] = (byte) colorDepthY;
        buf[46] = (byte) detailLevel;
        buf[47] = (byte) detailAdjustMode;
        buf[48] = (byte) detailVhBalance;
        buf[49] = (byte) detailBwBalance;
        buf[50] = (byte) detailLimit;
        buf[51] = (byte) detailCrispening;
        buf[52] = (byte) detailHilight;

        // WB & Exposure (6 bytes)
        buf[53] = (byte) wbMode;
        buf[54] = (byte) wbKelvin;
        buf[55] = (byte) wbAbShift;
        buf[56] = (byte) wbGmShift;
        buf[57] = (byte) exposureBias;
        buf[58] = (byte) droMode;

        // Reserved: buf[59..61] = 0

        // CRC16 over 62 bytes
        int crc = calcCrc16(buf, 62);
        buf[62] = (byte) (crc & 0xFF);
        buf[63] = (byte) ((crc >> 8) & 0xFF);
        return buf;
    }

    public static PPPreset fromBytes(byte[] buf, String path) {
        if (buf == null || buf.length != FILE_SIZE) return null;
        if (buf[0] != 'P' || buf[1] != 'P' || buf[2] != 'D' || buf[3] != 'T') return null;

        int expectedCrc = calcCrc16(buf, 62);
        int fileCrc = (buf[62] & 0xFF) | ((buf[63] & 0xFF) << 8);
        if (expectedCrc != fileCrc) return null;

        PPPreset p = new PPPreset();
        p.filePath = path;
        p.filmType = buf[5] & 0xFF;
        p.isFavorite = (buf[6] & 0xFF) == 1;

        // Extract name
        int nameLen = 0;
        while (nameLen < 20 && buf[7 + nameLen] != 0) {
            nameLen++;
        }
        p.name = new String(buf, 7, nameLen, Charset.forName("US-ASCII")).trim();
        p.targetSlot = buf[27] & 0xFF;
        if (p.targetSlot <= 0 || p.targetSlot > 7) p.targetSlot = 7;

        // Core PP
        p.blackLevel = (byte) buf[28];
        p.gamma = buf[29] & 0xFF;
        p.blackGammaRange = buf[30] & 0xFF;
        p.blackGammaLevel = (byte) buf[31];
        p.kneeMode = buf[32] & 0xFF;
        p.kneeAutoMax = buf[33] & 0xFF;
        p.kneeAutoSens = buf[34] & 0xFF;
        p.kneeManualPoint = buf[35] & 0xFF;
        p.kneeManualSlope = (byte) buf[36];
        p.colorMode = buf[37] & 0xFF;
        p.saturation = (byte) buf[38];
        p.colorPhase = (byte) buf[39];
        p.colorDepthR = (byte) buf[40];
        p.colorDepthG = (byte) buf[41];
        p.colorDepthB = (byte) buf[42];
        p.colorDepthC = (byte) buf[43];
        p.colorDepthM = (byte) buf[44];
        p.colorDepthY = (byte) buf[45];
        p.detailLevel = (byte) buf[46];
        p.detailAdjustMode = buf[47] & 0xFF;
        p.detailVhBalance = (byte) buf[48];
        p.detailBwBalance = buf[49] & 0xFF;
        p.detailLimit = buf[50] & 0xFF;
        p.detailCrispening = buf[51] & 0xFF;
        p.detailHilight = buf[52] & 0xFF;

        // WB & Exposure
        p.wbMode = buf[53] & 0xFF;
        p.wbKelvin = buf[54] & 0xFF;
        p.wbAbShift = (byte) buf[55];
        p.wbGmShift = (byte) buf[56];
        p.exposureBias = (byte) buf[57];
        p.droMode = buf[58] & 0xFF;

        return p;
    }

    public static PPPreset loadFromFile(File file) {
        if (!file.exists() || file.length() != FILE_SIZE) return null;
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] buf = new byte[FILE_SIZE];
            int read = fis.read(buf);
            fis.close();
            if (read == FILE_SIZE) {
                return fromBytes(buf, file.getAbsolutePath());
            }
        } catch (IOException ignored) {}
        return null;
    }

    public boolean saveToFile(File file) {
        try {
            byte[] buf = toBytes();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(buf);
            fos.flush();
            fos.close();
            this.filePath = file.getAbsolutePath();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public String getGammaLabel() {
        switch (gamma) {
            case 3: return "ITU709";
            case 4: return "Movie";
            case 5: return "Still";
            case 6: return "Cine1";
            case 7: return "Cine2";
            case 8: return "Cine3";
            case 9: return "Cine4";
            case 10: return "ITU709(800%)";
            case 11: return "S-Log2";
            default: return "Gamma " + gamma;
        }
    }

    public String getColorModeLabel() {
        switch (colorMode) {
            case 3: return "Pro";
            case 4: return "ITU709 Matrix";
            case 5: return "Movie";
            case 6: return "Still";
            case 7: return "Cinema";
            case 8: return "S-Gamut";
            case 9: return "B&W";
            default: return "Color " + colorMode;
        }
    }

    public String getWbLabel() {
        if (wbMode == 14) {
            return (wbKelvin * 100) + "K";
        } else if (wbMode == 1) {
            return "AWB";
        } else {
            return "Keep WB";
        }
    }

    public String getWbShiftLabel() {
        String ab = wbAbShift == 0 ? "0" : (wbAbShift > 0 ? "A" + wbAbShift : "B" + (-wbAbShift));
        String gm = wbGmShift == 0 ? "0" : (wbGmShift > 0 ? "M" + wbGmShift : "G" + (-wbGmShift));
        return ab + " " + gm;
    }

    public String getEvLabel() {
        if (exposureBias == 0) return "0.0 EV";
        float ev = exposureBias / 3.0f;
        return String.format("%+.1f EV", ev);
    }
}
