package org.example.lab8javafx;

public class KnightMove {
    private final int x, y, depth, branchIndex;
    private final KnightMove parent;

    public KnightMove(int x, int y, int depth, int branchIndex, KnightMove parent) {
        this.x = x;
        this.y = y;
        this.depth = depth;
        this.branchIndex = branchIndex;
        this.parent = parent;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getDepth() { return depth; }
    public int getBranchIndex() { return branchIndex; }
    public KnightMove getParent() { return parent; }

    @Override
    public String toString() {
        return "Ход: (" + x + ", " + y + "), Глубина: " + depth;
    }
}