package com.ajawalker.suchvideo.chemistry;

public class Gravity implements Force {
    private final Body other;
    private final double factor;

    public Gravity(Body other, double factor) {
        this.other = other;
        this.factor = factor;
    }

    @Override
    public Vector calc(Body body) {
        Vector to = body.pos().to(other.pos());
        double strength = factor * body.mass() * other.mass() / to.lengthSquared();
        return to.normalize().scale(strength);
    }
}
