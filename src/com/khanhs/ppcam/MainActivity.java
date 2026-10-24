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

    // CarouselListener Callbacks (All English, 100% ASCII-safe for PMCA)
    @Override
    public void onApplyPreset(PPPreset preset) {
        boolean ok = PPManager.applyPreset(preset);
        if (ok) {
            carouselView.showBanner("[OK] PP7 OVERWRITTEN & SYNCED!");
        } else {
            carouselView.showBanner("NVRAM ERROR (SONY BODY REQUIRED)");
        }
    }

    @Override
    public void onResetPP7() {
        boolean ok = PPManager.resetPP7();
        if (ok) {
            carouselView.showBanner("[OK] PP7 & WB RESET TO DEFAULT!");
        } else {
            carouselView.showBanner("RESET ERROR");
        }
    }

    @Override
    public void onDeletePreset(PPPreset preset) {
        boolean ok = StorageHelper.deletePreset(preset);
        if (ok) {
            carouselView.showBanner("[OK] RECIPE DELETED FROM SD CARD!");
            reloadPresets();
        } else {
            carouselView.showBanner("CANNOT DELETE FILE");
        }
    }

    @Override
    public void onToggleFavorite(PPPreset preset) {
        preset.isFavorite = !preset.isFavorite;
        if (preset.filePath != null) {
            StorageHelper.updatePresetFile(preset);
        }
        carouselView.showBanner(preset.isFavorite ? "[FAV] ADDED TO FAVORITES" : "REMOVED FROM FAVORITES");
    }

    @Override
    public void onReloadSD() {
        reloadPresets();
        carouselView.showBanner("[OK] SD CARD SCANNED & RELOADED!");
    }

    @Override
    public void onOpenEditor(PPPreset editingPreset) {
        boolean ok = StorageHelper.saveCustomPreset(editingPreset);
        if (ok) {
            carouselView.showBanner("[OK] SAVED: " + editingPreset.name);
            reloadPresets();
        } else {
            carouselView.showBanner("ERROR SAVING TO SD CARD");
        }
    }

    @Override
    public void onExitApp() {
        finish();
    }
}
