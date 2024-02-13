package com.batodev.jigsawpuzzle.cut;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.widget.ImageView;

import com.batodev.jigsawpuzzle.PuzzleActivity;
import com.batodev.jigsawpuzzle.PuzzlePiece;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PuzzleCutter {

    static int numProcessors = Runtime.getRuntime().availableProcessors();
    static ExecutorService executor = null;

    public static List<Bitmap> cut(
            final Bitmap sourceImage,
            int rows,
            int cols,
            String svgString,
            final ImageView imageView,
            PuzzleActivity puzzleActivity,
            List<PuzzlePiece> pieces) throws SVGParseException {
        final List<Bitmap> result = new ArrayList<>();
        final long startTime = System.currentTimeMillis();
        SVG svg = SVG.getFromString(svgString);
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        final Bitmap puzzleGridBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas puzzleGridCanvas = new Canvas(puzzleGridBitmap);
        Paint whiteFill = new Paint();
        whiteFill.setStyle(Paint.Style.FILL);
        whiteFill.setColor(Color.WHITE);
        puzzleGridCanvas.drawRect(0, 0, width, height, whiteFill);
        svg.renderToCanvas(puzzleGridCanvas);

        executor = Executors.newFixedThreadPool(numProcessors);

        final Point[][] puzzlesCenterPoints = divideImage(puzzleGridBitmap, rows, cols);
        int puzzleIndex = 0;
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            for (int colIndex = 0; colIndex < cols; colIndex++) {
                final PuzzlePiece piece = pieces.get(puzzleIndex++);
                int finalRowIndex = rowIndex;
                int finalColIndex = colIndex;
                Runnable puzzleCutJob = () -> {
                    Point puzzleCenter = puzzlesCenterPoints[finalRowIndex][finalColIndex];
                    Region reg = floodFill(puzzleGridBitmap, puzzleCenter.x, puzzleCenter.y);
                    final int regionWidth = reg.getWidth();
                    final int regionHeight = reg.getHeight();
                    final int regionMinX = reg.getMinX();
                    final int regionMinY = reg.getMinY();
                    final Bitmap puzzleBitmap = Bitmap.createBitmap(regionWidth + 1, regionHeight + 1, Bitmap.Config.ARGB_8888);

                    System.out.println("Flood fill took: " + (System.currentTimeMillis() - startTime) + "ms");
                    reg.points().forEach(point -> {
                        int rgbSource = sourceImage.getPixel(point.x(), point.y());
                        int x = point.x() - regionMinX;
                        int y = point.y() - regionMinY;
                        puzzleBitmap.setPixel(x, y, rgbSource);
                    });
                    result.add(puzzleBitmap);
                    Runnable setPuzzleImageAndPositions = () -> {
                        piece.setImageBitmap(puzzleBitmap);
                        piece.pieceWidth = regionWidth;
                        piece.pieceHeight = regionHeight;
                        piece.xCoord = regionMinX + imageView.getLeft() + 4;
                        piece.yCoord = regionMinY + imageView.getTop() + 7;
                    };
                    puzzleActivity.postToHandler(setPuzzleImageAndPositions);
                };
                executor.submit(puzzleCutJob);
                System.out.println("Filling target took: " + (System.currentTimeMillis() - startTime) + "ms");
            }
        }
        executor.shutdown();
        new Thread(() -> {
            try {
                boolean terminated = executor.awaitTermination(1, TimeUnit.HOURS);
                System.out.println(terminated);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            puzzleActivity.postToHandler(puzzleActivity::hideProgressSpinner);
        }).start();
        return result;
    }

    private static Region floodFill(Bitmap image, int startX, int startY) {
        Region reg = new Region(new ArrayList<>(), startX, startY);
        Queue<Point> queue = new ArrayDeque<>();
        int width = image.getWidth();
        int height = image.getHeight();

        // Check if starting point is within image bounds
        if (startX < 0 || startY < 0 || startX >= width || startY >= height) {
            return reg;
        }

        // Check if starting point color is same as target color
        if (image.getPixel(startX, startY) != Color.WHITE) {
            return reg;
        }

        // Add starting point to queue
        queue.add(new Point(startX, startY));

        // Perform flood fill
        while (!queue.isEmpty()) {
            Point current = queue.poll();
            int x = current.x;
            int y = current.y;

            // Check current pixel color
            if (image.getPixel(x, y) != Color.WHITE) {
                continue;
            }

            // Fill current pixel with fill color
            image.setPixel(x, y, Color.GREEN);
            reg.points.add(new Point(x, y));

            // Add neighboring pixels to queue
            if (x > 0) {
                queue.add(new Point(x - 1, y));
            }
            if (x < width - 1) {
                queue.add(new Point(x + 1, y));
            }
            if (y > 0) {
                queue.add(new Point(x, y - 1));
            }
            if (y < height - 1) {
                queue.add(new Point(x, y + 1));
            }
        }
        return reg;
    }

    private static Point[][] divideImage(Bitmap image, int rows, int cols) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Calculate the width and height of each cell
        int cellWidth = width / cols;
        int cellHeight = height / rows;

        Point[][] cellCenters = new Point[rows][cols];

        // Loop through each cell and find its center
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // Calculate the coordinates of the cell center
                int centerX = (j * cellWidth) + (cellWidth / 2);
                int centerY = (i * cellHeight) + (cellHeight / 2);
                cellCenters[i][j] = new Point(centerX, centerY);
            }
        }

        return cellCenters;
    }

    record Point(int x, int y) {
    }

    record Region(Collection<Point> points, int startX, int startY) {
        int getMaxX() {
            return points().stream().map(Point::x).max(Integer::compareTo).orElse(0);
        }

        int getMinX() {
            return points().stream().map(Point::x).min(Integer::compareTo).orElse(0);
        }

        int getMaxY() {
            return points().stream().map(Point::y).max(Integer::compareTo).orElse(0);
        }

        int getMinY() {
            return points().stream().map(Point::y).min(Integer::compareTo).orElse(0);
        }

        int getWidth() {
            return getMaxX() - getMinX();
        }

        int getHeight() {
            return getMaxY() - getMinY();
        }
    }
}
