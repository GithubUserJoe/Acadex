package com.acadex.fx.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private final Path dbPath;

    public Database() {
        this.dbPath = Paths.get("data", "acadex.sqlite").toAbsolutePath();
        initialize();
    }

    public Connection connection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    private void initialize() {
        try {
            Files.createDirectories(dbPath.getParent());
            try (Connection connection = connection(); Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("CREATE TABLE IF NOT EXISTS Subject(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE)");
                statement.execute("CREATE TABLE IF NOT EXISTS SubSubject(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, subject_id INTEGER NOT NULL, UNIQUE(name, subject_id), FOREIGN KEY(subject_id) REFERENCES Subject(id))");
                statement.execute("CREATE TABLE IF NOT EXISTS Chapter(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, subsubject_id INTEGER NOT NULL, UNIQUE(name, subsubject_id), FOREIGN KEY(subsubject_id) REFERENCES SubSubject(id))");
                statement.execute("CREATE TABLE IF NOT EXISTS Topic(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, chapter_id INTEGER NOT NULL, UNIQUE(name, chapter_id), FOREIGN KEY(chapter_id) REFERENCES Chapter(id))");
                statement.execute("CREATE TABLE IF NOT EXISTS Subtopic(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, topic_id INTEGER NOT NULL, UNIQUE(name, topic_id), FOREIGN KEY(topic_id) REFERENCES Topic(id))");
                statement.execute("CREATE TABLE IF NOT EXISTS PDF(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, path TEXT NOT NULL, size INTEGER NOT NULL, upload_date TEXT NOT NULL, type TEXT NOT NULL, subtopic_id INTEGER, topic_id INTEGER NOT NULL, FOREIGN KEY(subtopic_id) REFERENCES Subtopic(id), FOREIGN KEY(topic_id) REFERENCES Topic(id))");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_subsubject_subject ON SubSubject(subject_id)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_chapter_subsubject ON Chapter(subsubject_id)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_topic_chapter ON Topic(chapter_id)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_subtopic_topic ON Subtopic(topic_id)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_pdf_topic ON PDF(topic_id)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_pdf_subtopic ON PDF(subtopic_id)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_pdf_type ON PDF(type)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_pdf_upload_date ON PDF(upload_date)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_pdf_path ON PDF(path)");
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to initialize SQLite database", exception);
        }
    }
}
