# optimize_pngs.py
# Batch image optimizer for large PNG promo renders → web-friendly PNG + JPG.
#
# REQUIREMENTS:
# pip install pillow
#
# USAGE:
# python optimize_pngs.py              CLI (default folders next to script)
# python optimize_pngs.py --gui       Imagery Optimizery window

from __future__ import annotations

import argparse
import os
import threading
from io import BytesIO
from pathlib import Path

from PIL import Image

APP_NAME = "Imagery Optimizery"
TAGLINE = "Resize · deflate PNG · friendly JPG"

GUI_INFO_BLURB = (
    "Batch-optimizes images for the web: scales down to max width, writes lossless "
    "PNG (zlib 9) plus a progressive JPEG. JPEG is usually much smaller for screenshots "
    "and photos; PNG keeps sharp edges and transparency.\n\n"
    "Inputs: .png, .jpg, .jpeg, .webp.  "
    "Each source file becomes <name>_optimized.png and <name>_optimized.jpg in the output "
    "folder.  Max width 0 means do not resize (still recompresses).  You can pick individual "
    "files below; if the list is empty, the whole input folder is processed."
)

INPUT_FOLDER = "input_images"
OUTPUT_FOLDER = "optimized_images"

BASE_DIR = Path(__file__).resolve().parent

PNG_WIDTH = 960
JPG_WIDTH = 960
JPG_QUALITY = 85
PNG_OPTIMIZE = True
PNG_COMPRESS_LEVEL = 9

SUPPORTED = frozenset({".png", ".jpg", ".jpeg", ".webp"})


def open_normalized_rgba(path: Path) -> Image.Image:
    img = Image.open(path)
    if img.mode not in ("RGB", "RGBA"):
        img = img.convert("RGBA")
    return img


def resize_image(img, target_width: int):
    width, height = img.size

    if target_width <= 0 or width <= target_width:
        return img

    ratio = target_width / width
    new_height = int(height * ratio)

    return img.resize((target_width, new_height), Image.LANCZOS)


def optimize_one(
    file: Path,
    output_dir: Path,
    *,
    png_width: int,
    jpg_width: int,
    jpg_quality: int,
) -> list[str]:
    lines: list[str] = []

    img = open_normalized_rgba(file)

    png_img = resize_image(img, png_width)
    png_output = output_dir / f"{file.stem}_optimized.png"
    png_img.save(
        png_output,
        optimize=PNG_OPTIMIZE,
        compress_level=PNG_COMPRESS_LEVEL,
    )

    jpg_img = resize_image(img, jpg_width)

    if jpg_img.mode == "RGBA":
        background = Image.new("RGB", jpg_img.size, (0, 0, 0))
        background.paste(jpg_img, mask=jpg_img.split()[3])
        jpg_img = background
    else:
        jpg_img = jpg_img.convert("RGB")

    jpg_output = output_dir / f"{file.stem}_optimized.jpg"
    jpg_img.save(
        jpg_output,
        quality=jpg_quality,
        optimize=True,
        progressive=True,
    )

    original_size = os.path.getsize(file) / 1024
    png_size = os.path.getsize(png_output) / 1024
    jpg_size = os.path.getsize(jpg_output) / 1024

    lines.append(f"Processing: {file.name}")
    lines.append(f"  Original : {original_size:.1f} KB")
    lines.append(f"  PNG      : {png_size:.1f} KB")
    lines.append(f"  JPG      : {jpg_size:.1f} KB")
    lines.append("")
    return lines


def discover_files(input_dir: Path) -> list[Path]:
    return sorted(f for f in input_dir.iterdir() if f.suffix.lower() in SUPPORTED)


