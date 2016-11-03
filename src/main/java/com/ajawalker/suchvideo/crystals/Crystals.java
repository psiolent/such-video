package com.ajawalker.suchvideo.crystals;

import com.ajawalker.suchvideo.VideoMaker;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

/**
 * Grows code crystals.
 */
public class Crystals {
	// load settings from file
	private static final Properties SETTINGS;

	static {
		SETTINGS = new Properties();
		try {
			//SETTINGS.load(Crystals.class.getClassLoader().getResourceAsStream("crystals1.properties"));
			SETTINGS.load(Crystals.class.getClassLoader().getResourceAsStream("crystals2.properties"));
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	// initialize statics from settings
	private static final int WIDTH = Integer.valueOf(SETTINGS.getProperty("width"));
	private static final int HEIGHT = Integer.valueOf(SETTINGS.getProperty("height"));
	private static final int DENSITY = Integer.valueOf(SETTINGS.getProperty("density"));

	private static final int FRAME_RATE = Integer.valueOf(SETTINGS.getProperty("frameRate")) * DENSITY;
	private static final int FRAMES_PER_SECOND = Integer.valueOf(SETTINGS.getProperty("framesPerSecond"));

	private static final int FIELD_WIDTH = WIDTH * DENSITY;
	private static final int FIELD_HEIGHT = HEIGHT * DENSITY;

	private static final double OPACITY_FADE_FACTOR = Double.valueOf(SETTINGS.getProperty("opacityFadeFactor"));
	private static final double SEED_CREATION_RATE = Double.valueOf(SETTINGS.getProperty("seedCreationRate")) / DENSITY;
	private static final double SEED_ACTIVATION_RATE = Double.valueOf(SETTINGS.getProperty("seedActivationRate")) / DENSITY;
	private static final double DELTA_THETA_FACTOR = Double.valueOf(SETTINGS.getProperty("deltaThetaFactor")) / DENSITY;
	private static final double DELTA_DELTA_THETA_FACTOR = Double.valueOf(SETTINGS.getProperty("deltaDeltaThetaFactor")) / DENSITY;
	private static final double RADIUS_SCALE_FACTOR = Double.valueOf(SETTINGS.getProperty("radiusScaleFactor"));
	private static final double DELTA_COLOR = Double.valueOf(SETTINGS.getProperty("deltaColor")) / DENSITY;

	private static final int USED_COUNT_START_INIT = Integer.valueOf(SETTINGS.getProperty("usedCountStartInit")) * DENSITY;
	private static final int USED_COUNT_START_INCR = Integer.valueOf(SETTINGS.getProperty("usedCountStartIncr"));

	public static void main(String[] args) throws IOException, InterruptedException {
		new Crystals().run();
	}

	// source of randomness
	private final Random rnd = new Random();

	// our field of cells in which our crystals will grow
	private final Cell[][] field = new Cell[FIELD_WIDTH][FIELD_HEIGHT];

	// list of seeds for growing the crystals
	private final List<Seed> seeds = new LinkedList<>();

	private int usedCountStart = USED_COUNT_START_INIT;

	private VideoMaker video;

	/**
	 * Grows crystals until done.
	 *
	 * @throws IOException          on I/O error
	 * @throws InterruptedException on interruption
	 */
	private void run() throws IOException, InterruptedException {
		// create video maker instance
		video = new VideoMaker(SETTINGS.getProperty("outputFile"), WIDTH, HEIGHT, FRAMES_PER_SECOND);

		// initialize our field with empty cells
		for (int x = 0; x < FIELD_WIDTH; x++) {
			for (int y = 0; y < FIELD_HEIGHT; y++) {
				field[x][y] = new Cell();
			}
		}

		// create some initial seeds
		List<Seed> newSeeds = new LinkedList<>();
		seeds.add(new Seed(
				DENSITY,
				0.0,
				FIELD_WIDTH / 2.0,
				FIELD_HEIGHT / 2.0,
				Math.PI,
				rnd.nextGaussian() * DELTA_THETA_FACTOR,
				rnd.nextGaussian() * DELTA_DELTA_THETA_FACTOR,
				true
		));
		seeds.add(new Seed(
				DENSITY,
				0.0,
				FIELD_WIDTH / 2.0,
				FIELD_HEIGHT / 2.0,
				0.0,
				rnd.nextGaussian() * DELTA_THETA_FACTOR,
				rnd.nextGaussian() * DELTA_DELTA_THETA_FACTOR,
				true
		));

		int frameCount = 0;
		double maxOpacity = 1.0;

		// keep looping until everything fades away
		while (maxOpacity > 0.01) {
			// process all active seeds
			for (Seed seed : seeds) {
				if (seed.active) {
					newSeeds.addAll(seed.advance());
				}
			}

			// iterate over all seeds
			Iterator<Seed> iter = seeds.iterator();
			while (iter.hasNext()) {
				Seed seed = iter.next();

				// activate inactive seeds at activation rate
				if (!seed.active && rnd.nextDouble() < SEED_ACTIVATION_RATE) {
					seed.active = true;
					newSeeds.addAll(seed.advance());
				}

				// remove dead seeds
				if (seed.active && !seed.alive) {
					iter.remove();
				}
			}

			// add any newly created seeds
			seeds.addAll(newSeeds);
			newSeeds.clear();

			// update use count and opacity of all cells
			maxOpacity = 0.0;
			for (int x = 0; x < FIELD_WIDTH; x++) {
				for (int y = 0; y < FIELD_HEIGHT; y++) {
					Cell cell = field[x][y];
					if (cell.usedCount > 0) {
						cell.usedCount -= 1;
					} else {
						cell.opacity *= OPACITY_FADE_FACTOR;
					}
					if (cell.opacity > maxOpacity) {
						maxOpacity = cell.opacity;
					}
				}
			}

			// draw frames to video
			if (frameCount++ >= FRAME_RATE) {
				frameCount = 0;
				drawFrame();
			}
		}

		// finish the video
		video.finish();
		System.exit(0);
	}

	/**
	 * Draws cells to a frame.
	 */
	private void drawFrame() {
		// draw to a buffered image
		BufferedImage bi = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

		// draw each pixel
		for (int x = 0; x < WIDTH; x++) {
			for (int y = 0; y < HEIGHT; y++) {
				// calculate color of pixel from all cells for this pixel
				double rsum = 0.0;
				double gsum = 0.0;
				double bsum = 0.0;
				for (int fx = x * DENSITY; fx < (x + 1) * DENSITY; fx++) {
					for (int fy = y * DENSITY; fy < (y + 1) * DENSITY; fy++) {
						Cell cell = field[fx][fy];
						if (cell.color < 1.0 / 3.0) {
							rsum += 3.0 * cell.color * cell.opacity;
							gsum += (1.0 - 3.0 * cell.color) * cell.opacity;
						} else if (cell.color < 2.0 / 3.0) {
							bsum += 3.0 * (cell.color - 1.0 / 3.0) * cell.opacity;
							rsum += (1.0 - 3.0 * (cell.color - 1.0 / 3.0)) * cell.opacity;
						} else {
							gsum += 3.0 * (cell.color - 2.0 / 3.0) * cell.opacity;
							bsum += (1.0 - 3.0 * (cell.color - 2.0 / 3.0)) * cell.opacity;
						}
					}
				}
				rsum /= DENSITY * DENSITY;
				gsum /= DENSITY * DENSITY;
				bsum /= DENSITY * DENSITY;
				int r = (int) (bsum * 256.0);
				int g = (int) (gsum * 256.0);
				int b = (int) (rsum * 256.0);
				if (r >= 256) {
					r = 255;
				}
				if (g >= 256) {
					g = 255;
				}
				if (b >= 256) {
					b = 255;
				}
				int rgb = (r << 16) | (g << 8) | b;
				bi.setRGB(x, y, rgb);
			}
		}

		video.addFrame(bi);

		usedCountStart += USED_COUNT_START_INCR;
		System.out.println(usedCountStart);
	}

	/**
	 * A single cell on our field.
	 */
	private class Cell {
		private int usedCount = 0;
		private double opacity = 0.0;
		private double color = 0.0;

		/**
		 * Fills this cell with the specified color and opacity.
		 *
		 * @param c the color
		 * @param o the opacity
		 */
		private void fill(double c, double o) {
			color = (color * opacity + c * (1.0 - opacity) * o) / (opacity + (1.0 - opacity) * o);
			opacity += (1.0 - opacity) * o;
		}
	}

	/**
	 * A seed for growing a single crystal.
	 */
	private class Seed {
		private double r;
		private double c;
		private double x;
		private double y;
		private double t;
		private double dt;
		private double ddt;
		private boolean active;
		private boolean alive;

		/**
		 * Create a seed.
		 *
		 * @param r      the seed's radius
		 * @param c      the seed's color
		 * @param x      the seed's x coordinate
		 * @param y      the seed's y coordinate
		 * @param t      the seed's theta value (angle)
		 * @param dt     the seed's delta theta value (angle rate of change)
		 * @param ddt    the seed's delta delta theta value (angle acceleration)
		 * @param active whether this seed is active
		 */
		private Seed(double r, double c, double x, double y, double t, double dt, double ddt, boolean active) {
			this.r = r;
			this.c = c;
			this.x = x;
			this.y = y;
			this.t = t;
			this.dt = dt;
			this.ddt = ddt;
			this.active = active;
			this.alive = true;
		}

		/**
		 * Returns field x value for this seed.
		 */
		private int xf() {
			return (int) x;
		}

		/**
		 * Returns field y value for this seed.
		 */
		private int yf() {
			return (int) y;
		}

		/**
		 * Advances this seed.
		 *
		 * @return any newly created seeds
		 */
		private List<Seed> advance() {
			// a list to put new seeds in
			List<Seed> spawnedSeeds = new LinkedList<>();

			// save current field coords for later comparison
			int prevxf = xf();
			int prevyf = yf();

			// normalize theta
			while (t < 0.0) {
				t += 2.0 * Math.PI;
			}
			while (t >= 2.0 * Math.PI) {
				t -= 2.0 * Math.PI;
			}

			// move in direction of current theta
			x += Math.cos(t);
			y += Math.sin(t);

			// advance theta and also rate of change
			t += dt / DENSITY;
			dt += ddt / DENSITY;

			// advance color and normalize
			c += DELTA_COLOR;
			while (c < 0.0) {
				c += 1.0;
			}
			while (c >= 1.0) {
				c -= 1.0;
			}

			// see if we've moved to a new cell
			if (xf() != prevxf || yf() != prevyf) {
				// see if we're still alive
				alive = xf() >= 0 && xf() < FIELD_WIDTH && yf() >= 0 && yf() < FIELD_HEIGHT && field[xf()][yf()].usedCount == 0;
				if (alive) {
					// we're alive and in a new cell, so draw to the cell
					draw(xf(), yf(), new HashSet<Cell>());
					field[xf()][yf()].usedCount = usedCountStart;
					// spawn new seeds at seed creation rate
					if (rnd.nextDouble() < SEED_CREATION_RATE) {
						spawnedSeeds.add(new Seed(
								r * RADIUS_SCALE_FACTOR,
								c,
								x,
								y,
								t + Math.PI / 2.0,
								rnd.nextGaussian() * DELTA_THETA_FACTOR,
								rnd.nextGaussian() * DELTA_DELTA_THETA_FACTOR,
								false
						));
						spawnedSeeds.add(new Seed(
								r * RADIUS_SCALE_FACTOR,
								c,
								x,
								y,
								t - Math.PI / 2.0,
								rnd.nextGaussian() * DELTA_THETA_FACTOR,
								rnd.nextGaussian() * DELTA_DELTA_THETA_FACTOR,
								false
						));
					}
				}
			}
			return spawnedSeeds;
		}

		/**
		 * Draws this seed to the field.
		 *
		 * @param dx    x coordinate of cell to draw to
		 * @param dy    y coordinate of cell to draw to
		 * @param drawn list of cells we've already drawn to
		 */
		private void draw(int dx, int dy, Set<Cell> drawn) {
			if (dx >= 0 && dx < FIELD_WIDTH && dy >= 0 && dy < FIELD_HEIGHT) {
				// we're in the field
				Cell cell = field[dx][dy];
				if (cell.usedCount == 0 && !drawn.contains(cell)) {
					// cell is not already in use and we've not already drawn here, so draw now
					drawn.add(cell);
					double distx = x - dx;
					double disty = y - dy;
					double dist = Math.sqrt(distx * distx + disty * disty);
					if (dist < r) {
						// we're within the radius so fill this cell
						cell.fill(c, 1.0 - dist / r);
						draw(dx + 1, dy, drawn);
						draw(dx - 1, dy, drawn);
						draw(dx, dy + 1, drawn);
						draw(dx, dy - 1, drawn);
					}
				}
			}
		}
	}
}