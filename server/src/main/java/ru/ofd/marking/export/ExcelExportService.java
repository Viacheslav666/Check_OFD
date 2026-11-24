package ru.ofd.marking.export;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import ru.ofd.marking.service.MetricsService;
import ru.ofd.marking.web.dto.GroupStatsDto;
import ru.ofd.marking.web.dto.MatrixCellDto;
import ru.ofd.marking.web.dto.TradePointStatsDto;

@Service
public class ExcelExportService {

    private final MetricsService metricsService;

    public ExcelExportService(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    public byte[] exportTradePoints(Instant from, Instant to) {
        List<TradePointStatsDto> rows = metricsService.tradePointStats(from, to);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("ТТ");
            Row h = sheet.createRow(0);
            String[] headers = {"ТТ", "Адрес", "Место", "Чеков", "С маркировкой", "%"};
            for (int i = 0; i < headers.length; i++) h.createCell(i).setCellValue(headers[i]);
            int r = 1;
            for (TradePointStatsDto s : rows) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(s.displayName());
                row.createCell(1).setCellValue(s.address());
                row.createCell(2).setCellValue(s.place());
                row.createCell(3).setCellValue(s.totalReceipts());
                row.createCell(4).setCellValue(s.markedReceipts());
                row.createCell(5).setCellValue(s.markedPercent());
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Excel export failed", e);
        }
    }

    public byte[] exportGroups(Instant from, Instant to) {
        List<GroupStatsDto> rows = metricsService.groupStats(from, to);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Группы");
            Row h = sheet.createRow(0);
            String[] headers = {"Группа", "Позиций", "Маркированных", "%"};
            for (int i = 0; i < headers.length; i++) h.createCell(i).setCellValue(headers[i]);
            int r = 1;
            for (GroupStatsDto s : rows) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(s.groupTitle());
                row.createCell(1).setCellValue(s.totalItems());
                row.createCell(2).setCellValue(s.markedItems());
                row.createCell(3).setCellValue(s.markedPercent());
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Excel export failed", e);
        }
    }

    public byte[] exportMatrix(Instant from, Instant to) {
        List<MatrixCellDto> rows = metricsService.matrix(from, to);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Матрица");
            Row h = sheet.createRow(0);
            String[] headers = {"ТТ", "Группа", "Позиций", "Маркированных", "%"};
            for (int i = 0; i < headers.length; i++) h.createCell(i).setCellValue(headers[i]);
            int r = 1;
            for (MatrixCellDto s : rows) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(s.tradePointName());
                row.createCell(1).setCellValue(s.groupTitle());
                row.createCell(2).setCellValue(s.totalItems());
                row.createCell(3).setCellValue(s.markedItems());
                row.createCell(4).setCellValue(s.markedPercent());
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Excel export failed", e);
        }
    }
}
