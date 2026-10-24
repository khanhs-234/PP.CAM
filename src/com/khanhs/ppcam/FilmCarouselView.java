package com.khanhs.ppcam;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class FilmCarouselView extends View {
    public interface CarouselListener {
        void onApplyPreset(PPPreset preset);
        void onResetPP7();
        void onDeletePreset(PPPreset preset);
        void onToggleFavorite(PPPreset preset);
        void onOpenEditor(PPPreset preset);
        void onExitApp();
    }

    public enum Mode {
        CAROUSEL,
        CONFIRM_DELETE,
        CONFIRM_RESET,
        APPLIED_BANNER,
        EDITOR
    }

    private Mode currentMode = Mode.CAROUSEL;
    private CarouselListener listener;

    private List<PPPreset> presets = new ArrayList<PPPreset>();
    private int currentIndex = 0;

    // Editor state
    private PPPreset editingPreset = null;
    private int editorRowIndex = 0;
    private final String[] EDITOR_ROWS = {
        "Preset Name", "Gamma", "Color Mode", "Saturation", "Color Phase",
        "WB Mode", "WB Kelvin", "WB A-B Shift", "WB G-M Shift", "Exposure EV",
        "Black Level", "Detail Level", ">>> [ LƯU CÔNG THỨC THÀNH CUỘN FILM MỚI ] <<<"
    };

    // Notification toast timer
    private String bannerMessage = "";
    private long bannerExpiryTime = 0;

    // Graphics Paints
    private final Paint pText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pCanister = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pAccent = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pBg = new Paint();
    private final RectF rect = new RectF();

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
        bannerExpiryTime = System.currentTimeMillis() + 1500;
        invalidate();
    }

    // Navigation and Action Handlers
    public void onAction(KeyCodes.Action action) {
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
                    showBanner("Không thể xoá cuộn film mặc định!");
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

    private void handleEditorAction(KeyCodes.Action action) {
        if (action == KeyCodes.Action.BACK_MENU || action == KeyCodes.Action.EDIT_FN) {
            // Discard changes
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

        if (editorRowIndex == EDITOR_ROWS.length - 1) {
            // Save Recipe row
            if (action == KeyCodes.Action.CONFIRM) {
                if (listener != null && editingPreset != null) {
                    listener.onOpenEditor(editingPreset); // Triggers save
                }
                currentMode = Mode.CAROUSEL;
                invalidate();
            }
            return;
        }

        // Adjust parameter with Left / Right
        int step = (action == KeyCodes.Action.NAV_RIGHT) ? 1 : (action == KeyCodes.Action.NAV_LEFT) ? -1 : 0;
        if (step != 0 && editingPreset != null) {
            switch (editorRowIndex) {
                case 1: // Gamma
                    int[] gammas = { 3, 4, 5, 6, 7, 8, 9, 10, 11 };
                    int gIdx = 0;
                    for (int i = 0; i < gammas.length; i++) if (gammas[i] == editingPreset.gamma) gIdx = i;
                    gIdx = Math.max(0, Math.min(gammas.length - 1, gIdx + step));
                    editingPreset.gamma = gammas[gIdx];
                    break;
                case 2: // Color Mode
                    int[] modes = { 3, 4, 5, 6, 7, 8, 9 };
                    int mIdx = 0;
                    for (int i = 0; i < modes.length; i++) if (modes[i] == editingPreset.colorMode) mIdx = i;
                    mIdx = Math.max(0, Math.min(modes.length - 1, mIdx + step));
                    editingPreset.colorMode = modes[mIdx];
                    break;
                case 3: // Saturation (-32..+32)
                    editingPreset.saturation = Math.max(-32, Math.min(32, editingPreset.saturation + step));
                    break;
                case 4: // Color Phase (-7..+7)
                    editingPreset.colorPhase = Math.max(-7, Math.min(7, editingPreset.colorPhase + step));
                    break;
                case 5: // WB Mode (1=Auto, 14=Kelvin)
                    editingPreset.wbMode = (editingPreset.wbMode == 14) ? 1 : 14;
                    break;
                case 6: // WB Kelvin (25..99 -> 2500K..9900K)
                    editingPreset.wbKelvin = Math.max(25, Math.min(99, editingPreset.wbKelvin + step));
                    break;
                case 7: // WB A-B (-7..+7)
                    editingPreset.wbAbShift = Math.max(-7, Math.min(7, editingPreset.wbAbShift + step));
                    break;
                case 8: // WB G-M (-7..+7)
                    editingPreset.wbGmShift = Math.max(-7, Math.min(7, editingPreset.wbGmShift + step));
                    break;
                case 9: // EV (-6..+6 steps of 1/3 EV)
                    editingPreset.exposureBias = Math.max(-6, Math.min(6, editingPreset.exposureBias + step));
                    break;
                case 10: // Black level (-15..+15)
                    editingPreset.blackLevel = Math.max(-15, Math.min(15, editingPreset.blackLevel + step));
                    break;
                case 11: // Detail level (-7..+7)
                    editingPreset.detailLevel = Math.max(-7, Math.min(7, editingPreset.detailLevel + step));
                    break;
            }
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Sony 9-Row Grid System: Each row is h / 9
        float rowH = h / 9.0f;

        // Background
        canvas.drawRect(0, 0, w, h, pBg);

        if (currentMode == Mode.EDITOR) {
            drawEditor(canvas, w, h, rowH);
            return;
        }

        PPPreset curr = getCurrentPreset();

        // -------------------------------------------------------------
        // ROW 1: HEADER & STATUS BAR
        // -------------------------------------------------------------
        pCanister.setColor(0xFF1E2228);
        canvas.drawRect(0, 0, w, rowH, pCanister);

        pText.setTextSize(rowH * 0.40f);
        pText.setColor(0xFF00E5FF);
        pText.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(" [SD: " + presets.size() + " PRESETS]", 15, rowH * 0.65f, pText);

        pText.setColor(0xFFFFFFFF);
        pText.setTextAlign(Paint.Align.CENTER);
        pText.setFakeBoldText(true);
        canvas.drawText("PP-LOADER (A7 II)", w * 0.5f, rowH * 0.65f, pText);
        pText.setFakeBoldText(false);

        pText.setColor(0xFFFFB300);
        pText.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("[SLOT: PP7] ", w - 15, rowH * 0.65f, pText);

        // -------------------------------------------------------------
        // ROW 2: STAR (FAVORITE) & FILE NAME
        // -------------------------------------------------------------
        float r2Y = rowH * 1.65f;
        pText.setTextAlign(Paint.Align.CENTER);
        if (curr != null) {
            if (curr.isFavorite) {
                pText.setColor(0xFFFFD700); // Gold star
                pText.setTextSize(rowH * 0.48f);
                canvas.drawText("★ FAVORITE RECIPE ★", w * 0.5f, r2Y, pText);
            } else {
                pText.setColor(0xFF888888);
                pText.setTextSize(rowH * 0.35f);
                String fName = (curr.filePath != null) ? new java.io.File(curr.filePath).getName() : "BUILT-IN PRESET";
                canvas.drawText("FILE: " + fName, w * 0.5f, r2Y, pText);
            }
        }

        // -------------------------------------------------------------
        // ROWS 3, 4, 5: 35mm FILM CANISTER STAGE
        // -------------------------------------------------------------
        float stageCenterY = rowH * 4.0f;

        if (!presets.isEmpty()) {
            // Draw Previous Canister (Left, 70% scale, 50% opacity)
            if (presets.size() > 1) {
                int prevIdx = (currentIndex - 1 + presets.size()) % presets.size();
                drawCanister(canvas, presets.get(prevIdx), w * 0.15f, stageCenterY, 0.70f, 0.50f);
            }

            // Draw Next Canister (Right, 70% scale, 50% opacity)
            if (presets.size() > 1) {
                int nextIdx = (currentIndex + 1) % presets.size();
                drawCanister(canvas, presets.get(nextIdx), w * 0.85f, stageCenterY, 0.70f, 0.50f);
            }

            // Draw Active Canister (Center, 100% scale, 100% opacity)
            drawCanister(canvas, curr, w * 0.5f, stageCenterY, 1.0f, 1.0f);
        }

        // -------------------------------------------------------------
        // ROW 6: PRESET TITLE BANNER
        // -------------------------------------------------------------
        float r6Y = rowH * 5.75f;
        pText.setTextAlign(Paint.Align.CENTER);
        pText.setFakeBoldText(true);
        pText.setTextSize(rowH * 0.55f);
        pText.setColor(0xFFFFFFFF);
        if (curr != null) {
            canvas.drawText(curr.name, w * 0.5f, r6Y, pText);
        }
        pText.setFakeBoldText(false);

        // -------------------------------------------------------------
        // ROW 7: PP CORE SUMMARY
        // -------------------------------------------------------------
        float r7Y = rowH * 6.70f;
        pText.setTextSize(rowH * 0.38f);
        pText.setColor(0xFFCCCCCC);
        if (curr != null) {
            String ppInfo = "PP7: Gamma " + curr.getGammaLabel() +
                            " | Color " + curr.getColorModeLabel() +
                            " | Sat " + (curr.saturation >= 0 ? "+" : "") + curr.saturation +
                            " | Phase " + curr.colorPhase +
                            " | Detail " + curr.detailLevel;
            canvas.drawText(ppInfo, w * 0.5f, r7Y, pText);
        }

        // -------------------------------------------------------------
        // ROW 8: WB & EXPOSURE (KELVIN, SHIFT, EV, DRO)
        // -------------------------------------------------------------
        float r8Y = rowH * 7.70f;
        pText.setTextSize(rowH * 0.38f);
        pText.setColor(0xFFFFD54F);
        if (curr != null) {
            String colorInfo = "WB: " + curr.getWbLabel() + " (" + curr.getWbShiftLabel() + ")" +
                               " | EV " + curr.getEvLabel() +
                               " | DRO " + (curr.droMode == 1 ? "Auto" : curr.droMode > 1 ? "Lv" + (curr.droMode - 1) : "Off") +
                               " | BL " + curr.blackLevel;
            canvas.drawText(colorInfo, w * 0.5f, r8Y, pText);
        }

        // -------------------------------------------------------------
        // ROW 9: FOOTER / HINT BAR
        // -------------------------------------------------------------
        float r9Top = rowH * 8.0f;
        pCanister.setColor(0xFF000000);
        canvas.drawRect(0, r9Top, w, h, pCanister);
        pCanister.setColor(0xFF333333);
        canvas.drawLine(0, r9Top, w, r9Top, pCanister);

        pText.setTextSize(rowH * 0.38f);
        pText.setTextAlign(Paint.Align.CENTER);
        pText.setColor(0xFFFF9800);
        canvas.drawText("[C2]★  [C3]Reset  [C4]Xoá  [Fn]Sửa  [Center]Gán PP7  [MENU]Thoát", w * 0.5f, r9Top + rowH * 0.65f, pText);

        // Transient notification toast / banner
        if (System.currentTimeMillis() < bannerExpiryTime) {
            drawToast(canvas, w, h, bannerMessage);
            postInvalidateDelayed(100);
        }

        // Confirmation modals
        if (currentMode == Mode.CONFIRM_DELETE) {
            drawConfirmDialog(canvas, w, h, "Xoá cuộn film [" + (curr != null ? curr.name : "") + "]?",
                    "[Center] Đồng ý xoá", "[MENU] Huỷ bỏ", 0xFFD32F2F);
        } else if (currentMode == Mode.CONFIRM_RESET) {
            drawConfirmDialog(canvas, w, h, "Khôi phục PP7, WB, EV về mặc định?",
                    "[Center] Xác nhận Reset", "[MENU] Huỷ bỏ", 0xFFE65100);
        }
    }

    /**
     * Draws a 35mm film canister with metallic body and color banner.
     */
    private void drawCanister(Canvas canvas, PPPreset p, float cx, float cy, float scale, float opacity) {
        if (p == null) return;
        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(scale, scale);

        int alpha = (int) (255 * opacity);

        float cw = 110.0f;
        float ch = 130.0f;

        // Top Spool Knob
        pCanister.setColor(0xFF222222);
        pCanister.setAlpha(alpha);
        rect.set(-14, -ch * 0.5f - 14, 14, -ch * 0.5f);
        canvas.drawRoundRect(rect, 4, 4, pCanister);

        // Canister Body Background (Metallic)
        pCanister.setColor(0xFF262626);
        pCanister.setAlpha(alpha);
        rect.set(-cw * 0.5f, -ch * 0.5f, cw * 0.5f, ch * 0.5f);
        canvas.drawRoundRect(rect, 8, 8, pCanister);

        // Banner Color according to film_type
        int bannerColor;
        int textColor = 0xFFFFFFFF;
        switch (p.filmType) {
            case PPPreset.TYPE_KODAK:
                bannerColor = 0xFFF5A623; // Kodak Yellow
                textColor = 0xFF000000;
                break;
            case PPPreset.TYPE_FUJI:
                bannerColor = 0xFF1E7E34; // Fuji Green
                break;
            case PPPreset.TYPE_CINESTILL:
                bannerColor = 0xFFB71C1C; // CineStill Deep Red
                break;
            case PPPreset.TYPE_MONOCHROME:
                bannerColor = 0xFFE0E0E0; // Silver / White
                textColor = 0xFF000000;
                break;
            case PPPreset.TYPE_BODY_CUSTOM:
            default:
                bannerColor = 0xFF6A1B9A; // Cyber Violet for camera custom
                break;
        }

        // Color Label Band across center of canister
        pAccent.setColor(bannerColor);
        pAccent.setAlpha(alpha);
        rect.set(-cw * 0.5f, -ch * 0.28f, cw * 0.5f, ch * 0.28f);
        canvas.drawRect(rect, pAccent);

        // Film Brand / Type Label on Canister
        pText.setAlpha(alpha);
        pText.setTextSize(14);
        pText.setTextAlign(Paint.Align.CENTER);
        pText.setColor(textColor);
        pText.setFakeBoldText(true);
        String label = (p.filmType == PPPreset.TYPE_BODY_CUSTOM) ? "CUSTOM" :
                       (p.filmType == PPPreset.TYPE_CINESTILL) ? "CINESTILL" :
                       (p.filmType == PPPreset.TYPE_KODAK) ? "KODAK" :
                       (p.filmType == PPPreset.TYPE_FUJI) ? "FUJIFILM" : "35mm FILM";
        canvas.drawText(label, 0, -4, pText);

        pText.setTextSize(10);
        canvas.drawText("36 EXP / PP7", 0, 14, pText);
        pText.setFakeBoldText(false);

        // Star icon if favorite
        if (p.isFavorite) {
            pText.setColor(0xFFFFD700);
            pText.setTextSize(18);
            canvas.drawText("★", 0, -ch * 0.35f, pText);
        }

        canvas.restore();
    }

    private void drawEditor(Canvas canvas, float w, float h, float rowH) {
        // Editor Header
        pCanister.setColor(0xFF1E2228);
        canvas.drawRect(0, 0, w, rowH, pCanister);
        pText.setColor(0xFFFF9800);
        pText.setTextSize(rowH * 0.45f);
        pText.setTextAlign(Paint.Align.CENTER);
        pText.setFakeBoldText(true);
        canvas.drawText("CHỈNH SỬA CÔNG THỨC [Fn]", w * 0.5f, rowH * 0.65f, pText);
        pText.setFakeBoldText(false);

        // Parameter Rows
        float startY = rowH * 1.5f;
        float itemH = rowH * 0.52f;
        int maxVisible = 10;
        int startIdx = Math.max(0, editorRowIndex - 5);

        for (int i = 0; i < maxVisible && (startIdx + i) < EDITOR_ROWS.length; i++) {
            int row = startIdx + i;
            float y = startY + (i * itemH);
            boolean isSelected = (row == editorRowIndex);

            if (isSelected) {
                pCanister.setColor(0xFF37474F);
                rect.set(20, y - itemH * 0.7f, w - 20, y + itemH * 0.3f);
                canvas.drawRoundRect(rect, 4, 4, pCanister);
            }

            pText.setTextAlign(Paint.Align.LEFT);
            pText.setTextSize(itemH * 0.65f);
            pText.setColor(isSelected ? 0xFF00E5FF : 0xFFCCCCCC);
            canvas.drawText(EDITOR_ROWS[row], 35, y, pText);

            // Value column
            if (row < EDITOR_ROWS.length - 1 && editingPreset != null) {
                String valStr = getEditorValueString(row);
                pText.setTextAlign(Paint.Align.RIGHT);
                pText.setColor(isSelected ? 0xFFFFD54F : 0xFFFFFFFF);
                canvas.drawText(valStr, w - 35, y, pText);
            }
        }

        // Editor Footer
        float fTop = rowH * 8.0f;
        pCanister.setColor(0xFF000000);
        canvas.drawRect(0, fTop, w, h, pCanister);
        pText.setColor(0xFFFF9800);
        pText.setTextSize(rowH * 0.38f);
        pText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("[Lên/Xuống] Chọn   [Trái/Phải] Thay đổi   [Center] Lưu   [MENU] Huỷ", w * 0.5f, fTop + rowH * 0.65f, pText);
    }

    private String getEditorValueString(int row) {
        if (editingPreset == null) return "";
        switch (row) {
            case 0: return editingPreset.name;
            case 1: return editingPreset.getGammaLabel();
            case 2: return editingPreset.getColorModeLabel();
            case 3: return (editingPreset.saturation >= 0 ? "+" : "") + editingPreset.saturation;
            case 4: return String.valueOf(editingPreset.colorPhase);
            case 5: return editingPreset.wbMode == 14 ? "Kelvin" : "Auto";
            case 6: return (editingPreset.wbKelvin * 100) + "K";
            case 7: return String.valueOf(editingPreset.wbAbShift);
            case 8: return String.valueOf(editingPreset.wbGmShift);
            case 9: return editingPreset.getEvLabel();
            case 10: return String.valueOf(editingPreset.blackLevel);
            case 11: return String.valueOf(editingPreset.detailLevel);
            default: return "";
        }
    }

    private void drawConfirmDialog(Canvas canvas, float w, float h, String title, String btnOk, String btnCancel, int headerColor) {
        // Dim background
        pCanister.setColor(0xCC000000);
        canvas.drawRect(0, 0, w, h, pCanister);

        float dw = w * 0.70f;
        float dh = h * 0.45f;
        float dx = (w - dw) * 0.5f;
        float dy = (h - dh) * 0.5f;

        // Dialog frame
        pCanister.setColor(0xFF1E2228);
        rect.set(dx, dy, dx + dw, dy + dh);
        canvas.drawRoundRect(rect, 10, 10, pCanister);

        // Header bar
        pAccent.setColor(headerColor);
        rect.set(dx, dy, dx + dw, dy + dh * 0.25f);
        canvas.drawRoundRect(rect, 10, 10, pAccent);

        // Title
        pText.setColor(0xFFFFFFFF);
        pText.setTextSize(16);
        pText.setTextAlign(Paint.Align.CENTER);
        pText.setFakeBoldText(true);
        canvas.drawText("XÁC NHẬN", w * 0.5f, dy + dh * 0.18f, pText);

        // Message
        pText.setTextSize(14);
        pText.setFakeBoldText(false);
        canvas.drawText(title, w * 0.5f, dy + dh * 0.52f, pText);

        // Buttons
        pText.setColor(0xFFFF9800);
        pText.setTextSize(13);
        canvas.drawText(btnOk + "   |   " + btnCancel, w * 0.5f, dy + dh * 0.82f, pText);
    }

    private void drawToast(Canvas canvas, float w, float h, String msg) {
        float tw = w * 0.65f;
        float th = 40.0f;
        float tx = (w - tw) * 0.5f;
        float ty = h * 0.40f;

        pCanister.setColor(0xEE2E7D32); // Green banner
        rect.set(tx, ty, tx + tw, ty + th);
        canvas.drawRoundRect(rect, 8, 8, pCanister);

        pText.setColor(0xFFFFFFFF);
        pText.setTextSize(15);
        pText.setTextAlign(Paint.Align.CENTER);
        pText.setFakeBoldText(true);
        canvas.drawText(msg, w * 0.5f, ty + 26, pText);
        pText.setFakeBoldText(false);
    }
}
