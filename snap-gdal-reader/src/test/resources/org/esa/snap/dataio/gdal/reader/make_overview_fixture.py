#!/usr/bin/env python3
"""Build a tiny tiled GeoTIFF whose overviews decimate by 4, 8 and 16 rather than 2, 4, 8.

GDAL exposes every reduced-resolution IFD as an overview, and nothing requires
those to step by two. Cloud-Optimized GeoTIFFs built with `gdaladdo 4 8 16` are
common, and the ICEYE Open Data Initiative ships its products that way: a
20000 x 20000 GRD carries overviews of 5000, 2500 and 1250 and no 2x level.

The layout is tiled, like a real COG - a stripped image takes a different path
through the reader and does not reproduce the same geometry.

Every sample in every IFD is non-zero, so any part of a pyramid level that reads
back as zero is canvas the reader failed to fill.

Usage: python make_overview_fixture.py [<output-dir>]
"""

import os
import struct
import sys

BASE = 256
TILE = 64
DECIMATIONS = (4, 8, 16)        # -> 64, 32, 16
FILL = 200                      # uint8, non-zero everywhere

T_SUBFILE = 254
T_IMAGE_WIDTH, T_IMAGE_LENGTH, T_BITS, T_COMPRESSION = 256, 257, 258, 259
T_PHOTOMETRIC, T_SAMPLES, T_PLANAR, T_SAMPLE_FORMAT = 262, 277, 284, 339
T_TILE_WIDTH, T_TILE_LENGTH, T_TILE_OFFSETS, T_TILE_BYTES = 322, 323, 324, 325

TYPE_SHORT, TYPE_LONG = 3, 4

PAD = b'\x00\x00'


def tile_count(size):
    return (size + TILE - 1) // TILE


def ifd_entries(size, reduced, tile_offsets, tile_bytes):
    tags = {
        T_IMAGE_WIDTH: (TYPE_LONG, size),
        T_IMAGE_LENGTH: (TYPE_LONG, size),
        T_BITS: (TYPE_SHORT, 8),
        T_COMPRESSION: (TYPE_SHORT, 1),
        T_PHOTOMETRIC: (TYPE_SHORT, 1),
        T_SAMPLES: (TYPE_SHORT, 1),
        T_PLANAR: (TYPE_SHORT, 1),
        T_SAMPLE_FORMAT: (TYPE_SHORT, 1),
        T_TILE_WIDTH: (TYPE_SHORT, TILE),
        T_TILE_LENGTH: (TYPE_SHORT, TILE),
        T_TILE_OFFSETS: (TYPE_LONG, tile_offsets),
        T_TILE_BYTES: (TYPE_LONG, tile_bytes),
    }
    if reduced:
        tags[T_SUBFILE] = (TYPE_LONG, 1)   # bit 0 = reduced-resolution image
    return tags


def main():
    out_dir = sys.argv[1] if len(sys.argv) > 1 else os.path.dirname(__file__) or '.'
    path = os.path.join(out_dir, 'overviews_4_8_16.tif')

    sizes = [BASE] + [BASE // d for d in DECIMATIONS]
    tiles_per_image = [tile_count(s) * tile_count(s) for s in sizes]
    tile_bytes = TILE * TILE
    payload = bytes([FILL]) * tile_bytes

    counts = [len(ifd_entries(s, i > 0, 0, 0)) for i, s in enumerate(sizes)]
    ifd_sizes = [2 + n * 12 + 4 for n in counts]
    ifd_offsets, pos = [], 8
    for size in ifd_sizes:
        ifd_offsets.append(pos)
        pos += size

    # out-of-line tile offset / bytecount arrays, for the IFDs that need them
    offs_arr_pos, bytes_arr_pos = [], []
    for n in tiles_per_image:
        offs_arr_pos.append(pos)
        pos += 4 * n
        bytes_arr_pos.append(pos)
        pos += 4 * n

    tile_data_start = pos
    per_image_tile_offsets = []
    for n in tiles_per_image:
        per_image_tile_offsets.append([pos + i * tile_bytes for i in range(n)])
        pos += n * tile_bytes

    with open(path, 'wb') as out:
        out.write(b'II')
        out.write(struct.pack('<HI', 42, ifd_offsets[0]))

        for i, size in enumerate(sizes):
            n = tiles_per_image[i]
            inline = n == 1
            tags = ifd_entries(
                size, i > 0,
                per_image_tile_offsets[i][0] if inline else offs_arr_pos[i],
                tile_bytes if inline else bytes_arr_pos[i])
            out.write(struct.pack('<H', len(tags)))
            for tag in sorted(tags):
                typ, value = tags[tag]
                count = n if tag in (T_TILE_OFFSETS, T_TILE_BYTES) else 1
                out.write(struct.pack('<HHI', tag, typ, count))
                if typ == TYPE_SHORT and count == 1:
                    out.write(struct.pack('<H', value) + PAD)
                else:
                    out.write(struct.pack('<I', value))
            nxt = ifd_offsets[i + 1] if i + 1 < len(sizes) else 0
            out.write(struct.pack('<I', nxt))

        for i, n in enumerate(tiles_per_image):
            out.write(struct.pack('<%dI' % n, *per_image_tile_offsets[i]))
            out.write(struct.pack('<%dI' % n, *([tile_bytes] * n)))

        assert out.tell() == tile_data_start, (out.tell(), tile_data_start)
        for n in tiles_per_image:
            for _ in range(n):
                out.write(payload)

    print('%s  base %dx%d tiled %dx%d, overviews %s (decimation %s)  %d bytes'
          % (os.path.basename(path), BASE, BASE, TILE, TILE,
             ', '.join('%dx%d' % (s, s) for s in sizes[1:]),
             ', '.join(str(d) for d in DECIMATIONS), os.path.getsize(path)))


if __name__ == '__main__':
    main()
