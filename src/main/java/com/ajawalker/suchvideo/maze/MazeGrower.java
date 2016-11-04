package com.ajawalker.suchvideo.maze;

import com.ajawalker.suchvideo.VideoMaker;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.Queue;

public class MazeGrower {
	private static final Random RND = new Random();

	private static final int WIDTH = 400;
	private static final int HEIGHT = 300;
	private static final int DENSITY = 4;

	private static final int FIELD_WIDTH = WIDTH * DENSITY;
	private static final int FIELD_HEIGHT = HEIGHT * DENSITY;

	private static final double MIN_X = 0.0;
	private static final double MAX_X = WIDTH * DENSITY;
	private static final double MIN_Y = 0.0;
	private static final double MAX_Y = HEIGHT * DENSITY;

	private static final double BORDER_RADIUS = 5.0 * DENSITY;
	private static final double NEIGHBOR_RADIUS = 20.0 * DENSITY;
	private static final double CONNECTION_RADIUS = 1.0 * DENSITY;
	private static final double BRANCH_RADIUS = CONNECTION_RADIUS / 3.0;
	private static final double BRANCH_LENGTH = 3;
	private static final double GROW_RADIUS = CONNECTION_RADIUS / 1.5;

	private static final double DRAW_RADIUS = NEIGHBOR_RADIUS / 5.0;
	private static final double WALL_RADIUS = 3.0 * DENSITY;

	private static final double BOUNDARY_COEFF = 100000.0;
	private static final double NEIGHBOR_COEFF = 50000.0;
	private static final double CONNECTION_COEFF = 10000.0;
	private static final double STRAIGHTEN_COEFF = 10000.0;
	private static final double DRAG_COEFF = 40.0;

	private static final double MID_LIMIT = 40000.0;
	private static final double END_LIMIT = 20000.0;
	private static final double MID_GROW_RATE = 0.00002;
	private static final double END_GROW_RATE = 0.01;
	private static final double MID_BRANCH_RATE = 0.000014;
	private static final double END_BRANCH_RATE = 0.00007;
	private static final double RATE_MULTIPLIER = 1.01;
	private static final double RATE_REDUCER = 10.0;

	private static final double STEP_SIZE = 0.0001;
	private static final double FRAME_STEPS = 50;

	public static void main(String[] args) throws IOException, InterruptedException {
		new MazeGrower().go();
	}

	private void go() throws IOException, InterruptedException {
		VideoMaker video = new VideoMaker("target/maze-lines-small5.mp4", WIDTH, HEIGHT, 24);
		//FrameViewer viewer = new FrameViewer("maze", WIDTH, HEIGHT);
		Field field = new Field();
		BufferedImage buffer = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

		long lastFrameNanos = System.nanoTime();
		int lastGrowth = 0;
		while (lastGrowth < 8000) {
			for (int i = 0; i < FRAME_STEPS; i++) {
				if (field.advance(STEP_SIZE)) {
					lastGrowth = 0;
				} else {
					lastGrowth += 1;
				}
			}
			field.draw(buffer, true, false);
			int numFrames = video.addFrame(buffer);
			//viewer.showFrame(buffer);
			System.out.format("%f\t%f\t%d%n", (System.nanoTime() - lastFrameNanos) / 1000000000.0, numFrames / 24.0, lastGrowth);
			lastFrameNanos = System.nanoTime();
		}
		video.finish();
		System.exit(0);
	}

	private static class Field {
		private final Grid<Node> grid = new Grid<>(MIN_X, MAX_X, MIN_Y, MAX_Y, WIDTH, HEIGHT);
		private final List<Node> nodes = new ArrayList<>();

		private double rateFactor = 1.0;

		private Field() {
			Node node = new Node(new Vector((MAX_X - MIN_X) / 2.0, (MAX_Y - MIN_Y) / 2.0));
			grid.put(node);
			nodes.add(node);
		}

