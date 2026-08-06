package com.tuckshop.pos.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ReportExportService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd MMM, hh:mm a");

    @SuppressWarnings("unchecked")
    public byte[] toExcel(Map<String, Object> report) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // ---- Summary sheet ----
            Sheet summary = workbook.createSheet("Summary");
            int r = 0;
            writeRow(summary, r++, headerStyle, "Tuck Shop POS - Sales Report");
            writeRow(summary, r++, null, "Period", report.get("from") + " to " + report.get("to"));
            r++;
            writeRow(summary, r++, headerStyle, "Metric", "Value");
            writeRow(summary, r++, null, "Total revenue (incl. khata)", String.valueOf(report.get("totalRevenue")));
            writeRow(summary, r++, null, "Cash collected", String.valueOf(report.get("cashCollected")));
            writeRow(summary, r++, null, "Total cost value", String.valueOf(report.get("totalCostValue")));
            writeRow(summary, r++, null, "Total profit", String.valueOf(report.get("totalProfit")));
            writeRow(summary, r++, null, "Transactions", String.valueOf(report.get("totalTransactions")));
            writeRow(summary, r++, null, "Items sold", String.valueOf(report.get("totalItemsSold")));
            autoSize(summary, 2);

            // ---- Transactions sheet ----
            Sheet txSheet = workbook.createSheet("Transactions");
            int tr = 0;
            writeRow(txSheet, tr++, headerStyle, "Sale #", "Date", "Items", "Total", "Payment method", "Cashier", "Customer");
            List<Map<String, Object>> transactions = (List<Map<String, Object>>) report.get("transactions");
            for (Map<String, Object> t : transactions) {
                Object dateObj = t.get("date");
                String dateStr = dateObj instanceof LocalDateTime ? ((LocalDateTime) dateObj).format(DT_FMT) : String.valueOf(dateObj);
                writeRow(txSheet, tr++, null,
                        String.valueOf(t.get("id")), dateStr, String.valueOf(t.get("itemCount")),
                        String.valueOf(t.get("total")), String.valueOf(t.get("paymentMethod")),
                        String.valueOf(t.get("cashier")), t.get("customer") == null ? "" : String.valueOf(t.get("customer")));
            }
            autoSize(txSheet, 7);

            // ---- Top products sheet ----
            Sheet productsSheet = workbook.createSheet("Product profitability");
            int pr = 0;
            writeRow(productsSheet, pr++, headerStyle, "Product", "Qty sold", "Cost value", "Selling value", "Profit");
            List<Map<String, Object>> topProducts = (List<Map<String, Object>>) report.get("topProducts");
            for (Map<String, Object> p : topProducts) {
                writeRow(productsSheet, pr++, null, String.valueOf(p.get("name")), String.valueOf(p.get("qty")),
                        String.valueOf(p.get("costValue")), String.valueOf(p.get("revenue")), String.valueOf(p.get("profit")));
            }
            autoSize(productsSheet, 5);

            // ---- By customer (khata) sheet ----
            Sheet custSheet = workbook.createSheet("Khata customers");
            int cr = 0;
            writeRow(custSheet, cr++, headerStyle, "Customer", "Total bought on credit");
            List<Map<String, Object>> byCustomer = (List<Map<String, Object>>) report.get("byCustomer");
            for (Map<String, Object> c : byCustomer) {
                writeRow(custSheet, cr++, null, String.valueOf(c.get("customer")), String.valueOf(c.get("total")));
            }
            autoSize(custSheet, 2);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void writeRow(Sheet sheet, int rowIndex, CellStyle style, String... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(values[i]);
            if (style != null) cell.setCellStyle(style);
        }
    }

    private void autoSize(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    @SuppressWarnings("unchecked")
    public byte[] toPdf(Map<String, Object> report) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDPageContentStream cs = new PDPageContentStream(doc, page);
            float margin = 50;
            float y = page.getMediaBox().getHeight() - margin;
            float lineHeight = 16;

            cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
            cs.beginText();
            cs.newLineAtOffset(margin, y);
            cs.showText("Tuck Shop POS - Sales Report");
            cs.endText();
            y -= lineHeight * 1.5f;

            cs.setFont(PDType1Font.HELVETICA, 11);
            y = writeLine(cs, margin, y, lineHeight, "Period: " + report.get("from") + " to " + report.get("to"));
            y -= lineHeight * 0.5f;

            cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
            y = writeLine(cs, margin, y, lineHeight, "Summary");
            cs.setFont(PDType1Font.HELVETICA, 11);
            y = writeLine(cs, margin, y, lineHeight, "Total revenue (incl. khata): Rs " + report.get("totalRevenue"));
            y = writeLine(cs, margin, y, lineHeight, "Cash collected: Rs " + report.get("cashCollected"));
            y = writeLine(cs, margin, y, lineHeight, "Total cost value: Rs " + report.get("totalCostValue"));
            y = writeLine(cs, margin, y, lineHeight, "Total profit: Rs " + report.get("totalProfit"));
            y = writeLine(cs, margin, y, lineHeight, "Transactions: " + report.get("totalTransactions"));
            y = writeLine(cs, margin, y, lineHeight, "Items sold: " + report.get("totalItemsSold"));
            y -= lineHeight;

            cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
            y = writeLine(cs, margin, y, lineHeight, "Product profitability");
            cs.setFont(PDType1Font.HELVETICA, 10);
            List<Map<String, Object>> topProducts = (List<Map<String, Object>>) report.get("topProducts");
            for (Map<String, Object> p : topProducts) {
                if (y < margin + lineHeight * 4) {
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = page.getMediaBox().getHeight() - margin;
                    cs.setFont(PDType1Font.HELVETICA, 10);
                }
                y = writeLine(cs, margin, y, lineHeight,
                        p.get("name") + " - qty " + p.get("qty") + " - cost Rs " + p.get("costValue")
                                + " - sold Rs " + p.get("revenue") + " - profit Rs " + p.get("profit"));
            }
            y -= lineHeight;

            List<Map<String, Object>> byCustomer = (List<Map<String, Object>>) report.get("byCustomer");
            if (!byCustomer.isEmpty()) {
                if (y < margin + lineHeight * (4 + byCustomer.size())) {
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = page.getMediaBox().getHeight() - margin;
                }
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                y = writeLine(cs, margin, y, lineHeight, "Khata sales by customer");
                cs.setFont(PDType1Font.HELVETICA, 10);
                for (Map<String, Object> c : byCustomer) {
                    y = writeLine(cs, margin, y, lineHeight, c.get("customer") + " - Rs " + c.get("total"));
                }
            }

            cs.close();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private float writeLine(PDPageContentStream cs, float x, float y, float lineHeight, String text) throws IOException {
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(text));
        cs.endText();
        return y - lineHeight;
    }

    // PDFBox's base Helvetica font can't encode characters outside WinAnsi -
    // strip anything unsupported so a stray character never crashes the export
    private String sanitize(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            sb.append(c < 256 ? c : '?');
        }
        return sb.toString();
    }
}
