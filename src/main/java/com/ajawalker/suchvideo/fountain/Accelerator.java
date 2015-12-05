package com.ajawalker.suchvideo.fountain;

import java.awt.*;

public class Accelerator implements Force {
	private final Vector pos;
	private final double radius;
	private final Vector acc;

	public Accelerator(Vector pos, double radius, Vector acc) {
		this.pos = pos;
		this.radius = radius;
		this.acc = acc;
	}

	@Override
	public Vector calc(Body body) {
		double distance = pos.to(body.pos()).length();
		if (distance < radius) {
			return acc;
		} else {
			return Vector.ZERO;
		}
	}

	public void draw(Graphics graphics) {
		graphics.setColor(new Color(80, 0, 10));
		graphics.fillArc(
				(int) (pos.x() - radius),
				World.HEIGHT - (int) (pos.y() + radius),
				(int) (radius * 2),
				(int) (radius * 2),
				0,
				360);
	}
}
