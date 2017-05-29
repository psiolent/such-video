package com.ajawalker.suchvideo.electricblob;

import com.ajawalker.suchvideo.VideoMaker;
import com.ajawalker.suchvideo.position.Grid;
import com.ajawalker.suchvideo.position.Positioned;
import com.ajawalker.suchvideo.position.Vector;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Simulates and creates video of an electric-looking blob of particles.
 */
public class ElectricBlob {
	private static final double FRAME_STEPS = 50;
	private static final int FRAMES_PER_SECOND = 24;
	private static final int NUM_FRAMES = 20 * 60 * FRAMES_PER_SECOND;
	private static final double STEP_SIZE = 1.0 / (FRAME_STEPS * FRAMES_PER_SECOND);

	private static final int WIDTH = 1280;
	private static final int HEIGHT = 720;

	private static final double MIN_X = 0.0;
	private static final double MAX_X = WIDTH;
	private static final double MIN_Y = 0.0;
	private static final double MAX_Y = HEIGHT;

	private static final double GRID_CELL_SIZE = 2.5;

	private static final double NODE_RADIUS = 3.0;
	private static final double DRAW_RADIUS_FACTOR = 1.2;
	private static final double NODE_PUSH_FORCE = 500000.0;
	private static final double DRAG_FORCE = 0.2;
	private static final double FLOCK_FORCE = 10.0;
	private static final double GRAVITY_FORCE = 500.0;

	private static final double SPAWN_FREQUENCY = 1.0;
	private static final double SPAWN_RADIUS = 5.0;
	private static final double SPAWN_ROTATION = 10.0;

	private static final double RED_SCALE = 180000.0;
	private static final double GREEN_SCALE = 120000.0;
	private static final double BLUE_SCALE = 200.0;
	private static final double BLUE_MINIMUM = 0.3;

	private static final double[] NODE_PUSH_FORCE_POINTS = new double[]{
			1.1 * NODE_RADIUS,
			1.2 * NODE_RADIUS
	};
	private static final double[] NODE_PUSH_FORCE_SLOPE = new double[]{
			NODE_PUSH_FORCE / NODE_RADIUS,
			-NODE_PUSH_FORCE / NODE_RADIUS,
	};
	private static final double[] NODE_PUSH_FORCE_OFFSET = new double[]{
			-NODE_PUSH_FORCE,
			1.2 * NODE_PUSH_FORCE
	};

	private static final String OUTPUT_FILE = "target/electricblob.mp4";

	public static void main(String[] args) throws IOException, InterruptedException {
		new ElectricBlob().go();
	}

	/**
	 * Starts the simulation.
	 */
	private void go() throws IOException, InterruptedException {
		// setup video maker
		VideoMaker video = new VideoMaker(OUTPUT_FILE, WIDTH, HEIGHT, FRAMES_PER_SECOND);

		// create an image buffer to draw on
		BufferedImage buffer = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

		// we'll need some randomness
		Random rnd = new Random();

		// and a couple of data structures for holding our nodes (blob particles)
		Grid<Node> grid = new Grid<>(MIN_X, MAX_X, MIN_Y, MAX_Y, (int) (WIDTH / (NODE_RADIUS * GRID_CELL_SIZE)), (int) (HEIGHT / (NODE_RADIUS * GRID_CELL_SIZE)));
		List<Node> nodes = new ArrayList<>();

		// amount of time until we need to spawn some more nodes
		double timeToNextSpawn = 0.0;

		// nanosecond timestamp of our last frame
		long lastFrameNanos = System.nanoTime();

		// number of frames
		int numFrames = 0;

		// draw frames until we've reached our limit
		do {
			// grab current nanosecond timestamp and print out how long it took to draw our last frame
			long nowNanos = System.nanoTime();
			long frameMillis = (nowNanos - lastFrameNanos) / 1000000;
			lastFrameNanos = nowNanos;
			System.out.println(numFrames + " / " + NUM_FRAMES + " (" + frameMillis + " ms)");

			// perform configured number of simulation steps for this frame
			for (int i = 0; i < FRAME_STEPS; i++) {
				// for each node, push its neighbors around
				for (Node node : nodes) {
					node.push(grid.neighbors(node, NODE_PUSH_FORCE_POINTS[NODE_PUSH_FORCE_POINTS.length - 1]));
				}

				// for each node, apply its accumulated force and reposition in grid
				for (Node node : nodes) {
					node.move(STEP_SIZE);
					grid.put(node);
				}

				// if this is the last step, draw to our image buffer
				if (i == FRAME_STEPS - 1) {
					Graphics2D g = buffer.createGraphics();
					g.clearRect(0, 0, WIDTH, HEIGHT);
					for (Node node : nodes) {
						node.draw(g);
					}
				}

				// if its time to spawn, then do it
				if (timeToNextSpawn <= 0.0) {
					Vector c = new Vector((MAX_X + MIN_X) / 2, MIN_Y + SPAWN_RADIUS * NODE_RADIUS);
					Vector v = Vector.ZERO;
					double d = 1.0;
					double rv = rnd.nextDouble() * 2 * SPAWN_ROTATION - SPAWN_ROTATION;
					for (double r = NODE_RADIUS; r < SPAWN_RADIUS * NODE_RADIUS; r += d * NODE_RADIUS) {
						double dt = d * NODE_RADIUS / r;
						double rt = rnd.nextDouble() * 2 * Math.PI;
						for (double t = dt; t < 2 * Math.PI; t += dt) {
							Vector pos = c.add(Vector.radial(t + rt + rnd.nextDouble() * 0.2 * dt, r + rnd.nextDouble() * 0.2 * NODE_RADIUS));
							Vector toc = pos.to(c);
							Vector vel = Vector.radial(toc.angleOf() + (Math.PI / 2), toc.length() * rv).add(v);
							Node node = new Node(pos, vel);
							nodes.add(node);
							grid.put(node);
						}
					}
					timeToNextSpawn += SPAWN_FREQUENCY;
				}
				timeToNextSpawn -= STEP_SIZE;
			}
		} while ((numFrames = video.addFrame(buffer)) < NUM_FRAMES);

		// all done, finish the video and exit
		video.finish();
		System.exit(0);
	}

