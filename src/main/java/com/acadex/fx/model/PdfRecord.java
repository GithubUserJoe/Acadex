package com.acadex.fx.model;

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PdfRecord {
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty path = new SimpleStringProperty();
    private final LongProperty size = new SimpleLongProperty();
    private final StringProperty uploadDate = new SimpleStringProperty();
    private final StringProperty type = new SimpleStringProperty();
    private final StringProperty fullPath = new SimpleStringProperty();

    public PdfRecord(long id, String name, String path, long size, String uploadDate, String type, String fullPath) {
        this.id.set(id);
        this.name.set(name);
        this.path.set(path);
        this.size.set(size);
        this.uploadDate.set(uploadDate);
        this.type.set(type);
        this.fullPath.set(fullPath);
    }

    public long getId() { return id.get(); }
    public String getName() { return name.get(); }
    public String getPath() { return path.get(); }
    public long getSize() { return size.get(); }
    public String getUploadDate() { return uploadDate.get(); }
    public String getType() { return type.get(); }
    public String getFullPath() { return fullPath.get(); }

    public LongProperty idProperty() { return id; }
    public StringProperty nameProperty() { return name; }
    public StringProperty pathProperty() { return path; }
    public LongProperty sizeProperty() { return size; }
    public StringProperty uploadDateProperty() { return uploadDate; }
    public StringProperty typeProperty() { return type; }
    public StringProperty fullPathProperty() { return fullPath; }
}