def estimate_output_kb(
    path: Path,
    *,
    png_width: int,
    jpg_width: int,
    jpg_quality: int,
) -> tuple[int, int, float, int, int, float, float] | tuple[str]:
    """Returns input WxH, input KB, output WxH, est PNG KB, est JPG KB; or (error_msg,) on failure."""
    try:
        img = open_normalized_rgba(path)
        iw, ih = img.size
        in_kb = os.path.getsize(path) / 1024

        png_img = resize_image(img, png_width)
        ow, oh = png_img.size
        buf = BytesIO()
        png_img.save(
            buf,
            format="PNG",
            optimize=PNG_OPTIMIZE,
            compress_level=PNG_COMPRESS_LEVEL,
        )
        png_kb = len(buf.getvalue()) / 1024

        jpg_img = resize_image(img, jpg_width)
        if jpg_img.mode == "RGBA":
            background = Image.new("RGB", jpg_img.size, (0, 0, 0))
            background.paste(jpg_img, mask=jpg_img.split()[3])
            jpg_img = background
        else:
            jpg_img = jpg_img.convert("RGB")
        buf2 = BytesIO()
        jpg_img.save(
            buf2,
            format="JPEG",
            quality=jpg_quality,
            optimize=True,
            progressive=True,
        )
        jpg_kb = len(buf2.getvalue()) / 1024
        return (iw, ih, in_kb, ow, oh, png_kb, jpg_kb)
    except OSError as exc:
        return (str(exc),)
    except Exception as exc:
        return (str(exc),)


def _process_file_list(
    files: list[Path],
    output_dir: Path,
    *,
    png_width: int,
    jpg_width: int,
    jpg_quality: int,
    log,
) -> int:
    output_dir.mkdir(parents=True, exist_ok=True)

    if not files:
        log("No supported images to process.")
        return 1

    log(f"Processing {len(files)} image(s).\n")
    failures = 0

    for f in files:
        try:
            for line in optimize_one(
                f,
                output_dir,
                png_width=png_width,
                jpg_width=jpg_width,
                jpg_quality=jpg_quality,
            ):
                log(line)
        except Exception as exc:
            failures += 1
            log(f"Error processing {f.name}: {exc}")

    log("Done." if failures == 0 else f"Finished with {failures} error(s).")
    return 0 if failures == 0 else 1


def run_batch_paths(
    files: list[Path],
    output_dir: Path,
    *,
    png_width: int,
    jpg_width: int,
    jpg_quality: int,
    log,
) -> int:
    uniq: dict[str, Path] = {}
    for f in files:
        p = f.expanduser().resolve()
        if p.suffix.lower() in SUPPORTED and p.is_file():
            uniq[str(p)] = p
    ordered = sorted(uniq.values(), key=lambda x: str(x).lower())
    return _process_file_list(
        ordered,
        output_dir,
        png_width=png_width,
        jpg_width=jpg_width,
        jpg_quality=jpg_quality,
        log=log,
    )


def run_batch(
    input_dir: Path,
    output_dir: Path,
    *,
    png_width: int,
    jpg_width: int,
    jpg_quality: int,
    log,
) -> int:
    if not input_dir.is_dir():
        log(f"Input folder not found:\n  {input_dir.resolve()}")
        return 1

    files = discover_files(input_dir)

    if not files:
        log("No supported images found.")
        log(f"Put images inside:\n  {input_dir.resolve()}")
        return 1

    return _process_file_list(
        files,
        output_dir,
        png_width=png_width,
        jpg_width=jpg_width,
        jpg_quality=jpg_quality,
        log=log,
    )


def run_cli():
    inp = BASE_DIR / INPUT_FOLDER
    out = BASE_DIR / OUTPUT_FOLDER

    def print_log(msg):
        print(msg)

    rc = run_batch(
        inp,
        out,
        png_width=PNG_WIDTH,
        jpg_width=JPG_WIDTH,
        jpg_quality=JPG_QUALITY,
        log=print_log,
    )
    raise SystemExit(rc)


