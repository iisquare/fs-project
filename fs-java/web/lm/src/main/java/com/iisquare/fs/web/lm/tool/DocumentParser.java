package com.iisquare.fs.web.lm.tool;

import com.iisquare.fs.base.core.util.FileUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.sl.usermodel.Shape;
import org.apache.poi.sl.usermodel.Slide;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.sl.usermodel.SlideShowFactory;
import org.apache.poi.sl.usermodel.TextShape;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 文档解析工具：将 txt/markdown/doc/docx/xls/xlsx/pdf/ppt/pptx 等文件解析为 Markdown 文本
 */
public class DocumentParser {

    private static final Set<String> SUFFIXES = new LinkedHashSet<>(Arrays.asList(
            "txt", "md", "markdown", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx"
    ));

    public static Set<String> supportedSuffixes() {
        return SUFFIXES;
    }

    /**
     * 获取文件后缀（小写，不含点）
     */
    public static String suffix(String filename) {
        if (null == filename) return "";
        int index = filename.lastIndexOf(".");
        if (-1 == index) return "";
        return filename.substring(index + 1).toLowerCase();
    }

    public static boolean supported(String filename) {
        return SUFFIXES.contains(suffix(filename));
    }

    /**
     * 按文件名后缀解析输入流为 Markdown 文本
     */
    public static String parse(String filename, InputStream stream) throws IOException {
        String suffix = suffix(filename);
        if (!SUFFIXES.contains(suffix)) {
            throw new IllegalArgumentException("不支持的文件类型:" + suffix);
        }
        switch (suffix) {
            case "txt":
            case "md":
            case "markdown":
                return FileUtil.getContent(stream, StandardCharsets.UTF_8);
            case "pdf":
                return parsePdf(stream);
            case "doc":
                return parseDoc(stream);
            case "docx":
                return parseDocx(stream);
            case "xls":
            case "xlsx":
                return parseSheet(stream);
            case "ppt":
            case "pptx":
                return parseSlide(stream);
            default:
                throw new IllegalArgumentException("不支持的文件类型:" + suffix);
        }
    }

    /**
     * PDF 解析：逐页提取文本，页间空行分隔
     */
    private static String parsePdf(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (PDDocument document = PDDocument.load(stream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            int pages = document.getNumberOfPages();
            for (int i = 1; i <= pages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = stripper.getText(document);
                if (StringUtils.hasText(text)) {
                    if (sb.length() > 0) sb.append("\n\n");
                    sb.append(text.strip());
                }
            }
        }
        return sb.toString();
    }

    /**
     * 旧版 doc 解析：仅提取纯文本
     */
    private static String parseDoc(InputStream stream) throws IOException {
        try (HWPFDocument document = new HWPFDocument(stream)) {
            WordExtractor extractor = new WordExtractor(document);
            String[] paragraphs = extractor.getParagraphText();
            StringBuilder sb = new StringBuilder();
            for (String paragraph : paragraphs) {
                if (null == paragraph) continue;
                String text = paragraph.strip();
                if (text.isEmpty()) continue;
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(text);
            }
            return sb.toString();
        }
    }

    /**
     * docx 解析：保留标题层级与表格结构
     */
    private static String parseDocx(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(stream)) {
            for (IBodyElement element : document.getBodyElements()) {
                if (BodyElementType.PARAGRAPH == element.getElementType()) {
                    String line = paragraphToMarkdown((XWPFParagraph) element);
                    if (null == line) continue;
                    if (sb.length() > 0) sb.append("\n\n");
                    sb.append(line);
                } else if (BodyElementType.TABLE == element.getElementType()) {
                    String table = tableToMarkdown((XWPFTable) element);
                    if (null == table) continue;
                    if (sb.length() > 0) sb.append("\n\n");
                    sb.append(table);
                }
            }
        }
        return sb.toString();
    }

    private static String paragraphToMarkdown(XWPFParagraph paragraph) {
        String text = paragraph.getText();
        if (null == text || text.strip().isEmpty()) return null;
        String style = paragraph.getStyle();
        if (null != style) {
            style = style.toLowerCase();
            if (style.startsWith("heading")) {
                String level = style.substring("heading".length()).trim();
                int n;
                try {
                    n = Integer.parseInt(level);
                } catch (NumberFormatException e) {
                    n = 1;
                }
                if (n < 1) n = 1;
                if (n > 6) n = 6;
                return "#".repeat(n) + " " + text.strip();
            }
        }
        return text.strip();
    }

    private static String tableToMarkdown(XWPFTable table) {
        List<XWPFTableRow> rows = table.getRows();
        if (null == rows || rows.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rows.size(); r++) {
            List<XWPFTableCell> cells = rows.get(r).getTableCells();
            sb.append("|");
            for (XWPFTableCell cell : cells) {
                sb.append(" ").append(cell.getText().strip().replace("\n", " ")).append(" |");
            }
            sb.append("\n");
            if (0 == r) {
                sb.append("|");
                for (int i = 0; i < cells.size(); i++) sb.append(" --- |");
                sb.append("\n");
            }
        }
        return sb.toString().strip();
    }

    /**
     * 表格解析：逐 sheet 输出标题与 Markdown 表格
     */
    private static String parseSheet(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (Workbook workbook = WorkbookFactory.create(stream)) {
            DataFormatter formatter = new DataFormatter();
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                if (sb.length() > 0) sb.append("\n\n");
                sb.append("## ").append(sheet.getSheetName()).append("\n\n");
                List<String> tableLines = new ArrayList<>();
                int first = sheet.getFirstRowNum();
                int last = sheet.getLastRowNum();
                int maxCols = 0;
                for (int r = first; r <= last; r++) {
                    Row row = sheet.getRow(r);
                    if (null == row) continue;
                    int cols = row.getLastCellNum();
                    if (cols > maxCols) maxCols = cols;
                    StringBuilder line = new StringBuilder("|");
                    for (int c = 0; c < cols; c++) {
                        Cell cell = row.getCell(c);
                        String value = "";
                        if (null != cell) {
                            value = formatter.formatCellValue(cell);
                        }
                        line.append(" ").append(value.strip().replace("\n", " ")).append(" |");
                    }
                    tableLines.add(line.toString());
                }
                if (tableLines.isEmpty()) continue;
                sb.append(String.join("\n", tableLines));
                if (maxCols > 0) {
                    sb.append("\n|");
                    for (int i = 0; i < maxCols; i++) sb.append(" --- |");
                }
            }
        }
        return sb.toString();
    }

    /**
     * 幻灯片解析：逐页输出标题与文本框内容
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String parseSlide(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (SlideShow<?, ?> slideShow = SlideShowFactory.create(stream)) {
            List<? extends Slide<?, ?>> slides = slideShow.getSlides();
            for (int i = 0; i < slides.size(); i++) {
                Slide<?, ?> slide = slides.get(i);
                if (sb.length() > 0) sb.append("\n\n");
                sb.append("## 第").append(i + 1).append("页\n\n");
                StringBuilder body = new StringBuilder();
                for (Shape shape : slide.getShapes()) {
                    if (!(shape instanceof TextShape)) continue;
                    TextShape<?, ?> textShape = (TextShape<?, ?>) shape;
                    String text = textShape.getText();
                    if (null == text) continue;
                    text = text.strip();
                    if (text.isEmpty()) continue;
                    if (body.length() > 0) body.append("\n\n");
                    body.append(text);
                }
                sb.append(body);
            }
        }
        return sb.toString();
    }

}
