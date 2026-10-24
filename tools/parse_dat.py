#!/usr/bin/env python3
"""
Tool to parse and print PP.CAM 64-byte .DAT files.
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

GAMMA_NAMES = {
    3: "ITU709", 4: "Movie", 5: "Still", 6: "Cine1", 7: "Cine2", 8: "Cine3", 9: "Cine4",
    10: "ITU709(800%)", 11: "S-Log2"
}

COLOR_MODES = {
    3: "Pro", 4: "ITU709 Matrix", 5: "Movie", 6: "Still", 7: "Cinema", 8: "S-Gamut", 9: "Black & White"
}

FILM_TYPES = {
    0x01: "Kodak (Gold/Portra)",
    0x02: "Fujifilm (Superia/Pro400H)",
    0x03: "CineStill (800T/50D)",
    0x04: "Monochrome (B&W)",
    0x05: "Vintage / Retro",
    0xAA: "Body Custom (Created on Camera)"
}

def parse_file(path: str):
    with open(path, "rb") as f:
        data = f.read()

    if len(data) != 64:
        print(f"Error: {path} has size {len(data)}, expected 64 bytes.")
        return

    magic = data[:4]
    if magic != b"PPDT":
        print(f"Error: Invalid magic {magic}, expected PPDT.")
        return

    payload = data[:62]
    expected_crc = crc16_ccitt(payload)
    file_crc = struct.unpack("<H", data[62:64])[0]
    crc_ok = (expected_crc == file_crc)

    version, film_type, is_fav = struct.unpack("<BBB", data[4:7])
    name = data[7:27].decode('ascii', errors='ignore').rstrip('\x00')
    target_slot = data[27]

    core_fmt = "<bBBbBBBBbB" + ("b" * 9) + "BbBBBB"
    pp = struct.unpack(core_fmt, data[28:53])

    (black_level, gamma, bg_range, bg_lvl, knee_m, knee_am, knee_as,
     knee_mp, knee_ms, col_mode, sat, phase,
     dr, dg, db, dc, dm, dy,
     det_lvl, det_m, det_vh, det_bw, det_lim, det_crisp, det_hi) = pp

    wb_mode, wb_k, wb_ab, wb_gm, ev, dro = struct.unpack("<BBbbbB", data[53:59])

    print("=" * 60)
    print(f"File: {path} | Size: {len(data)}B | CRC16: {hex(file_crc)} ({'OK' if crc_ok else 'FAILED!'})")
    print("=" * 60)
    print(f"Preset Name : {name}")
    print(f"Film Type   : {hex(film_type)} - {FILM_TYPES.get(film_type, 'Custom')}")
    print(f"Favorite    : {'YES (★)' if is_fav else 'No'}")
    print(f"Target Slot : PP{target_slot}")
    print("-" * 60)
    print(f"[PICTURE PROFILE CORE (100%)]")
    print(f"  Black Level      : {black_level}")
    print(f"  Gamma            : {GAMMA_NAMES.get(gamma, str(gamma))} ({gamma})")
    print(f"  Black Gamma      : Range={'Wide' if bg_range==0 else 'Middle' if bg_range==1 else 'Narrow'}, Level={bg_lvl}")
    print(f"  Knee             : Mode={'Auto' if knee_m==0 else 'Manual'}, Point={75 + knee_mp * 2.5}%, Slope={knee_ms}")
    print(f"  Color Mode       : {COLOR_MODES.get(col_mode, str(col_mode))} ({col_mode})")
    print(f"  Saturation       : {sat}")
    print(f"  Color Phase      : {phase}")
    print(f"  Color Depth      : R:{dr:+d}  G:{dg:+d}  B:{db:+d}  C:{dc:+d}  M:{dm:+d}  Y:{dy:+d}")
    print(f"  Detail           : Lvl:{det_lvl}  Mode={'Auto' if det_m==0 else 'Manual'}  V/H:{det_vh:+d}  B/W:Type {det_bw+1}  Limit:{det_lim}  Crisp:{det_crisp}  Hi-Light:{det_hi}")
    print("-" * 60)
    print(f"[WHITE BALANCE & EXPOSURE]")
    ab_str = f"A{wb_ab}" if wb_ab > 0 else f"B{-wb_ab}" if wb_ab < 0 else "0"
    gm_str = f"M{wb_gm}" if wb_gm > 0 else f"G{-wb_gm}" if wb_gm < 0 else "0"
    print(f"  WB Mode          : {'Kelvin' if wb_mode==14 else 'Auto' if wb_mode==1 else 'Keep'}")
    print(f"  WB Temp          : {wb_k * 100}K")
    print(f"  WB Shift         : {ab_str} - {gm_str} (AB:{wb_ab}, GM:{wb_gm})")
    print(f"  Exposure Bias    : {ev * 0.333:+.1f} EV ({ev} steps)")
    print(f"  DRO              : {'Auto' if dro==1 else f'Lv{dro-1}' if dro>1 else 'Off'}")
    print("=" * 60)

if __name__ == "__main__":
    if len(sys.argv) > 1:
        parse_file(sys.argv[1])
    else:
        parse_file("presets/DEFAULT.DAT")
        parse_file("presets/KODAK200.DAT")
