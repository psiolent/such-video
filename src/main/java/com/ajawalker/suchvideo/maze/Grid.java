package com.ajawalker.suchvideo.maze;

import java.util.*;

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

	public boolean put(T item) {
		boolean contained = remove(item);
		Set<T> cell = grid.get(ndx(ndxx(item.pos().x()), ndxy(item.pos().y())));
		cell.add(item);
		index.put(item, cell);
		return !contained;
	}

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

	private int ndxx(double x) {
		if (x < minx) {
			return 0;
		}
		if (x >= maxx) {
			return resx + 1;
		}
		return (int) ((x - minx) / sizex) + 1;
	}

	private int ndxy(double y) {
		if (y < miny) {
			return 0;
		}
		if (y >= maxy) {
			return resy + 1;
		}
		return (int) ((y - miny) / sizey) + 1;
	}

	private int ndx(int ndxx, int ndxy) {
		return ndxx * (resy + 2) + ndxy;
	}
}
