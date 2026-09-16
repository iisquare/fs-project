package com.iisquare.fs.web.agent.tool;

import com.iisquare.fs.base.core.util.FileUtil;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.sl.usermodel.Shape;
import org.apache.poi.sl.usermodel.Slide;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.sl.usermodel.SlideShowFactory;
import org.apache.poi.sl.usermodel.TextShape;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFPictureData;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.geom.Point2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 文档解析工具：将 txt/markdown/doc/docx/xls/xlsx/pdf/ppt/pptx 等文件解析为 Markdown 文本
 * 同时抽取文档中的图片（图谱、图表等），插入位置以 {{kb-image:i}} 占位符标记：
 * PDF 紧随所在页、docx 紧随所在段落、pptx 紧随所在幻灯片、xlsx 紧随所在工作表，
 * 页眉页脚、文本框、版式母版、图表等无法定位的图片追加在文末，避免丢失
 */
public class DocumentParser {

    private static final Set<String> SUFFIXES = new LinkedHashSet<>(Arrays.asList(
            "txt", "md", "markdown", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx"
    ));

    private static final Set<String> RASTER_TYPES = new LinkedHashSet<>(Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp"
    ));

    private static final int MIN_IMAGE_SIZE = 32; // 过滤装饰线与小图标

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
     * 按文件名后缀解析输入流
     */
    public static ParsedDocument parse(String filename, InputStream stream) throws IOException {
        String suffix = suffix(filename);
        if (!SUFFIXES.contains(suffix)) {
            throw new IllegalArgumentException("不支持的文件类型:" + suffix);
        }
        switch (suffix) {
            case "txt":
            case "md":
            case "markdown":
                return text(FileUtil.getContent(stream, StandardCharsets.UTF_8));
            case "pdf":
                return parsePdf(stream);
            case "doc":
                return text(parseDoc(stream));
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

    private static ParsedDocument text(String markdown) {
        return ParsedDocument.builder().markdown(markdown).images(new ArrayList<>()).build();
    }

    /**
     * PDF 解析：逐页提取文本，并按图片在页面上的纵坐标插入到对应段落之间
     */
    private static ParsedDocument parsePdf(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        List<ParsedImage> images = new ArrayList<>();
        Map<String, String> located = new LinkedHashMap<>();
        try (PDDocument document = PDDocument.load(stream)) {
            LineStripper stripper = new LineStripper();
            int pages = document.getNumberOfPages();
            for (int i = 1; i <= pages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                stripper.resetLines();
                String text = stripper.getText(document);
                String content = insertImages(document.getPage(i - 1), null == text ? "" : text,
                        stripper.lines(), images, located, i);
                if (content.isBlank()) continue;
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(content.strip());
            }
        }
        return ParsedDocument.builder().markdown(sb.toString()).images(images).build();
    }

    /**
     * 按图片纵坐标把占位符插入到页面文本中：插入点为其下方第一行文本之前，没有则追加到页末
     */
    private static String insertImages(PDPage page, String text, List<TextLine> lines,
                                       List<ParsedImage> images, Map<String, String> located, int pageNo) {
        PageImageLocator locator = new PageImageLocator(page);
        try {
            locator.processPage(page);
        } catch (IOException e) {
            return text;
        }
        if (locator.images().isEmpty()) return text;
        int lead = text.length() - text.stripLeading().length();
        float height = page.getMediaBox().getHeight();
        List<Insertion> insertions = new ArrayList<>();
        for (PlacedImage placed : locator.images()) {
            PDImageXObject image = placed.image;
            if (image.isStencil()) continue;
            if (image.getWidth() < MIN_IMAGE_SIZE || image.getHeight() < MIN_IMAGE_SIZE) continue;
            String extension = image.getSuffix();
            String suffix = suffixOfType(extension);
            String type = typeOfSuffix(suffix);
            byte[] data;
            try {
                if (RASTER_TYPES.contains(type)) {
                    data = image.createInputStream().readAllBytes();
                } else {
                    data = encodePng(image.getImage());
                    suffix = ".png";
                    type = "image/png";
                }
            } catch (IOException e) {
                continue;
            }
            String marker = locateImage(images, located, "page-" + pageNo + suffix, suffix, data, pageNo);
            if (marker.isEmpty()) continue;
            float bottom = height - placed.y;
            int offset = text.length();
            for (TextLine line : lines) {
                if (line.y > bottom) {
                    offset = Math.max(0, line.offset - lead);
                    break;
                }
            }
            insertions.add(new Insertion(offset, marker));
        }
        if (insertions.isEmpty()) return text;
        insertions.sort((left, right) -> Integer.compare(right.offset, left.offset));
        StringBuilder result = new StringBuilder(text);
        for (Insertion insertion : insertions) {
            int offset = Math.min(insertion.offset, result.length());
            result.insert(offset, "\n\n" + insertion.marker + "\n\n");
        }
        return result.toString();
    }

    private static byte[] encodePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", stream);
        return stream.toByteArray();
    }

    /**
     * 旧版 doc 解析：仅提取纯文本（不支持图片抽取）
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
     * docx 解析：保留标题层级与表格结构，文档内图片追加在文末
     */
    private static ParsedDocument parseDocx(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        List<ParsedImage> images = new ArrayList<>();
        try (XWPFDocument document = new XWPFDocument(stream)) {
            Map<String, String> located = new LinkedHashMap<>();
            for (IBodyElement element : document.getBodyElements()) {
                if (BodyElementType.PARAGRAPH == element.getElementType()) {
                    XWPFParagraph paragraph = (XWPFParagraph) element;
                    StringBuilder part = new StringBuilder();
                    String line = paragraphToMarkdown(paragraph);
                    if (null != line) part.append(line);
                    appendParagraphPictures(paragraph, images, located, part);
                    if (part.length() < 1) continue;
                    if (sb.length() > 0) sb.append("\n\n");
                    sb.append(part);
                } else if (BodyElementType.TABLE == element.getElementType()) {
                    String table = tableToMarkdown((XWPFTable) element);
                    if (null == table) continue;
                    if (sb.length() > 0) sb.append("\n\n");
                    sb.append(table);
                }
            }
            // 页眉页脚、文本框等不在正文段落中的图片追加在文末，避免丢失
            for (XWPFPictureData picture : document.getAllPictures()) {
                appendUnlocatedPicture(images, located, sb, picture.getFileName(),
                        picture.suggestFileExtension(), picture.getData());
            }
        }
        return ParsedDocument.builder().markdown(sb.toString()).images(images).build();
    }

    /**
     * 段落内的图片紧随该段落之后，同一张图重复引用时复用同一记录
     */
    private static void appendParagraphPictures(XWPFParagraph paragraph, List<ParsedImage> images,
                                                Map<String, String> located, StringBuilder part) {
        for (XWPFRun run : paragraph.getRuns()) {
            for (XWPFPicture picture : run.getEmbeddedPictures()) {
                XWPFPictureData data = picture.getPictureData();
                appendPicture(images, located, part, data.getFileName(),
                        data.suggestFileExtension(), data.getData(), 0);
            }
        }
    }

    /**
     * 注册图片并把占位符追加到指定位置
     */
    private static boolean appendPicture(List<ParsedImage> images, Map<String, String> located,
                                         StringBuilder target, String name, String extension, byte[] data, int page) {
        String marker = locateImage(images, located, name, suffixOfExtension(extension), data, page);
        if (marker.isEmpty()) return false;
        if (target.length() > 0) target.append("\n\n");
        target.append(marker);
        return true;
    }

    /**
     * 追加尚未定位过的图片，已在正文中定位的图片跳过，避免重复出现
     */
    private static void appendUnlocatedPicture(List<ParsedImage> images, Map<String, String> located,
                                               StringBuilder target, String name, String extension, byte[] data) {
        if (null == data || data.length == 0) return;
        if (located.containsKey(signature(suffixOfExtension(extension), data))) return;
        appendPicture(images, located, target, name, extension, data, 0);
    }

    /**
     * 注册图片并返回占位符，同一张图在文档中重复引用时复用同一记录
     */
    private static String locateImage(List<ParsedImage> images, Map<String, String> located,
                                      String name, String suffix, byte[] data, int page) {
        if (null == data || data.length == 0) return "";
        String key = signature(suffix, data);
        String marker = located.get(key);
        if (null == marker) {
            marker = registerImage(images, name, suffix, typeOfSuffix(suffix), data, page);
            located.put(key, marker);
        }
        return marker;
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
     * 表格解析：逐 sheet 输出标题与 Markdown 表格，工作簿图片追加在文末
     */
    private static ParsedDocument parseSheet(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        List<ParsedImage> images = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(stream)) {
            DataFormatter formatter = new DataFormatter();
            Map<String, String> located = new LinkedHashMap<>();
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
                // 工作表图片紧随该表内容之后
                if (sheet instanceof XSSFSheet xssfSheet) {
                    XSSFDrawing drawing = xssfSheet.getDrawingPatriarch();
                    if (null != drawing) {
                        for (XSSFShape shape : drawing.getShapes()) {
                            if (!(shape instanceof XSSFPicture picture)) continue;
                            XSSFPictureData data = picture.getPictureData();
                            appendPicture(images, located, sb, null, data.suggestFileExtension(), data.getData(), 0);
                        }
                    }
                }
            }
            // 图表、页眉页脚等位置的图片追加在文末，避免丢失
            if (workbook instanceof XSSFWorkbook xssf) {
                for (XSSFPictureData picture : xssf.getAllPictures()) {
                    appendUnlocatedPicture(images, located, sb, null, picture.suggestFileExtension(), picture.getData());
                }
            }
        }
        return ParsedDocument.builder().markdown(sb.toString()).images(images).build();
    }

    /**
     * 幻灯片解析：逐页输出标题与文本框内容，演示文稿图片追加在文末
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ParsedDocument parseSlide(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        List<ParsedImage> images = new ArrayList<>();
        try (SlideShow<?, ?> slideShow = SlideShowFactory.create(stream)) {
            Map<String, String> located = new LinkedHashMap<>();
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
                // 幻灯片图片紧随该页内容之后
                for (Shape shape : slide.getShapes()) {
                    if (!(shape instanceof XSLFPictureShape pictureShape)) continue;
                    XSLFPictureData data = pictureShape.getPictureData();
                    appendPicture(images, located, sb, data.getFileName(),
                            data.suggestFileExtension(), data.getData(), i + 1);
                }
            }
            // 版式、母版等位置的图片追加在文末，避免丢失
            if (slideShow instanceof XMLSlideShow xml) {
                for (XSLFPictureData picture : xml.getPictureData()) {
                    appendUnlocatedPicture(images, located, sb, picture.getFileName(),
                            picture.suggestFileExtension(), picture.getData());
                }
            }
        }
        return ParsedDocument.builder().markdown(sb.toString()).images(images).build();
    }

    /**
     * 记录图片并返回占位符，插入位置由调用方决定，不支持的类型返回空串
     */
    private static String registerImage(List<ParsedImage> images, String name,
                                        String suffix, String type, byte[] data, int page) {
        if (null == data || data.length == 0) return "";
        String contentType = type;
        String fileSuffix = suffix;
        if (!RASTER_TYPES.contains(contentType)) {
            contentType = typeOfSuffix(fileSuffix);
            if (contentType.isEmpty()) return ""; // 暂不支持的类型直接忽略
        }
        int index = images.size();
        String alt = StringUtils.hasText(name) ? name : ("图片" + (index + 1));
        images.add(ParsedImage.builder()
                .name(name).suffix(fileSuffix).type(contentType).data(data).page(page).alt(alt).build());
        return "{{kb-image:" + index + "}}";
    }

    /**
     * 图片去重标识，同一张图在文档中多次引用时只入库一次
     */
    private static String signature(String suffix, byte[] data) {
        return (null == suffix ? "" : suffix) + ":" + data.length + ":" + Arrays.hashCode(data);
    }

    /**
     * 由 POI 提供的扩展名推断后缀（含点）
     */
    private static String suffixOfExtension(String extension) {
        if (null == extension || extension.isEmpty()) return "";
        String suffix = "." + extension.toLowerCase();
        return suffix.matches("^\\.[a-z0-9]+$") ? suffix : "";
    }

    /**
     * 由内容类型推断后缀（含点）
     */
    private static String suffixOfType(String type) {
        if (null == type) return "";
        switch (type.toLowerCase()) {
            case "jpg":
            case "jpeg":
            case "image/jpeg":
                return ".jpg";
            case "png":
            case "image/png":
                return ".png";
            case "gif":
            case "image/gif":
                return ".gif";
            case "bmp":
            case "image/bmp":
                return ".bmp";
            case "webp":
            case "image/webp":
                return ".webp";
            default:
                return "";
        }
    }

    /**
     * 由后缀推断内容类型
     */
    private static String typeOfSuffix(String suffix) {
        if (null == suffix) return "";
        switch (suffix.toLowerCase()) {
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            case ".gif":
                return "image/gif";
            case ".bmp":
                return "image/bmp";
            case ".webp":
                return "image/webp";
            default:
                return "";
        }
    }

    /**
     * 文本行：在页面文本中的偏移与自顶向下的纵坐标
     */
    private static class TextLine {
        final int offset;
        final float y;

        TextLine(int offset, float y) {
            this.offset = offset;
            this.y = y;
        }
    }

    /**
     * 图片在页面中的位置（PDF 坐标系，原点在左下角）
     */
    private static class PlacedImage {
        final PDImageXObject image;
        final float x;
        final float y;
        final float width;
        final float height;

        PlacedImage(PDImageXObject image, float x, float y, float width, float height) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    /**
     * 待插入的占位符及其在页面文本中的偏移
     */
    private static class Insertion {
        final int offset;
        final String marker;

        Insertion(int offset, String marker) {
            this.offset = offset;
            this.marker = marker;
        }
    }

    /**
     * 文本提取器：保持 PDFTextStripper 原有的文本质量，仅额外记录每行的偏移与纵坐标
     */
    private static class LineStripper extends PDFTextStripper {

        private final List<TextLine> lines = new ArrayList<>();

        LineStripper() throws IOException {
            super();
        }

        void resetLines() {
            lines.clear();
        }

        List<TextLine> lines() {
            return lines;
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            int offset = outputLength();
            super.writeString(text, textPositions);
            if (offset >= 0 && !textPositions.isEmpty()) {
                lines.add(new TextLine(offset, textPositions.get(0).getYDirAdj()));
            }
        }

        /**
         * 当前页面文本长度，非字符串输出流时返回 -1，图片退化为追加到页末
         */
        private int outputLength() {
            Writer writer = getOutput();
            return writer instanceof StringWriter ? ((StringWriter) writer).getBuffer().length() : -1;
        }

    }

    /**
     * 图片定位器：遍历页面内容流，记录每个图片对象绘制时的位置与尺寸
     */
    private static class PageImageLocator extends PDFGraphicsStreamEngine {

        private final List<PlacedImage> images = new ArrayList<>();

        PageImageLocator(PDPage page) {
            super(page);
        }

        List<PlacedImage> images() {
            return images;
        }

        @Override
        public void drawImage(PDImage pdImage) {
            if (!(pdImage instanceof PDImageXObject image)) return;
            Matrix matrix = getGraphicsState().getCurrentTransformationMatrix();
            images.add(new PlacedImage(image, matrix.getTranslateX(), matrix.getTranslateY(),
                    Math.abs(matrix.getScalingFactorX()), Math.abs(matrix.getScalingFactorY())));
        }

        @Override
        public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
        }

        @Override
        public void clip(int windingRule) {
        }

        @Override
        public void moveTo(float x, float y) {
        }

        @Override
        public void lineTo(float x, float y) {
        }

        @Override
        public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        }

        @Override
        public Point2D getCurrentPoint() {
            return new Point2D.Float();
        }

        @Override
        public void closePath() {
        }

        @Override
        public void endPath() {
        }

        @Override
        public void strokePath() {
        }

        @Override
        public void fillPath(int windingRule) {
        }

        @Override
        public void fillAndStrokePath(int windingRule) {
        }

        @Override
        public void shadingFill(COSName shadingName) {
        }

    }

}
