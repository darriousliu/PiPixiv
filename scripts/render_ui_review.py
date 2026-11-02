#!/usr/bin/env python3
"""Render selected temporary Compose previews and build a local gallery."""

import argparse
import html
import json
import re
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "build/reports/ui-review"
OUTPUT_MARKER = ".ui-review-output"
OUTPUT_SIGNATURE = "PiPixiv Compose Preview gallery\n"
BUILD_ARGS = ["-PcomposePreview=true", "--no-parallel", "--max-workers=1"]


def preview_fqn(value):
    if not re.fullmatch(r"[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*){2,}", value):
        raise argparse.ArgumentTypeError(
            "Expected a full package.ClassKt.previewFunction name"
        )
    return value


def output_directory(value):
    path = Path(value).expanduser().absolute()
    if path.is_symlink():
        raise argparse.ArgumentTypeError(
            "Output directory must not be a symbolic link"
        )
    path = path.resolve()
    if path == ROOT or path in ROOT.parents:
        raise argparse.ArgumentTypeError(
            "Use a dedicated report directory, not the project or its parents"
        )
    if path.exists() and not path.is_dir():
        raise argparse.ArgumentTypeError("Output path must be a directory")
    return path


def prepare_images(output):
    """Clear generated images while preserving all other output files."""
    images = output / "images"
    marker = output / OUTPUT_MARKER
    if images.is_symlink() or (images.exists() and not images.is_dir()):
        raise ValueError(
            f"Refusing to replace a non-directory or symbolic link: {images}"
        )
    owned = (
        marker.is_file()
        and marker.read_text(encoding="utf-8") == OUTPUT_SIGNATURE
    )
    if images.exists() and any(images.iterdir()) and not owned:
        raise ValueError(
            "Refusing to clear unrecognized images; "
            f"choose a new --output directory: {images}"
        )
    output.mkdir(parents=True, exist_ok=True)
    if images.exists():
        shutil.rmtree(images)
    images.mkdir()
    marker.write_text(OUTPUT_SIGNATURE, encoding="utf-8")
    return images


def render(output, fqns):
    names = list(dict.fromkeys(fqns))
    if not names:
        raise ValueError("Provide at least one preview function with --fqn")
    images = prepare_images(output)
    (output / "preview-functions.txt").write_text(
        "\n".join(names) + "\n", encoding="utf-8"
    )
    command = [
        "android", "preview", "render", "gradle://.?app",
        "--component-tree", "--output", str(images),
    ]
    for name in names:
        command.extend(["--fqn", name])
    with (output / "render.log").open("w", encoding="utf-8") as log:
        subprocess.run(
            [*command, "--", *BUILD_ARGS],
            cwd=ROOT,
            stdout=log,
            stderr=subprocess.STDOUT,
            check=True,
        )


def gallery(output):
    log_path = output / "render.log"
    log = log_path.read_text(encoding="utf-8") if log_path.exists() else ""
    if "Error rendering" in log or "Rendering failed" in log:
        raise RuntimeError(f"Render errors remain; inspect {log_path}")
    groups = {}
    for path in sorted((output / "images").rglob("*.png")):
        meta = json.loads(
            path.with_suffix(".json").read_text(encoding="utf-8")
        )
        fqn = meta["methodFQN"]
        group, method = fqn.rsplit(".", 1)
        groups.setdefault(group, [])
        scale = meta.get("densityDpi", 160) / 160
        info = (
            f'{round(meta["width"] / scale)}dp · '
            f'字号 {meta.get("fontScale", 1)}×'
        )
        info += " · 深色" if meta.get("uiMode", 0) & 0x30 == 0x20 else " · 浅色"
        src = html.escape(str(path.relative_to(output)))
        name = html.escape(method)
        groups[group].append(
            f'<article><h3>{name}</h3><p>{info}</p>'
            f'<a href="{src}" target="_blank">'
            f'<img src="{src}" loading="lazy" alt="{name}"></a></article>'
        )
    count = sum(map(len, groups.values()))
    if not count:
        raise RuntimeError("No rendered PNGs found")
    sections = "".join(
        f'<section><h2>{html.escape(title)}</h2><div class="grid">'
        f'{"".join(cards)}</div></section>'
        for title, cards in groups.items()
        if cards
    )
    page = '''<!doctype html><html lang="zh-CN"><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1"><title>PiPixiv UI 验收</title>
<style>body{margin:0;padding:32px;background:#f5f4f7;color:#252329;font:15px system-ui,sans-serif}
main{max-width:1600px;margin:auto}h1{font-size:28px}h2{margin-top:40px;overflow-wrap:anywhere}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:20px;align-items:start}
article{background:white;padding:16px;border:1px solid #e4e0e8;border-radius:16px;min-width:0}h3{font-size:14px;overflow-wrap:anywhere;margin:0}p{color:#6b6375;line-height:1.7}img{display:block;width:100%;height:auto;border-radius:8px}a{color:#66508a}</style>
<main><h1>PiPixiv · UI 验收</h1><p>当前选定的 Compose Preview 页面和组件。点击截图打开原图。</p>'''
    page += f'<p>{count} 张 Compose Preview 截图</p>{sections}</main></html>'
    (output / "index.html").write_text(page, encoding="utf-8")
    print(f"{count} screenshots: {output / 'index.html'}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--fqn",
        action="append",
        type=preview_fqn,
        default=[],
        help="Render this preview function; repeat to select several",
    )
    parser.add_argument(
        "--output",
        type=output_directory,
        default=str(OUTPUT),
        help=(
            "Dedicated report directory "
            "(default: build/reports/ui-review)"
        ),
    )
    parser.add_argument(
        "--gallery-only",
        action="store_true",
        help="Rebuild the gallery without rendering or clearing images",
    )
    args = parser.parse_args()
    if args.gallery_only and args.fqn:
        parser.error("--fqn cannot be combined with --gallery-only")
    if not args.gallery_only and not args.fqn:
        parser.error(
            "provide at least one --fqn package.ClassKt.previewFunction, "
            "or use --gallery-only for an existing report"
        )
    if not args.gallery_only:
        render(args.output, args.fqn)
    gallery(args.output)
