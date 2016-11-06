package com.ajawalker.delaunay;

/**
 * An intermediate data structure used in building the edges between regions in a Voronoi
 * diagram.
 * <p>
 * Adapted from Steven Benoit's library here:
 * <p>
 * http://www.math.colostate.edu/~benoit/Java/math/delaunay/delaunay.html
 */
class VorEdge {

	/**
	 * 'a' parameter in ax + by = c equation of the line
	 */
	public double aParam = 0;

	/**
	 * 'b' parameter in ax + by = c equation of the line
	 */
	public double bParam = 0;

	/**
	 * 'c' parameter in ax + by = c equation of the line
	 */
	public double cParam = 0;

	/**
	 * the end points of this edge
	 */
	public final Site[] endPoints;

	/**
	 * the sites this edge is the bisector of
	 */
	public final Site[] reg;

	/**
	 * the edge number
	 */
	public int edgeNumber;

	/**
	 * Constructs a new <code>VorEdge</code>.
	 */
	VorEdge() {

		this.endPoints = new Site[2];
		this.reg = new Site[2];
	}
}
