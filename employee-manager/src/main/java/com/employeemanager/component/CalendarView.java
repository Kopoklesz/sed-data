package com.employeemanager.component;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CalendarView extends VBox {
    private final ObservableList<LocalDate> selectedDates = FXCollections.observableArrayList();
    private LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
    private LocalDate notificationDate;
    private LocalDate lastClickedDate = null;

    private final GridPane calendarGrid = new GridPane();
    private final Label monthYearLabel = new Label();
    private final List<Label> dayLabels = new ArrayList<>();

    private static final String DEFAULT_STYLE = "-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-alignment: center; -fx-min-width: 40; -fx-min-height: 40; -fx-cursor: hand; -fx-background-color: white;";
    private static final String SELECTED_STYLE = "-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-alignment: center; -fx-min-width: 40; -fx-min-height: 40; -fx-cursor: hand; -fx-background-color: #2196F3; -fx-text-fill: white;";
    private static final String TODAY_STYLE = "-fx-border-color: #2196F3; -fx-border-width: 2; -fx-alignment: center; -fx-min-width: 40; -fx-min-height: 40; -fx-cursor: hand; -fx-background-color: white; -fx-font-weight: bold;";
    private static final String HOVER_STYLE = "-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-alignment: center; -fx-min-width: 40; -fx-min-height: 40; -fx-cursor: hand; -fx-background-color: #e3f2fd;";
    private static final String WARNING_SELECTED_STYLE = "-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-alignment: center; -fx-min-width: 40; -fx-min-height: 40; -fx-cursor: hand; -fx-background-color: #ff6b00; -fx-text-fill: white;";

    public CalendarView() {
        setupUI();
        refreshCalendar();
    }

    private void setupUI() {
        setSpacing(10);
        setPadding(new Insets(10));
        setMaxWidth(350);

        // Navigációs fejléc
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER);

        Button prevButton = new Button("<");
        Button nextButton = new Button(">");
        Button todayButton = new Button("Ma");
        Button clearButton = new Button("Törlés");

        prevButton.setOnAction(e -> {
            currentMonth = currentMonth.minusMonths(1);
            refreshCalendar();
        });

        nextButton.setOnAction(e -> {
            currentMonth = currentMonth.plusMonths(1);
            refreshCalendar();
        });

        todayButton.setOnAction(e -> {
            currentMonth = LocalDate.now().withDayOfMonth(1);
            refreshCalendar();
        });

        clearButton.setOnAction(e -> {
            clearSelection();
        });

        monthYearLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        Region spacer1 = new Region();
        Region spacer2 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        header.getChildren().addAll(prevButton, spacer1, monthYearLabel, spacer2, clearButton, todayButton, nextButton);

        // Hét napjai fejléc
        GridPane weekDaysHeader = new GridPane();
        weekDaysHeader.setHgap(5);
        weekDaysHeader.setVgap(5);

        for (int i = 0; i < 7; i++) {
            DayOfWeek day = DayOfWeek.of(((i + 6) % 7) + 1); // Hétfővel kezd
            Label dayLabel = new Label(day.getDisplayName(TextStyle.SHORT, new Locale("hu")));
            dayLabel.setStyle("-fx-font-weight: bold; -fx-alignment: center;");
            dayLabel.setMinWidth(40);
            dayLabel.setAlignment(Pos.CENTER);
            GridPane.setHalignment(dayLabel, HPos.CENTER);
            weekDaysHeader.add(dayLabel, i, 0);
        }

        // Naptár grid beállítása
        calendarGrid.setHgap(5);
        calendarGrid.setVgap(5);
        calendarGrid.setAlignment(Pos.CENTER);

        // Használati útmutató
        Label helpLabel = new Label("Kattintás: egy nap | Shift+kattintás: napok tartománya | Ctrl+kattintás: több nap");
        helpLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #757575;");

        getChildren().addAll(header, weekDaysHeader, calendarGrid, helpLabel);
    }

    private void refreshCalendar() {
        calendarGrid.getChildren().clear();
        dayLabels.clear();

        // Hónap és év kiírása
        monthYearLabel.setText(currentMonth.format(DateTimeFormatter.ofPattern("yyyy. MMMM", new Locale("hu"))));

        // Első nap pozíciójának meghatározása
        LocalDate firstDay = currentMonth.withDayOfMonth(1);
        int firstDayOfWeek = firstDay.getDayOfWeek().getValue() - 1; // 0 = hétfő

        // Előző hónap napjai
        LocalDate prevMonth = firstDay.minusDays(firstDayOfWeek);

        LocalDate today = LocalDate.now();

        // 6 hét megjelenítése
        for (int week = 0; week < 6; week++) {
            for (int day = 0; day < 7; day++) {
                LocalDate date = prevMonth.plusDays(week * 7 + day);
                Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
                dayLabel.setUserData(date);
                dayLabel.setMinSize(40, 40);
                dayLabel.setAlignment(Pos.CENTER);

                // Stílus beállítása
                updateDayStyle(dayLabel, date, today);

                // Kattintás esemény
                dayLabel.setOnMouseClicked(e -> {
                    if (e.getButton() == MouseButton.PRIMARY) {
                        handleDayClick(date, e.isShiftDown(), e.isControlDown());
                    }
                });

                // Hover effekt
                dayLabel.setOnMouseEntered(e -> {
                    if (!selectedDates.contains(date)) {
                        dayLabel.setStyle(HOVER_STYLE);
                        if (!date.getMonth().equals(currentMonth.getMonth())) {
                            dayLabel.setTextFill(Color.GRAY);
                        }
                    }
                });

                dayLabel.setOnMouseExited(e -> {
                    updateDayStyle(dayLabel, date, today);
                });

                dayLabels.add(dayLabel);
                calendarGrid.add(dayLabel, day, week);
            }
        }
    }

    private void handleDayClick(LocalDate date, boolean shiftDown, boolean ctrlDown) {
        if (shiftDown && lastClickedDate != null) {
            // Shift + kattintás: tartomány kijelölése
            selectedDates.clear();
            LocalDate start = lastClickedDate.isBefore(date) ? lastClickedDate : date;
            LocalDate end = lastClickedDate.isBefore(date) ? date : lastClickedDate;

            LocalDate current = start;
            while (!current.isAfter(end)) {
                selectedDates.add(current);
                current = current.plusDays(1);
            }
        } else if (ctrlDown) {
            // Ctrl + kattintás: hozzáadás/eltávolítás
            if (selectedDates.contains(date)) {
                selectedDates.remove(date);
            } else {
                selectedDates.add(date);
            }
            lastClickedDate = date;
        } else {
            // Sima kattintás: csak ez az egy nap
            selectedDates.clear();
            selectedDates.add(date);
            lastClickedDate = date;
        }

        refreshCalendar();
    }

    private void updateDayStyle(Label dayLabel, LocalDate date, LocalDate today) {
        String style = DEFAULT_STYLE;

        // Mai nap
        if (date.equals(today)) {
            style = TODAY_STYLE;
        }

        // Kiválasztott napok
        if (selectedDates.contains(date)) {
            // Figyelmeztetés, ha korábbi mint a bejelentés dátuma
            if (notificationDate != null && date.isBefore(notificationDate)) {
                style = WARNING_SELECTED_STYLE;
            } else {
                style = SELECTED_STYLE;
            }
        }

        dayLabel.setStyle(style);

        // Nem aktuális hónap szürke színnel
        if (!date.getMonth().equals(currentMonth.getMonth())) {
            if (!selectedDates.contains(date)) {
                dayLabel.setTextFill(Color.GRAY);
            }
        }
    }

    public ObservableList<LocalDate> getSelectedDates() {
        return selectedDates;
    }

    public void setSelectedDates(List<LocalDate> dates) {
        selectedDates.clear();
        if (dates != null && !dates.isEmpty()) {
            selectedDates.addAll(dates);
            lastClickedDate = dates.get(dates.size() - 1);
        }
        refreshCalendar();
    }

    public void setNotificationDate(LocalDate date) {
        this.notificationDate = date;
        refreshCalendar();
    }

    public void clearSelection() {
        selectedDates.clear();
        lastClickedDate = null;
        refreshCalendar();
    }
}