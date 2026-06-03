package com.acadex.fx.view;

import com.acadex.fx.controller.AppController;
import com.acadex.fx.model.HierarchyNode;
import com.acadex.fx.model.PdfRecord;
import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.chart.PieChart;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class MainView {
    private final AppController controller;
    private final StackPane root = new StackPane();
    private final BorderPane layout = new BorderPane();
    private final TreeView<HierarchyNode> tree = new TreeView<>();
    private final FlowPane grid = new FlowPane();
    private final VBox filterPanel = new VBox(14);
    private final VBox recentList = new VBox(8);
    private final PieChart coverageChart = new PieChart();
    private final Label dropOverlay = new Label("Drop PDF to upload");
    private final UploadDialog uploadDialog;
    private boolean recentListenerInstalled;

    public MainView(AppController controller) {
        this.controller = controller;
        this.uploadDialog = new UploadDialog(controller);
        build();
        controller.pdfs().addListener((ListChangeListener<PdfRecord>) change -> {
            renderGrid();
            updateCoverageChart();
        });
        controller.refreshAll();
        renderGrid();
        updateCoverageChart();
    }

    public Parent root() {
        return root;
    }

    private void build() {
        root.getStyleClass().add("app-root");
        layout.setLeft(sidebar());
        layout.setTop(toolbar());
        layout.setCenter(centerArea());
        layout.setRight(filters());
        dropOverlay.getStyleClass().add("drop-overlay");
        dropOverlay.setVisible(false);
        dropOverlay.setManaged(false);
        root.getChildren().addAll(layout, dropOverlay);
        installDragAndDrop();
    }

    private Parent toolbar() {
        HBox toolbar = new HBox(12);
        toolbar.getStyleClass().add("topbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);

        TextField search = new TextField();
        search.setPromptText("Search PDFs...");
        search.getStyleClass().add("search-field");
        search.textProperty().addListener((obs, oldValue, newValue) -> controller.setSearch(newValue));
        HBox.setHgrow(search, Priority.ALWAYS);

        Label welcome = new Label("Welcome back, Admin");
        welcome.getStyleClass().add("welcome-label");
        StackPane avatar = new StackPane(new Label("A"));
        avatar.getStyleClass().add("avatar");
        Label bell = new Label("◔");
        bell.getStyleClass().add("bell");

        Button upload = new Button("+ Upload");
        upload.getStyleClass().add("primary-button");
        upload.setOnAction(event -> choosePdf());
        toolbar.getChildren().addAll(search, welcome, avatar, bell, upload);
        return toolbar;
    }

    private Parent sidebar() {
        VBox sidebar = new VBox(12);
        sidebar.getStyleClass().addAll("sidebar", "card");
        sidebar.setPrefWidth(300);
        sidebar.setMinWidth(300);
        sidebar.setMaxWidth(300);
        Label title = new Label("Library");
        title.getStyleClass().add("section-title");
        tree.setRoot(controller.buildTree());
        tree.setShowRoot(true);
        tree.getStyleClass().add("library-tree");
        tree.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem != null) {
                HierarchyNode value = newItem.getValue();
                controller.setSelectedNode("root".equals(value.getLevel()) ? null : value);
            }
        });
        VBox.setVgrow(tree, Priority.ALWAYS);
        sidebar.getChildren().addAll(title, tree);
        return sidebar;
    }

    private Parent centerArea() {
        VBox center = new VBox(10);
        center.getStyleClass().add("center-wrap");
        center.getChildren().add(summaryStrip());

        ScrollPane scroll = new ScrollPane(grid);
        scroll.getStyleClass().add("grid-scroll");
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        grid.getStyleClass().add("pdf-grid");
        center.getChildren().add(scroll);
        return center;
    }

    private Parent summaryStrip() {
        HBox strip = new HBox(10);
        strip.getStyleClass().add("summary-strip");
        Parent drop = dropZoneCard();
        HBox.setHgrow(drop, Priority.ALWAYS);
        strip.getChildren().addAll(drop, coverageCard());
        return strip;
    }

    private Parent dropZoneCard() {
        StackPane card = new StackPane(new Label("Drop PDFs here to upload"));
        card.getStyleClass().addAll("mini-card", "drop-card");
        card.setOnMouseClicked(event -> choosePdf());
        return card;
    }

    private Parent coverageCard() {
        HBox card = new HBox(12);
        card.getStyleClass().add("mini-card");
        card.setAlignment(Pos.CENTER_LEFT);
        coverageChart.getStyleClass().add("coverage-chart");
        coverageChart.setLegendVisible(false);
        coverageChart.setLabelsVisible(false);
        coverageChart.setClockwise(true);
        coverageChart.setPrefSize(74, 74);
        coverageChart.setMinSize(74, 74);
        coverageChart.setMaxSize(74, 74);
        VBox copy = new VBox(4,
                new Label("Library Coverage"),
                new Label("● Quiz"),
                new Label("● Theory"));
        copy.getStyleClass().add("metric-copy");
        card.getChildren().addAll(coverageChart, copy);
        return card;
    }

    private Parent filters() {
        filterPanel.getStyleClass().addAll("filters", "card");
        filterPanel.setPrefWidth(290);
        filterPanel.setMinWidth(290);
        filterPanel.setMaxWidth(290);
        rebuildFilters();
        return filterPanel;
    }

    private void rebuildFilters() {
        controller.rebuildFilters(
                () -> { controller.syncSubjectSelections(); rebuildFilters(); controller.refreshAll(); },
                () -> { controller.syncChapterSelections(); rebuildFilters(); controller.refreshAll(); },
                () -> { controller.syncTopicSelections(); rebuildFilters(); controller.refreshAll(); },
                () -> { controller.syncSubtopicSelections(); controller.refreshAll(); },
                () -> { controller.syncTypeSelections(); controller.refreshAll(); }
        );
        filterPanel.getChildren().clear();
        filterPanel.getChildren().add(section("Filters", new Label("Pro")));
        filterPanel.getChildren().add(section("Type", controller.typeChecks()));
        filterPanel.getChildren().add(section("Subjects", controller.subjectChecks()));
        filterPanel.getChildren().add(section("Chapters", controller.chapterChecks()));
        filterPanel.getChildren().add(section("Topics", controller.topicChecks()));
        filterPanel.getChildren().add(section("Subtopics", controller.subtopicChecks()));
        filterPanel.getChildren().add(recentUploads());
    }

    private VBox section(String title, List<? extends javafx.scene.Node> nodes) {
        VBox box = new VBox(8);
        Label label = new Label(title);
        label.getStyleClass().add("section-title");
        box.getChildren().add(label);
        box.getChildren().addAll(nodes);
        return box;
    }

    private VBox section(String title, javafx.scene.Node node) {
        VBox box = new VBox(8);
        HBox head = new HBox();
        Label label = new Label(title);
        label.getStyleClass().add("section-title");
        HBox.setHgrow(label, Priority.ALWAYS);
        node.getStyleClass().add("tiny-badge");
        head.getChildren().addAll(label, node);
        box.getChildren().add(head);
        return box;
    }

    private Parent recentUploads() {
        VBox box = new VBox(8);
        Label title = new Label("Recent Activity");
        title.getStyleClass().add("section-title");
        recentList.getChildren().clear();
        if (!recentListenerInstalled) {
            controller.recentUploads().addListener((ListChangeListener<PdfRecord>) change -> renderRecent());
            recentListenerInstalled = true;
        }
        renderRecent();
        box.getChildren().addAll(title, recentList);
        return box;
    }

    private void renderRecent() {
        recentList.getChildren().clear();
        for (PdfRecord record : controller.recentUploads()) {
            VBox row = new VBox(2);
            row.getStyleClass().add("activity-row");
            Label name = new Label(record.getName());
            Label date = new Label(record.getUploadDate());
            date.getStyleClass().add("muted-text");
            row.getChildren().addAll(name, date);
            recentList.getChildren().add(row);
        }
        if (recentList.getChildren().isEmpty()) {
            Label empty = new Label("No uploads yet");
            empty.getStyleClass().add("muted-text");
            recentList.getChildren().add(empty);
        }
    }

    private void renderGrid() {
        grid.getChildren().clear();
        for (PdfRecord record : controller.pdfs()) {
            grid.getChildren().add(pdfCard(record));
        }
    }

    private void updateCoverageChart() {
        long quiz = controller.pdfs().stream().filter(record -> "QUIZ".equals(record.getType())).count();
        long theory = controller.pdfs().stream().filter(record -> "THEORY".equals(record.getType())).count();
        coverageChart.getData().setAll(
                new PieChart.Data("Quiz", quiz),
                new PieChart.Data("Theory", theory)
        );
    }

    private Parent pdfCard(PdfRecord record) {
        VBox card = new VBox(8);
        card.getStyleClass().add("pdf-card");

        StackPane preview = new StackPane();
        preview.getStyleClass().add("pdf-preview");
        Label placeholder = new Label(".PDF");
        placeholder.getStyleClass().add("pdf-placeholder");
        ImageView image = new ImageView();
        image.setPreserveRatio(true);
        image.setFitHeight(152);
        image.setFitWidth(176);
        Label size = new Label(formatSize(record.getSize()));
        size.getStyleClass().add("size-pill");
        StackPane.setAlignment(size, Pos.BOTTOM_RIGHT);
        preview.getChildren().addAll(placeholder, image, size);
        controller.thumbnails().load(Paths.get(record.getPath()), image::setImage);

        Label name = new Label(record.getName());
        name.getStyleClass().add("card-title");
        Label type = new Label(record.getType());
        type.getStyleClass().add("QUIZ".equals(record.getType()) ? "quiz-badge" : "theory-badge");

        Button open = new Button("↗ Open");
        open.getStyleClass().add("card-action");
        open.setOnAction(event -> {
            try {
                controller.open(record);
            } catch (Exception exception) {
                showError(exception);
            }
        });
        Button previewAction = new Button("▶");
        previewAction.getStyleClass().add("icon-action");
        previewAction.setOnAction(event -> open.fire());
        Button remove = new Button("⌫");
        remove.getStyleClass().add("icon-action");
        remove.setOnAction(event -> removeRecord(record));
        HBox actions = new HBox(8, open, spacer(), previewAction, remove);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(preview, name, type, actions);
        return card;
    }

    private javafx.scene.Node spacer() {
        Label spacer = new Label();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private void removeRecord(PdfRecord record) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove \"" + record.getName() + "\" from the library?",
                ButtonType.CANCEL, ButtonType.OK);
        confirm.setTitle("Remove PDF");
        confirm.setHeaderText("Remove PDF");
        confirm.initOwner(root.getScene().getWindow());
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                controller.remove(record);
                tree.setRoot(controller.buildTree());
                rebuildFilters();
                renderGrid();
            } catch (Exception exception) {
                showError(exception);
            }
        }
    }

    private void choosePdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        File file = chooser.showOpenDialog(root.getScene().getWindow());
        if (file != null) showUpload(file);
    }

    private void showUpload(File file) {
        uploadDialog.show(root.getScene().getWindow(), file, () -> {
            tree.setRoot(controller.buildTree());
            rebuildFilters();
            controller.refreshAll();
        });
    }

    private void installDragAndDrop() {
        root.setOnDragOver(event -> {
            Dragboard dragboard = event.getDragboard();
            if (dragboard.hasFiles() && dragboard.getFiles().stream().anyMatch(file -> file.getName().toLowerCase().endsWith(".pdf"))) {
                event.acceptTransferModes(TransferMode.COPY);
                showDropOverlay(true);
            }
            event.consume();
        });
        root.setOnDragEntered(event -> {
            Dragboard dragboard = event.getDragboard();
            if (dragboard.hasFiles() && dragboard.getFiles().stream().anyMatch(file -> file.getName().toLowerCase().endsWith(".pdf"))) {
                showDropOverlay(true);
            }
            event.consume();
        });
        root.setOnDragExited(event -> {
            showDropOverlay(false);
            event.consume();
        });
        root.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;
            if (dragboard.hasFiles()) {
                dragboard.getFiles().stream().filter(file -> file.getName().toLowerCase().endsWith(".pdf")).findFirst().ifPresent(this::showUpload);
                success = true;
            }
            showDropOverlay(false);
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void showDropOverlay(boolean visible) {
        dropOverlay.setVisible(visible);
        dropOverlay.setManaged(visible);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private void showError(Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR, exception.getMessage());
        alert.initOwner(root.getScene().getWindow());
        alert.showAndWait();
    }
}
