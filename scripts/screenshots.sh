#!/usr/bin/env bash
# Captures README screenshots of the sample app on a running emulator.
# adb can't tap reliably, so the sample opens each screen from the `scene` extra with fixed sample traffic.
set -euo pipefail
source "$(dirname "$0")/screenshot-lib.sh"

install_sample
for mode in light dark; do
  set_night_mode "$mode"
  for scene in home list detail overview; do
    fresh_launch --es scene "$scene"
    capture "$scene-$mode"
  done
done
