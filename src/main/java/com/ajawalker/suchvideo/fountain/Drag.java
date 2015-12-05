package com.ajawalker.suchvideo.fountain;

public class Drag implements Force {
	private final double factor;

	public Drag(double factor) {
		this.factor = factor;
	}

	@Override
	public Vector calc(Body body) {
		return body.vel().scale(-factor);
	}
}
