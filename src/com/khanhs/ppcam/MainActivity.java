package com.khanhs.ppcam;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import java.util.List;

public class MainActivity extends Activity implements FilmCarouselView.CarouselListener {
    private FilmCarouselView carouselView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.main);
        carouselView = (FilmCarouselView) findViewById(R.id.carousel_view);
        carouselView.setListener(this);

        reloadPresets();
    }

    private void reloadPresets() {
        List<PPPreset> presets = StorageHelper.loadPresets();
        carouselView.setPresets(presets);
    }

    /**
     * Intercept all key events before any view eats them. Essential for Sony PMCA buttons.
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            KeyCodes.Action action = KeyCodes.mapEvent(event);
            if (action == KeyCodes.Action.EXIT_SHUTTER) {
                finish();
                return true;
            }
            if (action != KeyCodes.Action.NONE) {
                carouselView.onAction(action);
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        KeyCodes.Action action = KeyCodes.mapEvent(event);
        if (action == KeyCodes.Action.EXIT_SHUTTER) {
            finish();
            return true;
        }
        if (action != KeyCodes.Action.NONE) {
            carouselView.onAction(action);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // CarouselListener Callbacks
    @Override
    public void onApplyPreset(PPPreset preset) {
        boolean ok = PPManager.applyPreset(preset);
        if (ok) {
            carouselView.showBanner("✓ ĐÃ GÁN ĐÈ VÀO PP7 & ĐỒNG BỘ!");
        } else {
            carouselView.showBanner("LỖI GHI NVRAM (CẦN MÁY ẢNH SONY)");
        }
    }

    @Override
    public void onResetPP7() {
        boolean ok = PPManager.resetPP7();
        if (ok) {
            carouselView.showBanner("✓ ĐÃ RESET PP7, WB, EV VỀ GỐC!");
        } else {
            carouselView.showBanner("LỖI RESET NVRAM");
        }
    }

    @Override
    public void onDeletePreset(PPPreset preset) {
        boolean ok = StorageHelper.deletePreset(preset);
        if (ok) {
            carouselView.showBanner("✓ ĐÃ XOÁ CUỘN FILM KHỎI THẺ NHỚ!");
            reloadPresets();
        } else {
            carouselView.showBanner("KHÔNG THỂ XOÁ FILE NÀY");
        }
    }

    @Override
    public void onToggleFavorite(PPPreset preset) {
        preset.isFavorite = !preset.isFavorite;
        if (preset.filePath != null) {
            StorageHelper.updatePresetFile(preset);
        }
        carouselView.showBanner(preset.isFavorite ? "★ ĐÃ THÊM VÀO YÊU THÍCH" : "ĐÃ BỎ YÊU THÍCH");
    }

    @Override
    public void onOpenEditor(PPPreset editingPreset) {
        boolean ok = StorageHelper.saveCustomPreset(editingPreset);
        if (ok) {
            carouselView.showBanner("✓ ĐÃ TẠO CUỘN FILM: " + editingPreset.name);
            reloadPresets();
        } else {
            carouselView.showBanner("LỖI LƯU THẺ NHỚ");
        }
    }

    @Override
    public void onExitApp() {
        finish();
    }
}
