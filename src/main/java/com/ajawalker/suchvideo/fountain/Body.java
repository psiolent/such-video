package com.ajawalker.suchvideo.fountain;

import java.awt.*;

public class Body {
	private final Vector pos;
	private final Vector vel;
	private Vector forceSum = new Vector(0.0, 0.0);
	private final double charge;
	private final double mass;

	public Body(Vector pos, Vector vel, double charge, double mass) {
		this.pos = pos;
		this.vel = vel;
		this.charge = charge;
		this.mass = mass;
	}

	public Body(Vector pos, double charge, double mass) {
		this(pos, new Vector(0.0, 0.0), charge, mass);
	}

	public Vector pos() {
		return pos;
	}

	public Vector vel() {
		return vel;
	}

	public double speed() {
		return vel.length();
	}

	public double charge() {
		return charge;
	}

	public double mass() {
		return mass;
	}

	public Body force(Force force) {
		forceSum = forceSum.add(force.calc(this));
		return this;
	}

	public Body move(double time) {
		Vector acc = forceSum.scale(1.0 / mass);
		Vector vel = this.vel.add(acc.scale(time));
		Vector pos = this.pos.add(vel.scale(time));
		return new Body(pos, vel, charge, mass);
	}

	public Body draw(Graphics graphics) {
		double r = 0.8 / (1.0 + Math.pow(Math.E, charge / 2.0));
		double g = 0.3;
		double b = (1.0 - r) * 0.8;
		graphics.setColor(new Color((float) r, (float) g, (float) b));
		for (double radius = World.BODY_DRAW_RADIUS * Math.sqrt(mass); radius > 1.0; radius -= 3.0) {
			graphics.drawArc(
					(int) (pos.x() - radius),
					World.HEIGHT - (int) (pos.y() + radius),
					(int) (radius * 2.0),
					(int) (radius * 2.0),
					0,
					360);
		}
		return this;
	}
}
