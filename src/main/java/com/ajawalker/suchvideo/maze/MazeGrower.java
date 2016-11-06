package com.ajawalker.suchvideo.maze;

import com.ajawalker.delaunay.GraphEdge;
import com.ajawalker.delaunay.Vertex;
import com.ajawalker.delaunay.Voronoi;
import com.ajawalker.suchvideo.VideoMaker;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class MazeGrower {
	private static final int WIDTH = 1280;
	private static final int HEIGHT = 720;

	private static final double MIN_X = 0.0;
	private static final double MAX_X = WIDTH;
	private static final double MIN_Y = 0.0;
	private static final double MAX_Y = HEIGHT;

	private static final double NEIGHBOR_RADIUS = 20.0;
	private static final double CONNECTION_RADIUS = 1.0;
	private static final double BRANCH_RADIUS = CONNECTION_RADIUS / 3.0;
	private static final double BRANCH_LENGTH = 3;
	private static final double GROW_RADIUS = CONNECTION_RADIUS / 1.5;

	private static final int ADJACENT_LIMIT = 4;

	private static final double LINE_WIDTH = 15.0;
	private static final double WALL_WIDTH = 5.0;

	private static final double BOUNDARY_COEFF = 1000000.0;
	private static final double NEIGHBOR_COEFF = 50000.0;
	private static final double CONNECTION_COEFF = 10000.0;
	private static final double STRAIGHTEN_COEFF = 10000.0;
	private static final double DRAG_COEFF = 40.0;

	private static final double MID_LIMIT = 40000.0;
	private static final double END_LIMIT = 20000.0;
	private static final double MID_GROW_RATE = 0.00002;
	private static final double END_GROW_RATE = 0.01;
	private static final double MID_BRANCH_RATE = 0.00002;
	private static final double END_BRANCH_RATE = 0.0001;
	private static final double RATE_MULTIPLIER = 1.01;
	private static final double RATE_REDUCER = 5.0;

	private static final double STEP_SIZE = 0.0001;
	private static final double FRAME_STEPS = 50;
	private static final int FRAMES_PER_SECOND = 24;

	private static final String OUTPUT_FILE = "target/maze.mp4";

	public static void main(String[] args) throws IOException, InterruptedException {
		new MazeGrower().go();
	}

	private void go() throws IOException, InterruptedException {
		VideoMaker video = new VideoMaker(OUTPUT_FILE, WIDTH, HEIGHT, FRAMES_PER_SECOND);
		Field field = new Field();
		BufferedImage buffer = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

		int lastGrowth = 0;
		while (lastGrowth < 8000) {
			for (int i = 0; i < FRAME_STEPS; i++) {
				if (field.advance(STEP_SIZE)) {
					lastGrowth = 0;
				} else {
					lastGrowth += 1;
				}
			}
			field.draw(buffer);
			video.addFrame(buffer);
		}
		video.finish();
		System.exit(0);
	}

	private static class Field {
		private static final Random RND = new Random();

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
						rootNode.updateAdjacents();
						branchNode.updateAdjacents();
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
						node.updateAdjacents();
						connection.updateAdjacents();
						growNode.updateAdjacents();
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

		private void draw(BufferedImage image) {
			Graphics2D g = image.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g.setPaint(Color.black);
			g.fillRect(0, 0, WIDTH, HEIGHT);
			g.setPaint(Color.white);
			g.setStroke(new BasicStroke((float) LINE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			for (Node node : nodes) {
				node.draw(g);
			}
			g.setPaint(Color.black);
			g.setStroke(new BasicStroke((float) WALL_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			Node[] nodeArray = nodes.toArray(new Node[0]);
			Vertex[] pointArray = new Vertex[nodeArray.length];
			for (int i = 0; i < nodeArray.length; i++) {
				pointArray[i] = new Vertex(nodeArray[i].pos().x(), nodeArray[i].pos().y());
			}
			Voronoi vor = new Voronoi(0.0);
			List<GraphEdge> edges = vor.generateVoronoi(pointArray);
			for (GraphEdge edge : edges) {
				if (!nodeArray[edge.site1].isAdjacent((nodeArray[edge.site2]))) {
					double xd = edge.xPos1 - edge.xPos2;
					double yd = edge.yPos1 - edge.yPos2;
					double d2 = xd * xd + yd * yd;
					if (d2 > 0.0001) {
						g.draw(new Line2D.Double(edge.xPos1, edge.yPos1, edge.xPos2, edge.yPos2));
					}
				}
			}
		}
	}

	private static class Node implements Positioned {
		private final Set<Node> connections = new HashSet<>();
		private final Set<Node> adjacents = new HashSet<>();
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
			if (pos.x() < MIN_X) {
				acc = acc.add(Vector.UNIT_X.scale(BOUNDARY_COEFF * (MIN_X - pos.x())));
				pressure += Math.abs(BOUNDARY_COEFF * (MIN_X - pos.x()));
			}
			if (pos.x() > MAX_X) {
				acc = acc.add(Vector.UNIT_X.scale(BOUNDARY_COEFF * (MAX_X - pos.x())));
				pressure += Math.abs(BOUNDARY_COEFF * (MAX_X - pos.x()));
			}
			if (pos.y() < MIN_Y) {
				acc = acc.add(Vector.UNIT_Y.scale(BOUNDARY_COEFF * (MIN_Y - pos.y())));
				pressure += Math.abs(BOUNDARY_COEFF * (MIN_Y - pos.y()));
			}
			if (pos.y() > MAX_Y ) {
				acc = acc.add(Vector.UNIT_Y.scale(BOUNDARY_COEFF * (MAX_Y - pos.y())));
				pressure += Math.abs(BOUNDARY_COEFF * (MAX_Y - pos.y()));
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

		private void draw(Graphics2D g) {
			for (Node connection : connections) {
				g.draw(new Line2D.Double(pos.x(), pos.y(), connection.pos.x(), connection.pos.y()));
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

		private void updateAdjacents() {
			Stack<Node> chain = new Stack<>();
			chain.add(this);
			for (Node connection : connections) {
				connection.setAdjacents(chain);
			}
		}

		private boolean isAdjacent(Node other) {
			return adjacents.contains(other);
		}

		private void setAdjacents(Stack<Node> chain) {
			if (chain.size() > ADJACENT_LIMIT + 1) {
				return;
			}
			int proximity = chain.size();
			for (Node adjacent : chain) {
				if (proximity <= ADJACENT_LIMIT) {
					addAdjacent(adjacent);
					adjacent.addAdjacent(this);
				} else {
					removeAdjacent(adjacent);
					adjacent.removeAdjacent(this);
				}
				proximity -= 1;
			}
			Node latest = chain.peek();
			chain.push(this);
			for (Node connection : connections) {
				if (connection != latest) {
					connection.setAdjacents(chain);
				}
			}
			chain.pop();
		}

		private void addAdjacent(Node other) {
			adjacents.add(other);
		}

		private void removeAdjacent(Node other) {
			adjacents.remove(other);
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
}