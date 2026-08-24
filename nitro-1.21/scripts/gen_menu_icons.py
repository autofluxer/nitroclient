"""Generate crisp white transparent UI icons for Nitro title / menu buttons."""
from __future__ import annotations

import math
import os

from PIL import Image, ImageDraw

OUT = os.path.join(
    os.path.dirname(__file__),
    "..",
    "src",
    "main",
    "resources",
    "assets",
    "nitro",
    "textures",
    "gui",
    "icons",
)
SZ = 64
STROKE = 5
WHITE = (255, 255, 255, 255)
CLEAR = (0, 0, 0, 0)


def blank() -> Image.Image:
    return Image.new("RGBA", (SZ, SZ), CLEAR)


def save(im: Image.Image, name: str) -> None:
    path = os.path.join(OUT, name)
    im.save(path, "PNG")
    print("wrote", name)


def circle(d: ImageDraw.ImageDraw, cx: int, cy: int, r: int, fill=WHITE) -> None:
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=fill)


def line(d: ImageDraw.ImageDraw, pts, w: int = STROKE) -> None:
    d.line(pts, fill=WHITE, width=w, joint="curve")


def user_icon() -> Image.Image:
    im = blank()
    d = ImageDraw.Draw(im)
    circle(d, 32, 20, 10)
    d.pieslice([12, 34, 52, 78], start=200, end=340, fill=WHITE)
    d.rectangle([0, 54, 64, 64], fill=CLEAR)
    return im


def users_icon() -> Image.Image:
    im = blank()
    d = ImageDraw.Draw(im)
    circle(d, 24, 18, 8)
    d.pieslice([6, 28, 42, 70], start=200, end=340, fill=WHITE)
    d.rectangle([0, 52, 64, 64], fill=CLEAR)
    circle(d, 40, 22, 9)
    d.pieslice([20, 32, 58, 74], start=200, end=340, fill=WHITE)
    d.rectangle([0, 54, 64, 64], fill=CLEAR)
    return im


def hanger_icon() -> Image.Image:
    im = blank()
    d = ImageDraw.Draw(im)
    d.arc([26, 6, 38, 22], start=200, end=20, fill=WHITE, width=STROKE)
    line(d, [(12, 42), (32, 22), (52, 42)], STROKE)
    line(d, [(12, 42), (52, 42)], STROKE)
    return im


def grid_icon() -> Image.Image:
    im = blank()
    d = ImageDraw.Draw(im)
    for row in range(3):
        for col in range(3):
            circle(d, 16 + col * 16, 16 + row * 16, 5)
    return im


def shop_icon() -> Image.Image:
    im = blank()
    d = ImageDraw.Draw(im)
    d.polygon([(6, 28), (32, 6), (58, 28)], fill=WHITE)
    d.rectangle([8, 28, 56, 38], fill=WHITE)
    for i in range(6):
        circle(d, 12 + i * 8, 38, 3, fill=CLEAR)
    d.rectangle([12, 38, 52, 58], fill=WHITE)
    d.rectangle([28, 44, 36, 58], fill=CLEAR)
    return im


def gear_icon() -> Image.Image:
    im = blank()
    d = ImageDraw.Draw(im)
    cx, cy = 32, 32
    for i in range(8):
        ang = i * (math.pi / 4)
        x1 = cx + math.cos(ang) * 18
        y1 = cy + math.sin(ang) * 18
        x2 = cx + math.cos(ang) * 26
        y2 = cy + math.sin(ang) * 26
        line(d, [(x1, y1), (x2, y2)], 7)
    circle(d, cx, cy, 16)
    circle(d, cx, cy, 7, fill=CLEAR)
    return im


def chat_icon() -> Image.Image:
    im = blank()
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([8, 10, 50, 42], radius=8, fill=WHITE)
    d.polygon([(18, 40), (14, 54), (30, 42)], fill=WHITE)
    return im


