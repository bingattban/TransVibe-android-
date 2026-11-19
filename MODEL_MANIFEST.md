# Model Manifest (Exact Seven ASR Artifacts)

| modelId | Physical Filename | Engine | Format | Notes |
|---|---|---|---|---|
| `whisper-tiny` | `ggml-tiny.bin` | `WhisperEngine` | GGML BIN | First model to benchmark & freeze |
| `whisper-base` | `ggml-base.bin` | `WhisperEngine` | GGML BIN | Isolated profile |
| `whisper-small` | `ggml-small.bin` | `WhisperEngine` | GGML BIN | Isolated profile |
| `whisper-medium` | `ggml-medium.bin` | `WhisperEngine` | GGML BIN | High memory policy |
| `whisper-large-v3-turbo` | `ggml-large-v3-turbo.bin` | `WhisperEngine` | GGML BIN | High memory policy; no Q5 substitution |
| `moonshine-base-ar` | `moonshine-base-ar.zip` | `MoonshineArabicEngine` | Distribution ZIP | Arabic ONLY |
| `sensevoice-small-int8` | `sensevoice-small-int8.zip` | `SenseVoiceEngine` | Distribution ZIP | `zh, yue, en, ja, ko` |
