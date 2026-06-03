package com.acadex.fx.model;

public class OptionItem {
    private final long id;
    private final String name;

    public OptionItem(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof OptionItem)) return false;
        OptionItem item = (OptionItem) other;
        return id == item.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