		private boolean advance(double time) {
			boolean grew = false;
			for (Node node : nodes) {
				node.push(grid.neighbors(node, NEIGHBOR_RADIUS));
			}
			for (Node node : nodes) {
				node.move(time);
				grid.put(node);
			}
			List<Node> newNodes = new LinkedList<>();
			for (Node node : nodes) {
				List<Double> branches = new LinkedList<>();
				double growRate = 0.0;
				if (nodes.size() < 2) {
					branches.add(RND.nextDouble() * 2.0 * Math.PI);
				} else if (node.connections().size() < 2) {
					if (node.pressure() < END_LIMIT * NEIGHBOR_COEFF && RND.nextDouble() < END_BRANCH_RATE * rateFactor) {
						branches.add(node.pressureAngle() + RND.nextGaussian() * 0.1 * Math.PI);
						branches.add(node.pressureAngle() + RND.nextGaussian() * 0.1 * Math.PI);
					} else if (!grew && node.pressure() < END_LIMIT * NEIGHBOR_COEFF) {
						growRate = END_GROW_RATE * rateFactor;
					}
				} else {
					if (node.pressure() < MID_LIMIT * NEIGHBOR_COEFF && RND.nextDouble() < MID_BRANCH_RATE * rateFactor) {
						branches.add(node.pressureAngle() + RND.nextGaussian() * 0.1 * Math.PI);
					} else if (!grew && node.pressure() < MID_LIMIT * NEIGHBOR_COEFF) {
						growRate = MID_GROW_RATE * rateFactor;
					}
				}
				for (double branchAngle : branches) {
					grew = true;
					Node rootNode = node;
					for (int j = 0; j < BRANCH_LENGTH; j++) {
						Node branchNode = new Node(rootNode.pos().add(Vector.radial(branchAngle, BRANCH_RADIUS)));
						rootNode.connect(branchNode);
						branchNode.connect(rootNode);
						newNodes.add(branchNode);
						rootNode = branchNode;
					}
				}
				for (Node connection : new LinkedList<>(node.connections())) {
					if (RND.nextDouble() < growRate && node.pos.distanceTo(connection.pos) >= GROW_RADIUS) {
						grew = true;
						Node growNode = new Node(node.pos().add(node.pos().to(connection.pos()).scale(0.5)));
						node.disconnect(connection);
						node.connect(growNode);
						growNode.connect(node);
						connection.disconnect(node);
						connection.connect(growNode);
						growNode.connect(connection);
						newNodes.add(growNode);
					}
				}
			}
			for (Node newNode : newNodes) {
				grid.put(newNode);
				nodes.add(newNode);
			}
			if (grew) {
				rateFactor -= RATE_REDUCER;
				if (rateFactor < 1.0) {
					rateFactor = 1.0;
				}
			} else {
				rateFactor *= RATE_MULTIPLIER;
			}
			return grew;
		}

		private void draw(BufferedImage image, boolean drawLines, boolean drawChannels) {
			if (drawLines) {
				Graphics g = image.getGraphics();
				g.setColor(Color.black);
				g.fillRect(0, 0, WIDTH, HEIGHT);
			}
			if (drawChannels) {
				boolean[][] field = new boolean[FIELD_WIDTH][FIELD_HEIGHT];
				Queue<Drawer> drawers = new LinkedList<>();
				for (Node node : nodes) {
					for (Node connection : node.connections()) {
						Vector to = node.pos().to(connection.pos());
						int steps = (int) to.length() + 1;
						to = to.normalize().scale(to.length() / steps);
						Vector pos = node.pos();
						for (int i = 0; i < steps; i++) {
							drawers.add(new Drawer((int) pos.x(), (int) pos.y(), pos.x(), pos.y(), true));
							pos = pos.add(to);
						}
					}
				}
				while (!drawers.isEmpty()) {
					drawers.remove().draw(field, drawers);
				}
				show(field, image);
			}
			if (drawLines) {
				for (Node node : nodes) {
					node.draw(image.getGraphics());
				}
			}
		}

