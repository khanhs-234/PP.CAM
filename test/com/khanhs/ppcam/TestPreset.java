package com.khanhs.ppcam;

import java.io.File;

public class TestPreset {
    public static void main(String[] args) {
        System.out.println("Testing PPPreset loader...");
        
        File f1 = new File("presets/DEFAULT.DAT");
        PPPreset p1 = PPPreset.loadFromFile(f1);
        if (p1 == null) {
            System.err.println("FAILED to load DEFAULT.DAT!");
            System.exit(1);
        }
        System.out.println("Loaded DEFAULT.DAT successfully: " + p1.name);
        System.out.println("  Gamma: " + p1.getGammaLabel() + ", ColorMode: " + p1.getColorModeLabel());
        System.out.println("  WB: " + p1.getWbLabel() + " (" + p1.getWbShiftLabel() + ")");
        System.out.println("  Target Slot: PP" + p1.targetSlot);

        // Verify round-trip serialization
        byte[] bytes = p1.toBytes();
        PPPreset p1_round = PPPreset.fromBytes(bytes, "memory");
        if (p1_round == null || !p1_round.name.equals(p1.name)) {
            System.err.println("FAILED round-trip serialization!");
            System.exit(1);
        }
        System.out.println("Round-trip serialization test: PASSED!");

        File f2 = new File("presets/KODAK200.DAT");
        PPPreset p2 = PPPreset.loadFromFile(f2);
        if (p2 == null) {
            System.err.println("FAILED to load KODAK200.DAT!");
            System.exit(1);
        }
        System.out.println("Loaded KODAK200.DAT successfully: " + p2.name);
        System.out.println("  Favorite: " + p2.isFavorite + ", EV: " + p2.getEvLabel());

        System.out.println("ALL PRESET UNIT TESTS PASSED!");
    }
}
