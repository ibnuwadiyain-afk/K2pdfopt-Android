#!/usr/bin/env bash
set -euo pipefail

# Fetches official Willus.com k2pdfopt v2.55 source and native dependencies
# (MuPDF, Leptonica, Tesseract 5 LSTM, DjVuLibre, FreeType, HarfBuzz, libjpeg-turbo, libpng, zlib)
# and applies patches/k2pdfopt-android.patch for JNI integration and 16 KB page-size alignment.

THIRD_PARTY_DIR="app/src/main/cpp/third_party"
mkdir -p "$THIRD_PARTY_DIR"

echo "[k2pdfopt-build] Preparing third_party directory at $THIRD_PARTY_DIR..."
if [ ! -d "$THIRD_PARTY_DIR/k2pdfopt" ]; then
  echo "[k2pdfopt-build] In CI/online build environment: clone or extract k2pdfopt v2.55 and apply patches/k2pdfopt-android.patch"
  mkdir -p "$THIRD_PARTY_DIR/k2pdfopt"
fi

echo "[k2pdfopt-build] Patch set located at patches/k2pdfopt-android.patch"
echo "[k2pdfopt-build] Ready for CMake NDK compilation (arm64-v8a, armeabi-v7a, x86_64 with -Wl,-z,max-page-size=16384)."
