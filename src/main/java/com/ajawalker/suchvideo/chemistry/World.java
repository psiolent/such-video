package com.ajawalker.suchvideo.chemistry;

import com.ajawalker.suchvideo.FrameViewer;
import com.ajawalker.suchvideo.VideoMaker;

import java.awt.*;
import java.awt.image.BufferedImage;
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
	public static final int NUM_FRAMES = 24 * 600;

	public static final int NUM_BODIES = 1000;

	public static final int ANCHOR_RADIUS = 2;
	public static final int ANCHOR_MASS = ANCHOR_RADIUS * ANCHOR_RADIUS;
	public static final double ANCHOR_HOLD_FACTOR = 10.0;
	public static final double ANCHOR_DRAG_FACTOR = -0.9;

	public static final double REPULSION_DISTANCE = 5.0;
	public static final double REPULSION_STRENGTH = 200.0;
	public static final double MAGNETISM_DISTANCE = 50.0;
	public static final double MAGNETISM_STRENGTH = 0.002;

	public static final double TIME_STEP = 1.0;
	public static final double MAX_MOVE = 0.01;

	public static void main(String[] args) {
		//FrameViewer viewer = new FrameViewer("World", WIDTH, HEIGHT);
		VideoMaker video = new VideoMaker("target/world.mp4", WIDTH, HEIGHT, 24);

		Collection<Body> bodies = new ArrayList<>();


		for (int x = 0; x <= WIDTH / (2 * ANCHOR_RADIUS * BODY_DRAW_RADIUS); x++) {
			Vector pos = new Vector((2 * x) * ANCHOR_RADIUS * BODY_DRAW_RADIUS, 0);
			Body body = new Body(pos, 0.0, ANCHOR_MASS);
			body.addForce(new Anchor(pos, ANCHOR_HOLD_FACTOR));
			body.addForce(new Drag(ANCHOR_DRAG_FACTOR));
			bodies.add(body);
			pos = new Vector((2 * x) * ANCHOR_RADIUS * BODY_DRAW_RADIUS, HEIGHT);
			body = new Body(pos, 0.0, ANCHOR_MASS);
			body.addForce(new Anchor(pos, ANCHOR_HOLD_FACTOR));
			body.addForce(new Drag(ANCHOR_DRAG_FACTOR));
			bodies.add(body);
		}
		for (int y = 1; y < HEIGHT / (2 * ANCHOR_RADIUS * BODY_DRAW_RADIUS); y++) {
			Vector pos = new Vector(0, (2 * y) * ANCHOR_RADIUS * BODY_DRAW_RADIUS);
			Body body = new Body(pos, 0.0, ANCHOR_MASS);
			body.addForce(new Anchor(pos, ANCHOR_HOLD_FACTOR));
			body.addForce(new Drag(ANCHOR_DRAG_FACTOR));
			bodies.add(body);
			pos = new Vector(WIDTH, (2 * y) * ANCHOR_RADIUS * BODY_DRAW_RADIUS);
			body = new Body(pos, 0.0, ANCHOR_MASS);
			body.addForce(new Anchor(pos, ANCHOR_HOLD_FACTOR));
			body.addForce(new Drag(ANCHOR_DRAG_FACTOR));
			bodies.add(body);
		}

		int numAnchors = bodies.size();

		Random rnd = new Random();
		while (bodies.size() - numAnchors < NUM_BODIES) {
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
			if (minDistance > 20) {
				double charge = (rnd.nextInt(3) + 1) * 3 * (rnd.nextBoolean() ? -1 : 1);
				double mass = (rnd.nextInt(5) + 2) * 5;
				bodies.add(new Body(pos, vel, charge, mass));
			}
		}

		final Accelerator accelerator = new Accelerator(new Vector(WIDTH / 2, HEIGHT / 2), 100, 0.001);

		BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

		ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		for (int frameCount = 0; frameCount < NUM_FRAMES; frameCount++) {
			System.out.format("%d of %d%n", frameCount, NUM_FRAMES);
			double timeLeft = TIME_STEP;
			while (timeLeft > 0.0) {
				double maxSpeed = 0.0;
				for (Body body : bodies) {
					double bodySpeed = body.speed();
					if (bodySpeed > maxSpeed) {
						maxSpeed = bodySpeed;
					}
				}
				final double timeStep = Math.min(timeLeft, MAX_MOVE / maxSpeed);
				timeLeft -= timeStep;
				final Collection<Body> currentBodies = new ArrayList<>(bodies);
				final Collection<Body> nextBodies = new ArrayList<>(bodies.size());
				final CountDownLatch latch = new CountDownLatch(bodies.size());
				for (final Body body : bodies) {
					exec.execute(new Runnable() {
						@Override
						public void run() {
							body.force();
							for (Body other : currentBodies) {
								if (other != body) {
									body.force(new Magnetism(other, MAGNETISM_DISTANCE, MAGNETISM_STRENGTH));
									body.force(new Repulsion(other, REPULSION_DISTANCE, REPULSION_STRENGTH));
									body.force(new Drag(-0.00001));
									body.force(accelerator);
								}
							}
							Body nextBody = body.move(timeStep);
							synchronized (nextBodies) {
								nextBodies.add(nextBody);
							}
							latch.countDown();
						}
					});
				}
				try {
					latch.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				//viewer.showFrame(image);
				synchronized (nextBodies) {
					bodies = nextBodies;
				}
			}
			Graphics graphics = image.getGraphics();
			graphics.clearRect(0, 0, WIDTH, HEIGHT);
			accelerator.draw(graphics);
			for (Body body : bodies) {
				body.draw(graphics);
			}
			video.addFrame(image);
		}

		video.finish();
		exec.shutdown();
	}

}
