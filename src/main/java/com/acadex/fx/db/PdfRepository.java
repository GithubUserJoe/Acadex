package com.acadex.fx.db;

import com.acadex.fx.model.HierarchyNode;
import com.acadex.fx.model.OptionItem;
import com.acadex.fx.model.PdfRecord;
import com.acadex.fx.model.PdfType;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PdfRepository {
    private final Database database;

    public PdfRepository(Database database) {
        this.database = database;
    }

    public long savePdf(Path path, String displayName, PdfType type, String subject, String subsubject, String chapter,
                        String topic, String subtopic) throws Exception {
        try (Connection connection = database.connection()) {
            connection.setAutoCommit(false);
            long subjectId = getOrCreate(connection, "Subject", normalize(subject), null, null);
            long subsubjectId = getOrCreate(connection, "SubSubject", normalize(subsubject), "subject_id", subjectId);
            long chapterId = getOrCreate(connection, "Chapter", normalize(chapter), "subsubject_id", subsubjectId);
            long topicId = getOrCreate(connection, "Topic", normalize(topic), "chapter_id", chapterId);
            Long subtopicId = null;
            if (subtopic != null && !subtopic.trim().isEmpty()) {
                subtopicId = getOrCreate(connection, "Subtopic", normalize(subtopic), "topic_id", topicId);
            }

            String sql = "INSERT INTO PDF(name, path, size, upload_date, type, subtopic_id, topic_id) VALUES(?,?,?,?,?,?,?)";
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, normalizeFileName(displayName));
                statement.setString(2, path.toAbsolutePath().toString());
                statement.setLong(3, java.nio.file.Files.size(path));
                statement.setString(4, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                statement.setString(5, type.name());
                if (subtopicId == null) statement.setNull(6, java.sql.Types.INTEGER);
                else statement.setLong(6, subtopicId);
                statement.setLong(7, topicId);
                statement.executeUpdate();
                connection.commit();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    return keys.next() ? keys.getLong(1) : 0;
                }
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public List<PdfRecord> findPdfs(String search, HierarchyNode node, List<Long> subjectIds,
                                    List<Long> chapterIds, List<Long> topicIds, List<Long> subtopicIds,
                                    List<PdfType> types) throws Exception {
        StringBuilder sql = new StringBuilder(basePdfSql());
        List<Object> params = new ArrayList<>();
        sql.append(" WHERE 1=1");
        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (lower(p.name) LIKE ? OR lower(p.type) LIKE ? OR lower(p.upload_date) LIKE ? ");
            sql.append("OR lower(s.name || ' > ' || ss.name || ' > ' || c.name || ' > ' || t.name || ");
            sql.append("CASE WHEN st.name IS NULL THEN '' ELSE ' > ' || st.name END) LIKE ?)");
            String term = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }
        if (node != null && node.getId() > 0) {
            appendNodeFilter(sql, params, node);
        }
        appendIn(sql, params, "s.id", subjectIds);
        appendIn(sql, params, "c.id", chapterIds);
        appendIn(sql, params, "t.id", topicIds);
        appendIn(sql, params, "st.id", subtopicIds);
        if (types != null && !types.isEmpty()) {
            sql.append(" AND p.type IN (");
            for (int i = 0; i < types.size(); i++) {
                if (i > 0) sql.append(",");
                sql.append("?");
                params.add(types.get(i).name());
            }
            sql.append(")");
        }
        sql.append(" ORDER BY p.upload_date DESC");
        return queryPdfs(sql.toString(), params);
    }

    public List<PdfRecord> recentUploads() throws Exception {
        return queryPdfs(basePdfSql() + " ORDER BY p.upload_date DESC LIMIT 5", new ArrayList<>());
    }

    public void deletePdf(long id) throws Exception {
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM PDF WHERE id=?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    public long countByPath(String path) throws Exception {
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM PDF WHERE path=?")) {
            statement.setString(1, path);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    public Map<OptionItem, Map<OptionItem, Map<OptionItem, Map<OptionItem, List<OptionItem>>>>> hierarchy() throws Exception {
        Map<OptionItem, Map<OptionItem, Map<OptionItem, Map<OptionItem, List<OptionItem>>>>> root = new LinkedHashMap<>();
        String sql = "SELECT s.id subject_id, s.name subject_name, ss.id subsubject_id, ss.name subsubject_name, " +
                "c.id chapter_id, c.name chapter_name, t.id topic_id, t.name topic_name, st.id subtopic_id, st.name subtopic_name " +
                "FROM Subject s LEFT JOIN SubSubject ss ON ss.subject_id=s.id " +
                "LEFT JOIN Chapter c ON c.subsubject_id=ss.id LEFT JOIN Topic t ON t.chapter_id=c.id " +
                "LEFT JOIN Subtopic st ON st.topic_id=t.id ORDER BY s.name, ss.name, c.name, t.name, st.name";
        try (Connection connection = database.connection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                OptionItem subject = new OptionItem(rs.getLong("subject_id"), rs.getString("subject_name"));
                root.putIfAbsent(subject, new LinkedHashMap<>());
                if (rs.getObject("subsubject_id") == null) continue;
                OptionItem subsubject = new OptionItem(rs.getLong("subsubject_id"), rs.getString("subsubject_name"));
                root.get(subject).putIfAbsent(subsubject, new LinkedHashMap<>());
                if (rs.getObject("chapter_id") == null) continue;
                OptionItem chapter = new OptionItem(rs.getLong("chapter_id"), rs.getString("chapter_name"));
                root.get(subject).get(subsubject).putIfAbsent(chapter, new LinkedHashMap<>());
                if (rs.getObject("topic_id") == null) continue;
                OptionItem topic = new OptionItem(rs.getLong("topic_id"), rs.getString("topic_name"));
                root.get(subject).get(subsubject).get(chapter).putIfAbsent(topic, new ArrayList<>());
                if (rs.getObject("subtopic_id") != null) {
                    root.get(subject).get(subsubject).get(chapter).get(topic)
                            .add(new OptionItem(rs.getLong("subtopic_id"), rs.getString("subtopic_name")));
                }
            }
        }
        return root;
    }

    public List<OptionItem> subjects() throws Exception { return options("SELECT id, name FROM Subject ORDER BY name"); }
    public List<OptionItem> chaptersBySubjects(List<Long> subjectIds) throws Exception {
        if (subjectIds == null || subjectIds.isEmpty()) return new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT DISTINCT c.id, c.name FROM Chapter c JOIN SubSubject ss ON ss.id=c.subsubject_id WHERE ss.subject_id IN (");
        return optionsByIds(sql, subjectIds, ") ORDER BY c.name");
    }

    public List<OptionItem> topicsByChapters(List<Long> chapterIds) throws Exception {
        if (chapterIds == null || chapterIds.isEmpty()) return new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT DISTINCT id, name FROM Topic WHERE chapter_id IN (");
        return optionsByIds(sql, chapterIds, ") ORDER BY name");
    }

    public List<OptionItem> subtopicsByTopics(List<Long> topicIds) throws Exception {
        if (topicIds == null || topicIds.isEmpty()) return new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT DISTINCT id, name FROM Subtopic WHERE topic_id IN (");
        return optionsByIds(sql, topicIds, ") ORDER BY name");
    }

    private List<OptionItem> options(String sql) throws Exception {
        List<OptionItem> items = new ArrayList<>();
        try (Connection connection = database.connection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {
            while (rs.next()) items.add(new OptionItem(rs.getLong("id"), rs.getString("name")));
        }
        return items;
    }

    private List<OptionItem> optionsByIds(StringBuilder sql, List<Long> ids, String suffix) throws Exception {
        List<Object> params = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
            params.add(ids.get(i));
        }
        sql.append(suffix);
        List<OptionItem> items = new ArrayList<>();
        try (Connection connection = database.connection(); PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) statement.setObject(i + 1, params.get(i));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) items.add(new OptionItem(rs.getLong("id"), rs.getString("name")));
            }
        }
        return items;
    }

    private String basePdfSql() {
        return "SELECT p.id, p.name, p.path, p.size, p.upload_date, p.type, " +
                "s.name || ' > ' || ss.name || ' > ' || c.name || ' > ' || t.name || " +
                "CASE WHEN st.name IS NULL THEN '' ELSE ' > ' || st.name END AS full_path " +
                "FROM PDF p JOIN Topic t ON t.id=p.topic_id JOIN Chapter c ON c.id=t.chapter_id " +
                "JOIN SubSubject ss ON ss.id=c.subsubject_id JOIN Subject s ON s.id=ss.subject_id " +
                "LEFT JOIN Subtopic st ON st.id=p.subtopic_id";
    }

    private List<PdfRecord> queryPdfs(String sql, List<Object> params) throws Exception {
        List<PdfRecord> pdfs = new ArrayList<>();
        try (Connection connection = database.connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) statement.setObject(i + 1, params.get(i));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    pdfs.add(new PdfRecord(rs.getLong("id"), rs.getString("name"), rs.getString("path"),
                            rs.getLong("size"), rs.getString("upload_date"), rs.getString("type"), rs.getString("full_path")));
                }
            }
        }
        return pdfs;
    }

    private void appendNodeFilter(StringBuilder sql, List<Object> params, HierarchyNode node) {
        String column;
        switch (node.getLevel()) {
            case "subject": column = "s.id"; break;
            case "subsubject": column = "ss.id"; break;
            case "chapter": column = "c.id"; break;
            case "topic": column = "t.id"; break;
            case "subtopic": column = "st.id"; break;
            default: return;
        }
        sql.append(" AND ").append(column).append("=?");
        params.add(node.getId());
    }

    private void appendIn(StringBuilder sql, List<Object> params, String column, List<Long> values) {
        if (values == null || values.isEmpty()) return;
        sql.append(" AND ").append(column).append(" IN (");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
            params.add(values.get(i));
        }
        sql.append(")");
    }

    private long getOrCreate(Connection connection, String table, String name, String parentColumn, Long parentId) throws Exception {
        String select = parentColumn == null
                ? "SELECT id FROM " + table + " WHERE name=?"
                : "SELECT id FROM " + table + " WHERE name=? AND " + parentColumn + "=?";
        try (PreparedStatement statement = connection.prepareStatement(select)) {
            statement.setString(1, name);
            if (parentColumn != null) statement.setLong(2, parentId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }

        String insert = parentColumn == null
                ? "INSERT INTO " + table + "(name) VALUES(?)"
                : "INSERT INTO " + table + "(name, " + parentColumn + ") VALUES(?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            if (parentColumn != null) statement.setLong(2, parentId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new IllegalStateException("Unable to save " + table);
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("All hierarchy fields except subtopic are required");
        String trimmed = value.trim();
        return trimmed.substring(0, 1).toUpperCase(Locale.ROOT) + trimmed.substring(1);
    }

    private String normalizeFileName(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Untitled.pdf";
        }
        return value.trim();
    }
}
