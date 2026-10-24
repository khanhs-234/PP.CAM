package com.khanhs.ppcam;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FilmCarouselView extends View {
    public interface CarouselListener {
        void onApplyPreset(PPPreset preset);
        void onResetPP7();
        void onDeletePreset(PPPreset preset);
        void onToggleFavorite(PPPreset preset);
        void onReloadSD();
        void onOpenEditor(PPPreset preset);
        void onExitApp();
    }

    public enum Mode {
        CAROUSEL,
        CONFIRM_DELETE,
        CONFIRM_RESET,
        EDITOR,
        KEYBOARD
    }

    private Mode currentMode = Mode.CAROUSEL;
    private CarouselListener listener;

    private List<PPPreset> presets = new ArrayList<PPPreset>();
    private int currentIndex = 0;

    // Toast Notification Banner
    private String bannerMessage = "";
    private long bannerExpiryTime = 0;

    // -------------------------------------------------------------
    // Full 100% Parameter List for Editor Mode (DRO removed)
    // -------------------------------------------------------------
    private PPPreset editingPreset = null;
    private int editorRowIndex = 0;
    private static final String[] EDITOR_ROWS = {
        "1. Preset Name",
        "2. Gamma",
        "3. Black Level",
        "4. Black Gamma Range",
        "5. Black Gamma Level",
        "6. Knee Mode",
        "7. Knee Auto Max Point",
        "8. Knee Auto Sensitivity",
        "9. Knee Manual Point",
        "10. Knee Manual Slope",
        "11. Color Mode",
        "12. Saturation",
        "13. Color Phase",
        "14. Color Depth R (Red)",
        "15. Color Depth G (Green)",
        "16. Color Depth B (Blue)",
        "17. Color Depth C (Cyan)",
        "18. Color Depth M (Magenta)",
        "19. Color Depth Y (Yellow)",
        "20. Detail Level",
        "21. Detail Adjust Mode",
        "22. Detail V/H Balance",
        "23. Detail B/W Balance",
        "24. Detail Limit",
        "25. Detail Crispening",
        "26. Detail Hi-Light Detail",
        "27. White Balance Mode",
        "28. WB Kelvin Temperature",
        "29. WB A-B Shift",
        "30. WB G-M Shift",
        "31. Exposure Bias (EV)",
        "32. [ RESET PP7 TO DEFAULT ]",
        ">>> [ SAVE AS NEW FILM ROLL ] <<<"
    };

    // -------------------------------------------------------------
    // Virtual Keyboard for Name Editing
    // -------------------------------------------------------------
    private StringBuilder keyboardBuffer = new StringBuilder();
    private int kbRow = 0;
    private int kbCol = 0;
    private static final String[][] KB_KEYS = {
        { "A", "B", "C", "D", "E", "F", "G", "H" },
        { "I", "J", "K", "L", "M", "N", "O", "P" },
        { "Q", "R", "S", "T", "U", "V", "W", "X" },
        { "Y", "Z", "0", "1", "2", "3", "4", "5" },
        { "6", "7", "8", "9", "-", "SPACE", "DEL", "OK" }
    };

    // Graphics Paints
    private final Paint pText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pDraw = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pBg = new Paint();
    private final RectF rect = new RectF();
    private final Path path = new Path();

    public FilmCarouselView(Context context) {
        super(context);
        init();
    }

    public FilmCarouselView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        pBg.setColor(0xFF101214);
        setFocusable(true);
    }

    public void setListener(CarouselListener listener) {
        this.listener = listener;
    }

    public void setPresets(List<PPPreset> list) {
        this.presets = list != null ? list : new ArrayList<PPPreset>();
        if (currentIndex >= presets.size()) {
            currentIndex = Math.max(0, presets.size() - 1);
        }
        invalidate();
    }

    public PPPreset getCurrentPreset() {
        if (presets.isEmpty()) return null;
        return presets.get(currentIndex);
    }

    public void showBanner(String msg) {
        bannerMessage = msg;
        bannerExpiryTime = System.currentTimeMillis() + 1600;
        invalidate();
    }

    // -------------------------------------------------------------
    // KEY EVENT DISPATCHER
    // -------------------------------------------------------------
    public void onAction(KeyCodes.Action action) {
        if (currentMode == Mode.KEYBOARD) {
            handleKeyboardAction(action);
            return;
        }
        if (currentMode == Mode.CONFIRM_DELETE) {
            handleDeleteModal(action);
            return;
        }
        if (currentMode == Mode.CONFIRM_RESET) {
            handleResetModal(action);
            return;
        }
        if (currentMode == Mode.EDITOR) {
            handleEditorAction(action);
            return;
        }

        // CAROUSEL MODE
        switch (action) {
            case NAV_LEFT:
                if (!presets.isEmpty()) {
                    currentIndex = (currentIndex - 1 + presets.size()) % presets.size();
                    invalidate();
                }
                break;
            case NAV_RIGHT:
                if (!presets.isEmpty()) {
                    currentIndex = (currentIndex + 1) % presets.size();
                    invalidate();
                }
                break;
            case CONFIRM:
                PPPreset current = getCurrentPreset();
                if (current != null && listener != null) {
                    listener.onApplyPreset(current);
                }
                break;
            case RELOAD_C1:
                if (listener != null) {
                    listener.onReloadSD();
                }
                break;
            case FAVORITE_C2:
                PPPreset favP = getCurrentPreset();
                if (favP != null && listener != null) {
                    listener.onToggleFavorite(favP);
                    invalidate();
                }
                break;
            case RESET_C3:
                currentMode = Mode.CONFIRM_RESET;
                invalidate();
                break;
            case DELETE_C4:
                PPPreset delP = getCurrentPreset();
                if (delP != null && delP.filePath != null) {
                    currentMode = Mode.CONFIRM_DELETE;
                    invalidate();
                } else {
                    showBanner("CANNOT DELETE BUILT-IN PRESET");
                }
                break;
            case EDIT_FN:
                PPPreset edP = getCurrentPreset();
                if (edP != null) {
                    editingPreset = edP.copy();
                    editorRowIndex = 0;
                    currentMode = Mode.EDITOR;
                    invalidate();
                }
                break;
            case BACK_MENU:
            case EXIT_SHUTTER:
                if (listener != null) listener.onExitApp();
                break;
            default:
                break;
        }
    }

    private void handleDeleteModal(KeyCodes.Action action) {
        if (action == KeyCodes.Action.CONFIRM) {
            PPPreset p = getCurrentPreset();
            if (p != null && listener != null) {
                listener.onDeletePreset(p);
            }
            currentMode = Mode.CAROUSEL;
            invalidate();
        } else if (action == KeyCodes.Action.BACK_MENU || action == KeyCodes.Action.DELETE_C4) {
            currentMode = Mode.CAROUSEL;
            invalidate();
        }
    }

    private void handleResetModal(KeyCodes.Action action) {
        if (action == KeyCodes.Action.CONFIRM) {
            if (listener != null) {
                listener.onResetPP7();
            }
            currentMode = Mode.CAROUSEL;
            invalidate();
        } else if (action == KeyCodes.Action.BACK_MENU || action == KeyCodes.Action.RESET_C3) {
            currentMode = Mode.CAROUSEL;
            invalidate();
        }
    }

    private void openKeyboard(String initialText) {
        keyboardBuffer.setLength(0);
        if (initialText != null) keyboardBuffer.append(initialText);
        kbRow = 0;
        kbCol = 0;
        currentMode = Mode.KEYBOARD;
        invalidate();
    }

    private void handleKeyboardAction(KeyCodes.Action action) {
        int rowCount = KB_KEYS.length;
        int colCount = KB_KEYS[kbRow].length;

        switch (action) {
            case NAV_UP:
                kbRow = (kbRow - 1 + rowCount) % rowCount;
                if (kbCol >= KB_KEYS[kbRow].length) kbCol = KB_KEYS[kbRow].length - 1;
                invalidate();
                break;
            case NAV_DOWN:
                kbRow = (kbRow + 1) % rowCount;
                if (kbCol >= KB_KEYS[kbRow].length) kbCol = KB_KEYS[kbRow].length - 1;
                invalidate();
                break;
            case NAV_LEFT:
                kbCol = (kbCol - 1 + colCount) % colCount;
                invalidate();
                break;
            case NAV_RIGHT:
                kbCol = (kbCol + 1) % colCount;
                invalidate();
                break;
            case CONFIRM:
                String key = KB_KEYS[kbRow][kbCol];
                if ("OK".equals(key)) {
                    if (editingPreset != null) {
                        String finalName = keyboardBuffer.toString().trim();
                        if (finalName.length() == 0) finalName = "CUSTOM ROLL";
                        editingPreset.name = finalName;
                        if (listener != null) {
                            listener.onOpenEditor(editingPreset);
                        }
                    }
                    currentMode = Mode.CAROUSEL;
                } else if ("DEL".equals(key)) {
                    if (keyboardBuffer.length() > 0) {
                        keyboardBuffer.deleteCharAt(keyboardBuffer.length() - 1);
                    }
                } else if ("SPACE".equals(key)) {
                    if (keyboardBuffer.length() < 18) keyboardBuffer.append(" ");
                } else {
                    if (keyboardBuffer.length() < 18) keyboardBuffer.append(key);
                }
                invalidate();
                break;
            case DELETE_C4:
                if (keyboardBuffer.length() > 0) {
                    keyboardBuffer.deleteCharAt(keyboardBuffer.length() - 1);
                    invalidate();
                }
                break;
            case BACK_MENU:
                currentMode = Mode.EDITOR;
                invalidate();
                break;
            default:
                break;
        }
    }

    private void handleEditorAction(KeyCodes.Action action) {
        if (action == KeyCodes.Action.BACK_MENU) {
            editingPreset = null;
            currentMode = Mode.CAROUSEL;
            invalidate();
            return;
        }

        if (action == KeyCodes.Action.NAV_UP) {
            editorRowIndex = (editorRowIndex - 1 + EDITOR_ROWS.length) % EDITOR_ROWS.length;
            invalidate();
            return;
        }
        if (action == KeyCodes.Action.NAV_DOWN) {
            editorRowIndex = (editorRowIndex + 1) % EDITOR_ROWS.length;
            invalidate();
            return;
        }

        // Row 0: Preset Name -> Open Virtual Keyboard
        if (editorRowIndex == 0 && action == KeyCodes.Action.CONFIRM) {
            openKeyboard(editingPreset != null ? editingPreset.name : "");
            return;
        }

        // Row 31: Reset PP7
        if (editorRowIndex == 31 && action == KeyCodes.Action.CONFIRM) {
            if (listener != null) listener.onResetPP7();
            currentMode = Mode.CAROUSEL;
            invalidate();
            return;
        }

        // Row 32: Save Recipe
        if (editorRowIndex == 32 && action == KeyCodes.Action.CONFIRM) {
            openKeyboard(editingPreset != null ? editingPreset.name : "MY ROLL");
            return;
        }

        // Parameter Adjustment
        int step = (action == KeyCodes.Action.NAV_RIGHT) ? 1 : (action == KeyCodes.Action.NAV_LEFT) ? -1 : 0;
        if (step != 0 && editingPreset != null) {
            adjustParam(editorRowIndex, step);
            invalidate();
        }
    }

    private void adjustParam(int row, int step) {
        switch (row) {
            case 1: // Gamma
                int[] gammas = { 3, 4, 5, 6, 7, 8, 9, 10, 11 };
                int gIdx = 0;
                for (int i = 0; i < gammas.length; i++) if (gammas[i] == editingPreset.gamma) gIdx = i;
                editingPreset.gamma = gammas[Math.max(0, Math.min(gammas.length - 1, gIdx + step))];
                break;
            case 2: // Black Level (-15..+15)
                editingPreset.blackLevel = Math.max(-15, Math.min(15, editingPreset.blackLevel + step));
                break;
            case 3: // Black Gamma Range
                editingPreset.blackGammaRange = (editingPreset.blackGammaRange + step + 3) % 3;
                break;
            case 4: // Black Gamma Level (-7..+7)
                editingPreset.blackGammaLevel = Math.max(-7, Math.min(7, editingPreset.blackGammaLevel + step));
                break;
            case 5: // Knee Mode (Auto/Manual)
                editingPreset.kneeMode = 1 - editingPreset.kneeMode;
                break;
            case 6: // Knee Auto Max
                editingPreset.kneeAutoMax = Math.max(0, Math.min(4, editingPreset.kneeAutoMax + step));
                break;
            case 7: // Knee Auto Sens
                editingPreset.kneeAutoSens = Math.max(0, Math.min(2, editingPreset.kneeAutoSens + step));
                break;
            case 8: // Knee Manual Point
                editingPreset.kneeManualPoint = Math.max(0, Math.min(12, editingPreset.kneeManualPoint + step));
                break;
            case 9: // Knee Manual Slope
                editingPreset.kneeManualSlope = Math.max(-5, Math.min(5, editingPreset.kneeManualSlope + step));
                break;
            case 10: // Color Mode
                int[] modes = { 3, 4, 5, 6, 7, 8, 9 };
                int mIdx = 0;
                for (int i = 0; i < modes.length; i++) if (modes[i] == editingPreset.colorMode) mIdx = i;
                editingPreset.colorMode = modes[Math.max(0, Math.min(modes.length - 1, mIdx + step))];
                break;
            case 11: // Saturation (-32..+32)
                editingPreset.saturation = Math.max(-32, Math.min(32, editingPreset.saturation + step));
                break;
            case 12: // Color Phase (-7..+7)
                editingPreset.colorPhase = Math.max(-7, Math.min(7, editingPreset.colorPhase + step));
                break;
            case 13: editingPreset.colorDepthR = Math.max(-7, Math.min(7, editingPreset.colorDepthR + step)); break;
            case 14: editingPreset.colorDepthG = Math.max(-7, Math.min(7, editingPreset.colorDepthG + step)); break;
            case 15: editingPreset.colorDepthB = Math.max(-7, Math.min(7, editingPreset.colorDepthB + step)); break;
            case 16: editingPreset.colorDepthC = Math.max(-7, Math.min(7, editingPreset.colorDepthC + step)); break;
            case 17: editingPreset.colorDepthM = Math.max(-7, Math.min(7, editingPreset.colorDepthM + step)); break;
            case 18: editingPreset.colorDepthY = Math.max(-7, Math.min(7, editingPreset.colorDepthY + step)); break;
            case 19: // Detail Level (-7..+7)
                editingPreset.detailLevel = Math.max(-7, Math.min(7, editingPreset.detailLevel + step));
                break;
            case 20: // Detail Adjust Mode (Auto/Manual)
                editingPreset.detailAdjustMode = 1 - editingPreset.detailAdjustMode;
                break;
            case 21: editingPreset.detailVhBalance = Math.max(-2, Math.min(2, editingPreset.detailVhBalance + step)); break;
            case 22: editingPreset.detailBwBalance = Math.max(0, Math.min(4, editingPreset.detailBwBalance + step)); break;
            case 23: editingPreset.detailLimit = Math.max(0, Math.min(7, editingPreset.detailLimit + step)); break;
            case 24: editingPreset.detailCrispening = Math.max(0, Math.min(7, editingPreset.detailCrispening + step)); break;
            case 25: editingPreset.detailHilight = Math.max(0, Math.min(4, editingPreset.detailHilight + step)); break;
            case 26: // WB Mode
                editingPreset.wbMode = (editingPreset.wbMode == 14) ? 1 : 14;
                break;
            case 27: // WB Kelvin (25..99)
                editingPreset.wbKelvin = Math.max(25, Math.min(99, editingPreset.wbKelvin + step));
                break;
            case 28: editingPreset.wbAbShift = Math.max(-7, Math.min(7, editingPreset.wbAbShift + step)); break;
            case 29: editingPreset.wbGmShift = Math.max(-7, Math.min(7, editingPreset.wbGmShift + step)); break;
            case 30: // Exposure EV (-6..+6 steps of 1/3)
                editingPreset.exposureBias = Math.max(-6, Math.min(6, editingPreset.exposureBias + step));
                break;
        }
    }

    private String getEditorValue(int row) {
        if (editingPreset == null) return "";
        switch (row) {
            case 0: return editingPreset.name;
            case 1: return editingPreset.getGammaLabel();
            case 2: return String.valueOf(editingPreset.blackLevel);
            case 3: return editingPreset.blackGammaRange == 0 ? "Wide" : editingPreset.blackGammaRange == 1 ? "Middle" : "Narrow";
            case 4: return String.valueOf(editingPreset.blackGammaLevel);
            case 5: return editingPreset.kneeMode == 0 ? "Auto" : "Manual";
            case 6: return (90 + editingPreset.kneeAutoMax * 2.5f) + "%";
            case 7: return editingPreset.kneeAutoSens == 0 ? "High" : editingPreset.kneeAutoSens == 1 ? "Mid" : "Low";
            case 8: return (75 + editingPreset.kneeManualPoint * 2.5f) + "%";
            case 9: return String.valueOf(editingPreset.kneeManualSlope);
            case 10: return editingPreset.getColorModeLabel();
            case 11: return (editingPreset.saturation >= 0 ? "+" : "") + editingPreset.saturation;
            case 12: return String.valueOf(editingPreset.colorPhase);
            case 13: return String.valueOf(editingPreset.colorDepthR);
            case 14: return String.valueOf(editingPreset.colorDepthG);
            case 15: return String.valueOf(editingPreset.colorDepthB);
            case 16: return String.valueOf(editingPreset.colorDepthC);
            case 17: return String.valueOf(editingPreset.colorDepthM);
            case 18: return String.valueOf(editingPreset.colorDepthY);
            case 19: return String.valueOf(editingPreset.detailLevel);
            case 20: return editingPreset.detailAdjustMode == 0 ? "Auto" : "Manual";
            case 21: return String.valueOf(editingPreset.detailVhBalance);
            case 22: return "Type " + (editingPreset.detailBwBalance + 1);
            case 23: return String.valueOf(editingPreset.detailLimit);
            case 24: return String.valueOf(editingPreset.detailCrispening);
            case 25: return String.valueOf(editingPreset.detailHilight);
            case 26: return editingPreset.wbMode == 14 ? "Kelvin" : "Auto";
            case 27: return (editingPreset.wbKelvin * 100) + "K";
            case 28: return editingPreset.wbAbShift == 0 ? "0" : (editingPreset.wbAbShift > 0 ? "A" + editingPreset.wbAbShift : "B" + (-editingPreset.wbAbShift));
            case 29: return editingPreset.wbGmShift == 0 ? "0" : (editingPreset.wbGmShift > 0 ? "M" + editingPreset.wbGmShift : "G" + (-editingPreset.wbGmShift));
            case 30: return editingPreset.getEvLabel();
            default: return "";
        }
    }

    // -------------------------------------------------------------
    // FITTED TEXT (PREVENTS OVERFLOW WITH HIGH-RES FONT)
    // -------------------------------------------------------------
    private void drawFittedText(Canvas canvas, String text, float x, float y, Paint paint, float maxW, Paint.Align align) {
        paint.setTextAlign(align);
        float origSize = paint.getTextSize();
        while (paint.measureText(text) > maxW && paint.getTextSize() > 11.0f) {
            paint.setTextSize(paint.getTextSize() - 0.5f);
        }
        canvas.drawText(text, x, y, paint);
        paint.setTextSize(origSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float rowH = h / 9.0f;
        canvas.drawRect(0, 0, w, h, pBg);

        if (currentMode == Mode.KEYBOARD) {
            drawVirtualKeyboard(canvas, w, h);
            return;
        }

        if (currentMode == Mode.EDITOR) {
            drawEditor(canvas, w, h, rowH);
            return;
        }

        PPPreset curr = getCurrentPreset();

        // -------------------------------------------------------------
        // ROW 1: HEADER
        // -------------------------------------------------------------
        pDraw.setColor(0xFF1E2228);
        canvas.drawRect(0, 0, w, rowH, pDraw);

        pText.setTextSize(14.0f);
        pText.setColor(0xFF00E5FF);
        drawFittedText(canvas, "[SD: " + presets.size() + " PRESETS]", 16, rowH * 0.65f, pText, w * 0.32f, Paint.Align.LEFT);

        pText.setColor(0xFFFFFFFF);
        pText.setFakeBoldText(true);
        drawFittedText(canvas, "PP.CAM (ILCE-7M2)", w * 0.5f, rowH * 0.65f, pText, w * 0.36f, Paint.Align.CENTER);
        pText.setFakeBoldText(false);

        pText.setColor(0xFFFFB300);
        drawFittedText(canvas, "[TARGET: PP7]", w - 16, rowH * 0.65f, pText, w * 0.30f, Paint.Align.RIGHT);

        // -------------------------------------------------------------
        // ROW 2: FAVORITE BADGE & FILE NAME
        // -------------------------------------------------------------
        float r2Y = rowH * 1.62f;
        if (curr != null) {
            if (curr.isFavorite) {
                pText.setColor(0xFFFFD700);
                pText.setTextSize(15.0f);
                pText.setFakeBoldText(true);
                drawFittedText(canvas, "★ FAVORITE FILM ROLL ★", w * 0.5f, r2Y, pText, w - 40, Paint.Align.CENTER);
                pText.setFakeBoldText(false);
            } else {
                pText.setColor(0xFF8E99A4);
                pText.setTextSize(13.0f);
                String fName = (curr.filePath != null) ? new File(curr.filePath).getName() : "BUILT-IN PRESET";
                drawFittedText(canvas, "FILE: " + fName, w * 0.5f, r2Y, pText, w - 40, Paint.Align.CENTER);
            }
        }

        // -------------------------------------------------------------
        // ROWS 3, 4, 5: RETRO ISOMETRIC 3D FILM CANISTER (Inspired by Game Boy screen)
        // -------------------------------------------------------------
        float stageCenterY = rowH * 3.82f;
        if (!presets.isEmpty()) {
            if (presets.size() > 1) {
                int prevIdx = (currentIndex - 1 + presets.size()) % presets.size();
                drawIsometricCanister(canvas, presets.get(prevIdx), w * 0.16f, stageCenterY, 0.65f, 0.45f);

                int nextIdx = (currentIndex + 1) % presets.size();
                drawIsometricCanister(canvas, presets.get(nextIdx), w * 0.84f, stageCenterY, 0.65f, 0.45f);
            }
            drawIsometricCanister(canvas, curr, w * 0.5f, stageCenterY, 1.0f, 1.0f);
        }

        // -------------------------------------------------------------
        // ROW 6: LARGE PRESET TITLE (Bold & High Legibility)
        // -------------------------------------------------------------
        float r6Y = rowH * 5.75f;
        pText.setTextSize(22.0f);
        pText.setColor(0xFFFFFFFF);
        pText.setFakeBoldText(true);
        if (curr != null) {
            drawFittedText(canvas, curr.name, w * 0.5f, r6Y, pText, w - 36, Paint.Align.CENTER);
        }
        pText.setFakeBoldText(false);

        // -------------------------------------------------------------
        // ROW 7: PP SUMMARY (DRO REMOVED, CLEAR FONT)
        // -------------------------------------------------------------
        float r7Y = rowH * 6.70f;
        pText.setTextSize(14.5f);
        pText.setColor(0xFFD4DCE4);
        if (curr != null) {
            String ppInfo = "PP7: " + curr.getGammaLabel() +
                            "  |  Color: " + curr.getColorModeLabel() +
                            "  |  Sat: " + (curr.saturation >= 0 ? "+" : "") + curr.saturation +
                            "  |  Phase: " + curr.colorPhase +
                            "  |  Detail: " + curr.detailLevel;
            drawFittedText(canvas, ppInfo, w * 0.5f, r7Y, pText, w - 24, Paint.Align.CENTER);
        }

        // -------------------------------------------------------------
        // ROW 8: WB & EXPOSURE (NO DRO, HIGH CONTRAST)
        // -------------------------------------------------------------
        float r8Y = rowH * 7.70f;
        pText.setTextSize(14.5f);
        pText.setColor(0xFFFFD54F);
        if (curr != null) {
            String colorInfo = "WB: " + curr.getWbLabel() + " (" + curr.getWbShiftLabel() + ")" +
                               "  |  EV: " + curr.getEvLabel() +
                               "  |  Black Level: " + curr.blackLevel;
            drawFittedText(canvas, colorInfo, w * 0.5f, r8Y, pText, w - 24, Paint.Align.CENTER);
        }

        // -------------------------------------------------------------
        // ROW 9: HINT BAR (PURE ENGLISH, NEAT SPACING)
        // -------------------------------------------------------------
        float r9Top = rowH * 8.0f;
        pDraw.setColor(0xFF000000);
        canvas.drawRect(0, r9Top, w, h, pDraw);
        pDraw.setColor(0xFF2C3238);
        canvas.drawLine(0, r9Top, w, r9Top, pDraw);

        pText.setTextSize(13.2f);
        pText.setColor(0xFFFF9800);
        drawFittedText(canvas, "[C1] Reload  [C2] ★ Fav  [C3] Reset  [C4] Del  [Fn] Edit  [Center] Overwrite PP7  [MENU] Exit",
                w * 0.5f, r9Top + rowH * 0.65f, pText, w - 20, Paint.Align.CENTER);

        // Toast Message Banner
        if (System.currentTimeMillis() < bannerExpiryTime) {
            drawToast(canvas, w, h, bannerMessage);
            postInvalidateDelayed(100);
        }

        // Confirmation modals
        if (currentMode == Mode.CONFIRM_DELETE) {
            drawConfirmDialog(canvas, w, h, "Delete recipe [" + (curr != null ? curr.name : "") + "] from SD card?",
                    "[Center] Confirm Delete", "[MENU] Cancel", 0xFFD32F2F);
        } else if (currentMode == Mode.CONFIRM_RESET) {
            drawConfirmDialog(canvas, w, h, "Reset PP7, WB, EV to factory defaults?",
                    "[Center] Confirm Reset", "[MENU] Cancel", 0xFFE65100);
        }
    }

    /**
     * Renders an iconic, retro isometric 3D film canister with dual-sprocket film leader
     * inspired by the Game Boy / retro camera aesthetic (Image 3).
     */
    private void drawIsometricCanister(Canvas canvas, PPPreset p, float cx, float cy, float scale, float opacity) {
        if (p == null) return;
        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(scale, scale);

        int alpha = (int) (255 * opacity);

        float canW = 96.0f;
        float canH = 126.0f;
        float halfW = canW * 0.5f;
        float halfH = canH * 0.5f;

        // Perspective ellipse heights
        float ry = 13.0f;

        // 1. Film Leader Strip (Long extending strip to the right with dual rows of sprocket holes)
        float stripX = halfW - 8.0f;
        float stripY = -halfH + 20.0f;
        float stripW = 82.0f;
        float stripH = canH - 32.0f;

        // Dark charcoal acetate film base
        pDraw.setColor(0xFF2E2218);
        pDraw.setAlpha(alpha);
        rect.set(stripX, stripY, stripX + stripW, stripY + stripH);
        canvas.drawRoundRect(rect, 4, 4, pDraw);

        // Film border outline
        pDraw.setStyle(Paint.Style.STROKE);
        pDraw.setStrokeWidth(2.0f);
        pDraw.setColor(0xFF4A3828);
        pDraw.setAlpha(alpha);
        canvas.drawRoundRect(rect, 4, 4, pDraw);
        pDraw.setStyle(Paint.Style.FILL);

        // Dual Rows of Sprocket Holes (Top and Bottom rows of holes)
        pDraw.setColor(0xFF101214); // Screen background cutout
        pDraw.setAlpha(alpha);
        int numHoles = 5;
        float holeW = 9.0f;
        float holeH = 12.0f;
        float holeGap = 15.0f;

        for (int i = 0; i < numHoles; i++) {
            float hx = stripX + 12.0f + (i * holeGap);
            // Top sprocket row
            rect.set(hx, stripY + 5.0f, hx + holeW, stripY + 5.0f + holeH);
            canvas.drawRoundRect(rect, 2, 2, pDraw);
            // Bottom sprocket row
            rect.set(hx, stripY + stripH - 5.0f - holeH, hx + holeW, stripY + stripH - 5.0f);
            canvas.drawRoundRect(rect, 2, 2, pDraw);
        }

        // Felt Velvet Black Exit Lip
        pDraw.setColor(0xFF080808);
        pDraw.setAlpha(alpha);
        rect.set(halfW - 10.0f, -halfH + 12, halfW + 1.0f, halfH - 12);
        canvas.drawRoundRect(rect, 2, 2, pDraw);

        // 2. Canister Main Body (Cylindrical gradient with metallic highlights)
        int shadeDark, shadeLight, baseColor, brandColor;
        String brandLabel = "FILM";
        String speedLabel = "36";

        switch (p.filmType) {
            case PPPreset.TYPE_KODAK:
                shadeDark = 0xFFB37400;
                shadeLight = 0xFFFFEA80;
                baseColor = 0xFFF5A623;
                brandColor = 0xFFD32F2F;
                brandLabel = "KODAK";
                speedLabel = "200";
                break;
            case PPPreset.TYPE_FUJI:
                shadeDark = 0xFF0D4D20;
                shadeLight = 0xFF63C780;
                baseColor = 0xFF1E7E34;
                brandColor = 0xFFFFFFFF;
                brandLabel = "FUJI";
                speedLabel = "400";
                break;
            case PPPreset.TYPE_CINESTILL:
                shadeDark = 0xFF181A1C;
                shadeLight = 0xFF54575A;
                baseColor = 0xFF2B2D2F;
                brandColor = 0xFFE53935;
                brandLabel = "CINE";
                speedLabel = "50D";
                break;
            case PPPreset.TYPE_MONOCHROME:
                shadeDark = 0xFF121212;
                shadeLight = 0xFF707070;
                baseColor = 0xFF2A2A2A;
                brandColor = 0xFFFFFFFF;
                brandLabel = "B&W";
                speedLabel = "100";
                break;
            case PPPreset.TYPE_BODY_CUSTOM:
            default:
                shadeDark = 0xFF350A50;
                shadeLight = 0xFFBF5BFF;
                baseColor = 0xFF6A1B9A;
                brandColor = 0xFF00E5FF;
                brandLabel = "CUSTOM";
                speedLabel = "PP7";
                break;
        }

        // Bottom Cap Ellipse
        pDraw.setColor(0xFF181818);
        pDraw.setAlpha(alpha);
        rect.set(-halfW - 2, halfH - ry - 2, halfW + 2, halfH + ry + 2);
        canvas.drawOval(rect, pDraw);

        // Cylinder Body
        pDraw.setShader(new LinearGradient(-halfW, 0, halfW, 0,
                new int[] { shadeDark, shadeLight, baseColor, shadeDark },
                new float[] { 0.0f, 0.24f, 0.65f, 1.0f },
                Shader.TileMode.CLAMP));
        pDraw.setAlpha(alpha);
        rect.set(-halfW, -halfH, halfW, halfH);
        canvas.drawRect(rect, pDraw);

        // Cylinder Body Contour Outline
        pDraw.setShader(null);
        pDraw.setStyle(Paint.Style.STROKE);
        pDraw.setStrokeWidth(2.0f);
        pDraw.setColor(0xFF000000);
        pDraw.setAlpha(alpha);
        canvas.drawLine(-halfW, -halfH, -halfW, halfH, pDraw);
        canvas.drawLine(halfW, -halfH, halfW, halfH, pDraw);
        pDraw.setStyle(Paint.Style.FILL);

        // Top Metallic Rim Ellipse
        pDraw.setShader(new LinearGradient(-halfW, -halfH - ry, halfW, -halfH + ry,
                new int[] { 0xFF202020, 0xFF707070, 0xFF202020 }, null, Shader.TileMode.CLAMP));
        pDraw.setAlpha(alpha);
        rect.set(-halfW - 2, -halfH - ry, halfW + 2, -halfH + ry);
        canvas.drawOval(rect, pDraw);

        // 3. Spool Knob on Top (Central core sticking out)
        pDraw.setShader(new LinearGradient(-14, 0, 14, 0,
                new int[] { 0xFF181818, 0xFF555555, 0xFF181818 }, null, Shader.TileMode.CLAMP));
        pDraw.setAlpha(alpha);
        rect.set(-14, -halfH - ry - 18, 14, -halfH - ry + 4);
        canvas.drawRoundRect(rect, 4, 4, pDraw);

        // Spool hollow oval on top
        pDraw.setShader(null);
        pDraw.setColor(0xFF0A0A0A);
        pDraw.setAlpha(alpha);
        rect.set(-10, -halfH - ry - 20, 10, -halfH - ry - 14);
        canvas.drawOval(rect, pDraw);

        // 4. Bold Retro Graphic Branding on Canister
        pText.setAlpha(alpha);
        pText.setTextAlign(Paint.Align.CENTER);
        pText.setFakeBoldText(true);

        // Brand Label
        pText.setColor(brandColor);
        pText.setTextSize(18.0f);
        canvas.drawText(brandLabel, -6, -14, pText);

        // Big Retro Speed Number
        pText.setColor(0xFFFFFFFF);
        pText.setTextSize(26.0f);
        canvas.drawText(speedLabel, -6, 16, pText);

        // Subtitle line
        pText.setTextSize(10.5f);
        pText.setColor(0xFFDDDDDD);
        pText.setFakeBoldText(false);
        canvas.drawText("35mm • 36 EXP", -6, 36, pText);

        // Star badge if favorite
        if (p.isFavorite) {
            pText.setColor(0xFFFFD700);
            pText.setTextSize(22.0f);
            canvas.drawText("★", -halfW + 18, -halfH + 18, pText);
        }

        canvas.restore();
    }

    // -------------------------------------------------------------
    // ON-SCREEN VIRTUAL KEYBOARD VIEW
    // -------------------------------------------------------------
    private void drawVirtualKeyboard(Canvas canvas, float w, float h) {
        float rowH = h / 9.0f;

        // Title Bar
        pDraw.setColor(0xFF1E2228);
        canvas.drawRect(0, 0, w, rowH, pDraw);
        pText.setTextSize(16.0f);
        pText.setColor(0xFFFF9800);
        pText.setFakeBoldText(true);
        drawFittedText(canvas, "NAME YOUR FILM ROLL", w * 0.5f, rowH * 0.65f, pText, w - 40, Paint.Align.CENTER);
        pText.setFakeBoldText(false);

        // Input Field Box
        float boxTop = rowH * 1.3f;
        float boxH = rowH * 1.0f;
        pDraw.setColor(0xFF263238);
        rect.set(24, boxTop, w - 24, boxTop + boxH);
        canvas.drawRoundRect(rect, 6, 6, pDraw);

        pText.setTextSize(20.0f);
        pText.setColor(0xFF00E5FF);
        pText.setFakeBoldText(true);
        String displayBuffer = "[" + keyboardBuffer.toString() + "_]";
        drawFittedText(canvas, displayBuffer, w * 0.5f, boxTop + boxH * 0.68f, pText, w - 60, Paint.Align.CENTER);
        pText.setFakeBoldText(false);

        // Keyboard Grid
        float gridTop = boxTop + boxH + 12;
        float gridH = (rowH * 8.0f - gridTop);
        float keyH = gridH / KB_KEYS.length;

        for (int r = 0; r < KB_KEYS.length; r++) {
            float keyW = (w - 48.0f) / KB_KEYS[r].length;
            float y = gridTop + (r * keyH);

            for (int c = 0; c < KB_KEYS[r].length; c++) {
                float x = 24.0f + (c * keyW);
                boolean isSel = (r == kbRow && c == kbCol);

                pDraw.setColor(isSel ? 0xFF0091EA : 0xFF212529);
                rect.set(x + 2, y + 2, x + keyW - 2, y + keyH - 2);
                canvas.drawRoundRect(rect, 4, 4, pDraw);

                pText.setTextSize(keyH * 0.52f);
                pText.setColor(isSel ? 0xFFFFFFFF : 0xFFDDDDDD);
                pText.setFakeBoldText(isSel);
                drawFittedText(canvas, KB_KEYS[r][c], x + keyW * 0.5f, y + keyH * 0.68f, pText, keyW - 4, Paint.Align.CENTER);
                pText.setFakeBoldText(false);
            }
        }

        // Keyboard Footer Hint
        float fTop = rowH * 8.0f;
        pDraw.setColor(0xFF000000);
        canvas.drawRect(0, fTop, w, h, pDraw);
        pText.setTextSize(13.2f);
        pText.setColor(0xFFFF9800);
        drawFittedText(canvas, "[Wheel/D-Pad] Move   [Center] Type   [C4/DEL] Delete   [MENU] Cancel",
                w * 0.5f, fTop + rowH * 0.65f, pText, w - 20, Paint.Align.CENTER);
    }

    // -------------------------------------------------------------
    // EDITOR VIEW (FULL 100% PARAMETERS LIST)
    // -------------------------------------------------------------
    private void drawEditor(Canvas canvas, float w, float h, float rowH) {
        // Header
        pDraw.setColor(0xFF1E2228);
        canvas.drawRect(0, 0, w, rowH, pDraw);
        pText.setColor(0xFFFF9800);
        pText.setTextSize(16.0f);
        pText.setFakeBoldText(true);
        drawFittedText(canvas, "RECIPE EDITOR (100% PP + WB + EV)", w * 0.5f, rowH * 0.65f, pText, w - 40, Paint.Align.CENTER);
        pText.setFakeBoldText(false);

        // Items Viewport
        float startY = rowH * 1.40f;
        float itemH = rowH * 0.62f;
        int maxVisible = 9;
        int startIdx = Math.max(0, editorRowIndex - 4);

        for (int i = 0; i < maxVisible && (startIdx + i) < EDITOR_ROWS.length; i++) {
            int row = startIdx + i;
            float y = startY + (i * itemH);
            boolean isSelected = (row == editorRowIndex);

            if (isSelected) {
                pDraw.setColor(0xFF37474F);
                rect.set(16, y - itemH * 0.72f, w - 16, y + itemH * 0.28f);
                canvas.drawRoundRect(rect, 4, 4, pDraw);
            }

            pText.setTextSize(13.5f);
            pText.setColor(isSelected ? 0xFF00E5FF : 0xFFCCCCCC);
            drawFittedText(canvas, EDITOR_ROWS[row], 24, y, pText, w * 0.58f, Paint.Align.LEFT);

            // Value column
            if (row < 31 && editingPreset != null) {
                String valStr = getEditorValue(row);
                pText.setColor(isSelected ? 0xFFFFD54F : 0xFFFFFFFF);
                drawFittedText(canvas, valStr, w - 24, y, pText, w * 0.35f, Paint.Align.RIGHT);
            }
        }

        // Editor Footer
        float fTop = rowH * 8.0f;
        pDraw.setColor(0xFF000000);
        canvas.drawRect(0, fTop, w, h, pDraw);
        pText.setTextSize(13.2f);
        pText.setColor(0xFFFF9800);
        drawFittedText(canvas, "[Up/Down] Select   [Left/Right] Adjust   [Center] Edit/Save   [MENU] Discard",
                w * 0.5f, fTop + rowH * 0.65f, pText, w - 20, Paint.Align.CENTER);
    }

    private void drawConfirmDialog(Canvas canvas, float w, float h, String title, String btnOk, String btnCancel, int headerColor) {
        pDraw.setColor(0xDD000000);
        canvas.drawRect(0, 0, w, h, pDraw);

        float dw = w * 0.76f;
        float dh = h * 0.44f;
        float dx = (w - dw) * 0.5f;
        float dy = (h - dh) * 0.5f;

        pDraw.setColor(0xFF1E2228);
        rect.set(dx, dy, dx + dw, dy + dh);
        canvas.drawRoundRect(rect, 10, 10, pDraw);

        pDraw.setColor(headerColor);
        rect.set(dx, dy, dx + dw, dy + dh * 0.25f);
        canvas.drawRoundRect(rect, 10, 10, pDraw);

        pText.setColor(0xFFFFFFFF);
        pText.setTextSize(15.5f);
        pText.setFakeBoldText(true);
        drawFittedText(canvas, "CONFIRM ACTION", w * 0.5f, dy + dh * 0.18f, pText, dw - 20, Paint.Align.CENTER);

        pText.setTextSize(13.5f);
        pText.setFakeBoldText(false);
        drawFittedText(canvas, title, w * 0.5f, dy + dh * 0.52f, pText, dw - 20, Paint.Align.CENTER);

        pText.setColor(0xFFFF9800);
        pText.setTextSize(13.0f);
        drawFittedText(canvas, btnOk + "   |   " + btnCancel, w * 0.5f, dy + dh * 0.82f, pText, dw - 20, Paint.Align.CENTER);
    }

    private void drawToast(Canvas canvas, float w, float h, String msg) {
        float tw = w * 0.72f;
        float th = 40.0f;
        float tx = (w - tw) * 0.5f;
        float ty = h * 0.40f;

        pDraw.setColor(0xEE2E7D32);
        rect.set(tx, ty, tx + tw, ty + th);
        canvas.drawRoundRect(rect, 6, 6, pDraw);

        pText.setColor(0xFFFFFFFF);
        pText.setTextSize(14.5f);
        pText.setFakeBoldText(true);
        drawFittedText(canvas, msg, w * 0.5f, ty + 26, pText, tw - 16, Paint.Align.CENTER);
        pText.setFakeBoldText(false);
    }
}
