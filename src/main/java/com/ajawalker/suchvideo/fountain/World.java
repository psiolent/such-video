package com.ajawalker.suchvideo.fountain;

import com.ajawalker.suchvideo.VideoMaker;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class World {
	public static final double BODY_DRAW_RADIUS = 2.0;
	public static final int WIDTH = 1280;
	public static final int HEIGHT = 720;
	public static final int NUM_FRAMES = 24 * 120;

	public static final int NUM_BODIES = WIDTH * HEIGHT / 100;

	public static final int ANCHOR_RADIUS = 2;
	public static final int ANCHOR_MASS = ANCHOR_RADIUS * ANCHOR_RADIUS;

	// finely tuned by trial and error :)
	public static final double REPULSION_DISTANCE = 20.0;
	public static final double REPULSION_STRENGTH = 0.5;
	public static final double MAGNETISM_DISTANCE = 8.0;
	public static final double MAGNETISM_STRENGTH = 1.0;

	public static final double ACCELERATOR_RADIUS = (int) (Math.sqrt(WIDTH * HEIGHT) / 8);
	public static final double ACCELERATOR_FACTOR = 8.0;

	public static final double GRAVITY_FACTOR = 0.05;

	public static final double BODY_DRAG_FACTOR = 0.1;

	public static final double TIME_STEP = 5.0;
	public static final double MAX_MOVE = 0.1;

	// a mutex for synchronizing access to shared state
	public static final Object MUTEX = new Object();

	public static void main(String[] args) throws IOException, InterruptedException {
		VideoMaker video = new VideoMaker("target/world4.mp4", WIDTH, HEIGHT, 24);

		// create a perimeter of "anchor" bodies that will keep everything
		// contained
		final Collection<Body> anchors = new ArrayList<>();

		for (int x = 0; x <= WIDTH / (2 * ANCHOR_RADIUS * BODY_DRAW_RADIUS); x++) {
			Vector pos = new Vector((2 * x) * ANCHOR_RADIUS * BODY_DRAW_RADIUS, 0);
			Body body = new Body(pos, 0.0, ANCHOR_MASS);
			anchors.add(body);
			pos = new Vector((2 * x) * ANCHOR_RADIUS * BODY_DRAW_RADIUS, HEIGHT);
			body = new Body(pos, 0.0, ANCHOR_MASS);
			anchors.add(body);
		}
		for (int y = 1; y < HEIGHT / (2 * ANCHOR_RADIUS * BODY_DRAW_RADIUS); y++) {
			Vector pos = new Vector(0, (2 * y) * ANCHOR_RADIUS * BODY_DRAW_RADIUS);
			Body body = new Body(pos, 0.0, ANCHOR_MASS);
			anchors.add(body);
			pos = new Vector(WIDTH, (2 * y) * ANCHOR_RADIUS * BODY_DRAW_RADIUS);
			body = new Body(pos, 0.0, ANCHOR_MASS);
			anchors.add(body);
		}

		// create our normally interacting bodies
		Collection<Body> bodies = new ArrayList<>();

		Random rnd = new Random();
		while (bodies.size() < NUM_BODIES) {
			double x = rnd.nextDouble() * (WIDTH - 2 * ANCHOR_RADIUS * BODY_DRAW_RADIUS) + ANCHOR_RADIUS * BODY_DRAW_RADIUS;
			double y = rnd.nextDouble() * (HEIGHT - 2 * ANCHOR_RADIUS * BODY_DRAW_RADIUS) + ANCHOR_RADIUS * BODY_DRAW_RADIUS;
			Vector pos = new Vector(x, y);
			Vector vel = new Vector(rnd.nextDouble() * 0.2 - 0.1, rnd.nextDouble() * 0.2 - 0.1);
			double minDistance = WIDTH;
			for (Body body : bodies) {
				double distance = pos.to(body.pos()).length();
				if (distance < minDistance) {
					minDistance = distance;
				}
			}
			if (minDistance > 15) {
				double charge = (rnd.nextInt(5) + 1) * (rnd.nextBoolean() ? -1 : 1);
				double mass = (rnd.nextInt(5) + 2) * 5;
				bodies.add(new Body(pos, vel, charge, mass));
			}
		}

		// the accelerator acts locally to accelerate bodies within its radius
		// in a specific direction
		final Accelerator heat = new Accelerator(new Vector(WIDTH / 2, 0), ACCELERATOR_RADIUS, new Vector(0, ACCELERATOR_FACTOR));

		// gravity acts globally on all bodies
		final Gravity down = new Gravity(new Vector(0, -GRAVITY_FACTOR));

		BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

		// we'll parallelize as much as possible
		ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		for (int frameCount = 0; frameCount < NUM_FRAMES; frameCount++) {
			System.out.format("%d of %d%n", frameCount + 1, NUM_FRAMES);
			double timeLeft = TIME_STEP;
			while (timeLeft > 0.0) {
				// find what timestep to advance the bodies by based on how fast
				// the fastest body is travelling and ensuring that it doesn't
				// move further than our MAX_MOVE parameter
				double maxSpeed = 0.0;
				for (Body body : bodies) {
					double bodySpeed = body.speed();
					if (bodySpeed > maxSpeed) {
						maxSpeed = bodySpeed;
					}
				}
				final double timeStep = Math.min(timeLeft, MAX_MOVE / maxSpeed);

				// calculate how much time we have left in this frame
				timeLeft -= timeStep;

				// force and move all bodies, creating a new set of bodies for
				// the next step; this helps with parallelization because
				// calculating forces on bodies has to look at the positions of
				// other bodies, which would be impossible if some of them had
				// already moved
				final Collection<Body> currentBodies = new ArrayList<>(bodies);
				final Collection<Body> nextBodies = new ArrayList<>(bodies.size());
				final CountDownLatch latch = new CountDownLatch(bodies.size());

				// create a task for each body
				for (final Body body : bodies) {
					exec.execute(new Runnable() {
						@Override
						public void run() {
							// apply forces from anchors
							for (Body anchor : anchors) {
								body.force(new Repulsion(anchor, REPULSION_DISTANCE, REPULSION_STRENGTH));
							}

							// apply forces from other bodies
							for (Body other : currentBodies) {
								if (other != body) {
									body.force(new Magnetism(other, MAGNETISM_DISTANCE, MAGNETISM_STRENGTH));
									body.force(new Repulsion(other, REPULSION_DISTANCE, REPULSION_STRENGTH));
								}
							}

							// apply world forces
							body.force(new Drag(BODY_DRAG_FACTOR));
							body.force(heat);
							body.force(down);

							// move the body
							Body nextBody = body.move(timeStep);
							synchronized (MUTEX) {
								nextBodies.add(nextBody);
							}
							latch.countDown();
						}
					});
				}

				// wait for all the bodies to finish
				try {
					latch.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				synchronized (MUTEX) {
					bodies = nextBodies;
				}
			}

			// draw the frame to video
			Graphics graphics = image.getGraphics();
			graphics.clearRect(0, 0, WIDTH, HEIGHT);
			heat.draw(graphics);
			for (Body body : bodies) {
				body.draw(graphics);
			}
			for (Body anchor : anchors) {
				anchor.draw(graphics);
			}
			video.addFrame(image);
		}

		// all done
		video.finish();
		exec.shutdown();
		System.exit(0);
	}
}
