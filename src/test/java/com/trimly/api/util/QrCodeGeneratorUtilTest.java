package com.trimly.api.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QR Code Generator Utility Tests")
class QrCodeGeneratorUtilTest {

    @Test
    @DisplayName("Should generate valid vector SVG with default square styles and trimly watermark")
    void testGenerateSvgDefault() {
        String svg = QrCodeGeneratorUtil.generateSvg("http://localhost:4000/r/test1", 1000, "#000000", "#ffffff");

        assertThat(svg).isNotNull();
        assertThat(svg).contains("<svg xmlns=\"http://www.w3.org/2000/svg\"");
        assertThat(svg).contains("viewBox=\"0 0 1000 1000\"");
        assertThat(svg).contains("fill=\"#ffffff\"");
        assertThat(svg).contains("fill=\"#000000\"");
        assertThat(svg).contains(">trimly</text>");
        assertThat(svg).endsWith("</svg>");
    }

    @Test
    @DisplayName("Should generate vector SVG with dots pattern and circular corners")
    void testGenerateSvgDotsAndCircleCorners() {
        String svg = QrCodeGeneratorUtil.generateSvg(
                "http://localhost:4000/r/dots1",
                "#2a5bd7",
                "#ffffff",
                "dots",
                "dot",
                "dot",
                null,
                null
        );

        assertThat(svg).isNotNull();
        assertThat(svg).contains("<circle");
        assertThat(svg).contains("fill=\"#2a5bd7\"");
        assertThat(svg).contains(">trimly</text>");
    }

    @Test
    @DisplayName("Should generate vector SVG with classy pattern and concentric circle corners (p5 + c5)")
    void testGenerateSvgClassyAndDotCorners() {
        String svg = QrCodeGeneratorUtil.generateSvg(
                "http://localhost:4000/r/classy1",
                "#2a5bd7",
                "#ffffff",
                "classy",
                "dot",
                "dot",
                null,
                null
        );

        assertThat(svg).isNotNull();
        assertThat(svg).contains("fill-rule=\"evenodd\"");
        assertThat(svg).contains("fill=\"#2a5bd7\"");
        assertThat(svg).contains(">trimly</text>");
    }

    @Test
    @DisplayName("Should generate vector SVG with classy-rounded pattern and extra-rounded corners (p6 + c4)")
    void testGenerateSvgClassyRounded() {
        String svg = QrCodeGeneratorUtil.generateSvg(
                "http://localhost:4000/r/classyround1",
                "#10b981",
                "#f8fafc",
                "classy-rounded",
                "extra-rounded",
                "dot",
                null,
                null
        );

        assertThat(svg).isNotNull();
        assertThat(svg).contains("fill-rule=\"evenodd\"");
        assertThat(svg).contains("fill=\"#10b981\"");
        assertThat(svg).contains(">trimly</text>");
    }

    @Test
    @DisplayName("Should generate vector SVG with rounded style and extra-rounded corners")
    void testGenerateSvgRounded() {
        String svg = QrCodeGeneratorUtil.generateSvg(
                "http://localhost:4000/r/round1",
                "#10b981",
                "#f8fafc",
                "rounded",
                "extra-rounded",
                "square",
                null,
                null
        );

        assertThat(svg).isNotNull();
        assertThat(svg).contains("fill=\"#10b981\"");
        assertThat(svg).contains(">trimly</text>");
    }

    @Test
    @DisplayName("Should render center text when configured")
    void testGenerateSvgWithCenterText() {
        String svg = QrCodeGeneratorUtil.generateSvg(
                "http://localhost:4000/r/text1",
                "#000000",
                "#ffffff",
                "square",
                "square",
                "square",
                "Scan Me",
                null
        );

        assertThat(svg).isNotNull();
        assertThat(svg).contains("Scan Me");
        assertThat(svg).contains(">trimly</text>");
    }

    @Test
    @DisplayName("Should render center logo when configured")
    void testGenerateSvgWithLogo() {
        String svg = QrCodeGeneratorUtil.generateSvg(
                "http://localhost:4000/r/logo1",
                "#000000",
                "#ffffff",
                "square",
                "square",
                "square",
                null,
                "https://example.com/logo.png"
        );

        assertThat(svg).isNotNull();
        assertThat(svg).contains("<image href=\"https://example.com/logo.png\"");
        assertThat(svg).contains(">trimly</text>");
    }

    @Test
    @DisplayName("Should generate high-resolution PNG image binary at least 1000x1000")
    void testGeneratePng() {
        byte[] png = QrCodeGeneratorUtil.generatePng("http://localhost:4000/r/png1", 1024, "#000000", "#ffffff");

        assertThat(png).isNotNull();
        assertThat(png.length).isGreaterThan(1000);
        // Verify PNG magic number bytes: 0x89 0x50 0x4E 0x47
        assertThat(png[0]).isEqualTo((byte) 0x89);
        assertThat(png[1]).isEqualTo((byte) 0x50);
        assertThat(png[2]).isEqualTo((byte) 0x4E);
        assertThat(png[3]).isEqualTo((byte) 0x47);
    }

    @Test
    @DisplayName("Should generate styled 1000x1000 PNG image binary with classy pattern and dot corners")
    void testGenerateStyledPng() {
        byte[] png = QrCodeGeneratorUtil.generatePng(
                "http://localhost:4000/r/styledPng",
                "#2a5bd7",
                "#ffffff",
                "classy",
                "dot",
                "dot",
                "Scan Me",
                null
        );

        assertThat(png).isNotNull();
        assertThat(png.length).isGreaterThan(1000);
        assertThat(png[0]).isEqualTo((byte) 0x89);
        assertThat(png[1]).isEqualTo((byte) 0x50);
        assertThat(png[2]).isEqualTo((byte) 0x4E);
        assertThat(png[3]).isEqualTo((byte) 0x47);

        // Verify PNG dimensions in IHDR chunk: bytes 16-19 width, 20-23 height
        int width = ((png[16] & 0xFF) << 24) | ((png[17] & 0xFF) << 16) | ((png[18] & 0xFF) << 8) | (png[19] & 0xFF);
        int height = ((png[20] & 0xFF) << 24) | ((png[21] & 0xFF) << 16) | ((png[22] & 0xFF) << 8) | (png[23] & 0xFF);
        assertThat(width).isEqualTo(1000);
        assertThat(height).isEqualTo(1000);
    }
}
