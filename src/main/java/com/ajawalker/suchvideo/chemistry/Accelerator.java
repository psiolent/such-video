package com.ajawalker.suchvideo.chemistry;

import java.awt.*;

public class Accelerator implements Force {
	private final Vector pos;
	private final double radius;
	private final double factor;

	public Accelerator(Vector pos, double radius, double factor) {
		this.pos = pos;
		this.radius = radius;
		this.factor = factor;
	}

	@Override
	public Vector calc(Body body) {
		double distance = pos.to(body.pos()).length();
		double accelFactor = 0.0;
		if (distance < radius) {
			accelFactor = factor * (radius - distance) / radius;
		}
		return body.vel().scale(accelFactor);
	}

	public void draw(Graphics graphics) {
		graphics.setColor(new Color(100, 0, 100));
		graphics.drawArc((int)(pos.x() - radius), (int)(pos.y() - radius), (int)(radius * 2), (int)(radius * 2), 0, 360);
	}
}
