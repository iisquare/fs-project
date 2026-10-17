"""Generate the transparent loading GIF from the application icon.

The source icon is a dark rounded badge.  The loader keeps only its light atom
mark and adds two electrons orbiting the mark, so the asset has no rectangular
background and stays visually tied to the client's existing brand.
"""

from __future__ import annotations

import math
import os
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "resources" / "icon.png"
OUTPUT = ROOT / "src" / "renderer" / "src" / "assets" / "images" / "app-loading.gif"
STATIC = ROOT / "src" / "renderer" / "src" / "assets" / "images" / "app-loading-static.png"
PREVIEW = Path(os.environ.get("TEMP", str(ROOT))) / "app-loading-preview.png"

SIZE = 160
SUPERSAMPLE = 4
CANVAS = SIZE * SUPERSAMPLE
FRAMES = 60
DURATION_MS = 40
LOGO_PX = 100

MARK_COLOR = (58, 168, 214)
ELECTRON_A = (94, 203, 237)
ELECTRON_B = (45, 145, 191)


def extract_mark() -> Image.Image:
    """Drop the dark badge and keep the original atom silhouette."""
    icon = np.asarray(Image.open(SOURCE).convert("RGBA")).astype(np.float32)
    rgb = icon[..., :3]
    background = np.array([43, 46, 58], dtype=np.float32)
    distance = np.linalg.norm(rgb - background, axis=2)
    mask = np.clip((distance - 18.0) / 62.0, 0.0, 1.0) ** 0.85
    alpha = mask * (icon[..., 3] / 255.0)

    mark = np.zeros_like(icon)
    mark[..., :3] = MARK_COLOR
    mark[..., 3] = alpha * 255.0
    image = Image.fromarray(mark.astype(np.uint8), "RGBA")

    bounds = image.getchannel("A").point(lambda value: 255 if value > 10 else 0).getbbox()
    if bounds is None:
        raise RuntimeError("The source icon does not contain a visible mark")
    image = image.crop(bounds)

    width, height = image.size
    side = max(width, height) + 24
    square = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    square.alpha_composite(image, ((side - width) // 2, (side - height) // 2))
    return square.resize((LOGO_PX * SUPERSAMPLE, LOGO_PX * SUPERSAMPLE), Image.Resampling.LANCZOS)


def orbit_point(angle: float, radius_x: float, radius_y: float, rotation: float) -> tuple[float, float]:
    x = math.cos(angle) * radius_x
    y = math.sin(angle) * radius_y
    cos_r = math.cos(rotation)
    sin_r = math.sin(rotation)
    return x * cos_r - y * sin_r, x * sin_r + y * cos_r


def render_frame(mark: Image.Image, index: int) -> Image.Image:
    progress = index / FRAMES
    phase = progress * math.tau
    frame = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    draw = ImageDraw.Draw(frame)

    # The atom breathes very slightly and floats by a couple of pixels.
    pulse = 1.0 + 0.022 * math.sin(phase)
    float_y = 2.0 * math.sin(phase - math.pi / 2)
    logo_size = round(LOGO_PX * SUPERSAMPLE * pulse)
    logo = mark.resize((logo_size, logo_size), Image.Resampling.LANCZOS)
    logo = logo.rotate(
        1.5 * math.sin(phase),
        resample=Image.Resampling.BICUBIC,
        expand=True,
    )
    frame.alpha_composite(
        logo,
        (
            (CANVAS - logo.width) // 2,
            round((CANVAS - logo.height) / 2 + float_y * SUPERSAMPLE),
        ),
    )

    center = CANVAS / 2
    radius_x = 70.0 * SUPERSAMPLE
    radius_y = 43.0 * SUPERSAMPLE
    rotation = math.radians(-21)

    # Two electrons with short trails: smaller trailing dots read as motion
    # without relying on alpha, which GIF cannot blend.
    for base_angle, color, head_radius, direction in (
        (phase, ELECTRON_A, 5.0, 1.0),
        (phase + math.pi, ELECTRON_B, 4.2, 1.0),
    ):
        for trail, scale in ((0.0, 1.0), (-0.055, 0.68), (-0.105, 0.44), (-0.15, 0.24)):
            angle = base_angle + direction * trail
            x, y = orbit_point(angle, radius_x, radius_y, rotation)
            radius = head_radius * scale * SUPERSAMPLE
            draw.ellipse(
                (
                    center + x - radius,
                    center + y - radius,
                    center + x + radius,
                    center + y + radius,
                ),
                fill=(*color, 255),
            )
            if trail == 0.0:
                glint = radius * 0.36
                draw.ellipse(
                    (
                        center + x - glint,
                        center + y - glint,
                        center + x + glint,
                        center + y + glint,
                    ),
                    fill=(238, 252, 255, 255),
                )

    return frame.resize((SIZE, SIZE), Image.Resampling.LANCZOS)


def to_gif_frame(frame: Image.Image) -> Image.Image:
    alpha = frame.getchannel("A").point(lambda value: 255 if value >= 112 else 0)
    palette = frame.convert("RGB").quantize(colors=255, method=Image.Quantize.MEDIANCUT)
    colors = palette.getpalette() or []
    colors += [0, 0, 0] * (768 - len(colors))
    palette.putpalette(colors[:768])
    transparent = alpha.point(lambda value: 255 if value == 0 else 0)
    palette.paste(255, mask=transparent)
    return palette


def main() -> None:
    mark = extract_mark()
    render_frame(mark, 0).save(STATIC, optimize=True)
    frames = [to_gif_frame(render_frame(mark, index)) for index in range(FRAMES)]

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    frames[0].save(
        OUTPUT,
        save_all=True,
        append_images=frames[1:],
        duration=DURATION_MS,
        loop=0,
        transparency=255,
        disposal=2,
        optimize=True,
    )

    # Four-frame contact sheet for visual review.
    samples = [render_frame(mark, index) for index in (0, 15, 30, 45)]
    sheet = Image.new("RGB", (SIZE * len(samples), SIZE), (247, 249, 252))
    for offset, sample in enumerate(samples):
        sheet.paste(sample, (offset * SIZE, 0), sample)
    sheet.save(PREVIEW)

    print(f"gif: {OUTPUT}")
    print(f"static: {STATIC}")
    print(f"preview: {PREVIEW}")
    print(f"size: {OUTPUT.stat().st_size} bytes")


if __name__ == "__main__":
    main()
