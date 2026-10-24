#!/usr/bin/env python3
"""
Tool to create binary .DAT presets for PP.CAM (Sony A7 Mark II / PP7).
Total size: exactly 64 bytes.
"""

import struct
import sys

def crc16_ccitt(data: bytes) -> int:
    crc = 0xFFFF
    for byte in data:
        crc ^= (byte << 8)
        for _ in range(8):
            if crc & 0x8000:
                crc = ((crc << 1) ^ 0x1021) & 0xFFFF
            else:
                crc = (crc << 1) & 0xFFFF
    return crc

def pack_preset(
    name: str,
    film_type: int,         # 0x01: Kodak, 0x02: Fuji, 0x03: CineStill, 0x04: Mono, 0xAA: Body Custom
    is_favorite: int,       # 0 or 1
    target_slot: int,       # 0x07 for PP7
    black_level: int,       # -15 to +15
    gamma: int,             # 3=ITU709, 4=Movie, 5=Still, 6=Cine1..9=Cine4, 10=ITU800, 11=S-Log2
    black_gamma_range: int, # 0=Wide, 1=Middle, 2=Narrow
    black_gamma_level: int, # -7 to +7
    knee_mode: int,         # 0=Auto, 1=Manual
    knee_auto_max: int,     # 0..4 (90%..100%)
    knee_auto_sens: int,    # 0=High, 1=Mid, 2=Low
    knee_manual_point: int, # 0..12 (75%..105%)
    knee_manual_slope: int, # -5 to +5
    color_mode: int,        # 3=Pro, 4=ITU709, 5=Movie, 6=Still, 7=Cinema, 8=S-Gamut, 9=B&W
    saturation: int,        # -32 to +32
    color_phase: int,       # -7 to +7
    color_depth_r: int,     # -7 to +7
    color_depth_g: int,
    color_depth_b: int,
    color_depth_c: int,
    color_depth_m: int,
    color_depth_y: int,
    detail_level: int,      # -7 to +7
    detail_adjust_mode: int,# 0=Auto, 1=Manual
    detail_vh_balance: int, # -2 to +2
    detail_bw_balance: int, # 0=Type 1 .. 4=Type 5
    detail_limit: int,      # 0 to 7
    detail_crispening: int, # 0 to 7
    detail_hilight: int,    # 0 to 4
    wb_mode: int,           # 1=Auto, 14=Kelvin
    wb_kelvin: int,         # Kelvin / 100 (e.g. 74 for 7400K)
    wb_ab_shift: int,       # -7 (Blue) to +7 (Amber)
    wb_gm_shift: int,       # -7 (Green) to +7 (Magenta)
    exposure_bias: int,     # signed 1/3 EV steps
    dro_mode: int           # 0=Off, 1=Auto, 2..6=Lv1..5
) -> bytes:
    name_bytes = name.encode('ascii')[:19]
    name_padded = name_bytes.ljust(20, b'\x00')

    # Pack 62 bytes (payload)
    # Header: 4s B B B 20s B (28 bytes)
    header = struct.pack(
        "<4sBBB20sB",
        b"PPDT",
        1,          # version
        film_type,
        is_favorite,
        name_padded,
        target_slot
    )

    # Core PP: 25 parameters
    core_fmt = "<bBBbBBBBbB" + ("b" * 9) + "BbBBBB"
    core = struct.pack(
        core_fmt,
        black_level,
        gamma,
        black_gamma_range,
        black_gamma_level,
        knee_mode,
        knee_auto_max,
        knee_auto_sens,
        knee_manual_point,
        knee_manual_slope,
        color_mode,
        saturation,
        color_phase,
        color_depth_r,
        color_depth_g,
        color_depth_b,
        color_depth_c,
        color_depth_m,
        color_depth_y,
        detail_level,
        detail_adjust_mode,
        detail_vh_balance,
        detail_bw_balance,
        detail_limit,
        detail_crispening,
        detail_hilight
    )

    # Companion: B B b b b B (6 bytes)
    companion = struct.pack(
        "<BBbbbB",
        wb_mode,
        wb_kelvin,
        wb_ab_shift,
        wb_gm_shift,
        exposure_bias,
        dro_mode
    )

    # Reserved: 3 bytes
    reserved = b"\x00\x00\x00"

    payload = header + core + companion + reserved
    assert len(payload) == 62, f"Payload must be 62 bytes, got {len(payload)}"

    crc = crc16_ccitt(payload)
    full_block = payload + struct.pack("<H", crc)
    assert len(full_block) == 64, f"Full block must be 64 bytes, got {len(full_block)}"
    return full_block

