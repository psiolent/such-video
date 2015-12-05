package com.ajawalker.suchvideo.fountain;

public class Vector {
	private final double x;
	private final double y;

	public Vector(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double x() {
		return x;
	}

	public double y() {
		return y;
	}

	public double length() {
		return Math.sqrt(x * x + y * y);
	}

	public Vector normalize() {
		double length = this.length();
		return new Vector(x / length, y / length);
	}

	public Vector scale(double magnitude) {
		return new Vector(x * magnitude, y * magnitude);
	}

	public Vector add(Vector other) {
		return new Vector(x + other.x, y + other.y);
	}

	public Vector to(Vector other) {
		return new Vector(other.x - x, other.y - y);
	}

	public static final Vector ZERO = new Vector(0.0, 0.0);
}
