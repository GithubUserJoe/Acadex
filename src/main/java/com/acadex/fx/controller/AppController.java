package com.acadex.fx.controller;

import com.acadex.fx.db.Database;
import com.acadex.fx.db.PdfRepository;
import com.acadex.fx.model.HierarchyNode;
import com.acadex.fx.model.OptionItem;
import com.acadex.fx.model.PdfRecord;
import com.acadex.fx.model.PdfType;
import com.acadex.fx.service.FileHash;
import com.acadex.fx.service.ThumbnailService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeItem;
import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AppController {
    private final PdfRepository repository = new PdfRepository(new Database());
    private final ThumbnailService thumbnailService = new ThumbnailService();
    private final ObservableList<PdfRecord> pdfs = FXCollections.observableArrayList();
    private final ObservableList<PdfRecord> recentUploads = FXCollections.observableArrayList();
    private final List<CheckBox> subjectChecks = new ArrayList<>();
    private final List<CheckBox> chapterChecks = new ArrayList<>();
    private final List<CheckBox> topicChecks = new ArrayList<>();
    private final List<CheckBox> subtopicChecks = new ArrayList<>();
    private final List<CheckBox> typeChecks = new ArrayList<>();
    private final Set<Long> selectedSubjectIds = new HashSet<>();
    private final Set<Long> selectedChapterIds = new HashSet<>();
    private final Set<Long> selectedTopicIds = new HashSet<>();
    private final Set<Long> selectedSubtopicIds = new HashSet<>();
    private final Set<PdfType> selectedTypes = new HashSet<>();
    private String search = "";
    private HierarchyNode selectedNode;

    public ObservableList<PdfRecord> pdfs() { return pdfs; }
    public ObservableList<PdfRecord> recentUploads() { return recentUploads; }
    public List<CheckBox> subjectChecks() { return subjectChecks; }
    public List<CheckBox> chapterChecks() { return chapterChecks; }
    public List<CheckBox> topicChecks() { return topicChecks; }
    public List<CheckBox> subtopicChecks() { return subtopicChecks; }
    public List<CheckBox> typeChecks() { return typeChecks; }
    public ThumbnailService thumbnails() { return thumbnailService; }

    public void refreshAll() {
        refreshPdfs();
        refreshRecent();
    }

    public TreeItem<HierarchyNode> buildTree() {
        TreeItem<HierarchyNode> root = new TreeItem<>(new HierarchyNode("root", 0, "Library"));
        root.setExpanded(true);
        try {
            Map<OptionItem, Map<OptionItem, Map<OptionItem, Map<OptionItem, List<OptionItem>>>>> hierarchy = repository.hierarchy();
            for (Map.Entry<OptionItem, Map<OptionItem, Map<OptionItem, Map<OptionItem, List<OptionItem>>>>> subjectEntry : hierarchy.entrySet()) {
                TreeItem<HierarchyNode> subject = item("subject", subjectEntry.getKey());
                root.getChildren().add(subject);
                for (Map.Entry<OptionItem, Map<OptionItem, Map<OptionItem, List<OptionItem>>>> subEntry : subjectEntry.getValue().entrySet()) {
                    TreeItem<HierarchyNode> subsubject = item("subsubject", subEntry.getKey());
                    subject.getChildren().add(subsubject);
                    for (Map.Entry<OptionItem, Map<OptionItem, List<OptionItem>>> chapterEntry : subEntry.getValue().entrySet()) {
                        TreeItem<HierarchyNode> chapter = item("chapter", chapterEntry.getKey());
                        subsubject.getChildren().add(chapter);
                        for (Map.Entry<OptionItem, List<OptionItem>> topicEntry : chapterEntry.getValue().entrySet()) {
                            TreeItem<HierarchyNode> topic = item("topic", topicEntry.getKey());
                            chapter.getChildren().add(topic);
                            for (OptionItem subtopicOption : topicEntry.getValue()) {
                                topic.getChildren().add(item("subtopic", subtopicOption));
                            }
                        }
                    }
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        return root;
    }

    public void rebuildFilters(Runnable onSubjectChange, Runnable onChapterChange, Runnable onTopicChange, Runnable onSubtopicChange, Runnable onTypeChange) {
        subjectChecks.clear();
        chapterChecks.clear();
        topicChecks.clear();
        subtopicChecks.clear();
        typeChecks.clear();
        CheckBox quiz = check("Quiz", PdfType.QUIZ, selectedTypes.contains(PdfType.QUIZ), onTypeChange);
        CheckBox theory = check("Theory", PdfType.THEORY, selectedTypes.contains(PdfType.THEORY), onTypeChange);
        typeChecks.add(quiz);
        typeChecks.add(theory);
        try {
            for (OptionItem item : repository.subjects()) {
                subjectChecks.add(check(item.getName(), item.getId(), selectedSubjectIds.contains(item.getId()), onSubjectChange));
            }
            for (OptionItem item : repository.chaptersBySubjects(new ArrayList<>(selectedSubjectIds))) {
                chapterChecks.add(check(item.getName(), item.getId(), selectedChapterIds.contains(item.getId()), onChapterChange));
            }
            selectedChapterIds.retainAll(values(chapterChecks));
            for (OptionItem item : repository.topicsByChapters(new ArrayList<>(selectedChapterIds))) {
                topicChecks.add(check(item.getName(), item.getId(), selectedTopicIds.contains(item.getId()), onTopicChange));
            }
            selectedTopicIds.retainAll(values(topicChecks));
            for (OptionItem item : repository.subtopicsByTopics(new ArrayList<>(selectedTopicIds))) {
                subtopicChecks.add(check(item.getName(), item.getId(), selectedSubtopicIds.contains(item.getId()), onSubtopicChange));
            }
            selectedSubtopicIds.retainAll(values(subtopicChecks));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    public void syncSubjectSelections() {
        selectedSubjectIds.clear();
        selectedSubjectIds.addAll(checkedIds(subjectChecks));
        selectedChapterIds.clear();
        selectedTopicIds.clear();
        selectedSubtopicIds.clear();
    }

    public void syncChapterSelections() {
        selectedChapterIds.clear();
        selectedChapterIds.addAll(checkedIds(chapterChecks));
        selectedTopicIds.clear();
        selectedSubtopicIds.clear();
    }

    public void syncTopicSelections() {
        selectedTopicIds.clear();
        selectedTopicIds.addAll(checkedIds(topicChecks));
        selectedSubtopicIds.clear();
    }

    public void syncSubtopicSelections() {
        selectedSubtopicIds.clear();
        selectedSubtopicIds.addAll(checkedIds(subtopicChecks));
    }

    public void syncTypeSelections() {
        selectedTypes.clear();
        selectedTypes.addAll(checkedTypes());
    }

    public void setSearch(String search) {
        this.search = search == null ? "" : search;
        refreshPdfs();
    }

    public void setSelectedNode(HierarchyNode selectedNode) {
        this.selectedNode = selectedNode;
        refreshPdfs();
    }

    public void upload(File source, PdfType type, String subject, String subsubject, String chapter, String topic, String subtopic) throws Exception {
        if (source == null || !source.getName().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Choose a PDF file");
        }
        Files.createDirectories(Paths.get("uploads"));
        String hash = FileHash.sha256(source.toPath());
        Path target = Paths.get("uploads", hash + ".pdf");
        if (!Files.exists(target)) {
            Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        }
        repository.savePdf(target, source.getName(), type, subject, subsubject, chapter, topic, subtopic);
        refreshAll();
    }

    public void open(PdfRecord record) throws Exception {
        Desktop.getDesktop().open(new File(record.getPath()));
    }

    public void remove(PdfRecord record) throws Exception {
        if (record == null) return;
        Path path = Paths.get(record.getPath());
        repository.deletePdf(record.getId());
        if (repository.countByPath(record.getPath()) == 0) {
            thumbnailService.deleteFor(path);
            Files.deleteIfExists(path);
        }
        refreshAll();
    }

    public void shutdown() {
        thumbnailService.shutdown();
    }

    private void refreshPdfs() {
        try {
            pdfs.setAll(repository.findPdfs(search, selectedNode, checkedIds(subjectChecks), checkedIds(chapterChecks),
                    checkedIds(topicChecks), checkedIds(subtopicChecks), checkedTypes()));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void refreshRecent() {
        try {
            recentUploads.setAll(repository.recentUploads());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private TreeItem<HierarchyNode> item(String level, OptionItem option) {
        return new TreeItem<>(new HierarchyNode(level, option.getId(), option.getName()));
    }

    private CheckBox check(String label, Object value, boolean selected, Runnable onChange) {
        CheckBox checkBox = new CheckBox(label);
        checkBox.getProperties().put("value", value);
        checkBox.setSelected(selected);
        checkBox.setOnAction(event -> onChange.run());
        return checkBox;
    }

    private Set<Long> values(List<CheckBox> checks) {
        return checks.stream().map(check -> (Long) check.getProperties().get("value")).collect(Collectors.toSet());
    }

    private List<Long> checkedIds(List<CheckBox> checks) {
        return checks.stream().filter(CheckBox::isSelected).map(check -> (Long) check.getProperties().get("value")).collect(Collectors.toList());
    }

    private List<PdfType> checkedTypes() {
        return typeChecks.stream().filter(CheckBox::isSelected).map(check -> (PdfType) check.getProperties().get("value")).collect(Collectors.toList());
    }
}
