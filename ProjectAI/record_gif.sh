#!/usr/bin/env bash
# Record one game of any Pacman AI as a GIF (needs ffmpeg).
#   ./record_gif.sh out.gif -search 10
#   ./record_gif.sh out.gif -qlearn
set -euo pipefail
cd "$(dirname "$0")"
out="$1"; shift
frames="$(mktemp -d)"
trap 'rm -rf "$frames"' EXIT
./build.sh run "$@" -record "$frames" | tail -1
ffmpeg -loglevel error -y -framerate 10 -i "$frames/frame_%05d.png" \
    -vf "tpad=stop_mode=clone:stop_duration=2,split[a][b];[a]palettegen=max_colors=32:stats_mode=full[p];[b][p]paletteuse=dither=none" \
    "$out"
echo "Saved $out"