		private void show(boolean[][] field, BufferedImage image) {
			for (int x = 0; x < WIDTH; x++) {
				for (int y = 0; y < HEIGHT; y++) {
					int openCount = 0;
					for (int fx = x * DENSITY; fx < (x + 1) * DENSITY; fx++) {
						for (int fy = y * DENSITY; fy < (y + 1) * DENSITY; fy++) {
							if (field[fx][fy]) {
								openCount += 1;
							}
						}
					}
					int c = (int) ((openCount * 255.0) / (DENSITY * DENSITY));
					image.setRGB(x, y, c << 16 | c << 8 | c);
				}
			}
		}
	}

	private static class Node implements Positioned {
		private final Set<Node> connections = new HashSet<>();
		private final boolean fixed;

		private Vector acc = Vector.ZERO;
		private Vector vel = Vector.ZERO;
		private Vector pos;
		private double pressure = 0.0;
		private Vector pressureVector = Vector.ZERO;

		private Node(Vector pos) {
			this(pos, false);
		}

		private Node(Vector pos, boolean fixed) {
			this.pos = pos;
			this.fixed = fixed;
		}

		private void push(Iterable<Node> neighbors) {
			if (fixed) return;
			pressure = 0.0;
			acc = vel.scale(-DRAG_COEFF);
			if (pos.x() < MIN_X + BORDER_RADIUS) {
				acc = acc.add(Vector.UNIT_X.scale(BOUNDARY_COEFF * (MIN_X + BORDER_RADIUS - pos.x())));
				pressure += Math.abs(BOUNDARY_COEFF * (MIN_X + BORDER_RADIUS - pos.x()));
			}
			if (pos.x() > MAX_X - BORDER_RADIUS) {
				acc = acc.add(Vector.UNIT_X.scale(BOUNDARY_COEFF * (MAX_X - BORDER_RADIUS - pos.x())));
				pressure += Math.abs(BOUNDARY_COEFF * (MAX_X - BORDER_RADIUS - pos.x()));
			}
			if (pos.y() < MIN_Y + BORDER_RADIUS) {
				acc = acc.add(Vector.UNIT_Y.scale(BOUNDARY_COEFF * (MIN_Y + BORDER_RADIUS - pos.y())));
				pressure += Math.abs(BOUNDARY_COEFF * (MIN_Y + BORDER_RADIUS - pos.y()));
			}
			if (pos.y() > MAX_Y - BORDER_RADIUS) {
				acc = acc.add(Vector.UNIT_Y.scale(BOUNDARY_COEFF * (MAX_Y - BORDER_RADIUS - pos.y())));
				pressure += Math.abs(BOUNDARY_COEFF * (MAX_Y - BORDER_RADIUS - pos.y()));
			}
			for (Node neighbor : neighbors) {
				if (!connections.contains(neighbor)) {
					double dist = pos.distanceTo(neighbor.pos);
					if (dist < NEIGHBOR_RADIUS) {
						double factor = -NEIGHBOR_COEFF * (1.0 - dist / NEIGHBOR_RADIUS);
						acc = acc.add(pos.to(neighbor.pos).normalize().scale(factor));
						pressure += Math.abs(-factor * factor);
					}
				}
			}
			pressureVector = acc;
			Vector avgPos = Vector.ZERO;
			for (Node connection : connections) {
				double dist = pos.distanceTo(connection.pos);
				double factor = -CONNECTION_COEFF * (1.0 - dist / CONNECTION_RADIUS);
				acc = acc.add(pos.to(connection.pos).normalize().scale(factor));
				avgPos = avgPos.add(connection.pos);
			}
			if (connections.size() >= 2) {
				avgPos = avgPos.scale(1.0 / connections.size());
				acc = acc.add(pos.to(avgPos).scale(STRAIGHTEN_COEFF));
			}
		}

