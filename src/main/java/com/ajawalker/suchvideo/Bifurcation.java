package com.ajawalker.suchvideo;

import java.awt.image.BufferedImage;

/**
 * Creates a video illustrating the progression of a logistic map bifurcation
 * diagram.
 * <p/>
 * https://en.wikipedia.org/wiki/Bifurcation_diagram
 */
public class Bifurcation {
	private static final double MIN_X = 2.8;
	private static final double MAX_X = 4.0;
	private static final double MIN_Y = 0.0;
	private static final double MAX_Y = 1.0;

	private static final double START = 0.5;

	private static final double PERSISTENCE = 0.6;
	private static final double BRIGHTNESS = 0.6;
	private static final double ILLUMINATION = 4.0;

	private static final int WIDTH = 1280;
	private static final int HEIGHT = 720;
	private static final int DENSITY = 10;

	private static final double CELL_WIDTH = (MAX_X - MIN_X) / (WIDTH * DENSITY);
	private static final double CELL_HEIGHT = (MAX_Y - MIN_Y) / (HEIGHT * DENSITY);

	private static final int FPS = 24;
	private static final int NUM_FRAMES = FPS * 60;

	private static final String OUTPUT_FILE = "target/bifurcation.mp4";

	public static void main(String[] args) {
		VideoMaker video = new VideoMaker(OUTPUT_FILE, WIDTH, HEIGHT, FPS);
		BufferedImage bi = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

		double[][] field = new double[WIDTH * DENSITY][];
		double[] val = new double[WIDTH * DENSITY];
		for (int i = 0; i < WIDTH * DENSITY; i++) {
			field[i] = new double[HEIGHT * DENSITY];
			val[i] = START;
		}

		int numFrames = 0;

		while (numFrames < NUM_FRAMES) {
			for (int i = 0; i < val.length; i++) {
				for (int j = 0; j < field[i].length; j++) {
					field[i][j] *= PERSISTENCE;
				}
				double r = MIN_X + i * CELL_WIDTH;
				val[i] = r * val[i] * (1.0 - val[i]);

				for (int d = 0; d < DENSITY; d++) {
					double p = val[i] + d * CELL_HEIGHT;
					int j = (int) ((p - MIN_Y) / CELL_HEIGHT);
					if (j >= 0 && j < field[i].length) {
						field[i][j] += (1.0 - field[i][j]) * BRIGHTNESS;
					}
				}
			}

			numFrames = video.addFrame(bi);
			System.out.println(numFrames);

			for (int x = 0; x < WIDTH; x++) {
				for (int y = 0; y < HEIGHT; y++) {
					double wu = 0.0;
					for (int i = 0; i < DENSITY; i++) {
						for (int j = 0; j < DENSITY; j++) {
							wu += field[x * DENSITY + i][y * DENSITY + j];
						}
					}
					wu /= (DENSITY * DENSITY);
					bi.setRGB(x, HEIGHT - y - 1, rgb(wu));
				}
			}
		}

		video.finish();
	}

	private static int rgb(double m) {
		m = Math.min(m * ILLUMINATION, 1.0);
		int r = (int) (255 * Math.pow(m, 2.0));
		int g = (int) (255 * Math.pow(m, 0.8));
		int b = (int) (255 * Math.pow(m, 0.5));
		return (r << 16) + (g << 8) + b;
	}
}
