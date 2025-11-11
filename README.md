# Android Jigsaw Puzzle Game

A simple yet feature-rich jigsaw puzzle game for Android.

Based on: [How to Build a Jigsaw Puzzle Android Game](https://dragosholban.com/2018/03/09/how-to-build-a-jigsaw-puzzle-android-game/)

Play store link: [Puzzled Girls](https://play.google.com/store/apps/details?id=com.batodev.jigsawpuzzle)

## Features

*   **Custom Images:** Play with a variety of bundled images, or use your own pictures from your phone's gallery or by taking a new photo with the camera.
*   **Variable Difficulty:** Choose from a range of puzzle sizes to adjust the difficulty to your preference.
*   **High Scores:** Challenge yourself and keep track of your best completion times for each difficulty level.
*   **Rewards Gallery:** Successfully completed puzzles unlock the full image in a dedicated gallery.
*   **Wallpapers:** Set any of your unlocked reward images as your phone's wallpaper directly from the app.
*   **Immersive Experience:** Includes sound effects for a more engaging gameplay.
*   **User-Friendly Interface:** The puzzle area is zoomable and pannable, making it easy to handle puzzles of any size.

## Rewards System

Upon completing a puzzle, the image you just solved is added to your personal in-game gallery. You can browse all your unlocked images anytime. To personalize the experience, you can add your own images to the `app/src/main/assets/img` directory. These images will then be available to be used as puzzles and subsequently as rewards in the gallery.

## Cutting Algorithm

The project previously used a single static cutter. The cutting code has been refactored into a small strategy API so multiple cutting algorithms can coexist and be swapped easily.

- PuzzleCutter (interface)
  - Defines `cut(sourceImage: Bitmap, rows: Int, cols: Int, svgString: String?, imageView: ImageView, puzzleProgressListener: PuzzleProgressListener, pieces: List<PuzzlePiece>): List<Bitmap>`.

- Implementations included in this repo:
  - FloodFillPuzzleCutter — The original approach: renders the SVG grid into a bitmap and performs a flood-fill on that rendered grid to collect per-piece pixels directly from the source image. This is CPU-bound and parallelized across a fixed thread pool.
  - MaskBitmapPuzzleCutter — An alternative mask-based approach: renders the SVG once to a mask bitmap, then for each piece creates a per-piece mask (flood-filling the mask's transparent areas), applies the mask with Porter-Duff SRC_IN to the source image, and then crops the visible bounds. This approach tends to be easier to reason about and may perform differently depending on image size and device.

- Default
  - The code exposes a factory `PuzzleCutter.default()` returning the default implementation (currently `MaskBitmapPuzzleCutter`). You can change the default there or inject a different implementation where `PuzzleGameManager` is constructed.

- Benchmark/test
  - An instrumented test `ImageMaskTest.cutOutAllPuzzlePieces` (androidTest) runs both implementations against the same image and logs timings and piece counts. The test writes results to `cacheDir/cutter_benchmark/timing.txt` for quick inspection.

- Notes and trade-offs
  - Both cutters use a fixed thread pool; the FloodFill version marks pixels directly on a rendered SVG grid (modifies a working bitmap during flood-fill), while the MaskBitmap version operates on pixel arrays and uses alpha-based transparency checks. The best choice can vary per-device and per-image; use the benchmark test to compare on your target devices.
  - If you need to force a specific cutter at runtime, pass the desired `PuzzleCutter` implementation into `PuzzleGameManager` (a constructor parameter) or change the factory function.

## License

**Warning:** This project is licensed under the **GNU General Public License v3.0**.

This means that any derivative works (i.e., if you fork this project, modify it, and distribute it) **must also be open-sourced** under the same GNU GPL v3.0 license. Please review the full license before using this code for your own projects.