		private void move(double time) {
			if (fixed) return;
			this.vel = this.vel.add(this.acc.scale(time));
			this.pos = this.pos.add(this.vel.scale(time));
		}

		private void draw(Graphics g) {
			float c1 = 1.0f;
			if (pressure < END_LIMIT * NEIGHBOR_COEFF) {
				c1 = 0.0f;
			} else if (pressure < MID_LIMIT * NEIGHBOR_COEFF) {
				c1 = 0.5f;
			}
			c1 = 1.0f;
			g.setColor(new Color(c1, c1, 1.0f));
			for (Node connection : connections) {
				g.drawLine((int) (pos.x / DENSITY), (int) (pos.y / DENSITY), (int) (connection.pos.x / DENSITY), (int) (connection.pos.y / DENSITY));
			}
		}

		private Collection<Node> connections() {
			return Collections.unmodifiableCollection(connections);
		}

		private void connect(Node connection) {
			connections.add(connection);
		}

		private void disconnect(Node connection) {
			connections.remove(connection);
		}

		private double pressure() {
			return pressure;
		}

		private double pressureAngle() {
			return pressureVector.angleOf();
		}

		@Override
		public Vector pos() {
			return pos;
		}
	}

	private static class Drawer {
		private final Cell cell;
		private final double cx;
		private final double cy;
		private final boolean core;
		private final Set<Cell> visited;

		private Drawer(Cell cell, double cx, double cy, Set<Cell> visited) {
			this.cell = cell;
			this.cx = cx;
			this.cy = cy;
			this.core = false;
			this.visited = visited;
		}

		private Drawer(int x, int y, double cx, double cy, boolean core) {
			this.cell = new Cell(x, y);
			this.cx = cx;
			this.cy = cy;
			this.core = core;
			this.visited = new HashSet<>();
		}

		private Drawer by(int xoff, int yoff) {
			return new Drawer(cell.by(xoff, yoff), cx, cy, visited);
		}

		private void draw(boolean[][] field, Queue<Drawer> drawers) {
			if (cell.x < 0 || cell.x >= FIELD_WIDTH || cell.y < 0 || cell.y >= FIELD_HEIGHT) {
				return;
			}

			if (visited.contains(cell)) {
				return;
			}
			visited.add(cell);

			double dx = cx - cell.x;
			double dy = cy - cell.y;
			if (Math.sqrt(dx * dx + dy * dy) > DRAW_RADIUS) {
				return;
			}
			int openCount = countOpen(field);
			int reachableCount = countReachable(cell, field, new HashSet<Cell>());
			if (!core && openCount > reachableCount) {
				return;
			}
			field[cell.x][cell.y] = true;
			drawers.add(by(-1, 0));
			drawers.add(by(1, 0));
			drawers.add(by(0, -1));
			drawers.add(by(0, 1));
		}

		private int countOpen(boolean[][] field) {
			int count = 0;
			for (int fx = (int) (cell.x - WALL_RADIUS); fx <= (int) (cell.x + WALL_RADIUS + 1); fx++) {
				if (fx >= 0 && fx < FIELD_WIDTH) {
					for (int fy = (int) (cell.y - WALL_RADIUS); fy <= (int) (cell.y + WALL_RADIUS + 1); fy++) {
						if (fy >= 0 && fy < FIELD_HEIGHT) {
							if (field[fx][fy] && Math.sqrt((fx - cell.x) * (fx - cell.x) + (fy - cell.y) * (fy - cell.y)) <= WALL_RADIUS) {
								count += 1;
							}
						}
					}
				}
			}
			return count;
		}

