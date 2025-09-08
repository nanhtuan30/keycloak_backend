package com.anhto.keycloak.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.export.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

@Service
public class ReportService {
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    public byte[] exportReport(String format) {
        try {
            logger.info("Starting report generation for format: {}", format);

            // Load template JRXML từ resource
            ClassPathResource resource = new ClassPathResource("reports/TrasuaJR.jrxml");
            JasperReport jasperReport;
            try (InputStream inputStream = resource.getInputStream()) {
                JasperDesign jasperDesign = JRXmlLoader.load(inputStream);
                jasperReport = JasperCompileManager.compileReport(jasperDesign);
            }

            // Fake data demo
            List<Map<String, Object>> dataList = new ArrayList<>();
            dataList.add(Map.of("id", 1, "name", "Trà Sữa Trân Châu", "price", 30000));
            dataList.add(Map.of("id", 2, "name", "Trà Đào", "price", 25000));
            dataList.add(Map.of("id", 3, "name", "Trà Matcha", "price", 35000));

            JRBeanCollectionDataSource jrDataSource = new JRBeanCollectionDataSource(dataList);

            // Fill report
            Map<String, Object> parameters = new HashMap<>();
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, jrDataSource);

            // Export theo format
            return switch (format.toLowerCase()) {
                case "pdf" -> exportToPdf(jasperPrint);
                case "xlsx" -> exportToXlsx(jasperPrint);
                case "csv" -> exportToCsv(jasperPrint);
                default -> throw new IllegalArgumentException("Unsupported format: " + format);
            };

        } catch (Exception e) {
            logger.error("Error generating report: {}", e.getMessage(), e);
            throw new RuntimeException("Error generating report: " + e.getMessage(), e);
        }
    }

    private byte[] exportToPdf(JasperPrint jasperPrint) throws JRException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            JasperExportManager.exportReportToPdfStream(jasperPrint, out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new JRException("Error exporting to PDF: " + e.getMessage(), e);
        }
    }

    private byte[] exportToXlsx(JasperPrint jasperPrint) throws JRException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            JRXlsxExporter exporter = new JRXlsxExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));

            SimpleXlsxReportConfiguration config = new SimpleXlsxReportConfiguration();
            config.setOnePagePerSheet(false);
            config.setDetectCellType(true);
            exporter.setConfiguration(config);

            exporter.exportReport();
            return out.toByteArray();
        } catch (Exception e) {
            throw new JRException("Error exporting to XLSX: " + e.getMessage(), e);
        }
    }

    private byte[] exportToCsv(JasperPrint jasperPrint) throws JRException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            JRCsvExporter exporter = new JRCsvExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            exporter.setExporterOutput(new SimpleWriterExporterOutput(out));
            exporter.exportReport();
            return out.toByteArray();
        } catch (Exception e) {
            throw new JRException("Error exporting to CSV: " + e.getMessage(), e);
        }
    }

    public String getContentType(String format) {
        return switch (format.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "csv" -> "text/csv";
            default -> "application/octet-stream";
        };
    }

    public String getFileExtension(String format) {
        return switch (format.toLowerCase()) {
            case "pdf" -> ".pdf";
            case "xlsx" -> ".xlsx";
            case "csv" -> ".csv";
            default -> ".bin";
        };
    }
}
