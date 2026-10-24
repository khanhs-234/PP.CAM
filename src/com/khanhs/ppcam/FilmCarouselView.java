package com.khanhs.ppcam;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
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
    // Custom Fonts (Minimalist Retro Aesthetic)
    // -------------------------------------------------------------
    private Typeface customFont;
    private Typeface customFontBold;

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
    private final Path cardPath = new Path();

    public FilmCarouselView(Context context) {
        super(context);
        init();
    }

    public FilmCarouselView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        pBg.setColor(0xFF191B1D); // Soft dark slate film background
        setFocusable(true);

        // Load Custom Fonts from assets/fonts/
        try {
            customFont = Typeface.createFromAsset(getContext().getAssets(), "fonts/Oswald-Regular.ttf");
            customFontBold = Typeface.createFromAsset(getContext().getAssets(), "fonts/Oswald-Bold.ttf");
        } catch (Exception e) {
            customFont = Typeface.SANS_SERIF;
            customFontBold = Typeface.DEFAULT_BOLD;
        }
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

        // Clean dark slate film canvas
        canvas.drawRect(0, 0, w, h, pBg);

        if (currentMode == Mode.KEYBOARD) {
            drawVirtualKeyboard(canvas, w, h);
            return;
        }

        if (currentMode == Mode.EDITOR) {
            drawEditor(canvas, w, h);
            return;
        }

        PPPreset curr = getCurrentPreset();

        // -------------------------------------------------------------
        // HEADER BAR (Minimalist Slate Header)
        // -------------------------------------------------------------
        float headerH = 42.0f;
        pDraw.setColor(0xFF22262B);
        canvas.drawRect(0, 0, w, headerH, pDraw);
        pDraw.setColor(0xFF2E343B);
        canvas.drawLine(0, headerH, w, headerH, pDraw);

        pText.setTypeface(customFontBold != null ? customFontBold : Typeface.DEFAULT_BOLD);
        pText.setTextSize(17.0f);

        pText.setColor(0xFF00E5FF);
        drawFittedText(canvas, "[SD: " + presets.size() + " ROLLS]", 18, 27, pText, w * 0.32f, Paint.Align.LEFT);

        pText.setColor(0xFFFFFFFF);
        drawFittedText(canvas, "PP.CAM", w * 0.5f, 27, pText, w * 0.32f, Paint.Align.CENTER);

        pText.setColor(0xFFFFB300);
        drawFittedText(canvas, "[SLOT: PP7]", w - 18, 27, pText, w * 0.32f, Paint.Align.RIGHT);

        // -------------------------------------------------------------
        // MINIMALIST RETRO FLAT FILM BOX CAROUSEL
        // -------------------------------------------------------------
        float stageCenterY = 142.0f;
        if (!presets.isEmpty()) {
            if (presets.size() > 1) {
                int prevIdx = (currentIndex - 1 + presets.size()) % presets.size();
                drawFlatFilmBox(canvas, presets.get(prevIdx), w * 0.16f, stageCenterY, 0.70f, 0.40f);

                int nextIdx = (currentIndex + 1) % presets.size();
                drawFlatFilmBox(canvas, presets.get(nextIdx), w * 0.84f, stageCenterY, 0.70f, 0.40f);
            }
            drawFlatFilmBox(canvas, curr, w * 0.5f, stageCenterY, 1.0f, 1.0f);
        }

        // -------------------------------------------------------------
        // PRESET TITLE BANNER & FAVORITE STATUS
        // -------------------------------------------------------------
        float titleY = 250.0f;
        pText.setTypeface(customFontBold != null ? customFontBold : Typeface.DEFAULT_BOLD);
        pText.setTextSize(32.0f);
        pText.setColor(0xFFFFFFFF);
        if (curr != null) {
            drawFittedText(canvas, curr.name, w * 0.5f, titleY, pText, w - 40, Paint.Align.CENTER);
        }

        float statusY = 274.0f;
        pText.setTextSize(15.0f);
        if (curr != null) {
            if (curr.isFavorite) {
                pText.setColor(0xFFFFD700);
                drawFittedText(canvas, "★ FAVORITE RECIPE ★", w * 0.5f, statusY, pText, w - 40, Paint.Align.CENTER);
            } else {
                pText.setColor(0xFF7E8A96);
                pText.setTypeface(customFont != null ? customFont : Typeface.DEFAULT);
                String fName = (curr.filePath != null) ? new File(curr.filePath).getName() : "BUILT-IN PRESET";
                drawFittedText(canvas, "FILE: " + fName, w * 0.5f, statusY, pText, w - 40, Paint.Align.CENTER);
            }
        }

        // -------------------------------------------------------------
        // MINIMALIST 2-COLUMN TECHNICAL SPECIFICATIONS GRID
        // -------------------------------------------------------------
        float gridTop = 302.0f;
        float lineGap = 28.0f;
        float col1X = w * 0.14f;
        float col2X = w * 0.56f;

        // Background panel for grid
        pDraw.setColor(0xFF1E2228);
        rect.set(w * 0.08f, gridTop - 18, w * 0.92f, gridTop + lineGap * 2 + 14);
        canvas.drawRoundRect(rect, 8, 8, pDraw);

        pText.setTypeface(customFontBold != null ? customFontBold : Typeface.DEFAULT_BOLD);
        pText.setTextSize(18.5f);

        if (curr != null) {
            // Column 1 (Left)
            pText.setColor(0xFFE2E8F0);
            drawFittedText(canvas, "GAMMA: " + curr.getGammaLabel(), col1X, gridTop + 2, pText, w * 0.38f, Paint.Align.LEFT);
            drawFittedText(canvas, "COLOR: " + curr.getColorModeLabel(), col1X, gridTop + lineGap + 2, pText, w * 0.38f, Paint.Align.LEFT);

            pText.setColor(0xFFFFD54F);
            drawFittedText(canvas, "WB: " + curr.getWbLabel() + " (" + curr.getWbShiftLabel() + ")", col1X, gridTop + lineGap * 2 + 2, pText, w * 0.40f, Paint.Align.LEFT);

            // Column 2 (Right)
            pText.setColor(0xFFE2E8F0);
            drawFittedText(canvas, "SAT: " + (curr.saturation >= 0 ? "+" : "") + curr.saturation, col2X, gridTop + 2, pText, w * 0.34f, Paint.Align.LEFT);
            drawFittedText(canvas, "DETAIL: " + curr.detailLevel, col2X, gridTop + lineGap + 2, pText, w * 0.34f, Paint.Align.LEFT);

            pText.setColor(0xFFFFD54F);
            drawFittedText(canvas, "EV: " + curr.getEvLabel() + "  |  BL: " + curr.blackLevel, col2X, gridTop + lineGap * 2 + 2, pText, w * 0.34f, Paint.Align.LEFT);
        }

        // -------------------------------------------------------------
        // HINT BAR (Minimalist Slate Footer)
        // -------------------------------------------------------------
        float footerH = 46.0f;
        float footerY = h - footerH;
        pDraw.setColor(0xFF22262B);
        canvas.drawRect(0, footerY, w, h, pDraw);
        pDraw.setColor(0xFF2E343B);
        canvas.drawLine(0, footerY, w, footerY, pDraw);

        pText.setTextSize(14.8f);
        pText.setColor(0xFFFFA726);
        pText.setTypeface(customFontBold != null ? customFontBold : Typeface.DEFAULT_BOLD);
        drawFittedText(canvas, "[C1] RELOAD   [C2] ★ FAV   [C3] RESET   [C4] DEL   [Fn] EDIT   [CENTER] OVERWRITE PP7   [MENU] EXIT",
                w * 0.5f, footerY + 29, pText, w - 24, Paint.Align.CENTER);

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
     * Renders a Minimalist Flat 2D Film Box Label (Clean Retro Aesthetic).
     */
    private void drawFlatFilmBox(Canvas canvas, PPPreset p, float cx, float cy, float scale, float opacity) {
        if (p == null) return;
        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(scale, scale);

        int alpha = (int) (255 * opacity);

        float boxW = 124.0f;
        float boxH = 162.0f;
        float halfW = boxW * 0.5f;
        float halfH = boxH * 0.5f;

        int baseColor, accentColor;
        String brandLabel, speedLabel;

        switch (p.filmType) {
            case PPPreset.TYPE_KODAK:
                baseColor = 0xFFFFD700; // Kodak Gold
                accentColor = 0xFFD32F2F; // Red
                brandLabel = "KODAK";
                speedLabel = "200";
                break;
            case PPPreset.TYPE_FUJI:
                baseColor = 0xFF1E7E34; // Fuji Emerald Green
                accentColor = 0xFFFFFFFF; // White
                brandLabel = "FUJIFILM";
                speedLabel = "400";
                break;
            case PPPreset.TYPE_CINESTILL:
                baseColor = 0xFF181A1C; // Deep Slate Titanium
                accentColor = 0xFFE53935; // CineStill Red
                brandLabel = "CINESTILL";
                speedLabel = "50D";
                break;
            case PPPreset.TYPE_MONOCHROME:
                baseColor = 0xFF2B2B2B; // Monochrome Black
                accentColor = 0xFFFFFFFF; // White
                brandLabel = "ILFORD";
                speedLabel = "100";
                break;
            case PPPreset.TYPE_BODY_CUSTOM:
            default:
                baseColor = 0xFF5B168C; // Cyber Violet
                accentColor = 0xFF00E5FF; // Cyan
                brandLabel = "CUSTOM";
                speedLabel = "PP7";
                break;
        }

        // 1. Subtle card drop shadow
        pDraw.setColor(0x55000000);
        rect.set(-halfW + 4, -halfH + 5, halfW + 4, halfH + 5);
        canvas.drawRoundRect(rect, 8, 8, pDraw);

        // 2. Off-white clean film card base
        pDraw.setColor(0xFFF5F5F5);
        pDraw.setAlpha(alpha);
        rect.set(-halfW, -halfH, halfW, halfH);
        canvas.drawRoundRect(rect, 8, 8, pDraw);

        // 3. Top Color Block Header (46% of height)
        float headerBlockH = boxH * 0.46f;
        pDraw.setColor(baseColor);
        pDraw.setAlpha(alpha);
        rect.set(-halfW, -halfH, halfW, -halfH + headerBlockH);

        cardPath.reset();
        cardPath.addRoundRect(rect, new float[] { 8, 8, 8, 8, 0, 0, 0, 0 }, Path.Direction.CW);
        canvas.drawPath(cardPath, pDraw);

        // 4. Brand Label (Top left inside color block)
        pText.setAlpha(alpha);
        pText.setTextAlign(Paint.Align.LEFT);
        pText.setTypeface(customFontBold != null ? customFontBold : Typeface.DEFAULT_BOLD);
        pText.setColor(accentColor);
        pText.setTextSize(21.0f);
        canvas.drawText(brandLabel, -halfW + 12, -halfH + 28, pText);

        // 5. Large ISO Speed Number
        pText.setColor(accentColor == 0xFFFFFFFF ? 0xFFFFFFFF : 0xFF151515);
        pText.setTextSize(40.0f);
        canvas.drawText(speedLabel, -halfW + 12, -halfH + 67, pText);

        // 6. Bottom Information (On off-white paper base)
        pText.setTypeface(customFont != null ? customFont : Typeface.DEFAULT);
        pText.setColor(0xFF757575);
        pText.setTextSize(13.0f);
        canvas.drawText("ISO / ASA", -halfW + 12, 12, pText);

        // Preset Display Name
        pText.setTextSize(19.0f);
        pText.setTypeface(customFontBold != null ? customFontBold : Typeface.DEFAULT_BOLD);
        pText.setColor(0xFF1E2228);
        String shortName = p.name.length() > 11 ? p.name.substring(0, 11) : p.name;
        canvas.drawText(shortName, -halfW + 12, 38, pText);

        // Film Format line
        pText.setTypeface(customFont != null ? customFont : Typeface.DEFAULT);
        pText.setColor(0xFF888888);
        pText.setTextSize(12.0f);
        canvas.drawText("35mm • 36 EXP", -halfW + 12, 60, pText);

        // 7. Favorite Star Icon
        if (p.isFavorite) {
            pText.setColor(0xFFF5A623);
            pText.setTextSize(26.0f);
            pText.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("★", halfW - 10, halfH - 12, pText);
        }

        canvas.restore();
    }

    // -------------------------------------------------------------
    // ON-SCREEN VIRTUAL KEYBOARD VIEW
    // -------------------------------------------------------------
    private void drawVirtualKeyboard(Canvas canvas, float w, float h) {
        float headerH = 44.0f;

        // Title Bar
        pDraw.setColor(0xFF22262B);
        canvas.drawRect(0, 0, w, headerH, pDraw);
        pText.setTypeface(customFontBold != null ? customFontBold : Typeface.DEFAULT_BOLD);
        pText.setTextSize(19.0f);
        pText.setColor(0xFFFF9800);
        drawFittedText(canvas, "NAME YOUR FILM ROLL", w * 0.5f, 29, pText, w - 40, Paint.Align.CENTER);

        // Input Field Box
        float boxTop = 58.0f;
        float boxH = 48.0f;
        pDraw.setColor(0xFF22272E);
        rect.set(24, boxTop, w - 24, boxTop + boxH);
        canvas.drawRoundRect(rect, 8, 8, pDraw);

        pText.setTextSize(26.0f);
        pText.setColor(0xFF00E5FF);
        String displayBuffer = "[" + keyboardBuffer.toString() + "_]";
        drawFittedText(canvas, displayBuffer, w * 0.5f, boxTop + 34, pText, w - 60, Paint.Align.CENTER);

        // Keyboard Grid
        float gridTop = boxTop + boxH + 14;
        float gridH = (h - 46.0f - gridTop);
        float keyH = gridH / KB_KEYS.length;

        for (int r = 0; r < KB_KEYS.length; r++) {
            float keyW = (w - 48.0f) / KB_KEYS[r].length;
            float y = gridTop + (r * keyH);

            for (int c = 0; c < KB_KEYS[r].length; c++) {
                float x = 24.0f + (c * keyW);
                boolean isSel = (r == kbRow && c == kbCol);

                pDraw.setColor(isSel ? 0xFF0091EA : 0xFF24292F);
                rect.set(x + 2, y + 2, x + keyW - 2, y + keyH - 2);
                canvas.drawRoundRect(rect, 6, 6, pDraw);

                pText.setTextSize(keyH * 0.52f);
                pText.setColor(isSel ? 0xFFFFFFFF : 0xFFCFD8DC);
                drawFittedText(canvas, KB_KEYS[r][c], x + keyW * 0.5f, y + keyH * 0.68f, pText, keyW - 4, Paint.Align.CENTER);
            }
        }

        // Keyboard Footer Hint
        float footerY = h - 46.0f;
        pDraw.setColor(0xFF22262B);
        canvas.drawRect(0, footerY, w, h, pDraw);
        pText.setTextSize(14.5f);
        pText.setColor(0xFFFF9800);
        drawFittedText(canvas, "[WHEEL/D-PAD] MOVE   [CENTER] TYPE   [C4/DEL] DELETE   [MENU] CANCEL",
                w * 0.5f, footerY + 29, pText, w - 20, Paint.Align.CENTER);
    }

    // -------------------------------------------------------------
    // EDITOR VIEW (FULL 100% PARAMETERS LIST)
    // -------------------------------------------------------------
    private void drawEditor(Canvas canvas, float w, float h) {
        float headerH = 44.0f;

        // Header
        pDraw.setColor(0xFF22262B);
        canvas.drawRect(0, 0, w, headerH, pDraw);
        pText.setTypeface(customFontBold != null ? customFontBold : Typeface.DEFAULT_BOLD);
        pText.setTextSize(19.0f);
        pText.setColor(0xFFFF9800);
        drawFittedText(canvas, "RECIPE EDITOR (100% PP + WB + EV)", w * 0.5f, 29, pText, w - 40, Paint.Align.CENTER);

        // Items Viewport (Spacious 6-7 visible rows with large font)
        float startY = 78.0f;
        float itemH = 52.0f;
        int maxVisible = 6;
        int startIdx = Math.max(0, editorRowIndex - 3);

        for (int i = 0; i < maxVisible && (startIdx + i) < EDITOR_ROWS.length; i++) {
            int row = startIdx + i;
            float y = startY + (i * itemH);
            boolean isSelected = (row == editorRowIndex);

            if (isSelected) {
                pDraw.setColor(0xFF2C3844);
                rect.set(16, y - itemH * 0.68f, w - 16, y + itemH * 0.32f);
                canvas.drawRoundRect(rect, 6, 6, pDraw);
            }

            pText.setTextSize(18.5f);
            pText.setColor(isSelected ? 0xFF00E5FF : 0xFFCFD8DC);
            drawFittedText(canvas, EDITOR_ROWS[row], 26, y, pText, w * 0.58f, Paint.Align.LEFT);

            // Value column
            if (row < 31 && editingPreset != null) {
                String valStr = getEditorValue(row);
                pText.setColor(isSelected ? 0xFFFFD54F : 0xFFFFFFFF);
                drawFittedText(canvas, valStr, w - 26, y, pText, w * 0.35f, Paint.Align.RIGHT);
            }
        }

        // Editor Footer
        float footerY = h - 46.0f;
        pDraw.setColor(0xFF22262B);
        canvas.drawRect(0, footerY, w, h, pDraw);
        pText.setTextSize(14.5f);
        pText.setColor(0xFFFF9800);
        drawFittedText(canvas, "[UP/DN] SELECT   [LT/RT] ADJUST   [CENTER] EDIT/SAVE   [MENU] DISCARD",
                w * 0.5f, footerY + 29, pText, w - 20, Paint.Align.CENTER);
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

        pText.setTypeface(customFontBold != null ? customFontBold : Typeface.DEFAULT_BOLD);
        pText.setColor(0xFFFFFFFF);
        pText.setTextSize(18.0f);
        drawFittedText(canvas, "CONFIRM ACTION", w * 0.5f, dy + dh * 0.18f, pText, dw - 20, Paint.Align.CENTER);

        pText.setTypeface(customFont != null ? customFont : Typeface.DEFAULT);
        pText.setTextSize(16.0f);
        drawFittedText(canvas, title, w * 0.5f, dy + dh * 0.52f, pText, dw - 20, Paint.Align.CENTER);

        pText.setTypeface(customFontBold != null ? customFontBold : Typeface.DEFAULT_BOLD);
        pText.setColor(0xFFFF9800);
        pText.setTextSize(15.0f);
        drawFittedText(canvas, btnOk + "   |   " + btnCancel, w * 0.5f, dy + dh * 0.82f, pText, dw - 20, Paint.Align.CENTER);
    }

    private void drawToast(Canvas canvas, float w, float h, String msg) {
        float tw = w * 0.72f;
        float th = 44.0f;
        float tx = (w - tw) * 0.5f;
        float ty = h * 0.40f;

        pDraw.setColor(0xEE2E7D32);
        rect.set(tx, ty, tx + tw, ty + th);
        canvas.drawRoundRect(rect, 6, 6, pDraw);

        pText.setTypeface(customFontBold != null ? customFontBold : Typeface.DEFAULT_BOLD);
        pText.setColor(0xFFFFFFFF);
        pText.setTextSize(17.0f);
        drawFittedText(canvas, msg, w * 0.5f, ty + 28, pText, tw - 16, Paint.Align.CENTER);
    }
}