		private int countReachable(Cell cell, boolean[][] field, Set<Cell> visited) {
			if (visited.contains(cell) ||
					cell.x < 0 || cell.x >= FIELD_WIDTH || cell.y < 0 || cell.y >= FIELD_HEIGHT ||
					((cell.x != this.cell.x || cell.y != this.cell.y) && !field[cell.x][cell.y]) ||
					Math.sqrt((cell.x - this.cell.x) * (cell.x - this.cell.x) + (cell.y - this.cell.y) * (cell.y - this.cell.y)) > WALL_RADIUS) {
				return 0;
			}
			visited.add(cell);
			return (field[cell.x][cell.y] ? 1 : 0) +
					countReachable(cell.by(-1, 0), field, visited) +
					countReachable(cell.by(1, 0), field, visited) +
					countReachable(cell.by(0, -1), field, visited) +
					countReachable(cell.by(0, 1), field, visited) +
					countReachable(cell.by(-1, 1), field, visited) +
					countReachable(cell.by(1, 1), field, visited) +
					countReachable(cell.by(-1, -1), field, visited) +
					countReachable(cell.by(1, -1), field, visited);
		}

		private static class Cell {
			private final int x;
			private final int y;

			private Cell(int x, int y) {
				this.x = x;
				this.y = y;
			}

			private Cell by(int xoff, int yoff) {
				return new Cell(x + xoff, y + yoff);
			}

			@Override
			public boolean equals(Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;
				Cell cell = (Cell) o;
				return x == cell.x && y == cell.y;
			}

			@Override
			public int hashCode() {
				int result = x;
				result = 31 * result + y;
				return result;
			}
		}
	}

	private static class Grid<T extends Positioned> {
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

		private Grid(double minx, double maxx, double miny, double maxy, int resx, int resy) {
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

		private boolean put(T item) {
			boolean contained = remove(item);
			Set<T> cell = grid.get(ndx(ndxx(item.pos().x()), ndxy(item.pos().y())));
			cell.add(item);
			index.put(item, cell);
			return !contained;
		}

		private boolean remove(T item) {
			Set<T> cell = index.get(item);
			if (cell == null) {
				return false;
			} else {
				cell.remove(item);
				index.remove(item);
				return true;
			}
		}

		private Collection<T> neighbors(T item, double radius) {
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

	private static class Vector {
		private final double x;
		private final double y;

		private Vector(double x, double y) {
			this.x = x;
			this.y = y;
		}

		private static Vector radial(double angle, double length) {
			return new Vector(Math.cos(angle) * length, Math.sin(angle) * length);
		}

		private double x() {
			return x;
		}

		private double y() {
			return y;
		}

		private double length() {
			return Math.sqrt(x * x + y * y);
		}

		private double distanceToSqr(Vector other) {
			double xd = x - other.x;
			double yd = y - other.y;
			return xd * xd + yd * yd;
		}

		private double distanceTo(Vector other) {
			double xd = x - other.x;
			double yd = y - other.y;
			return Math.sqrt(xd * xd + yd * yd);
		}

		private double angleOf() {
			double t = Math.atan2(y, x);
			if (t < 0.0) {
				t += 2.0 * Math.PI;
			}
			return t;
		}

		private Vector normalize() {
			double length = this.length();
			if (length == 0.0) {
				return Vector.UNIT_X;
			}
			return new Vector(x / length, y / length);
		}

		private Vector scale(double magnitude) {
			return new Vector(x * magnitude, y * magnitude);
		}

		private Vector add(Vector other) {
			return new Vector(x + other.x, y + other.y);
		}

		private Vector to(Vector other) {
			return new Vector(other.x - x, other.y - y);
		}

		@Override
		public String toString() {
			return "{x=" + x + ", y=" + y + "}";
		}

		private static final Vector ZERO = new Vector(0.0, 0.0);
		private static final Vector UNIT_X = new Vector(1.0, 0.0);
		private static final Vector UNIT_Y = new Vector(0.0, 1.0);
	}

	private interface Positioned {
		Vector pos();
	}
}