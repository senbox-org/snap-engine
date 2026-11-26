"""Derive SNAP's 5 arc-minute EGM2008 geoid grid from NGA's 2.5' grid as published by PROJ
(https://cdn.proj.org/us_nga_egm08_25.tif, public domain).

Output egm2008_5min_cm.bin: 2161 rows (90N to 90S) x 4320 columns (180W to 180E - 5'), pixel-is-point,
row-major, big-endian int16 geoid undulation in centimetres. Packed as egm2008_5min_cm.zip.
Usage: python make_egm2008_5min.py <us_nga_egm08_25.tif> <out_dir>
"""
import sys, os, zipfile, hashlib
import numpy as np, tifffile

src, out_dir = sys.argv[1], sys.argv[2]
with tifffile.TiffFile(src) as t:
    page = t.pages[0]
    assert page.shape == (4321, 8640), page.shape
    assert page.tags["ModelTiepointTag"].value[3:5] == (-180.0, 90.0)
    assert abs(page.tags["ModelPixelScaleTag"].value[0] - 2.5 / 60) < 1e-12
    full = page.asarray().astype(np.float64)

grid = full[::2, ::2]                       # 5' nodes: every second 2.5' node
assert grid.shape == (2161, 4320)
cm = np.rint(grid * 100.0)
assert cm.min() >= -32768 and cm.max() <= 32767
data = cm.astype(">i2").tobytes()

os.makedirs(out_dir, exist_ok=True)
zip_path = os.path.join(out_dir, "egm2008_5min_cm.zip")
with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
    z.writestr("egm2008_5min_cm.bin", data)
print("grid", grid.shape, "min/max m", grid.min(), grid.max())
print("max quantisation error m", float(np.abs(cm / 100.0 - grid).max()))
print("zip", zip_path, os.path.getsize(zip_path), "bytes, sha256", hashlib.sha256(open(zip_path, "rb").read()).hexdigest())
