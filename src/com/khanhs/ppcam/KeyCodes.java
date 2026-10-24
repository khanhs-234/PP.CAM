package com.khanhs.ppcam;

import android.view.KeyEvent;

public class KeyCodes {
    // Sony Scalar Input Scan Codes
    public static final int SONY_UP        = 103;
    public static final int SONY_DOWN      = 108;
    public static final int SONY_LEFT      = 105;
    public static final int SONY_RIGHT     = 106;
    public static final int SONY_ENTER     = 232;
    public static final int SONY_MENU      = 514;
    public static final int SONY_DELETE    = 595; // C4 / Trash
    public static final int SONY_FN        = 520; // Fn
    public static final int SONY_C1        = 622; // C1 (Reload SD / Import)
    public static final int SONY_C2        = 623; // C2 (Favorite ★)
    public static final int SONY_C3        = 659; // C3 (Reset PP7)
    public static final int SONY_CUSTOM    = 588; // Custom button
    public static final int SONY_AEL       = 532; // AEL button / lever
    public static final int SONY_WHEEL_CW  = 522;
    public static final int SONY_WHEEL_CCW = 523;
    public static final int SONY_DIAL_CW   = 525;
    public static final int SONY_DIAL_CCW  = 526;
    public static final int SONY_DIAL2_CW  = 528;
    public static final int SONY_DIAL2_CCW = 529;
    public static final int SONY_S1        = 516; // Shutter half-press
    public static final int SONY_S2        = 518; // Shutter full-press

    public enum Action {
        NONE,
        NAV_LEFT,
        NAV_RIGHT,
        NAV_UP,
        NAV_DOWN,
        CONFIRM,
        RELOAD_C1,     // C1: Reload / Import SD
        FAVORITE_C2,   // C2: Favorite ★
        RESET_C3,      // C3: Reset PP7, WB, EV
        DELETE_C4,     // C4: Delete recipe
        EDIT_FN,       // Fn: Edit recipe
        BACK_MENU,     // Menu: Exit / Back
        EXIT_SHUTTER   // Shutter: Quick exit
    }

    public static Action mapEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return Action.NONE;
        }

        int scan = event.getScanCode();
        int code = event.getKeyCode();

        // 1. Shutter button
        if (scan == SONY_S1 || scan == SONY_S2 || code == KeyEvent.KEYCODE_CAMERA) {
            return Action.EXIT_SHUTTER;
        }

        // 2. Rotary Dials & Left/Right
        if (scan == SONY_WHEEL_CW || scan == SONY_DIAL_CW || scan == SONY_DIAL2_CW ||
            scan == SONY_RIGHT || code == KeyEvent.KEYCODE_DPAD_RIGHT) {
            return Action.NAV_RIGHT;
        }
        if (scan == SONY_WHEEL_CCW || scan == SONY_DIAL_CCW || scan == SONY_DIAL2_CCW ||
            scan == SONY_LEFT || code == KeyEvent.KEYCODE_DPAD_LEFT) {
            return Action.NAV_LEFT;
        }

        // 3. Up/Down
        if (scan == SONY_UP || code == KeyEvent.KEYCODE_DPAD_UP) return Action.NAV_UP;
        if (scan == SONY_DOWN || code == KeyEvent.KEYCODE_DPAD_DOWN) return Action.NAV_DOWN;

        // 4. Center / Enter
        if (scan == SONY_ENTER || code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) {
            return Action.CONFIRM;
        }

        // 5. C1: RELOAD / IMPORT SD CARD
        if (scan == SONY_C1) {
            return Action.RELOAD_C1;
        }

        // 6. C2: FAVORITE ★
        if (scan == SONY_C2) {
            return Action.FAVORITE_C2;
        }

        // 7. C3: RESET PP7 / WB / EV
        if (scan == SONY_C3 || scan == SONY_CUSTOM || scan == SONY_AEL ||
            code == KeyEvent.KEYCODE_BUTTON_C || code == KeyEvent.KEYCODE_PROG_BLUE ||
            code == KeyEvent.KEYCODE_F3) {
            return Action.RESET_C3;
        }

        // 8. C4 / Trash: DELETE
        if (scan == SONY_DELETE || code == KeyEvent.KEYCODE_DEL || code == KeyEvent.KEYCODE_FORWARD_DEL) {
            return Action.DELETE_C4;
        }

        // 9. Fn: EDIT
        if (scan == SONY_FN || code == KeyEvent.KEYCODE_FUNCTION) {
            return Action.EDIT_FN;
        }

        // 10. Menu: EXIT / BACK
        if (scan == SONY_MENU || code == KeyEvent.KEYCODE_MENU || code == KeyEvent.KEYCODE_BACK) {
            return Action.BACK_MENU;
        }

        return Action.NONE;
    }
}
