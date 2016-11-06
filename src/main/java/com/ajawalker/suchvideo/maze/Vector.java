package com.ajawalker.suchvideo.maze;

/**
 * An immutable 2 dimensional vector.
 */
public class Vector {
	private final double x;
	private final double y;

	/**
	 * Creates a new vector with the provided coordinates.
	 * @param x the x value
	 * @param y the y value
	 */
	public Vector(double x, double y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Creates a new vector from the provided radial coordinates.
	 * @param angle the angle in radians
	 * @param length the length of the vector
	 * @return the new vector
	 */
	public static Vector radial(double angle, double length) {
		return new Vector(Math.cos(angle) * length, Math.sin(angle) * length);
	}

	/**
	 * Returns the x coordinate of this vector.
	 */
	public double x() {
		return x;
	}

	/**
	 * Returns the y coordinate of this vector.
	 */
	public double y() {
		return y;
	}

	/**
	 * Returns the length of this vector.
	 */
	public double length() {
		return Math.sqrt(x * x + y * y);
	}

	/**
	 * Returns the square of the distance from this vector's coordinates to another vector's coordinates.
	 * @param other the other vector to calculate the square of the distance to
	 */
	public double distanceToSqr(Vector other) {
		double xd = x - other.x;
		double yd = y - other.y;
		return xd * xd + yd * yd;
	}

	/**
	 * Returns the distance from this vector's coordinates to another vector's coordinates.
	 * @param other the other vector to calculate the distance to
	 */
	public double distanceTo(Vector other) {
		double xd = x - other.x;
		double yd = y - other.y;
		return Math.sqrt(xd * xd + yd * yd);
	}

	/**
	 * Returns the angle of this vector in radians. An angle of zero corresponds to a vector pointing directly
	 * along the x-axis and an angle of PI / 2 corresponds to a vector pointing directly along the y-axis.
	 */
	public double angleOf() {
		double t = Math.atan2(y, x);
		if (t < 0.0) {
			t += 2.0 * Math.PI;
		}
		return t;
	}

	/**
	 * Returns a new vector pointing in the same direction as this vector with a length of 1.
	 */
	public Vector normalize() {
		double length = this.length();
		if (length == 0.0) {
			return Vector.UNIT_X;
		}
		return new Vector(x / length, y / length);
	}

	/**
	 * Returns a new vector which is this vector scaled by the provided magnitude.
	 * @param magnitude how much to scale the vector by
	 */
	public Vector scale(double magnitude) {
		return new Vector(x * magnitude, y * magnitude);
	}

	/**
	 * Returns a new vector which is the result of adding the provided vector to this vector.
	 * @param other the vector to add to this vector
	 */
	public Vector add(Vector other) {
		return new Vector(x + other.x, y + other.y);
	}

	/**
	 * Returns a new vector which is the vector that points from this vector's coordinates to another
	 * vector's coordinates.
	 * @param other the vector whose coordinates to point to
	 */
	public Vector to(Vector other) {
		return new Vector(other.x - x, other.y - y);
	}

	@Override
	public String toString() {
		return "{x=" + x + ", y=" + y + "}";
	}

	/**
	 * The zero vector, {0, 0}.
	 */
	public static final Vector ZERO = new Vector(0.0, 0.0);

	/**
	 * The unit vector along the x-axis, {1, 0}.
	 */
	public static final Vector UNIT_X = new Vector(1.0, 0.0);

	/**
	 * The unit vector along the y-axis, {0, 1}.
	 */
	public static final Vector UNIT_Y = new Vector(0.0, 1.0);
}
