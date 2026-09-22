package com.trimly.api.util;

import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;
import lombok.extern.slf4j.Slf4j;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;

@Slf4j
public final class QrCodeGeneratorUtil {

    public static final int CANVAS_SIZE = 1000;
    public static final double MARGIN = 70.0;
    public static final double PRINTABLE_AREA = CANVAS_SIZE - 2 * MARGIN; // 860.0
    private static final double KAPPA = 0.5522847498307935; // Standard circle cubic bezier control factor

    private QrCodeGeneratorUtil() {
    }

    /**
     * Generates a scalable vector SVG matching NestJS / qr-code-styling output with brand watermark.
     */
    public static String generateSvg(String content,
                                     String dotsColor,
                                     String backgroundColor,
                                     String dotsStyle,
                                     String cornersStyle,
                                     String cornersDotStyle,
                                     String centerText,
                                     String logoUrl) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 0);

            QRCode qrCode = Encoder.encode(content, ErrorCorrectionLevel.H, hints);
            ByteMatrix matrix = qrCode.getMatrix();
            int matrixSize = matrix.getWidth();

            String fg = (dotsColor != null && !dotsColor.trim().isEmpty()) ? dotsColor.trim() : "#000000";
            String bg = (backgroundColor != null && !backgroundColor.trim().isEmpty()) ? backgroundColor.trim() : "#FFFFFF";
            String dStyle = normalizeDotsStyle(dotsStyle);
            String cStyle = normalizeCornersStyle(cornersStyle);
            String cDotStyle = normalizeCornersDotStyle(cornersDotStyle);

            double cellSize = PRINTABLE_AREA / matrixSize;
            double startX = MARGIN;
            double startY = MARGIN;

            StringBuilder svg = new StringBuilder(32768);
            svg.append(String.format(Locale.ROOT,
                    "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 %d %d\" width=\"100%%\" height=\"100%%\">\n",
                    CANVAS_SIZE, CANVAS_SIZE));

            // 1. Background
            svg.append(String.format(Locale.ROOT,
                    "  <rect width=\"%d\" height=\"%d\" fill=\"%s\"/>\n",
                    CANVAS_SIZE, CANVAS_SIZE, bg));

            // 2. Render Finder Patterns (3 outer corners)
            renderFinderPatternSvg(svg, startX, startY, cellSize, fg, cStyle, cDotStyle); // Top-Left
            renderFinderPatternSvg(svg, startX + (matrixSize - 7) * cellSize, startY, cellSize, fg, cStyle, cDotStyle); // Top-Right
            renderFinderPatternSvg(svg, startX, startY + (matrixSize - 7) * cellSize, cellSize, fg, cStyle, cDotStyle); // Bottom-Left

            // 3. Render Body Modules
            for (int y = 0; y < matrixSize; y++) {
                for (int x = 0; x < matrixSize; x++) {
                    if (isCornerArea(x, y, matrixSize)) {
                        continue;
                    }

                    if (matrix.get(x, y) == 1) {
                        double posX = startX + x * cellSize;
                        double posY = startY + y * cellSize;

                        int left = isDarkModule(matrix, x - 1, y, matrixSize) ? 1 : 0;
                        int right = isDarkModule(matrix, x + 1, y, matrixSize) ? 1 : 0;
                        int top = isDarkModule(matrix, x, y - 1, matrixSize) ? 1 : 0;
                        int bottom = isDarkModule(matrix, x, y + 1, matrixSize) ? 1 : 0;

                        renderModuleSvg(svg, posX, posY, cellSize, fg, dStyle, left, right, top, bottom);
                    }
                }
            }

            // 4. Center Logo or Text if configured
            if (centerText != null && !centerText.trim().isEmpty()) {
                renderCenterText(svg, centerText.trim(), fg, bg);
            } else if (logoUrl != null && !logoUrl.trim().isEmpty()) {
                renderCenterLogo(svg, logoUrl.trim(), bg);
            }

            // 5. Signature 'trimly' watermark in bottom-right corner
            boolean isDark = isDarkColor(bg);
            String watermarkTextColor = isDark ? "#f8fafc" : "#273144";
            svg.append(String.format(Locale.ROOT,
                    "  <text x=\"%d\" y=\"%d\" text-anchor=\"end\" " +
                    "font-family=\"system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif\" " +
                    "font-weight=\"bold\" font-style=\"italic\" font-size=\"28\" fill=\"%s\" opacity=\"0.9\">trimly</text>\n",
                    CANVAS_SIZE - 40, CANVAS_SIZE - 25, watermarkTextColor));

            svg.append("</svg>");
            return svg.toString();
        } catch (Exception ex) {
            log.error("Failed to generate styled QR SVG: {}", ex.getMessage(), ex);
            throw new RuntimeException("QR Code SVG generation failed", ex);
        }
    }

    public static String generateSvg(String content, int size, String dotsColor, String backgroundColor) {
        return generateSvg(content, dotsColor, backgroundColor, "square", "square", "square", null, null);
    }

    /**
     * Generates a 1000x1000 PNG image binary matching the SVG vector rendering.
     */
    public static byte[] generatePng(String content,
                                     String dotsColor,
                                     String backgroundColor,
                                     String dotsStyle,
                                     String cornersStyle,
                                     String cornersDotStyle,
                                     String centerText,
                                     String logoUrl) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 0);

            QRCode qrCode = Encoder.encode(content, ErrorCorrectionLevel.H, hints);
            ByteMatrix matrix = qrCode.getMatrix();
            int matrixSize = matrix.getWidth();

            Color fg = parseColor(dotsColor, Color.BLACK);
            Color bg = parseColor(backgroundColor, Color.WHITE);
            String dStyle = normalizeDotsStyle(dotsStyle);
            String cStyle = normalizeCornersStyle(cornersStyle);
            String cDotStyle = normalizeCornersDotStyle(cornersDotStyle);

            BufferedImage image = new BufferedImage(CANVAS_SIZE, CANVAS_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            // 1. Fill background
            g2d.setColor(bg);
            g2d.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);

            double cellSize = PRINTABLE_AREA / matrixSize;
            double startX = MARGIN;
            double startY = MARGIN;

            // 2. Render Finder Patterns
            renderFinderPatternPng(g2d, startX, startY, cellSize, fg, cStyle, cDotStyle);
            renderFinderPatternPng(g2d, startX + (matrixSize - 7) * cellSize, startY, cellSize, fg, cStyle, cDotStyle);
            renderFinderPatternPng(g2d, startX, startY + (matrixSize - 7) * cellSize, cellSize, fg, cStyle, cDotStyle);

            // 3. Render Body Modules
            g2d.setColor(fg);
            for (int y = 0; y < matrixSize; y++) {
                for (int x = 0; x < matrixSize; x++) {
                    if (isCornerArea(x, y, matrixSize)) {
                        continue;
                    }
                    if (matrix.get(x, y) == 1) {
                        double posX = startX + x * cellSize;
                        double posY = startY + y * cellSize;

                        int left = isDarkModule(matrix, x - 1, y, matrixSize) ? 1 : 0;
                        int right = isDarkModule(matrix, x + 1, y, matrixSize) ? 1 : 0;
                        int top = isDarkModule(matrix, x, y - 1, matrixSize) ? 1 : 0;
                        int bottom = isDarkModule(matrix, x, y + 1, matrixSize) ? 1 : 0;

                        renderModulePng(g2d, posX, posY, cellSize, dStyle, left, right, top, bottom);
                    }
                }
            }

            // 4. Center text
            if (centerText != null && !centerText.trim().isEmpty()) {
                renderCenterTextPng(g2d, centerText.trim(), fg, bg);
            }

            // 5. Watermark
            boolean isDark = isDarkColor(backgroundColor);
            Color watermarkColor = isDark
                    ? new Color(248, 250, 252, 230)
                    : new Color(39, 49, 68, 230);
            g2d.setColor(watermarkColor);
            Font font = new Font(Font.SANS_SERIF, Font.BOLD | Font.ITALIC, 28);
            g2d.setFont(font);
            FontMetrics metrics = g2d.getFontMetrics(font);
            int textWidth = metrics.stringWidth("trimly");
            g2d.drawString("trimly", CANVAS_SIZE - 40 - textWidth, CANVAS_SIZE - 25);

            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception ex) {
            log.error("Failed to generate styled QR PNG: {}", ex.getMessage(), ex);
            throw new RuntimeException("QR Code PNG generation failed", ex);
        }
    }

    public static byte[] generatePng(String content, int size, String dotsColor, String backgroundColor) {
        return generatePng(content, dotsColor, backgroundColor, "square", "square", "square", null, null);
    }

    // ==========================================
    // FINDER PATTERNS (CORNERS)
    // ==========================================

    private static void renderFinderPatternSvg(StringBuilder svg, double x, double y, double cellSize,
                                               String fg, String cornersStyle, String cornersDotStyle) {
        double outerSize = 7 * cellSize;
        double cx = x + outerSize / 2.0;
        double cy = y + outerSize / 2.0;

        // Outer Frame
        if ("dot".equals(cornersStyle)) {
            double R = 3.5 * cellSize;
            double r = 2.5 * cellSize;
            svg.append(String.format(Locale.ROOT,
                    "  <path fill-rule=\"evenodd\" d=\"M %.2f %.2f a %.2f %.2f 0 1 0 0.001 0 z m 0 %.2f a %.2f %.2f 0 1 1 -0.001 0 z\" fill=\"%s\"/>\n",
                    cx, cy - R, R, R, R - r, r, r, fg));
        } else if ("extra-rounded".equals(cornersStyle)) {
            double s = cellSize;
            svg.append(String.format(Locale.ROOT,
                    "  <path fill-rule=\"evenodd\" d=\"M %.2f %.2f v %.2f a %.2f %.2f 0 0 0 %.2f %.2f h %.2f a %.2f %.2f 0 0 0 %.2f %.2f v %.2f a %.2f %.2f 0 0 0 %.2f %.2f h %.2f a %.2f %.2f 0 0 0 %.2f %.2f z M %.2f %.2f h %.2f a %.2f %.2f 0 0 1 %.2f %.2f v %.2f a %.2f %.2f 0 0 1 %.2f %.2f h %.2f a %.2f %.2f 0 0 1 %.2f %.2f v %.2f a %.2f %.2f 0 0 1 %.2f %.2f z\" fill=\"%s\"/>\n",
                    x, y + 2.5 * s, 2 * s, 2.5 * s, 2.5 * s, 2.5 * s, 2.5 * s, 2 * s, 2.5 * s, 2.5 * s, 2.5 * s, -2.5 * s, -2 * s, 2.5 * s, 2.5 * s, -2.5 * s, -2.5 * s, -2 * s, 2.5 * s, 2.5 * s, -2.5 * s, 2.5 * s,
                    x + 2.5 * s, y + s, 2 * s, 1.5 * s, 1.5 * s, 1.5 * s, 1.5 * s, 2 * s, 1.5 * s, 1.5 * s, -1.5 * s, 1.5 * s, -2 * s, 1.5 * s, 1.5 * s, -1.5 * s, -1.5 * s, -2 * s, 1.5 * s, 1.5 * s, 1.5 * s, -1.5 * s,
                    fg));
        } else {
            // Square frame
            double s = cellSize;
            svg.append(String.format(Locale.ROOT,
                    "  <path fill-rule=\"evenodd\" d=\"M %.2f %.2f v %.2f h %.2f v %.2f z M %.2f %.2f h %.2f v %.2f h %.2f z\" fill=\"%s\"/>\n",
                    x, y, 7 * s, 7 * s, -7 * s,
                    x + s, y + s, 5 * s, 5 * s, -5 * s,
                    fg));
        }

        // Center Dot (3x3 modules centered at cx, cy)
        if ("dot".equals(cornersDotStyle)) {
            double r = 1.5 * cellSize;
            svg.append(String.format(Locale.ROOT,
                    "  <circle cx=\"%.2f\" cy=\"%.2f\" r=\"%.2f\" fill=\"%s\"/>\n",
                    cx, cy, r, fg));
        } else {
            double dotSize = 3 * cellSize;
            svg.append(String.format(Locale.ROOT,
                    "  <rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" fill=\"%s\"/>\n",
                    x + 2 * cellSize, y + 2 * cellSize, dotSize, dotSize, fg));
        }
    }

    private static void renderFinderPatternPng(Graphics2D g2d, double x, double y, double cellSize,
                                               Color fg, String cornersStyle, String cornersDotStyle) {
        double outerSize = 7 * cellSize;
        double cx = x + outerSize / 2.0;
        double cy = y + outerSize / 2.0;

        g2d.setColor(fg);

        // Outer Frame
        if ("dot".equals(cornersStyle)) {
            double R = 3.5 * cellSize;
            double r = 2.5 * cellSize;
            Path2D.Double ring = new Path2D.Double(Path2D.WIND_EVEN_ODD);
            ring.append(new Ellipse2D.Double(cx - R, cy - R, 2 * R, 2 * R), false);
            ring.append(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r), false);
            g2d.fill(ring);
        } else if ("extra-rounded".equals(cornersStyle)) {
            double s = cellSize;
            Path2D.Double frame = new Path2D.Double(Path2D.WIND_EVEN_ODD);
            frame.append(new RoundRectangle2D.Double(x, y, 7 * s, 7 * s, 5.0 * s, 5.0 * s), false);
            frame.append(new RoundRectangle2D.Double(x + s, y + s, 5 * s, 5 * s, 3.0 * s, 3.0 * s), false);
            g2d.fill(frame);
        } else {
            // Square frame
            Path2D.Double frame = new Path2D.Double(Path2D.WIND_EVEN_ODD);
            frame.append(new Rectangle2D.Double(x, y, 7 * cellSize, 7 * cellSize), false);
            frame.append(new Rectangle2D.Double(x + cellSize, y + cellSize, 5 * cellSize, 5 * cellSize), false);
            g2d.fill(frame);
        }

        // Center Dot
        if ("dot".equals(cornersDotStyle)) {
            double r = 1.5 * cellSize;
            g2d.fill(new Ellipse2D.Double(cx - r, cy - r, 3 * cellSize, 3 * cellSize));
        } else {
            double dotSize = 3 * cellSize;
            g2d.fill(new Rectangle2D.Double(x + 2 * cellSize, y + 2 * cellSize, dotSize, dotSize));
        }
    }

    // ==========================================
    // BODY MODULES (DOTS / PATTERNS)
    // ==========================================

    private static void renderModuleSvg(StringBuilder svg, double x, double y, double size, String fg,
                                        String dotsStyle, int left, int right, int top, int bottom) {
        double cx = x + size / 2.0;
        double cy = y + size / 2.0;
        int neighborsCount = left + right + top + bottom;

        if ("dots".equals(dotsStyle)) {
            appendDotSvg(svg, cx, cy, size / 2.0, fg);
            return;
        }

        if ("rounded".equals(dotsStyle)) {
            if (neighborsCount == 0) {
                appendDotSvg(svg, cx, cy, size / 2.0, fg);
            } else if (neighborsCount > 2 || (left == 1 && right == 1) || (top == 1 && bottom == 1)) {
                appendSquareSvg(svg, x, y, size, fg);
            } else if (neighborsCount == 2) {
                int rotation = 0;
                if (left == 1 && top == 1) rotation = 90;
                else if (top == 1 && right == 1) rotation = 180;
                else if (right == 1 && bottom == 1) rotation = 270;
                appendCornerRoundedSvg(svg, x, y, size, cx, cy, rotation, fg);
            } else {
                // neighborsCount == 1
                int rotation = 0;
                if (top == 1) rotation = 90;
                else if (right == 1) rotation = 180;
                else if (bottom == 1) rotation = 270;
                appendSideRoundedSvg(svg, x, y, size, cx, cy, rotation, fg);
            }
            return;
        }

        if ("extra-rounded".equals(dotsStyle)) {
            if (neighborsCount == 0) {
                appendDotSvg(svg, cx, cy, size / 2.0, fg);
            } else if (neighborsCount > 2 || (left == 1 && right == 1) || (top == 1 && bottom == 1)) {
                appendSquareSvg(svg, x, y, size, fg);
            } else if (neighborsCount == 2) {
                int rotation = 0;
                if (left == 1 && top == 1) rotation = 90;
                else if (top == 1 && right == 1) rotation = 180;
                else if (right == 1 && bottom == 1) rotation = 270;
                appendCornerExtraRoundedSvg(svg, x, y, size, cx, cy, rotation, fg);
            } else {
                int rotation = 0;
                if (top == 1) rotation = 90;
                else if (right == 1) rotation = 180;
                else if (bottom == 1) rotation = 270;
                appendSideRoundedSvg(svg, x, y, size, cx, cy, rotation, fg);
            }
            return;
        }

        if ("classy".equals(dotsStyle)) {
            if (neighborsCount == 0) {
                appendCornersRoundedSvg(svg, x, y, size, cx, cy, 90, fg);
            } else if (left == 0 && top == 0) {
                appendCornerRoundedSvg(svg, x, y, size, cx, cy, 270, fg);
            } else if (right == 0 && bottom == 0) {
                appendCornerRoundedSvg(svg, x, y, size, cx, cy, 90, fg);
            } else {
                appendSquareSvg(svg, x, y, size, fg);
            }
            return;
        }

        if ("classy-rounded".equals(dotsStyle)) {
            if (neighborsCount == 0) {
                appendCornersRoundedSvg(svg, x, y, size, cx, cy, 90, fg);
            } else if (left == 0 && top == 0) {
                appendCornerExtraRoundedSvg(svg, x, y, size, cx, cy, 270, fg);
            } else if (right == 0 && bottom == 0) {
                appendCornerExtraRoundedSvg(svg, x, y, size, cx, cy, 90, fg);
            } else {
                appendSquareSvg(svg, x, y, size, fg);
            }
            return;
        }

        // Default: square
        appendSquareSvg(svg, x, y, size, fg);
    }

    private static void renderModulePng(Graphics2D g2d, double x, double y, double size,
                                        String dotsStyle, int left, int right, int top, int bottom) {
        double cx = x + size / 2.0;
        double cy = y + size / 2.0;
        int neighborsCount = left + right + top + bottom;

        if ("dots".equals(dotsStyle)) {
            g2d.fill(new Ellipse2D.Double(x, y, size, size));
            return;
        }

        if ("rounded".equals(dotsStyle)) {
            if (neighborsCount == 0) {
                g2d.fill(new Ellipse2D.Double(x, y, size, size));
            } else if (neighborsCount > 2 || (left == 1 && right == 1) || (top == 1 && bottom == 1)) {
                g2d.fill(new Rectangle2D.Double(x, y, size, size));
            } else if (neighborsCount == 2) {
                int rotation = 0;
                if (left == 1 && top == 1) rotation = 90;
                else if (top == 1 && right == 1) rotation = 180;
                else if (right == 1 && bottom == 1) rotation = 270;
                renderCornerRoundedPng(g2d, x, y, size, cx, cy, rotation);
            } else {
                int rotation = 0;
                if (top == 1) rotation = 90;
                else if (right == 1) rotation = 180;
                else if (bottom == 1) rotation = 270;
                renderSideRoundedPng(g2d, x, y, size, cx, cy, rotation);
            }
            return;
        }

        if ("extra-rounded".equals(dotsStyle)) {
            if (neighborsCount == 0) {
                g2d.fill(new Ellipse2D.Double(x, y, size, size));
            } else if (neighborsCount > 2 || (left == 1 && right == 1) || (top == 1 && bottom == 1)) {
                g2d.fill(new Rectangle2D.Double(x, y, size, size));
            } else if (neighborsCount == 2) {
                int rotation = 0;
                if (left == 1 && top == 1) rotation = 90;
                else if (top == 1 && right == 1) rotation = 180;
                else if (right == 1 && bottom == 1) rotation = 270;
                renderCornerExtraRoundedPng(g2d, x, y, size, cx, cy, rotation);
            } else {
                int rotation = 0;
                if (top == 1) rotation = 90;
                else if (right == 1) rotation = 180;
                else if (bottom == 1) rotation = 270;
                renderSideRoundedPng(g2d, x, y, size, cx, cy, rotation);
            }
            return;
        }

        if ("classy".equals(dotsStyle)) {
            if (neighborsCount == 0) {
                renderCornersRoundedPng(g2d, x, y, size, cx, cy, 90);
            } else if (left == 0 && top == 0) {
                renderCornerRoundedPng(g2d, x, y, size, cx, cy, 270);
            } else if (right == 0 && bottom == 0) {
                renderCornerRoundedPng(g2d, x, y, size, cx, cy, 90);
            } else {
                g2d.fill(new Rectangle2D.Double(x, y, size, size));
            }
            return;
        }

        if ("classy-rounded".equals(dotsStyle)) {
            if (neighborsCount == 0) {
                renderCornersRoundedPng(g2d, x, y, size, cx, cy, 90);
            } else if (left == 0 && top == 0) {
                renderCornerExtraRoundedPng(g2d, x, y, size, cx, cy, 270);
            } else if (right == 0 && bottom == 0) {
                renderCornerExtraRoundedPng(g2d, x, y, size, cx, cy, 90);
            } else {
                g2d.fill(new Rectangle2D.Double(x, y, size, size));
            }
            return;
        }

        // Default: square
        g2d.fill(new Rectangle2D.Double(x, y, size, size));
    }

    // ==========================================
    // SVG PRIMITIVE BUILDERS
    // ==========================================

    private static void appendDotSvg(StringBuilder svg, double cx, double cy, double r, String fg) {
        svg.append(String.format(Locale.ROOT,
                "  <circle cx=\"%.2f\" cy=\"%.2f\" r=\"%.2f\" fill=\"%s\"/>\n",
                cx, cy, r, fg));
    }

    private static void appendSquareSvg(StringBuilder svg, double x, double y, double size, String fg) {
        // Adding tiny 0.05px overlap prevents subpixel hairline gaps in browser SVG rendering
        svg.append(String.format(Locale.ROOT,
                "  <rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" fill=\"%s\"/>\n",
                x, y, size + 0.05, size + 0.05, fg));
    }

    private static void appendSideRoundedSvg(StringBuilder svg, double x, double y, double size,
                                             double cx, double cy, int rotationDeg, String fg) {
        String tr = rotationDeg != 0 ? String.format(Locale.ROOT, " transform=\"rotate(%d %.2f %.2f)\"", rotationDeg, cx, cy) : "";
        svg.append(String.format(Locale.ROOT,
                "  <path d=\"M %.2f %.2f v %.2f h %.2f a %.2f %.2f 0 0 0 0 -%.2f z\"%s fill=\"%s\"/>\n",
                x, y, size, size / 2.0, size / 2.0, size / 2.0, size, tr, fg));
    }

    private static void appendCornerRoundedSvg(StringBuilder svg, double x, double y, double size,
                                               double cx, double cy, int rotationDeg, String fg) {
        String tr = rotationDeg != 0 ? String.format(Locale.ROOT, " transform=\"rotate(%d %.2f %.2f)\"", rotationDeg, cx, cy) : "";
        svg.append(String.format(Locale.ROOT,
                "  <path d=\"M %.2f %.2f v %.2f h %.2f v -%.2f a %.2f %.2f 0 0 0 -%.2f -%.2f z\"%s fill=\"%s\"/>\n",
                x, y, size, size, size / 2.0, size / 2.0, size / 2.0, size / 2.0, size / 2.0, tr, fg));
    }

    private static void appendCornerExtraRoundedSvg(StringBuilder svg, double x, double y, double size,
                                                    double cx, double cy, int rotationDeg, String fg) {
        String tr = rotationDeg != 0 ? String.format(Locale.ROOT, " transform=\"rotate(%d %.2f %.2f)\"", rotationDeg, cx, cy) : "";
        svg.append(String.format(Locale.ROOT,
                "  <path d=\"M %.2f %.2f v %.2f h %.2f a %.2f %.2f 0 0 0 -%.2f -%.2f z\"%s fill=\"%s\"/>\n",
                x, y, size, size, size, size, size, size, tr, fg));
    }

    private static void appendCornersRoundedSvg(StringBuilder svg, double x, double y, double size,
                                                double cx, double cy, int rotationDeg, String fg) {
        String tr = rotationDeg != 0 ? String.format(Locale.ROOT, " transform=\"rotate(%d %.2f %.2f)\"", rotationDeg, cx, cy) : "";
        double h = size / 2.0;
        svg.append(String.format(Locale.ROOT,
                "  <path d=\"M %.2f %.2f v %.2f a %.2f %.2f 0 0 0 %.2f %.2f h %.2f v -%.2f a %.2f %.2f 0 0 0 -%.2f -%.2f z\"%s fill=\"%s\"/>\n",
                x, y, h, h, h, h, h, h, h, h, h, h, h, tr, fg));
    }

    // ==========================================
    // PNG PRIMITIVE BUILDERS (JAVA2D PATH2D)
    // ==========================================

    private static void renderSideRoundedPng(Graphics2D g2d, double x, double y, double size,
                                             double cx, double cy, int rotationDeg) {
        double r = size / 2.0;
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x, y);
        p.lineTo(x, y + size);
        p.lineTo(x + r, y + size);
        p.curveTo(x + r + KAPPA * r, y + size, x + size, y + r + KAPPA * r, x + size, y + r);
        p.curveTo(x + size, y + r - KAPPA * r, x + r + KAPPA * r, y, x + r, y);
        p.closePath();

        fillRotatedPath(g2d, p, cx, cy, rotationDeg);
    }

    private static void renderCornerRoundedPng(Graphics2D g2d, double x, double y, double size,
                                               double cx, double cy, int rotationDeg) {
        double r = size / 2.0;
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x, y);
        p.lineTo(x, y + size);
        p.lineTo(x + size, y + size);
        p.lineTo(x + size, y + r);
        p.curveTo(x + size, y + r - KAPPA * r, x + r + KAPPA * r, y, x + r, y);
        p.closePath();

        fillRotatedPath(g2d, p, cx, cy, rotationDeg);
    }

    private static void renderCornerExtraRoundedPng(Graphics2D g2d, double x, double y, double size,
                                                    double cx, double cy, int rotationDeg) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x, y);
        p.lineTo(x, y + size);
        p.lineTo(x + size, y + size);
        p.curveTo(x + size, y + size - KAPPA * size, x + KAPPA * size, y, x, y);
        p.closePath();

        fillRotatedPath(g2d, p, cx, cy, rotationDeg);
    }

    private static void renderCornersRoundedPng(Graphics2D g2d, double x, double y, double size,
                                                double cx, double cy, int rotationDeg) {
        double r = size / 2.0;
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x, y);
        p.lineTo(x, y + r);
        p.curveTo(x, y + r + KAPPA * r, x + r - KAPPA * r, y + size, x + r, y + size);
        p.lineTo(x + size, y + size);
        p.lineTo(x + size, y + size - r);
        p.curveTo(x + size, y + size - r - KAPPA * r, x + r + KAPPA * r, y, x + r, y);
        p.closePath();

        fillRotatedPath(g2d, p, cx, cy, rotationDeg);
    }

    private static void fillRotatedPath(Graphics2D g2d, Path2D path, double cx, double cy, int rotationDeg) {
        if (rotationDeg % 360 == 0) {
            g2d.fill(path);
            return;
        }
        AffineTransform old = g2d.getTransform();
        g2d.rotate(Math.toRadians(rotationDeg), cx, cy);
        g2d.fill(path);
        g2d.setTransform(old);
    }

    // ==========================================
    // HELPERS & OVERLAYS
    // ==========================================

    private static boolean isCornerArea(int x, int y, int matrixSize) {
        // Top-Left (7x7 pattern + 1 module quiet separator = 8x8)
        if (x < 8 && y < 8) return true;
        // Top-Right
        if (x >= matrixSize - 8 && y < 8) return true;
        // Bottom-Left
        if (x < 8 && y >= matrixSize - 8) return true;
        return false;
    }

    private static boolean isDarkModule(ByteMatrix matrix, int x, int y, int matrixSize) {
        if (x < 0 || x >= matrixSize || y < 0 || y >= matrixSize) {
            return false;
        }
        if (isCornerArea(x, y, matrixSize)) {
            return false;
        }
        return matrix.get(x, y) == 1;
    }

    private static String normalizeDotsStyle(String style) {
        if (style == null) return "square";
        String s = style.trim().toLowerCase(Locale.ROOT);
        return switch (s) {
            case "p1", "square" -> "square";
            case "p2", "dots", "dot" -> "dots";
            case "p3", "rounded" -> "rounded";
            case "p4", "extra-rounded", "extrarounded" -> "extra-rounded";
            case "p5", "classy" -> "classy";
            case "p6", "classy-rounded", "classyrounded" -> "classy-rounded";
            default -> "square";
        };
    }

    private static String normalizeCornersStyle(String style) {
        if (style == null) return "square";
        String s = style.trim().toLowerCase(Locale.ROOT);
        return switch (s) {
            case "c1", "c2", "square" -> "square";
            case "c3", "c4", "extra-rounded", "extrarounded" -> "extra-rounded";
            case "c5", "c6", "dot", "dots", "circle" -> "dot";
            default -> "square";
        };
    }

    private static String normalizeCornersDotStyle(String style) {
        if (style == null) return "square";
        String s = style.trim().toLowerCase(Locale.ROOT);
        return switch (s) {
            case "c1", "c3", "c6", "square" -> "square";
            case "c2", "c4", "c5", "dot", "dots", "circle" -> "dot";
            default -> "square";
        };
    }

    private static void renderCenterText(StringBuilder svg, String text, String fg, String bg) {
        double boxW = 280;
        double boxH = 70;
        double boxX = (CANVAS_SIZE - boxW) / 2.0;
        double boxY = (CANVAS_SIZE - boxH) / 2.0;
        String escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");

        svg.append(String.format(Locale.ROOT,
                "  <rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" rx=\"16\" ry=\"16\" " +
                "fill=\"%s\" stroke=\"%s\" stroke-width=\"3\"/>\n",
                boxX, boxY, boxW, boxH, bg, fg));
        svg.append(String.format(Locale.ROOT,
                "  <text x=\"500\" y=\"508\" text-anchor=\"middle\" dominant-baseline=\"middle\" " +
                "font-family=\"system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif\" " +
                "font-weight=\"bold\" font-size=\"26\" fill=\"%s\">%s</text>\n",
                fg, escaped));
    }

    private static void renderCenterLogo(StringBuilder svg, String logoUrl, String bg) {
        double boxSize = 180;
        double boxX = (CANVAS_SIZE - boxSize) / 2.0;
        double boxY = (CANVAS_SIZE - boxSize) / 2.0;
        double imgSize = 140;
        double imgX = (CANVAS_SIZE - imgSize) / 2.0;
        double imgY = (CANVAS_SIZE - imgSize) / 2.0;
        String escapedUrl = logoUrl.replace("&", "&amp;").replace("\"", "&quot;");

        svg.append(String.format(Locale.ROOT,
                "  <rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" rx=\"24\" ry=\"24\" " +
                "fill=\"%s\" stroke=\"#e2e8f0\" stroke-width=\"2\"/>\n",
                boxX, boxY, boxSize, boxSize, bg));
        svg.append(String.format(Locale.ROOT,
                "  <image href=\"%s\" x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" preserveAspectRatio=\"xMidYMid meet\"/>\n",
                escapedUrl, imgX, imgY, imgSize, imgSize));
    }

    private static void renderCenterTextPng(Graphics2D g2d, String text, Color fg, Color bg) {
        double boxW = 280;
        double boxH = 70;
        double boxX = (CANVAS_SIZE - boxW) / 2.0;
        double boxY = (CANVAS_SIZE - boxH) / 2.0;

        g2d.setColor(bg);
        g2d.fill(new RoundRectangle2D.Double(boxX, boxY, boxW, boxH, 16, 16));
        g2d.setColor(fg);
        g2d.setStroke(new BasicStroke(3f));
        g2d.draw(new RoundRectangle2D.Double(boxX, boxY, boxW, boxH, 16, 16));

        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 26);
        g2d.setFont(font);
        FontMetrics metrics = g2d.getFontMetrics(font);
        int textW = metrics.stringWidth(text);
        int textH = metrics.getAscent() - metrics.getDescent();
        double textX = (CANVAS_SIZE - textW) / 2.0;
        double textY = boxY + (boxH + textH) / 2.0;
        g2d.drawString(text, (int) textX, (int) textY);
    }

    private static Color parseColor(String hex, Color fallback) {
        if (hex == null || !hex.startsWith("#")) return fallback;
        try {
            String clean = hex.replace("#", "").trim();
            if (clean.length() == 3) {
                clean = "" + clean.charAt(0) + clean.charAt(0) + clean.charAt(1) + clean.charAt(1) + clean.charAt(2) + clean.charAt(2);
            }
            int r = Integer.parseInt(clean.substring(0, 2), 16);
            int g = Integer.parseInt(clean.substring(2, 4), 16);
            int b = Integer.parseInt(clean.substring(4, 6), 16);
            return new Color(r, g, b);
        } catch (Exception e) {
            return fallback;
        }
    }

    public static String toBase64DataUrl(byte[] pngBytes) {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes);
    }

    private static boolean isDarkColor(String hexColor) {
        if (hexColor == null || !hexColor.startsWith("#")) return false;
        try {
            String hex = hexColor.replace("#", "").trim();
            if (hex.length() == 3) {
                hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
            }
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            double brightness = (r * 299 + g * 587 + b * 114) / 1000.0;
            return brightness < 128;
        } catch (Exception e) {
            return false;
        }
    }
}
