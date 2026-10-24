package com.khanhs.ppcam;

import com.voxivoid.recipelab.NativeBackup;

/**
 * Manages reading, writing, and applying Picture Profile and Camera settings to NVRAM.
 */
public class PPManager {
    // Picture Profile Selector
    public static final int ID_PP_SELECT = 0x0107031c; // 0=Off, 1..7=PP1..PP7

    // Picture Profile Base IDs (PP1)
    public static final int BASE_BLACK_LEVEL       = 0x010701e5;
    public static final int BASE_GAMMA             = 0x01070299;
    public static final int BASE_BG_RANGE          = 0x010701db;
    public static final int BASE_BG_LEVEL          = 0x010701d1;
    public static final int BASE_KNEE_MODE         = 0x010702ad;
    public static final int BASE_KNEE_AUTO_MAX     = 0x010702a3;
    public static final int BASE_KNEE_AUTO_SENS    = 0x010702c1;
    public static final int BASE_KNEE_MANUAL_POINT = 0x010702b7;
    public static final int BASE_KNEE_MANUAL_SLOPE = 0x010702cb;
    public static final int BASE_COLOR_MODE        = 0x01070249;
    public static final int BASE_SATURATION        = 0x01070235;
    public static final int BASE_COLOR_PHASE       = 0x0107022b;
    public static final int BASE_COLOR_DEPTH_R     = 0x01070217;
    public static final int BASE_COLOR_DEPTH_G     = 0x01070203;
    public static final int BASE_COLOR_DEPTH_B     = 0x010701ef;
    public static final int BASE_COLOR_DEPTH_C     = 0x010701f9;
    public static final int BASE_COLOR_DEPTH_M     = 0x0107020d;
    public static final int BASE_COLOR_DEPTH_Y     = 0x01070221;
    public static final int BASE_DETAIL_LEVEL      = 0x01070271;
    public static final int BASE_DETAIL_ADJUST     = 0x01070285;
    public static final int BASE_DETAIL_VH_BAL     = 0x0107028f;
    public static final int BASE_DETAIL_BW_BAL     = 0x01070253;
    public static final int BASE_DETAIL_LIMIT      = 0x0107027b;
    public static final int BASE_DETAIL_CRISPENING = 0x0107025d;
    public static final int BASE_DETAIL_HILIGHT    = 0x01070267;

    // White Balance & Exposure
    public static final int ID_WB_MODE    = 0x01070019;
    public static final int ID_WB_KELVIN  = 0x01070018;
    public static final int ID_WB_AB      = 0x01070017;
    public static final int ID_WB_GM      = 0x01070016;
    public static final int ID_WB_AB_TEMP = 0x01070683;
    public static final int ID_WB_GM_TEMP = 0x01070682;
    public static final int ID_WB_AB_AWB  = 0x0107067f;
    public static final int ID_WB_GM_AWB  = 0x0107067e;

    public static final int ID_EV         = 0x010700b8;
    public static final int ID_EV_COPY    = 0x01070c7f;
    public static final int ID_DRO        = 0x01070104;
    public static final int ID_DRO_LEVEL  = 0x01070775;

    private static void safeWrite(int id, int val) {
        try {
            NativeBackup.writeByte(id, val);
        } catch (Throwable ignored) {}
    }

