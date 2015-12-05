package com.ajawalker.suchvideo.fountain;

public class Gravity implements Force {
	private final Vector acc;

	public Gravity(Vector acc) {
		this.acc = acc;
	}

	@Override
	public Vector calc(Body body) {
		return acc.scale(body.mass());
	}
}