def run_gui():
    import tkinter as tk
    from tkinter import filedialog
    from tkinter import scrolledtext
    from tkinter import ttk

    root = tk.Tk()
    root.title(APP_NAME)
    root.minsize(680, 520)
    root.configure(bg="#1e272e")

    inp = tk.StringVar(master=root, value=str((BASE_DIR / INPUT_FOLDER).resolve()))
    out = tk.StringVar(master=root, value=str((BASE_DIR / OUTPUT_FOLDER).resolve()))
    max_width = tk.IntVar(master=root, value=PNG_WIDTH)
    quality = tk.IntVar(master=root, value=JPG_QUALITY)

    brand = "#2ecc71"
    muted = "#b2bec3"
    paper = "#dfe6e9"

    tk.Label(
        root,
        text=APP_NAME,
        font=("Segoe UI", 17, "bold"),
        fg=brand,
        bg="#1e272e",
    ).pack(pady=(14, 0))

    tk.Label(
        root,
        text=TAGLINE,
        font=("Segoe UI", 9),
        fg=muted,
        bg="#1e272e",
    ).pack(pady=(2, 10))

    tk.Label(
        root,
        text="About",
        font=("Segoe UI", 10, "bold"),
        fg=brand,
        bg="#1e272e",
        anchor=tk.W,
    ).pack(fill=tk.X, padx=16)
    tk.Label(
        root,
        text=GUI_INFO_BLURB,
        justify=tk.LEFT,
        wraplength=520,
        fg=muted,
        bg="#1e272e",
        font=("Segoe UI", 9),
        anchor=tk.W,
    ).pack(fill=tk.X, padx=16, pady=(2, 8))

    tk.Frame(root, bg="#34495e", height=1).pack(fill=tk.X, padx=16, pady=(4, 10))

    tk.Label(
        root,
        text="Folders, files, & compression",
        font=("Segoe UI", 10, "bold"),
        fg=brand,
        bg="#1e272e",
        anchor=tk.W,
    ).pack(fill=tk.X, padx=16)

    selected_paths: list[Path] = []
    est_job: dict[str, int | None] = {"id": None}
    est_gen: list[int] = [0]

    form = tk.Frame(root, bg="#1e272e")
    form.pack(fill=tk.X, padx=16, pady=(6, 4))
    form.columnconfigure(1, weight=1)

    def browse_input():
        p = filedialog.askdirectory(initialdir=inp.get() or str(BASE_DIR))
        if p:
            inp.set(p)

    def browse_output():
        p = filedialog.askdirectory(initialdir=out.get() or str(BASE_DIR))
        if p:
            out.set(p)

    r = 0
    tk.Label(form, text="Input folder", fg=paper, bg="#1e272e", anchor=tk.W).grid(
        row=r, column=0, sticky=tk.W, pady=4
    )
    e_in = tk.Entry(form, textvariable=inp, fg="#2d3436", bg="#f5f6fa")
    e_in.grid(row=r, column=1, sticky=tk.EW, padx=(10, 8), pady=4)
    tk.Button(
        form,
        text="Browse…",
        command=browse_input,
        bg="#2d3436",
        fg=paper,
        activebackground="#636e72",
        activeforeground=paper,
        relief=tk.FLAT,
        padx=10,
        cursor="hand2",
    ).grid(row=r, column=2, pady=4)

    r = 1
    tk.Label(form, text="Output folder", fg=paper, bg="#1e272e", anchor=tk.W).grid(
        row=r, column=0, sticky=tk.W, pady=4
    )
    e_out = tk.Entry(form, textvariable=out, fg="#2d3436", bg="#f5f6fa")
    e_out.grid(row=r, column=1, sticky=tk.EW, padx=(10, 8), pady=4)
    tk.Button(
        form,
        text="Browse…",
        command=browse_output,
        bg="#2d3436",
        fg=paper,
        activebackground="#636e72",
        activeforeground=paper,
        relief=tk.FLAT,
        padx=10,
        cursor="hand2",
    ).grid(row=r, column=2, pady=4)

    sel_outer = tk.Frame(root, bg="#1e272e")
    sel_outer.pack(fill=tk.BOTH, expand=False, padx=16, pady=(2, 8))

    tk.Label(
        sel_outer,
        text="Selected images (optional)",
        font=("Segoe UI", 10, "bold"),
        fg=brand,
        bg="#1e272e",
        anchor=tk.W,
    ).pack(anchor=tk.W)
    tk.Label(
        sel_outer,
        text="Est. sizes encode in memory like the final export (no temp files). Change width/quality to refresh.",
        fg=muted,
        bg="#1e272e",
        font=("Segoe UI", 8),
        wraplength=520,
        justify=tk.LEFT,
    ).pack(anchor=tk.W, pady=(2, 6))

    style = ttk.Style()
    try:
        style.theme_use("clam")
    except tk.TclError:
        pass
    style.configure(
        "IO.Treeview",
        background="#101820",
        foreground="#c9d6de",
        fieldbackground="#101820",
        rowheight=22,
    )
    style.configure("IO.Treeview.Heading", background="#2d3436", foreground=paper)
    style.map("IO.Treeview", background=[("selected", "#34495e")])

    tree_fr = tk.Frame(sel_outer, bg="#1e272e")
    tree_fr.pack(fill=tk.BOTH, expand=True, pady=(0, 8))
    tree_fr.columnconfigure(0, weight=1)
    tree_fr.rowconfigure(0, weight=1)

    cols = ("name", "in_px", "in_kb", "out_px", "png_kb", "jpg_kb")
    tree = ttk.Treeview(
        tree_fr,
        columns=cols,
        show="headings",
        height=6,
        style="IO.Treeview",
    )
    tree.grid(row=0, column=0, sticky=tk.NSEW)
    tree_scroll = ttk.Scrollbar(tree_fr, orient=tk.VERTICAL, command=tree.yview)
    tree.configure(yscrollcommand=tree_scroll.set)
    tree_scroll.grid(row=0, column=1, sticky=tk.NS)

    tree.heading("name", text="File")
    tree.column("name", width=120, anchor=tk.W)
    tree.heading("in_px", text="In (px)")
    tree.column("in_px", width=76, anchor=tk.CENTER)
    tree.heading("in_kb", text="In (KB)")
    tree.column("in_kb", width=64, anchor=tk.E)
    tree.heading("out_px", text="Out (px)")
    tree.column("out_px", width=76, anchor=tk.CENTER)
    tree.heading("png_kb", text="Est. PNG (KB)")
    tree.column("png_kb", width=86, anchor=tk.E)
    tree.heading("jpg_kb", text="Est. JPG (KB)")
    tree.column("jpg_kb", width=86, anchor=tk.E)

    def sync_paths_from_tree():
        selected_paths[:] = [Path(iid) for iid in tree.get_children()]

    def merge_selected(added: list[Path]) -> None:
        by_key: dict[str, Path] = {}
        for p in selected_paths + added:
            rp = Path(p).expanduser().resolve()
            if rp.suffix.lower() in SUPPORTED and rp.is_file():
                by_key.setdefault(str(rp), rp)
        selected_paths[:] = list(by_key.values())

    def apply_estimate_rows(result: list[tuple[Path, tuple]]) -> None:
        for row in tree.get_children():
            tree.delete(row)
        for p, est in result:
            if len(est) == 1:
                err = est[0]
                short = err if len(err) <= 44 else err[:41] + "…"
                tree.insert(
                    "",
                    "end",
                    iid=str(p.resolve()),
                    values=(p.name, "—", "—", "—", short, "—"),
                )
            else:
                iw, ih, ikb, ow, oh, pk, jk = est
                tree.insert(
                    "",
                    "end",
                    iid=str(p.resolve()),
                    values=(
                        p.name,
                        f"{iw}×{ih}",
                        f"{ikb:.0f}",
                        f"{ow}×{oh}",
                        f"{pk:.0f}",
                        f"{jk:.0f}",
                    ),
                )

    def run_estimate_job(generation: int) -> None:
        est_job["id"] = None
        if generation != est_gen[0]:
            return
        snap = list(selected_paths)
        mw = int(max_width.get())
        jq = int(round(float(quality.get())))

        def work() -> None:
            if generation != est_gen[0]:
                return
            rows: list[tuple[Path, tuple]] = []
            for p in snap:
                if generation != est_gen[0]:
                    return
                rows.append(
                    (p, estimate_output_kb(p, png_width=mw, jpg_width=mw, jpg_quality=jq))
                )

            def apply_safe() -> None:
                if generation != est_gen[0]:
                    return
                apply_estimate_rows(rows)

            root.after(0, apply_safe)

        threading.Thread(target=work, daemon=True).start()

    def schedule_estimate(*_args: object) -> None:
        est_gen[0] += 1
        g = est_gen[0]
        jid = est_job.get("id")
        if jid is not None:
            try:
                root.after_cancel(jid)
            except tk.TclError:
                pass
        est_job["id"] = root.after(120, lambda gi=g: run_estimate_job(gi))

    sel_btns: list[tk.Button] = []

    def add_files() -> None:
        paths = filedialog.askopenfilenames(
            parent=root,
            title="Add images",
            filetypes=[
                ("Image files", "*.png *.jpg *.jpeg *.webp"),
                ("PNG", "*.png"),
                ("JPEG", "*.jpg *.jpeg"),
                ("WebP", "*.webp"),
                ("All files", "*.*"),
            ],
            initialdir=inp.get() or str(BASE_DIR),
        )
        if paths:
            merge_selected([Path(x) for x in paths])
            schedule_estimate()

    def add_folder_to_selection() -> None:
        d = filedialog.askdirectory(initialdir=inp.get() or str(BASE_DIR))
        if d:
            discovered = discover_files(Path(d))
            if discovered:
                merge_selected(discovered)
                schedule_estimate()

    def remove_selected_files() -> None:
        for iid in tree.selection():
            tree.delete(iid)
        sync_paths_from_tree()
        schedule_estimate()

    def clear_file_list() -> None:
        for iid in tree.get_children():
            tree.delete(iid)
        selected_paths.clear()
        schedule_estimate()

    btn_row = tk.Frame(sel_outer, bg="#1e272e")
    btn_row.pack(fill=tk.X)

    def _sel_btn(**kwargs):
        b = tk.Button(**kwargs)
        sel_btns.append(b)
        return b

    _sel_btn(
        master=btn_row,
        text="Add files…",
        command=add_files,
        bg="#2d3436",
        fg=paper,
        activebackground="#636e72",
        activeforeground=paper,
        relief=tk.FLAT,
        padx=10,
        pady=6,
        cursor="hand2",
    ).pack(side=tk.LEFT, padx=(0, 8))

    _sel_btn(
        master=btn_row,
        text="Add folder…",
        command=add_folder_to_selection,
        bg="#2d3436",
        fg=paper,
        activebackground="#636e72",
        activeforeground=paper,
        relief=tk.FLAT,
        padx=10,
        pady=6,
        cursor="hand2",
    ).pack(side=tk.LEFT, padx=(0, 8))

    _sel_btn(
        master=btn_row,
        text="Remove",
        command=remove_selected_files,
        bg="#2d3436",
        fg=paper,
        activebackground="#636e72",
        activeforeground=paper,
        relief=tk.FLAT,
        padx=10,
        pady=6,
        cursor="hand2",
    ).pack(side=tk.LEFT, padx=(0, 8))

    _sel_btn(
        master=btn_row,
        text="Clear list",
        command=clear_file_list,
        bg="#2d3436",
        fg=paper,
        activebackground="#636e72",
        activeforeground=paper,
        relief=tk.FLAT,
        padx=10,
        pady=6,
        cursor="hand2",
    ).pack(side=tk.LEFT)

    row_opts = tk.Frame(root, bg="#1e272e")
    row_opts.pack(fill=tk.X, padx=16, pady=(8, 4))
    tk.Label(row_opts, text="Max width (px)", fg=paper, bg="#1e272e").pack(side=tk.LEFT)
    spin_w = tk.Spinbox(
        row_opts,
        from_=0,
        to=8192,
        textvariable=max_width,
        width=8,
        fg="#2d3436",
        bg="#f5f6fa",
        buttonbackground="#2d3436",
    )
    spin_w.pack(side=tk.LEFT, padx=(8, 20))
    tk.Label(
        row_opts,
        text="JPEG quality",
        fg=paper,
        bg="#1e272e",
    ).pack(side=tk.LEFT)
    tk.Scale(
        row_opts,
        from_=55,
        to=95,
        orient=tk.HORIZONTAL,
        variable=quality,
        showvalue=True,
        length=200,
        bg="#1e272e",
        fg=paper,
        highlightthickness=0,
        troughcolor="#2d3436",
        activebackground=brand,
    ).pack(side=tk.LEFT, padx=(8, 0))

    max_width.trace_add("write", schedule_estimate)
    quality.trace_add("write", schedule_estimate)

    log_box = scrolledtext.ScrolledText(
        root,
        height=12,
        font=("Consolas", 9),
        bg="#101820",
        fg="#c9d6de",
        insertbackground="#c9d6de",
        relief=tk.FLAT,
        state=tk.DISABLED,
    )

    def append_log(line: str):
        log_box.configure(state=tk.NORMAL)
        log_box.insert(tk.END, line + "\n")
        log_box.see(tk.END)
        log_box.configure(state=tk.DISABLED)

    def clear_log():
        log_box.configure(state=tk.NORMAL)
        log_box.delete("1.0", tk.END)
        log_box.configure(state=tk.DISABLED)

    run_bar = tk.Frame(root, bg="#1e272e")
    run_bar.pack(fill=tk.X, padx=16, pady=(4, 6))

    clear_btn = tk.Button(
        run_bar,
        text="Clear log",
        font=("Segoe UI", 9),
        bg="#34495e",
        fg=paper,
        activebackground="#636e72",
        activeforeground=paper,
        relief=tk.FLAT,
        padx=12,
        pady=8,
        cursor="hand2",
        command=clear_log,
    )
    clear_btn.pack(side=tk.LEFT)

    tk.Label(
        run_bar,
        text="Uses the file list when it is not empty; else all supported images in the input folder.",
        fg=muted,
        bg="#1e272e",
        font=("Segoe UI", 8),
    ).pack(side=tk.LEFT, padx=(12, 0), fill=tk.X, expand=True)

    optimize_btn = tk.Button(
        run_bar,
        text="Optimize now →",
        font=("Segoe UI", 11, "bold"),
        fg="#1e272e",
        bg=brand,
        activebackground="#58d68d",
        activeforeground="#1e272e",
        relief=tk.FLAT,
        padx=20,
        pady=10,
        cursor="hand2",
    )
    optimize_btn.pack(side=tk.RIGHT)

    tk.Label(
        root,
        text="Output log",
        font=("Segoe UI", 10, "bold"),
        fg=paper,
        bg="#1e272e",
        anchor=tk.W,
    ).pack(fill=tk.X, padx=16, pady=(6, 2))
    log_box.pack(fill=tk.BOTH, expand=True, padx=16, pady=(0, 6))

    tk.Label(
        root,
        text=f"Imagery Optimizery lives in:\n{BASE_DIR}",
        fg="#636e72",
        bg="#1e272e",
        font=("Segoe UI", 8),
        justify=tk.LEFT,
        wraplength=520,
        anchor=tk.W,
    ).pack(fill=tk.X, padx=16, pady=(0, 12))

    worker_lock = threading.Lock()
    worker_running = False

    def on_optimize():
        nonlocal worker_running
        with worker_lock:
            if worker_running:
                return
            worker_running = True

        inp_path = Path(inp.get().strip()).expanduser()
        out_path = Path(out.get().strip()).expanduser()
        mw = max_width.get()
        q = quality.get()

        optimize_btn.configure(state=tk.DISABLED)
        clear_btn.configure(state=tk.DISABLED)
        for _b in sel_btns:
            _b.configure(state=tk.DISABLED)

        def worker():
            def thread_log(line: str):
                root.after(0, lambda l=line: append_log(l))

            try:
                paths_mode = list(selected_paths)
                if paths_mode:
                    run_batch_paths(
                        paths_mode,
                        out_path,
                        png_width=int(mw),
                        jpg_width=int(mw),
                        jpg_quality=int(round(float(q))),
                        log=thread_log,
                    )
                else:
                    run_batch(
                        inp_path,
                        out_path,
                        png_width=int(mw),
                        jpg_width=int(mw),
                        jpg_quality=int(round(float(q))),
                        log=thread_log,
                    )
            finally:

                def finish():
                    nonlocal worker_running
                    optimize_btn.configure(state=tk.NORMAL)
                    clear_btn.configure(state=tk.NORMAL)
                    for _b in sel_btns:
                        _b.configure(state=tk.NORMAL)
                    with worker_lock:
                        worker_running = False

                root.after(0, finish)

        log_box.configure(state=tk.NORMAL)
        log_box.delete("1.0", tk.END)
        log_box.configure(state=tk.DISABLED)
        threading.Thread(target=worker, daemon=True).start()

    optimize_btn.configure(command=on_optimize)

    root.mainloop()


def main():
    parser = argparse.ArgumentParser(description=APP_NAME)
    parser.add_argument(
        "--gui",
        action="store_true",
        help="Open Imagery Optimizery window.",
    )
    args = parser.parse_args()

    if args.gui:
        run_gui()
    else:
        run_cli()


if __name__ == "__main__":
    main()
