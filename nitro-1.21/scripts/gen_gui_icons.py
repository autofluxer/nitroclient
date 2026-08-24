"""
FastClient-style GUI icons — thin white strokes on transparent, 64×64.

Drawn at 256×256 with stroke width tuned so glyphs stay crisp at 10–12px.
Extra padding keeps icons from looking chunky when blitted small.
"""
from __future__ import annotations

import math
from pathlib import Path
from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/nitro/textures/gui/icons"
OUT.mkdir(parents=True, exist_ok=True)

SRC = 256
DST = 64
W = (255, 255, 255, 255)
CLEAR = (0, 0, 0, 0)
# Thin stroke — FastClient icons are line-weight, not heavy fills
SW = 14
PAD = 36  # keep glyph away from edges


def new():
    return Image.new("RGBA", (SRC, SRC), CLEAR)


def save(img: Image.Image, name: str):
    out = img.resize((DST, DST), Image.Resampling.LANCZOS)
    out.save(OUT / f"{name}.png")
    print("wrote", name)


def icon_user():
    im = new()
    d = ImageDraw.Draw(im)
    # head ring
    d.ellipse((96, 40, 160, 104), outline=W, width=SW)
    # shoulders arc
    d.arc((56, 130, 200, 250), 200, 340, fill=W, width=SW)
    return im


def icon_users():
    im = new()
    d = ImageDraw.Draw(im)
    dim = (255, 255, 255, 150)
    # back person
    d.ellipse((130, 44, 186, 100), outline=dim, width=SW - 2)
    d.arc((100, 128, 220, 240), 200, 340, fill=dim, width=SW - 2)
    # front person
    d.ellipse((60, 48, 124, 112), outline=W, width=SW)
    d.arc((32, 136, 170, 248), 200, 340, fill=W, width=SW)
    return im


def icon_hanger():
    im = new()
    d = ImageDraw.Draw(im)
    d.arc((104, 36, 152, 90), 200, 340, fill=W, width=SW)
    d.line((128, 84, 128, 108), fill=W, width=SW)
    d.line((48, 120, 208, 120), fill=W, width=SW)
    d.line((48, 120, 128, 208), fill=W, width=SW)
    d.line((208, 120, 128, 208), fill=W, width=SW)
    return im


def icon_grid():
    """3×3 dots — FastClient Mods icon."""
    im = new()
    d = ImageDraw.Draw(im)
    r = 14
    gap = 52
    o = 70
    for row in range(3):
        for col in range(3):
            cx = o + col * gap
            cy = o + row * gap
            d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=W)
    return im


def icon_layout():
    """2×2 squares + plus — toolbar."""
    im = new()
    d = ImageDraw.Draw(im)
    cell = 64
    gap = 20
    o = 50
    for row in range(2):
        for col in range(2):
            if row == 1 and col == 1:
                continue
            x0 = o + col * (cell + gap)
            y0 = o + row * (cell + gap)
            d.rounded_rectangle((x0, y0, x0 + cell, y0 + cell), radius=10, outline=W, width=SW)
    # plus in bottom-right
    cx = o + (cell + gap) + cell // 2
    cy = o + (cell + gap) + cell // 2
    d.line((cx, cy - 28, cx, cy + 28), fill=W, width=SW)
    d.line((cx - 28, cy, cx + 28, cy), fill=W, width=SW)
    return im


def icon_bag():
    im = new()
    d = ImageDraw.Draw(im)
    d.rounded_rectangle((56, 100, 200, 220), radius=18, outline=W, width=SW)
    d.arc((78, 44, 128, 110), 200, 340, fill=W, width=SW)
    d.arc((128, 44, 178, 110), 200, 340, fill=W, width=SW)
    return im


def icon_shop():
    """Storefront awning — FastClient Store."""
    im = new()
    d = ImageDraw.Draw(im)
    # roof
    d.polygon([(40, 100), (128, 40), (216, 100)], outline=W)
    d.line([(40, 100), (128, 40), (216, 100)], fill=W, width=SW)
    # awning scallops
    for i in range(4):
        x0 = 48 + i * 40
        d.arc((x0, 92, x0 + 40, 132), 0, 180, fill=W, width=SW - 2)
    # walls
    d.rectangle((56, 128, 200, 210), outline=W, width=SW)
    # door
    d.rectangle((108, 150, 148, 210), outline=W, width=SW - 2)
    return im


