package com.ajawalker.suchvideo.maze;

import java.util.*;

/**
 * A 2-dimensional index of positioned things. The index is implemented by naively separating space
 * into equal sized cells and putting things in the cell corresponding to their position.
 * @param <T> the type of thing which will be indexed in this grid
 */
public class Grid<T extends Positioned> {
	private final ArrayList<Set<T>> grid;
	private final Map<T, Set<T>> index;
	private final double minx;
	private final double maxx;
	private final double miny;
	private final double maxy;
	private final int resx;
	private final int resy;
	private final double sizex;
	private final double sizey;

	/**
	 * Constructs a grid using the provided extents. Any indexed items outside the extents will be placed
	 * in the same cell, thus lookups of those items will not be optimized.
	 * @param minx minimum expected x coordinate of grid space
	 * @param maxx maximum expected x coordinate of grid space
	 * @param miny minimum expected y coordinate of grid space
	 * @param maxy maximum expected y coordinate of grid space
	 * @param resx how many cells to separate x-axis into; more cells require more memory but lookups are faster
	 * @param resy how many cells to separate y-axis into; more cells require more memory but lookups are faster
	 */
	public Grid(double minx, double maxx, double miny, double maxy, int resx, int resy) {
		this.minx = minx;
		this.maxx = maxx;
		this.miny = miny;
		this.maxy = maxy;
		this.resx = resx;
		this.resy = resy;
		this.sizex = (maxx - minx) / resx;
		this.sizey = (maxy - miny) / resy;
		grid = new ArrayList<>((resx + 2) * (resy + 2));
		index = new HashMap<>();
		for (int i = 0; i < (resx + 2) * (resy + 2); i++) {
			grid.add(new HashSet<T>());
		}
	}

	/**
	 * Puts the item into the grid.
	 * @param item the item to put into the grid
	 * @return true if the item was not already in the grid, false if it was
	 */
	public boolean put(T item) {
		boolean contained = remove(item);
		Set<T> cell = grid.get(ndx(ndxx(item.pos().x()), ndxy(item.pos().y())));
		cell.add(item);
		index.put(item, cell);
		return !contained;
	}

	/**
	 * Removes the item from the grid.
	 * @param item the item to remove from the grid
	 * @return true if the item was in the grid, false if it was not
	 */
	public boolean remove(T item) {
		Set<T> cell = index.get(item);
		if (cell == null) {
			return false;
		} else {
			cell.remove(item);
			index.remove(item);
			return true;
		}
	}

	/**
	 * Returns all neighbors which are within the specified radius of the provided item.
	 * @param item the item whose neighbors to find
	 * @param radius the radius within which to find neighbors
	 * @return a collection of neighbors within the specified radius
	 */
	public Collection<T> neighbors(T item, double radius) {
		double x = item.pos().x();
		double y = item.pos().y();
		int ndxxmin = ndxx(x - radius);
		int ndxxmax = ndxx(x + radius);
		int ndxymin = ndxy(y - radius);
		int ndxymax = ndxy(y + radius);
		double rsqr = radius * radius;
		List<T> neighbors = new ArrayList<>();
		for (int ndxx = ndxxmin; ndxx <= ndxxmax; ndxx++) {
			for (int ndxy = ndxymin; ndxy <= ndxymax; ndxy++) {
				for (T neighbor : grid.get(ndx(ndxx, ndxy))) {
					if (neighbor != item && item.pos().distanceToSqr(neighbor.pos()) <= rsqr) {
						neighbors.add(neighbor);
					}
				}
			}
		}
		return Collections.unmodifiableList(neighbors);
	}

	/**
	 * Calculates the index x coordinate from the provided space x coordinate.
	 * @param x the space x coordinate
	 * @return the corresponding index x coordinate
	 */
	private int ndxx(double x) {
		if (x < minx) {
			return 0;
		}
		if (x >= maxx) {
			return resx + 1;
		}
		return (int) ((x - minx) / sizex) + 1;
	}

	/**
	 * Calculates the index y coordinate from the provided space y coordinate.
	 * @param y the space y coordinate
	 * @return the corresponding index y coordinate
	 */
	private int ndxy(double y) {
		if (y < miny) {
			return 0;
		}
		if (y >= maxy) {
			return resy + 1;
		}
		return (int) ((y - miny) / sizey) + 1;
	}

	/**
	 * Calculates the index absolute coordinate from the provided index x and y coordinates
	 * @param ndxx the index x coordinate
	 * @param ndxy the index y coordinate
	 * @return the corresponding index absolute coordinate
	 */
	private int ndx(int ndxx, int ndxy) {
		return ndxx * (resy + 2) + ndxy;
	}
}
