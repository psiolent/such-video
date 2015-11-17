package com.ajawalker.suchvideo.chemistry;

import java.awt.*;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

public class Body {
    private final Vector pos;
    private final Vector vel;
    private Vector forceSum = new Vector(0.0, 0.0);
    private final double charge;
    private final double mass;

    private final Collection<Force> forces = new CopyOnWriteArrayList<>();

    public Body(Vector pos, Vector vel, double charge, double mass) {
        this.pos = pos;
        this.vel = vel;
        this.charge = charge;
        this.mass = mass;
    }

    public Body(Vector pos, double charge, double mass) {
        this(pos, new Vector(0.0, 0.0), charge, mass);
    }

    public Body addForce(Force force) {
        forces.add(force);
        return this;
    }

    public Body addForces(Collection<Force> forces) {
        this.forces.addAll(forces);
        return this;
    }

    public Vector pos() {
        return pos;
    }

    public Vector vel() {
        return vel;
    }

    public double speed() {
        return vel.length();
    }

    public double charge() {
        return charge;
    }

    public double mass() {
        return mass;
    }

    public Body force() {
        for (Force force : forces) {
            force(force);
        }
        return this;
    }

    public Body force(Force force) {
        forceSum = forceSum.add(force.calc(this));
        return this;
    }

    public Body move(double time) {
        Vector acc = forceSum.scale(1.0 / mass);
        Vector vel = this.vel.add(acc.scale(time));
        Vector pos = this.pos.add(vel.scale(time));
        return new Body(pos, vel, charge, mass).addForces(forces);
    }

    public Body draw(Graphics graphics) {
        double r = 1.0 / (1.0 + Math.pow(Math.E, charge / 8.0));
        double g = 0.5;
        double b = 1.0 - r;
        graphics.setColor(new Color((float) r, (float) g, (float) b));
        double radius = World.BODY_DRAW_RADIUS * Math.sqrt(mass);
        graphics.drawArc((int) (pos.x() - radius), World.HEIGHT - (int) (pos.y() + radius), (int) (radius * 2.0), (int) (radius * 2.0), 0, 360);
        return this;
    }
}
