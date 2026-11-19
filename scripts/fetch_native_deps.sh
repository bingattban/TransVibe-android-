#!/usr/bin/env bash
set -euo pipefail
# Fetches pinned upstream native sources and checks AAR/VAD assets for TransVibe.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WHISPER_DIR="$ROOT_DIR/app/src/main/cpp/third_party/whisper.cpp"
LIBS_DIR="$ROOT_DIR/app/libs"
VAD_DIR="$ROOT_DIR/app/src/main/assets/vad"

mkdir -p "$ROOT_DIR/app/src/main/cpp/third_party" "$LIBS_DIR" "$VAD_DIR"

if [ ! -d "$WHISPER_DIR" ]; then
  echo "[1/3] Cloning whisper.cpp (pinned tag v1.7.4)..."
  git clone --depth 1 --branch v1.7.4 https://github.com/ggml-org/whisper.cpp.git "$WHISPER_DIR"
else
  echo "[1/3] whisper.cpp already present at $WHISPER_DIR"
fi

echo "[2/3] Checking app/libs for sherpa-onnx AAR..."
if ! ls "$LIBS_DIR"/sherpa-onnx-*.aar >/dev/null 2>&1; then
  echo "  -> Place verified sherpa-onnx-<version>.aar (arm64-v8a) in: $LIBS_DIR/"
  echo "  -> Source: https://github.com/k2-fsa/sherpa-onnx/releases"
  echo "  -> Do NOT add a separate onnxruntime AAR (sherpa-onnx bundles its own)."
fi

echo "[3/3] Checking optional Silero VAD ONNX model..."
if [ ! -f "$VAD_DIR/silero_vad.onnx" ]; then
  echo "  -> Optional: download silero_vad.onnx from https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/silero_vad.onnx"
  echo "  -> Place in $VAD_DIR/silero_vad.onnx (or app filesDir/aux_models/vad/silero_vad.onnx)."
fi
