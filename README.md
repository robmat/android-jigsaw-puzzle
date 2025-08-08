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

The puzzle pieces are dynamically generated using a sophisticated cutting algorithm. The core of this is the `PuzzleCurvesGenerator`, which creates unique, pseudo-random SVG paths for the puzzle edges. This ensures that every puzzle has a distinct set of piece shapes.

The `PuzzleCutter` then uses these SVG paths to slice the source image. It employs a flood fill algorithm to accurately define the bitmap for each individual piece, resulting in a clean and precise cut. This advanced technique guarantees a different cut for every game and realistic look.

## License

**Warning:** This project is licensed under the **GNU General Public License v3.0**.

This means that any derivative works (i.e., if you fork this project, modify it, and distribute it) **must also be open-sourced** under the same GNU GPL v3.0 license. Please review the full license before using this code for your own projects.