def camera_icon() -> Image.Image:
    im = blank()
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([6, 20, 58, 52], radius=6, fill=WHITE)
    d.rectangle([22, 12, 42, 22], fill=WHITE)
    circle(d, 32, 36, 11)
    circle(d, 32, 36, 6, fill=CLEAR)
    return im


def diamond_icon() -> Image.Image:
    im = blank()
    d = ImageDraw.Draw(im)
    d.polygon([(32, 6), (56, 28), (32, 58), (8, 28)], fill=WHITE)
    line(d, [(8, 28), (56, 28)], 2)
    line(d, [(32, 6), (20, 28), (32, 58)], 2)
    line(d, [(32, 6), (44, 28), (32, 58)], 2)
    return im


def folder_icon() -> Image.Image:
    im = blank()
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([8, 18, 56, 52], radius=4, fill=WHITE)
    d.rectangle([8, 14, 30, 24], fill=WHITE)
    return im


def layout_icon() -> Image.Image:
    im = blank()
    d = ImageDraw.Draw(im)
    gap, s = 4, 18
    for r in range(2):
        for c in range(2):
            x0 = 12 + c * (s + gap)
            y0 = 12 + r * (s + gap)
            d.rounded_rectangle([x0, y0, x0 + s, y0 + s], radius=3, fill=WHITE)
    return im


def monitor_icon() -> Image.Image:
    im = blank()
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([8, 10, 56, 42], radius=4, fill=WHITE)
    d.rectangle([14, 16, 50, 36], fill=CLEAR)
    d.rectangle([28, 42, 36, 48], fill=WHITE)
    d.rectangle([20, 48, 44, 54], fill=WHITE)
    return im


def mods_icon() -> Image.Image:
    im = blank()
    d = ImageDraw.Draw(im)
    s = 12
    for row in range(3):
        for col in range(3):
            x0 = 11 + col * 16
            y0 = 11 + row * 16
            d.rounded_rectangle([x0, y0, x0 + s, y0 + s], radius=2, fill=WHITE)
    return im


def speed_icon() -> Image.Image:
    im = blank()
    d = ImageDraw.Draw(im)
    d.arc([8, 12, 56, 60], start=200, end=340, fill=WHITE, width=STROKE)
    line(d, [(32, 36), (48, 20)], STROKE)
    circle(d, 32, 36, 4)
    return im


def discord_icon() -> Image.Image:
    im = blank()
    d = ImageDraw.Draw(im)
    d.ellipse([6, 10, 58, 52], fill=WHITE)
    d.polygon([(14, 16), (10, 4), (24, 14)], fill=WHITE)
    d.polygon([(50, 16), (54, 4), (40, 14)], fill=WHITE)
    circle(d, 24, 30, 5, fill=CLEAR)
    circle(d, 40, 30, 5, fill=CLEAR)
    return im


def bag_icon() -> Image.Image:
    im = blank()
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([14, 22, 50, 56], radius=6, fill=WHITE)
    d.arc([22, 10, 42, 30], start=0, end=180, fill=WHITE, width=STROKE)
    return im


ICONS = {
    "user.png": user_icon,
    "users.png": users_icon,
    "hanger.png": hanger_icon,
    "grid.png": grid_icon,
    "shop.png": shop_icon,
    "gear.png": gear_icon,
    "chat.png": chat_icon,
    "camera.png": camera_icon,
    "diamond.png": diamond_icon,
    "folder.png": folder_icon,
    "layout.png": layout_icon,
    "monitor.png": monitor_icon,
    "mods.png": mods_icon,
    "speed.png": speed_icon,
    "discord.png": discord_icon,
    "bag.png": bag_icon,
}


def main() -> None:
    os.makedirs(OUT, exist_ok=True)
    for name, fn in ICONS.items():
        save(fn(), name)
    print("done", len(ICONS))


if __name__ == "__main__":
    main()
