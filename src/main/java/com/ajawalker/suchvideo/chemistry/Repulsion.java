package com.ajawalker.suchvideo.chemistry;

public class Repulsion implements Force {
    private final Body other;
	private final double distance;
	private final double strength;

    public Repulsion(Body other, double distance, double strength) {
        this.other = other;
		this.distance = distance;
		this.strength = strength;
    }

    @Override
    public Vector calc(Body body) {
        Vector to = body.pos().to(other.pos());
        double scale = -strength / Math.pow(to.length() / distance, 4.0);
        return to.normalize().scale(scale);
    }
}
