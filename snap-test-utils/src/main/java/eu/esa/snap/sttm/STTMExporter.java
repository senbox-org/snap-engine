package eu.esa.snap.sttm;


import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

class STTMExporter {

    private static final int MODULE_COL = 0;
    private static final int PACKAGE_COL = 1;
    private static final int CLASS_COL = 2;
    private static final int METHOD_COL = 3;
    private static final int ISSUE_COL = 4;

    private static XSSFWorkbook createWorkbook(List<STTMInfo> sttmInfos) {
        final XSSFWorkbook workbook = new XSSFWorkbook();
        final XSSFSheet sheet = workbook.createSheet("STTM");

        final XSSFCellStyle titleRowStyle = createTitleRowStyle(workbook);
        createTitleRow(sheet, titleRowStyle);

        // @todo 1 tb/tb extract method and write test 2023-11-24
        XSSFCellStyle hlinkstyle = workbook.createCellStyle();
        XSSFFont hlinkfont = workbook.createFont();
        hlinkfont.setUnderline(XSSFFont.U_SINGLE);
        hlinkfont.setColor(IndexedColors.BLUE.index);
        hlinkstyle.setFont(hlinkfont);

        int rowIndex = 1;
        for (final STTMInfo sttmInfo : sttmInfos) {
            createDataRow(sheet, rowIndex, sttmInfo, hlinkstyle);
            rowIndex++;
        }

        // make column width fit to content tb 2023-11-24
        for (int i = 0; i < 5; i++) {
            sheet.autoSizeColumn(i);
        }

        return workbook;
    }

    static void createDataRow(XSSFSheet sheet, int rowIndex, STTMInfo sttmInfo, XSSFCellStyle hlinkStyle) {
        final XSSFRow dataRow = sheet.createRow(rowIndex);
        final XSSFCreationHelper creationHelper = sheet.getWorkbook().getCreationHelper();

        XSSFCell cell = dataRow.createCell(MODULE_COL, CellType.STRING);
        cell.setCellValue(sttmInfo.module);

        cell = dataRow.createCell(PACKAGE_COL, CellType.STRING);
        cell.setCellValue(sttmInfo.pckg);

        cell = dataRow.createCell(CLASS_COL, CellType.STRING);
        cell.setCellValue(sttmInfo.clazz);

        cell = dataRow.createCell(METHOD_COL, CellType.STRING);
        cell.setCellValue(sttmInfo.method);

        cell = dataRow.createCell(ISSUE_COL, CellType.STRING);
        final XSSFHyperlink hyperlink = creationHelper.createHyperlink(HyperlinkType.URL);
        hyperlink.setAddress(sttmInfo.jiraUrl);
        cell.setHyperlink(hyperlink);
        cell.setCellValue(sttmInfo.jiraIssue);
        cell.setCellStyle(hlinkStyle);
    }

    static void createTitleRow(XSSFSheet sheet, XSSFCellStyle titleRowStyle) {
        final XSSFRow titleRow = sheet.createRow(0);

        XSSFCell cell = createTitleRowCell(titleRowStyle, titleRow, MODULE_COL);
        cell.setCellValue("Module");

        cell = createTitleRowCell(titleRowStyle, titleRow, PACKAGE_COL);
        cell.setCellValue("Package");

        cell = createTitleRowCell(titleRowStyle, titleRow, CLASS_COL);
        cell.setCellValue("Class");

        cell = createTitleRowCell(titleRowStyle, titleRow, METHOD_COL);
        cell.setCellValue("Method");

        cell = createTitleRowCell(titleRowStyle, titleRow, ISSUE_COL);
        cell.setCellValue("Issue");
    }

    private static XSSFCell createTitleRowCell(XSSFCellStyle titleRowStyle, XSSFRow titleRow, int column) {
        final XSSFCell cell = titleRow.createCell(column, CellType.STRING);
        cell.setCellStyle(titleRowStyle);
        return cell;
    }

    static XSSFCellStyle createTitleRowStyle(XSSFWorkbook workbook) {
        final XSSFCellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return cellStyle;
    }

    static String createFileName() {
        final Date now = new Date();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd");
        final String dateString = dateFormat.format(now);
        return "STTM_report_" + dateString + ".xlsx";
    }

    void writeTo(Path path, List<STTMInfo> sttmInfos) throws IOException {
        final XSSFWorkbook workbook = createWorkbook(sttmInfos);

        final String fileName = createFileName();

        // create file
        final Path outputFile = path.resolve(fileName);
        // @todo 1 check if exists and throw if so tb 2023-11-23
        Files.createFile(outputFile);

        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile.toFile())) {
            workbook.write(fileOutputStream);
        }
    }
}