def icon_chat():
    im = new()
    d = ImageDraw.Draw(im)
    d.rounded_rectangle((44, 44, 200, 168), radius=24, outline=W, width=SW)
    d.line([(72, 160), (52, 210), (110, 160)], fill=W, width=SW)
    return im


def icon_camera():
    im = new()
    d = ImageDraw.Draw(im)
    d.rounded_rectangle((36, 88, 220, 196), radius=18, outline=W, width=SW)
    d.rounded_rectangle((92, 56, 152, 96), radius=8, outline=W, width=SW - 2)
    d.ellipse((92, 112, 164, 184), outline=W, width=SW)
    return im


def icon_gear():
    im = new()
    d = ImageDraw.Draw(im)
    cx = cy = 128
    teeth = 8
    for i in range(teeth):
        ang = math.radians(i * (360 / teeth) - 90)
        x = cx + int(math.cos(ang) * 78)
        y = cy + int(math.sin(ang) * 78)
        d.ellipse((x - 16, y - 16, x + 16, y + 16), fill=W)
    d.ellipse((cx - 58, cy - 58, cx + 58, cy + 58), outline=W, width=SW)
    # clear center then ring
    for yy in range(cy - 28, cy + 29):
        for xx in range(cx - 28, cx + 29):
            if (xx - cx) ** 2 + (yy - cy) ** 2 <= 28 * 28:
                im.putpixel((xx, yy), CLEAR)
    d.ellipse((cx - 30, cy - 30, cx + 30, cy + 30), outline=W, width=SW - 2)
    return im


def icon_diamond():
    im = new()
    d = ImageDraw.Draw(im)
    d.line([(128, 36), (212, 108), (128, 220), (44, 108), (128, 36)], fill=W, width=SW)
    d.line([(44, 108), (212, 108)], fill=W, width=SW - 2)
    d.line([(128, 36), (128, 220)], fill=W, width=SW - 4)
    return im


def icon_folder():
    im = new()
    d = ImageDraw.Draw(im)
    d.rounded_rectangle((44, 64, 120, 100), radius=8, outline=W, width=SW - 2)
    d.rounded_rectangle((44, 88, 212, 204), radius=14, outline=W, width=SW)
    return im


def icon_monitor():
    im = new()
    d = ImageDraw.Draw(im)
    d.rounded_rectangle((40, 44, 216, 164), radius=14, outline=W, width=SW)
    d.line((128, 164, 128, 192), fill=W, width=SW)
    d.rounded_rectangle((88, 192, 168, 216), radius=6, outline=W, width=SW - 2)
    return im


def icon_mods():
    im = new()
    d = ImageDraw.Draw(im)
    s = 72
    positions = [(52, 110), (92, 70), (132, 120)]
    for x, y in positions:
        d.rounded_rectangle((x, y, x + s, y + s), radius=10, outline=W, width=SW)
    return im


def icon_speed():
    im = new()
    d = ImageDraw.Draw(im)
    d.arc((44, 56, 212, 224), 200, 340, fill=W, width=SW)
    d.line((128, 140, 186, 86), fill=W, width=SW)
    d.ellipse((116, 128, 140, 152), fill=W)
    return im


def icon_discord():
    """Clyde-style Discord mark."""
    im = new()
    d = ImageDraw.Draw(im)
    # body blob
    d.rounded_rectangle((48, 70, 208, 180), radius=40, fill=W)
    # cut bottom notches (ears-ish) by clearing triangles then redrawing? simplify:
    # eyes (clear)
    for cx, cy in [(100, 120), (156, 120)]:
        for yy in range(cy - 14, cy + 15):
            for xx in range(cx - 10, cx + 11):
                if 0 <= xx < SRC and 0 <= yy < SRC and (xx - cx) ** 2 / 100 + (yy - cy) ** 2 / 196 <= 1:
                    im.putpixel((xx, yy), CLEAR)
    return im


ICONS = {
    "user": icon_user,
    "users": icon_users,
    "hanger": icon_hanger,
    "grid": icon_grid,
    "layout": icon_layout,
    "bag": icon_bag,
    "shop": icon_shop,
    "chat": icon_chat,
    "camera": icon_camera,
    "gear": icon_gear,
    "diamond": icon_diamond,
    "folder": icon_folder,
    "monitor": icon_monitor,
    "mods": icon_mods,
    "speed": icon_speed,
    "discord": icon_discord,
}


if __name__ == "__main__":
    for name, fn in ICONS.items():
        save(fn(), name)
