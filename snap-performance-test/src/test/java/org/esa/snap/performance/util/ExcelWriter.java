package org.esa.snap.performance.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.esa.snap.performance.actions.ActionName;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

public class ExcelWriter {

    private final static String[] headerTitles = {
            "Test Name",
            "Product Name(s)",
            "Individual Test Measure",
            "Result 1",
            "Result 2",
            "Unit",
            "Performance Indicator (Result 2 > Result 1)"
    };
    private final static Logger logger = Logger.getLogger(ExcelWriter.class.getName());

    public void writeResults(String outputDir, List<PerformanceTestResult> allResults) {
        File outputFile = new File(outputDir + "/" + TestUtils.RESULTS_DIR);
        writeResultsToExcel(outputFile, allResults);
    }

    private static void writeResultsToExcel(File outputDir, List<PerformanceTestResult> results) {
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

        logger.info("Excel file written to: " + outputFile.getAbsolutePath());
    }

    private static void populateSheet(Workbook workbook, List<PerformanceTestResult> results) {
        Sheet sheet = createHeader(workbook);

        int rowIndex = 2;

        for (PerformanceTestResult result : results) {
            rowIndex = writeTestResult(result, workbook, sheet, rowIndex);
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

        for (int ii = 0; ii < headerTitles.length; ii++) {
            Cell cell = header.createCell(ii);
            cell.setCellStyle(headerStyle);
            cell.setCellValue(headerTitles[ii]);
        }

        return sheet;
    }

    private static void setCoulumWidths(Sheet sheet) {
        int[] columnWidths = {30 * 256, 80 * 256, 25 * 256, 20 * 256, 20 * 256, 10 * 256, 40 * 256};
        for (int ii = 0; ii < columnWidths.length; ii++) {
            sheet.setColumnWidth(ii, columnWidths[ii]);
        }
    }

    private static int writeTestResult(PerformanceTestResult result, Workbook workbook, Sheet sheet, int rowIndex) {
        Row row = sheet.createRow(rowIndex++);
        row.createCell(3).setCellValue(result.getFormat1());
        row.createCell(4).setCellValue(result.getFormat2());

        row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue(result.getTestName());

        for (int ii = 0; ii < result.getResult1().size(); ii++) {
            if (ii > 0) {
                row = sheet.createRow(rowIndex++);
            }
            if (ii == 0) {
                String[] splitProduct = result.getProduct1().split("/");
                row.createCell(1).setCellValue(splitProduct[splitProduct.length - 1]);
            }
            if (ii == 1) {
                String[] splitProduct = result.getProduct2().split("/");
                row.createCell(1).setCellValue(splitProduct[splitProduct.length - 1]);
            }

            row.createCell(2).setCellValue(result.getDescriptions().get(ii));
            row.createCell(3).setCellValue(result.getResult1().get(ii));
            row.createCell(4).setCellValue(result.getResult2().get(ii));
            row.createCell(5).setCellValue(result.getUnits().get(ii));

            CellStyle style = getCellStyle(workbook, result.getResult1().get(ii), result.getResult2().get(ii), result.getDescriptions().get(ii));
            Cell indicatorCell = row.createCell(6);
            indicatorCell.setCellStyle(style);
        }
        rowIndex++;
        return rowIndex;
    }

    private static CellStyle getCellStyle(Workbook workbook, double result1, double result2, String description) {
        CellStyle style = workbook.createCellStyle();
        boolean twoIsSmaller = result2 < result1;
        boolean areEqual = result2 == result1;

        short colorIndex;
        if (description.contains(ActionName.THROUGHPUT.getName())) {
            colorIndex = twoIsSmaller ? IndexedColors.RED.getIndex() :
                    areEqual
                    ? IndexedColors.GREY_40_PERCENT.getIndex()
                    : IndexedColors.GREEN.getIndex();
        } else {
            colorIndex = twoIsSmaller ? IndexedColors.GREEN.getIndex() :
                    areEqual
                    ? IndexedColors.GREY_40_PERCENT.getIndex()
                    : IndexedColors.RED.getIndex();
        }

        style.setFillForegroundColor(colorIndex);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
