package eu.esa.snap.sttm;

import com.bc.ceres.annotation.STTM;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;

@SuppressWarnings("resource")
public class STTMExporterTest {

    @Test
    @STTM("SNAP-3506")
    public void testCreateTitleRowStyle() {
        final XSSFWorkbook workbook = new XSSFWorkbook();

        final XSSFCellStyle style = STTMExporter.createTitleRowStyle(workbook);
        assertEquals(HorizontalAlignment.CENTER, style.getAlignment());
        assertEquals(IndexedColors.GREY_25_PERCENT.getIndex(), style.getFillForegroundColor());
        assertEquals(FillPatternType.SOLID_FOREGROUND, style.getFillPattern());
    }

    @Test
    @STTM("SNAP-3506")
    public void testCreateTitleRow() {
        final XSSFWorkbook workbook = new XSSFWorkbook();
        final XSSFSheet sheet = workbook.createSheet();

        STTMExporter.createTitleRow(sheet, workbook.createCellStyle());

        final XSSFRow row = sheet.getRow(0);
        assertNotNull(row);
        assertEquals(0, row.getFirstCellNum());
        assertEquals(5, row.getLastCellNum());

        assertEquals("Module", row.getCell(0).getStringCellValue());
        assertEquals("Package", row.getCell(1).getStringCellValue());
        assertEquals("Class", row.getCell(2).getStringCellValue());
        assertEquals("Method", row.getCell(3).getStringCellValue());
        assertEquals("Issue", row.getCell(4).getStringCellValue());
    }

    @Test
    @STTM("SNAP-3506")
    public void testCreateDataRow()  {
        final XSSFWorkbook workbook = new XSSFWorkbook();
        final XSSFSheet sheet = workbook.createSheet();

        final STTMInfo sttmInfo = new STTMInfo();
        sttmInfo.module = "mod";
        sttmInfo.pckg = "package";
        sttmInfo.clazz = "Klasse";
        sttmInfo.method = "meth";
        sttmInfo.jiraIssue = "123465";
        sttmInfo.jiraUrl = "http://bla.de";

        STTMExporter.createDataRow(sheet, 1, sttmInfo, workbook.createCellStyle());

        final XSSFRow row = sheet.getRow(1);
        assertNotNull(row);
        assertEquals(0, row.getFirstCellNum());
        assertEquals(5, row.getLastCellNum());

        assertEquals("mod", row.getCell(0).getStringCellValue());
        assertEquals("package", row.getCell(1).getStringCellValue());
        assertEquals("Klasse", row.getCell(2).getStringCellValue());
        assertEquals("meth", row.getCell(3).getStringCellValue());
        assertEquals("123465", row.getCell(4).getStringCellValue());
    }

    @Test
    @STTM("SNAP-3506")
    public void testCreateFileName()   {
        final String fileName = STTMExporter.createFileName();

        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        assertTrue(fileName.contains("STTM_report"));
        assertTrue(fileName.contains(".xlsx"));

        assertTrue(fileName.contains("_" + calendar.get(Calendar.YEAR)));
        assertTrue(fileName.contains("_" + (calendar.get(Calendar.MONTH) + 1)));
        assertTrue(fileName.contains("_" + calendar.get(Calendar.DAY_OF_MONTH)));
    }
}
