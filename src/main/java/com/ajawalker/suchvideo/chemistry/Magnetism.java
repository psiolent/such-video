package com.ajawalker.suchvideo.chemistry;

public class Magnetism implements Force {
	private final Body other;
	private final double distance;
	private final double strength;

	public Magnetism(Body other, double distance, double strength) {
		this.other = other;
		this.distance = distance;
		this.strength = strength;
	}

	@Override
	public Vector calc(Body body) {
		Vector to = body.pos().to(other.pos());
		double scale = -strength * body.charge() * other.charge() / Math.pow(to.length() / distance, 3.0);
		return to.normalize().scale(scale);
	}
}
