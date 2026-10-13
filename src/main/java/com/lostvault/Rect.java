package com.lostvault;

/** Small immutable axis-aligned rectangle used by collision and trigger logic. */
public record Rect(double x, double y, double w, double h) {
    public boolean intersects(Rect other) {
        return x < other.x + other.w && x + w > other.x && y < other.y + other.h && y + h > other.y;
    }
    public double right() { return x + w; }
    public double bottom() { return y + h; }
}