def main():
    # 1. DEFAULT RECIPE: "Blue Velvet / Cinestill 50D"
    cinestill_50d = pack_preset(
        name="CINESTILL 50D",
        film_type=0x03,         # CineStill Look
        is_favorite=0,
        target_slot=0x07,       # PP7
        black_level=0,
        gamma=5,                # Still
        black_gamma_range=1,    # Middle
        black_gamma_level=7,    # +7
        knee_mode=1,            # Manual
        knee_auto_max=0,
        knee_auto_sens=0,
        knee_manual_point=0,    # 75%
        knee_manual_slope=4,    # +4
        color_mode=6,           # Still
        saturation=10,          # +10
        color_phase=-3,         # -3
        color_depth_r=4,
        color_depth_g=5,
        color_depth_b=-2,
        color_depth_c=3,
        color_depth_m=5,
        color_depth_y=2,
        detail_level=0,
        detail_adjust_mode=1,   # Manual
        detail_vh_balance=2,    # +2
        detail_bw_balance=2,    # Type 3 (0=T1, 1=T2, 2=T3)
        detail_limit=7,
        detail_crispening=7,
        detail_hilight=4,
        wb_mode=14,             # Color Temp (Kelvin)
        wb_kelvin=74,           # 7400K
        wb_ab_shift=-4,         # B4 (Blue 4)
        wb_gm_shift=2,          # M1.5 (~2 Magenta)
        exposure_bias=0,
        dro_mode=1              # Auto
    )

    with open("presets/DEFAULT.DAT", "wb") as f:
        f.write(cinestill_50d)
    print("Created presets/DEFAULT.DAT (64 bytes) - Cinestill 50D Blue Velvet")

    # 2. VARIANT TEST RECIPE: "KODAK GOLD 200"
    kodak_gold = pack_preset(
        name="KODAK GOLD 200",
        film_type=0x01,         # Kodak Yellow/Red
        is_favorite=1,          # Favorite star
        target_slot=0x07,       # PP7
        black_level=-2,
        gamma=9,                # Cine4
        black_gamma_range=0,    # Wide
        black_gamma_level=2,    # +2
        knee_mode=0,            # Auto
        knee_auto_max=2,        # 95%
        knee_auto_sens=0,       # High
        knee_manual_point=0,
        knee_manual_slope=0,
        color_mode=3,           # Pro
        saturation=12,          # +12
        color_phase=-1,
        color_depth_r=2,
        color_depth_g=0,
        color_depth_b=-1,
        color_depth_c=0,
        color_depth_m=1,
        color_depth_y=3,
        detail_level=-5,
        detail_adjust_mode=0,   # Auto
        detail_vh_balance=0,
        detail_bw_balance=0,
        detail_limit=0,
        detail_crispening=0,
        detail_hilight=0,
        wb_mode=14,             # Kelvin
        wb_kelvin=55,           # 5500K
        wb_ab_shift=2,          # A2 (Amber 2)
        wb_gm_shift=1,          # M1 (Magenta 1)
        exposure_bias=2,        # +0.7 EV (+2 steps)
        dro_mode=2              # Lv1
    )

    with open("presets/KODAK200.DAT", "wb") as f:
        f.write(kodak_gold)
    print("Created presets/KODAK200.DAT (64 bytes) - Kodak Gold 200")

if __name__ == "__main__":
    main()
