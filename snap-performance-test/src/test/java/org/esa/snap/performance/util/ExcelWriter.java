package org.esa.snap.performance.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExcelWriter {

    private final static String[] headerTitles = {
            "Test Name",
            "Product Name",
            "Individual Test Measure",
            "Result DiMap",
            "Result ZNAP",
            "Unit",
            "Performance Indicator (ZNAP < DiMap)"
    };

    public static void writeExcelFile(String outputDir, List<TestResult> results) {
        File outputFile = new File(outputDir + "/" + TestUtils.RESULTS_DIR);
        writeResultsToExcel(outputFile, results);
    }

    private static void writeResultsToExcel(File outputDir, List<TestResult> results) {
        Workbook workbook = new XSSFWorkbook();
        populateSheet(workbook, results);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        File outputFile = new File(outputDir, "performance-results_" + timestamp + ".xlsx");
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            workbook.write(fos);
            workbook.close();
        } catch (IOException e) {
            throw new RuntimeException("Result file could not be written: " + e.getMessage(), e);
        }

        System.out.println("Excel file written to: " + outputFile.getAbsolutePath());
    }

    private static void populateSheet(Workbook workbook, List<TestResult> results) {
        Sheet sheet = createHeader(workbook);

        int rowIndex = 2;
        for (TestResult result : results) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(result.getTestName());
            row.createCell(1).setCellValue(result.getProductName());

            boolean isFirstIteration = true;
            for (IndividualTestResult individualTestResult : result.getResults()) {
                if (!isFirstIteration) {
                    row = sheet.createRow(rowIndex++);
                }
                row.createCell(2).setCellValue(individualTestResult.getTestName());

                double resultDiMap = individualTestResult.getResultDiMap();
                double resultZNAP = individualTestResult.getResultZNAP();
                row.createCell(3).setCellValue(resultDiMap);
                row.createCell(4).setCellValue(resultZNAP);
                row.createCell(5).setCellValue(individualTestResult.getUnit().getName());

                CellStyle style = workbook.createCellStyle();
                style.setFillForegroundColor(
                        resultZNAP < resultDiMap
                                ? IndexedColors.GREEN.getIndex()
                                : resultZNAP == resultDiMap
                                ? IndexedColors.GREY_40_PERCENT.getIndex()
                                : IndexedColors.RED.getIndex()
                );
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                Cell indicatorCell = row.createCell(6);
                indicatorCell.setCellStyle(style);

                isFirstIteration = false;
            }
            rowIndex++;
        }

        setCoulumWidths(sheet);
    }

    private static Sheet createHeader(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Performance Results");

        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        Row header = sheet.createRow(0);

        for (int ii = 0; ii < ExcelWriter.headerTitles.length; ii++) {
            Cell cell = header.createCell(ii);
            cell.setCellStyle(headerStyle);
            cell.setCellValue(ExcelWriter.headerTitles[ii]);
        }

        return sheet;
    }

    private static void setCoulumWidths(Sheet sheet) {
        int[] columnWidths = {25 * 256, 80 * 256, 25 * 256, 20 * 256, 20 * 256, 10 * 256, 40 * 256};
        for (int ii = 0; ii < columnWidths.length; ii++) {
            sheet.setColumnWidth(ii, columnWidths[ii]);
        }
    }
}
