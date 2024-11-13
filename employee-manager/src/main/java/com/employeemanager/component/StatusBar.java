package com.employeemanager.component;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class StatusBar extends HBox {
    private final Label label;

    public StatusBar() {
        this.label = new Label();
        this.label.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(label, Priority.ALWAYS);

        getChildren().add(label);

        setStyle("-fx-padding: 5; -fx-background-color: #f4f4f4; -fx-border-width: 1 0 0 0; -fx-border-color: #c8c8c8;");
    }

    public void setText(String text) {
        label.setText(text);
    }

    public String getText() {
        return label.getText();
    }
}