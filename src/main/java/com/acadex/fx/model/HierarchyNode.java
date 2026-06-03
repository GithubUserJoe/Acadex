package com.acadex.fx.model;

public class HierarchyNode {
    private final String level;
    private final long id;
    private final String name;

    public HierarchyNode(String level, long id, String name) {
        this.level = level;
        this.id = id;
        this.name = name;
    }

    public String getLevel() { return level; }
    public long getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return name;
    }
}
