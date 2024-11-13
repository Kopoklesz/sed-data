package com.employeemanager.util;

import com.employeemanager.model.fx.WorkRecordFX;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ExcelExporter {

    private static final String EXPORT_DIRECTORY = "exports";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String[] HEADERS = {
            "Azonosító", "Alkalmazott", "Bejelentés dátuma", "EBEV azonosító",
            "Munkanap", "Bérezés", "Munkaórák"
    };

    public String exportWorkRecords(List<WorkRecordFX> records, LocalDate startDate, LocalDate endDate) throws Exception {
        createExportDirectory();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Munkanaplók");

            // Stílusok létrehozása
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            // Fejléc létrehozása
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Adatok feltöltése
            int rowNum = 1;
            for (WorkRecordFX record : records) {
                Row row = sheet.createRow(rowNum++);

                // ID
                row.createCell(0).setCellValue(record.getId());

                // Alkalmazott neve
                row.createCell(1).setCellValue(record.getEmployeeName());

                // Bejelentés dátuma
                Cell notificationCell = row.createCell(2);
                notificationCell.setCellValue(record.getNotificationDate().format(DATE_FORMATTER));
                notificationCell.setCellStyle(dateStyle);

                // EBEV azonosító
                row.createCell(3).setCellValue(record.getEbevSerialNumber());

                // Munkanap
                Cell workDateCell = row.createCell(4);
                workDateCell.setCellValue(record.getWorkDate().format(DATE_FORMATTER));
                workDateCell.setCellStyle(dateStyle);

                // Bérezés
                Cell paymentCell = row.createCell(5);
                paymentCell.setCellValue(record.getPayment().doubleValue());
                paymentCell.setCellStyle(currencyStyle);

                // Munkaórák
                row.createCell(6).setCellValue(record.getHoursWorked());
            }

            // Összesítő sor
            Row summaryRow = sheet.createRow(rowNum + 1);
            summaryRow.createCell(0).setCellValue("Összesen:");

            // Összesítő képletek
            Cell totalPaymentCell = summaryRow.createCell(5);
            totalPaymentCell.setCellFormula("SUM(F2:F" + rowNum + ")");
            totalPaymentCell.setCellStyle(currencyStyle);

            Cell totalHoursCell = summaryRow.createCell(6);
            totalHoursCell.setCellFormula("SUM(G2:G" + rowNum + ")");

            // Oszlopszélesség beállítása
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Fájl mentése
            String fileName = String.format("munkanaplot_%s_%s.xlsx",
                    startDate.format(DateTimeFormatter.BASIC_ISO_DATE),
                    endDate.format(DateTimeFormatter.BASIC_ISO_DATE));

            Path filePath = Paths.get(EXPORT_DIRECTORY, fileName);
            try (FileOutputStream fileOut = new FileOutputStream(filePath.toFile())) {
                workbook.write(fileOut);
            }

            return filePath.toString();
        }
    }

    private void createExportDirectory() throws Exception {
        Path directory = Paths.get(EXPORT_DIRECTORY);
        if (!Files.exists(directory)) {
            Files.createDirectory(directory);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0 Ft"));
        return style;
    }
}