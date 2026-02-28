# Poly Pixelator — Retro Pixel Art Converter

A Java Swing application that converts images into retro pixel art using downsampling, palette restriction, and optional Floyd-Steinberg dithering.

## Requirements

- **Java JDK 8+** (needs `javac` and `java` on your PATH)

## Quick Start

**Option A — Use the batch script:**
```
build.bat
```

**Option B — Manual compile & run:**
```
mkdir out
javac -d out src\PolyPixelator.java
java -cp out PolyPixelator
```

## How It Works

1. **Load** any PNG/JPG/BMP/GIF image via the file chooser
2. **Pixel Size slider** (1–20 px) controls the block size for downsampling — each block is averaged into a single color
3. **Palette restriction** maps every pixel to the nearest color in a PICO-8 inspired 16-color palette using Euclidean RGB distance
4. **Floyd-Steinberg dithering** (toggle via checkbox) diffuses quantization error to neighboring pixels for smoother gradients
5. **Save** the result as a PNG

All controls update the preview in real time.