	/**
	 * Represents a single blob particle.
	 */
	private static class Node implements Positioned {
		private Vector acc = Vector.ZERO;
		private Vector vel;
		private Vector pos;
		private double stress = 0.0;

		/**
		 * Create a node.
		 * @param pos the position of the node.
		 * @param vel the velocity of the node
		 */
		private Node(Vector pos, Vector vel) {
			this.pos = pos;
			this.vel = vel;
		}

		/**
		 * Apply a force to this node.
		 * @param f the force vector
		 */
		private void force(Vector f) {
			acc = acc.add(f);
			stress += f.length();
		}

		/**
		 * Push ourselves around based on positions of neighboring nodes.
		 * @param neighbors our neighboring nodes
		 */
		private void push(Collection<Node> neighbors) {
			// clear accumulated stress and apply some gravitational acceleration
			stress = 0.0;
			acc = Vector.UNIT_Y.scale(GRAVITY_FORCE);

			// apply appropriate force based on position and velocity of each neighbor
			for (Node neighbor : neighbors) {
				Vector to = pos.to(neighbor.pos());
				double dist = to.length();
				int fi = 0;
				while (fi < NODE_PUSH_FORCE_POINTS.length && dist > NODE_PUSH_FORCE_POINTS[fi]) {
					fi += 1;
				}
				if (fi < NODE_PUSH_FORCE_POINTS.length) {
					force(to.scale((NODE_PUSH_FORCE_SLOPE[fi] * dist + NODE_PUSH_FORCE_OFFSET[fi]) / dist));
					force(vel.scale(-1).add(neighbor.vel).scale(FLOCK_FORCE));
				}
			}

			// apply some drag
			force(vel.scale(-DRAG_FORCE));
		}

		/**
		 * Move this node over the specified time step.
		 * @param time the amount of time to move for
		 */
		private void move(double time) {
			vel = vel.add(acc.scale(time));
			pos = pos.add(vel.scale(time));
			if (pos.x() < MIN_X) {
				pos = pos.add(Vector.UNIT_X.scale(2 * (MIN_X - pos.x())));
				vel = vel.add(Vector.UNIT_X.scale(-1.2 * vel.x()));
			}
			if (pos.x() > MAX_X) {
				pos = pos.add(Vector.UNIT_X.scale(-2 * (pos.x() - MAX_X)));
				vel = vel.add(Vector.UNIT_X.scale(-1.2 * vel.x()));
			}
			if (pos.y() < MIN_Y) {
				pos = pos.add(Vector.UNIT_Y.scale(2 * (MIN_Y - pos.y())));
				vel = vel.add(Vector.UNIT_Y.scale(-2 * vel.y()));
			}
			if (pos.y() > MAX_Y) {
				pos = pos.add(Vector.UNIT_Y.scale(-2 * (pos.y() - MAX_Y)));
				vel = vel.add(Vector.UNIT_Y.scale(-1.1 * vel.y()));
				vel = vel.add(Vector.UNIT_X.scale(-1.1 * vel.x()));
			}
		}

		/**
		 * Draw the node
		 * @param g2d the graphics object to use to draw
		 */
		private void draw(Graphics2D g2d) {
			float r = (float) Math.min(stress / RED_SCALE, 1.0);
			float g = (float) Math.min(acc.length() / GREEN_SCALE, 1.0);
			float b = (float) (BLUE_MINIMUM + (float) Math.min(vel.length() / BLUE_SCALE, 1.0 - BLUE_MINIMUM));
			g2d.setPaint(new Color(r, g, b));
			g2d.fill(new Ellipse2D.Double(pos.x() - NODE_RADIUS * (DRAW_RADIUS_FACTOR / 2.0), pos.y() - NODE_RADIUS * (DRAW_RADIUS_FACTOR / 2.0), NODE_RADIUS * DRAW_RADIUS_FACTOR, NODE_RADIUS * DRAW_RADIUS_FACTOR));
		}

		@Override
		public Vector pos() {
			return pos;
		}
	}
}