    /**
     * Overwrites PP7 (and optional companion WB/EV settings) and commits to NVRAM.
     */
    public static boolean applyPreset(PPPreset p) {
        try {
            int slot = p.targetSlot;
            if (slot <= 0 || slot > 7) slot = 7;
            int offset = slot - 1; // 6 for PP7

            // 1. Write all 25 PP Core parameters
            safeWrite(BASE_BLACK_LEVEL + offset, p.blackLevel);
            safeWrite(BASE_GAMMA + offset, p.gamma);
            safeWrite(BASE_BG_RANGE + offset, p.blackGammaRange);
            safeWrite(BASE_BG_LEVEL + offset, p.blackGammaLevel);
            safeWrite(BASE_KNEE_MODE + offset, p.kneeMode);
            safeWrite(BASE_KNEE_AUTO_MAX + offset, p.kneeAutoMax);
            safeWrite(BASE_KNEE_AUTO_SENS + offset, p.kneeAutoSens);
            safeWrite(BASE_KNEE_MANUAL_POINT + offset, p.kneeManualPoint);
            safeWrite(BASE_KNEE_MANUAL_SLOPE + offset, p.kneeManualSlope);
            safeWrite(BASE_COLOR_MODE + offset, p.colorMode);
            safeWrite(BASE_SATURATION + offset, p.saturation);
            safeWrite(BASE_COLOR_PHASE + offset, p.colorPhase);
            safeWrite(BASE_COLOR_DEPTH_R + offset, p.colorDepthR);
            safeWrite(BASE_COLOR_DEPTH_G + offset, p.colorDepthG);
            safeWrite(BASE_COLOR_DEPTH_B + offset, p.colorDepthB);
            safeWrite(BASE_COLOR_DEPTH_C + offset, p.colorDepthC);
            safeWrite(BASE_COLOR_DEPTH_M + offset, p.colorDepthM);
            safeWrite(BASE_COLOR_DEPTH_Y + offset, p.colorDepthY);
            safeWrite(BASE_DETAIL_LEVEL + offset, p.detailLevel);
            safeWrite(BASE_DETAIL_ADJUST + offset, p.detailAdjustMode);
            safeWrite(BASE_DETAIL_VH_BAL + offset, p.detailVhBalance);
            safeWrite(BASE_DETAIL_BW_BAL + offset, p.detailBwBalance);
            safeWrite(BASE_DETAIL_LIMIT + offset, p.detailLimit);
            safeWrite(BASE_DETAIL_CRISPENING + offset, p.detailCrispening);
            safeWrite(BASE_DETAIL_HILIGHT + offset, p.detailHilight);

            // 2. White Balance
            if (p.wbMode != 0) {
                safeWrite(ID_WB_MODE, p.wbMode);
                if (p.wbMode == 14) {
                    safeWrite(ID_WB_KELVIN, p.wbKelvin);
                    safeWrite(ID_WB_AB_TEMP, p.wbAbShift);
                    safeWrite(ID_WB_GM_TEMP, p.wbGmShift);
                } else if (p.wbMode == 1) {
                    safeWrite(ID_WB_AB_AWB, p.wbAbShift);
                    safeWrite(ID_WB_GM_AWB, p.wbGmShift);
                }
                safeWrite(ID_WB_AB, p.wbAbShift);
                safeWrite(ID_WB_GM, p.wbGmShift);
            }

            // 3. Exposure Bias (EV)
            safeWrite(ID_EV, p.exposureBias);
            safeWrite(ID_EV_COPY, p.exposureBias);

            // 4. DRO
            if (p.droMode > 0) {
                safeWrite(ID_DRO, p.droMode);
                safeWrite(ID_DRO_LEVEL, p.droMode);
            }

            // 5. Activate Slot PP7
            safeWrite(ID_PP_SELECT, slot);

            // 6. Commit to flash storage
            NativeBackup.sync();
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Resets PP7, WB, and EV to camera factory defaults.
     */
    public static boolean resetPP7() {
        try {
            int offset = 6; // PP7
            safeWrite(BASE_BLACK_LEVEL + offset, 0);
            safeWrite(BASE_GAMMA + offset, 4); // Movie default
            safeWrite(BASE_BG_RANGE + offset, 1); // Middle
            safeWrite(BASE_BG_LEVEL + offset, 0);
            safeWrite(BASE_KNEE_MODE + offset, 0); // Auto
            safeWrite(BASE_KNEE_AUTO_MAX + offset, 0);
            safeWrite(BASE_KNEE_AUTO_SENS + offset, 1);
            safeWrite(BASE_COLOR_MODE + offset, 5); // Movie
            safeWrite(BASE_SATURATION + offset, 0);
            safeWrite(BASE_COLOR_PHASE + offset, 0);
            for (int i = 0; i < 6; i++) {
                int[] depthIds = {
                    BASE_COLOR_DEPTH_R, BASE_COLOR_DEPTH_G, BASE_COLOR_DEPTH_B,
                    BASE_COLOR_DEPTH_C, BASE_COLOR_DEPTH_M, BASE_COLOR_DEPTH_Y
                };
                safeWrite(depthIds[i] + offset, 0);
            }
            safeWrite(BASE_DETAIL_LEVEL + offset, 0);
            safeWrite(BASE_DETAIL_ADJUST + offset, 0); // Auto

            // Reset WB to AWB (mode 1, shift 0,0)
            safeWrite(ID_WB_MODE, 1);
            safeWrite(ID_WB_AB, 0);
            safeWrite(ID_WB_GM, 0);
            safeWrite(ID_WB_AB_AWB, 0);
            safeWrite(ID_WB_GM_AWB, 0);

            // Reset EV to 0.0
            safeWrite(ID_EV, 0);
            safeWrite(ID_EV_COPY, 0);

            // Keep PP7 active or off
            safeWrite(ID_PP_SELECT, 7);

            NativeBackup.sync();
            return true;
        } catch (Throwable e) {
            return false;
        }
    }
}
