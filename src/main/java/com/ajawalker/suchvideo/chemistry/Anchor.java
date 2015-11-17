package com.ajawalker.suchvideo.chemistry;

public class Anchor implements Force {
	private final Vector pos;
	private final double factor;

	public Anchor(Vector pos, double factor) {
		this.pos = pos;
		this.factor = factor;
	}

	@Override
	public Vector calc(Body body) {
		return body.pos().to(pos).scale(factor);
	}
}
