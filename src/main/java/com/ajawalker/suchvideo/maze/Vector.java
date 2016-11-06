package com.ajawalker.suchvideo.maze;

public class Vector {
	private final double x;
	private final double y;

	public Vector(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public static Vector radial(double angle, double length) {
		return new Vector(Math.cos(angle) * length, Math.sin(angle) * length);
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

	public double distanceToSqr(Vector other) {
		double xd = x - other.x;
		double yd = y - other.y;
		return xd * xd + yd * yd;
	}

	public double distanceTo(Vector other) {
		double xd = x - other.x;
		double yd = y - other.y;
		return Math.sqrt(xd * xd + yd * yd);
	}

	public double angleOf() {
		double t = Math.atan2(y, x);
		if (t < 0.0) {
			t += 2.0 * Math.PI;
		}
		return t;
	}

	public Vector normalize() {
		double length = this.length();
		if (length == 0.0) {
			return Vector.UNIT_X;
		}
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

	@Override
	public String toString() {
		return "{x=" + x + ", y=" + y + "}";
	}

	public static final Vector ZERO = new Vector(0.0, 0.0);
	public static final Vector UNIT_X = new Vector(1.0, 0.0);
	public static final Vector UNIT_Y = new Vector(0.0, 1.0);
}
