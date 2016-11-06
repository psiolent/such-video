package com.ajawalker.delaunay;

/**
 * An edge in the Voronoi graph.
 * <p>
 * Adapted from Steven Benoit's library here:
 * <p>
 * http://www.math.colostate.edu/~benoit/Java/math/delaunay/delaunay.html
 */
public class GraphEdge {

	/**
	 * The start X coordinate
	 */
	public double xPos1;

	/**
	 * The start Y coordinate
	 */
	public double yPos1;

	/**
	 * The end X coordinate
	 */
	public double xPos2;

	/**
	 * The endY coordinate
	 */
	public double yPos2;

	/**
	 * the index of the site to the left of the edge
	 */
	public int site1;

	/**
	 * the index of the site to the right of the edge
	 */
	public int site2;
}
