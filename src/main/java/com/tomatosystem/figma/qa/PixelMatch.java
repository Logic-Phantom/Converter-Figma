package com.tomatosystem.figma.qa;

import java.awt.image.BufferedImage;

/**
 * Java port of mapbox/pixelmatch (ISC): perceptual colour difference in YIQ space with anti-aliasing detection.
 * Differing pixels are painted red on a faded grey copy of the reference, anti-aliased ones yellow. Images of different
 * size are compared on the union canvas, the missing area counting as white.
 */
public final class PixelMatch {
	private PixelMatch() { }

	public static final class Result {
		public final int width;
		public final int height;
		public final int mismatched;
		public final int antialiased;
		public final BufferedImage diff;
		Result(int width, int height, int mismatched, int antialiased, BufferedImage diff) {
			this.width = width; this.height = height; this.mismatched = mismatched; this.antialiased = antialiased; this.diff = diff;
		}
		public int total() { return width * height; }
		/** 0.0 = identical, 1.0 = every pixel differs. */
		public double ratio() { return total() == 0 ? 0 : mismatched / (double) total(); }
	}

	/** @param threshold 0..1, 0.1 is the pixelmatch default (smaller = more sensitive) */
	public static Result compare(BufferedImage a, BufferedImage b, double threshold, boolean includeAntialiased) {
		int width = Math.max(a.getWidth(), b.getWidth());
		int height = Math.max(a.getHeight(), b.getHeight());
		int[] img1 = pixels(a, width, height);
		int[] img2 = pixels(b, width, height);
		BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		double maxDelta = 35215 * threshold * threshold;
		int mismatched = 0;
		int antialiased = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int pos = y * width + x;
				if (img1[pos] == img2[pos]) { out.setRGB(x, y, faded(img1[pos])); continue; }
				double delta = colorDelta(img1[pos], img2[pos], false);
				if (Math.abs(delta) > maxDelta) {
					if (!includeAntialiased && (isAntialiased(img1, x, y, width, height, img2) || isAntialiased(img2, x, y, width, height, img1))) {
						out.setRGB(x, y, 0xFFFFFF00);
						antialiased++;
					} else {
						out.setRGB(x, y, delta < 0 ? 0xFFFF0000 : 0xFFE60000);
						mismatched++;
					}
				} else {
					out.setRGB(x, y, faded(img1[pos]));
				}
			}
		}
		return new Result(width, height, mismatched, antialiased, out);
	}

	/** Mismatch ratio per cell of a rows×cols grid, for locating where the layouts drift apart. */
	public static double[][] heatmap(Result result, int rows, int cols) {
		double[][] cells = new double[rows][cols];
		int[][] counts = new int[rows][cols];
		int[][] hits = new int[rows][cols];
		for (int y = 0; y < result.height; y++) {
			int r = Math.min(rows - 1, y * rows / Math.max(1, result.height));
			for (int x = 0; x < result.width; x++) {
				int c = Math.min(cols - 1, x * cols / Math.max(1, result.width));
				counts[r][c]++;
				int rgb = result.diff.getRGB(x, y);
				if (rgb == 0xFFFF0000 || rgb == 0xFFE60000) hits[r][c]++;
			}
		}
		for (int r = 0; r < rows; r++) { for (int c = 0; c < cols; c++) cells[r][c] = counts[r][c] == 0 ? 0 : hits[r][c] / (double) counts[r][c]; }
		return cells;
	}

	private static int[] pixels(BufferedImage image, int width, int height) {
		int[] out = new int[width * height];
		java.util.Arrays.fill(out, 0xFFFFFFFF);
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) out[y * width + x] = image.getRGB(x, y);
		}
		return out;
	}

	private static int faded(int argb) {
		double alpha = ((argb >>> 24) & 0xFF) / 255.0;
		int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
		double y = rgb2y(r, g, b, alpha);
		int v = (int) Math.round(255 + (y - 255) * 0.1);
		v = Math.max(0, Math.min(255, v));
		return 0xFF000000 | (v << 16) | (v << 8) | v;
	}

	private static double colorDelta(int p1, int p2, boolean yOnly) {
		double a1 = ((p1 >>> 24) & 0xFF) / 255.0, a2 = ((p2 >>> 24) & 0xFF) / 255.0;
		int r1 = (p1 >> 16) & 0xFF, g1 = (p1 >> 8) & 0xFF, b1 = p1 & 0xFF;
		int r2 = (p2 >> 16) & 0xFF, g2 = (p2 >> 8) & 0xFF, b2 = p2 & 0xFF;
		if (a1 == a2 && r1 == r2 && g1 == g2 && b1 == b2) return 0;
		double y1 = rgb2y(r1, g1, b1, a1), y2 = rgb2y(r2, g2, b2, a2);
		double y = y1 - y2;
		if (yOnly) return y;
		double i = rgb2i(r1, g1, b1, a1) - rgb2i(r2, g2, b2, a2);
		double q = rgb2q(r1, g1, b1, a1) - rgb2q(r2, g2, b2, a2);
		double delta = 0.5053 * y * y + 0.299 * i * i + 0.1957 * q * q;
		return y1 > y2 ? -delta : delta;
	}

	private static double blend(int c, double a) { return 255 + (c - 255) * a; }
	private static double rgb2y(int r, int g, int b, double a) { return blend(r, a) * 0.29889531 + blend(g, a) * 0.58662247 + blend(b, a) * 0.11448223; }
	private static double rgb2i(int r, int g, int b, double a) { return blend(r, a) * 0.59597799 - blend(g, a) * 0.27417610 - blend(b, a) * 0.32180189; }
	private static double rgb2q(int r, int g, int b, double a) { return blend(r, a) * 0.21147017 - blend(g, a) * 0.52261711 + blend(b, a) * 0.31114694; }

	/** pixelmatch's antialiased(): a pixel between a darkest and a brightest neighbour that themselves have equal siblings. */
	private static boolean isAntialiased(int[] img, int x1, int y1, int width, int height, int[] other) {
		int x0 = Math.max(x1 - 1, 0), y0 = Math.max(y1 - 1, 0), x2 = Math.min(x1 + 1, width - 1), y2 = Math.min(y1 + 1, height - 1);
		int pos = y1 * width + x1;
		int zeroes = (x1 == x0 || x1 == x2 || y1 == y0 || y1 == y2) ? 1 : 0;
		double min = 0, max = 0;
		int minX = 0, minY = 0, maxX = 0, maxY = 0;
		for (int x = x0; x <= x2; x++) {
			for (int y = y0; y <= y2; y++) {
				if (x == x1 && y == y1) continue;
				double delta = colorDelta(img[pos], img[y * width + x], true);
				if (delta == 0) { zeroes++; if (zeroes > 2) return false; }
				else if (delta < min) { min = delta; minX = x; minY = y; }
				else if (delta > max) { max = delta; maxX = x; maxY = y; }
			}
		}
		if (min == 0 || max == 0) return false;
		return (hasManySiblings(img, minX, minY, width, height) && hasManySiblings(other, minX, minY, width, height))
			|| (hasManySiblings(img, maxX, maxY, width, height) && hasManySiblings(other, maxX, maxY, width, height));
	}

	private static boolean hasManySiblings(int[] img, int x1, int y1, int width, int height) {
		int x0 = Math.max(x1 - 1, 0), y0 = Math.max(y1 - 1, 0), x2 = Math.min(x1 + 1, width - 1), y2 = Math.min(y1 + 1, height - 1);
		int pos = y1 * width + x1;
		int zeroes = (x1 == x0 || x1 == x2 || y1 == y0 || y1 == y2) ? 1 : 0;
		for (int x = x0; x <= x2; x++) {
			for (int y = y0; y <= y2; y++) {
				if (x == x1 && y == y1) continue;
				if (img[pos] == img[y * width + x]) zeroes++;
				if (zeroes > 2) return true;
			}
		}
		return false;
	}
}
