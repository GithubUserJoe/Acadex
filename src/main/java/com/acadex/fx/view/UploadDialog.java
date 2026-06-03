package com.acadex.fx.view;

import com.acadex.fx.controller.AppController;
import com.acadex.fx.model.PdfType;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class UploadDialog {
    private final AppController controller;

    public UploadDialog(AppController controller) {
        this.controller = controller;
    }

    public void show(Window owner, File selectedFile, Runnable afterSave) {
        Stage dialog = new Stage();
        dialog.setTitle("Upload PDF");
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);

        Label fileName = new Label(selectedFile.getName());
        fileName.getStyleClass().add("file-name");
        ComboBox<PdfType> type = new ComboBox<>();
        type.getItems().setAll(PdfType.values());
        type.setValue(PdfType.THEORY);

        ComboBox<String> subject = editableCombo(labels(controller.subjectChecks()));
        ComboBox<String> subsubject = editableCombo(java.util.Collections.emptyList());
        ComboBox<String> chapter = editableCombo(labels(controller.chapterChecks()));
        ComboBox<String> topic = editableCombo(labels(controller.topicChecks()));
        ComboBox<String> subtopic = editableCombo(java.util.Collections.emptyList());

        GridPane form = new GridPane();
        form.getStyleClass().add("upload-form");
        form.setHgap(16);
        form.setVgap(12);
        form.setPadding(new Insets(8));
        addRow(form, 0, "PDF", fileName);
        addRow(form, 1, "Type", type);
        addRow(form, 2, "Subject", subject);
        addRow(form, 3, "SubSubject", subsubject);
        addRow(form, 4, "Chapter", chapter);
        addRow(form, 5, "Topic", topic);
        addRow(form, 6, "Subtopic", subtopic);

        Button cancel = new Button("Cancel");
        Button save = new Button("Save");
        save.getStyleClass().add("primary-button");
        cancel.setOnAction(event -> dialog.close());
        save.setOnAction(event -> {
            try {
                save.setDisable(true);
                cancel.setDisable(true);
                controller.upload(selectedFile, type.getValue(), value(subject), value(subsubject),
                        value(chapter), value(topic), value(subtopic));
                afterSave.run();
                dialog.close();
            } catch (Exception exception) {
                save.setDisable(false);
                cancel.setDisable(false);
                Alert alert = new Alert(Alert.AlertType.ERROR, exception.getMessage());
                alert.initOwner(owner);
                alert.showAndWait();
            }
        });

        javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(8, cancel, save);
        actions.getStyleClass().add("dialog-actions");
        javafx.scene.layout.VBox body = new javafx.scene.layout.VBox(16, form, actions);
        body.getStyleClass().add("card");
        javafx.scene.layout.StackPane shell = new javafx.scene.layout.StackPane(body);
        shell.getStyleClass().add("upload-dialog");
        shell.setPadding(new Insets(12));
        Scene scene = new Scene(shell, 560, 360);
        scene.getStylesheets().add(getClass().getResource("/fx/styles.css").toExternalForm());
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.showAndWait();
    }

    private ComboBox<String> editableCombo(List<String> values) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setEditable(true);
        comboBox.getItems().setAll(values);
        comboBox.setMaxWidth(Double.MAX_VALUE);
        return comboBox;
    }

    private List<String> labels(List<javafx.scene.control.CheckBox> checks) {
        return checks.stream().map(javafx.scene.control.CheckBox::getText).collect(Collectors.toList());
    }

    private String value(ComboBox<String> comboBox) {
        return comboBox.getEditor().getText();
    }

    private void addRow(GridPane form, int row, String label, Node field) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("form-label");
        form.add(labelNode, 0, row);
        form.add(field, 1, row);
        GridPane.setHgrow(field, javafx.scene.layout.Priority.ALWAYS);
    }
}
