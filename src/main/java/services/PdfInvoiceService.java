package services;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import models.Reservation;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class PdfInvoiceService {

    public void generateInvoice(Reservation reservation) {

        try {

            String fileName = System.getProperty("user.home") + "/Desktop/Invoice_" + reservation.getId() + ".pdf";
            PdfWriter writer = new PdfWriter(fileName);
            System.out.println("Invoice will be created at: " + fileName);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // ================= LOGO =================
            InputStream logoStream = getClass()
                    .getClassLoader()
                    .getResourceAsStream("Backoffice/icons/blue.png");

            if (logoStream != null) {
                Image logo = new Image(ImageDataFactory.create(logoStream.readAllBytes()));
                logo.scaleToFit(140, 140);
                logo.setHorizontalAlignment(HorizontalAlignment.CENTER);
                document.add(logo);
            } else {
                System.out.println("⚠️ Logo not found, skipping logo");
            }



            document.add(new Paragraph("\n"));

            // ================= HEADER BAR =================
            Paragraph header = new Paragraph("INVOICE")
                    .setFontSize(26)
                    .setBold()
                    .setFontColor(new DeviceRgb(255,255,255))
                    .setBackgroundColor(new DeviceRgb(58,91,199))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(10);

            document.add(header);

            document.add(new Paragraph("\n"));

            // ================= INVOICE META =================
            String uniqueCode = UUID.randomUUID().toString().substring(0,8).toUpperCase();

            Paragraph meta = new Paragraph()
                    .add("Invoice Code: " + uniqueCode + "\n")
                    .add("Generated On: " +
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setFontSize(10);

            document.add(meta);

            document.add(new Paragraph("\n"));

            // ================= MAIN INFO TABLE =================
            Table table = new Table(UnitValue.createPercentArray(new float[]{1,2}))
                    .useAllAvailableWidth();

            table.setBorder(new com.itextpdf.layout.borders.SolidBorder(new DeviceRgb(220,220,220),1));

            addStyledRow(table, "Reservation ID", "#" + reservation.getId());
            addStyledRow(table, "Status", "PAID ✓");
            addStyledRow(table, "Start Date", reservation.getDateDebut().toString());
            addStyledRow(table, "End Date", reservation.getDateFin().toString());
            addStyledRow(table, "Total Amount", reservation.getCoutTotal() + " TND");

            document.add(table);

            document.add(new Paragraph("\n\n"));

            // ================= THANK YOU MESSAGE =================
            Paragraph thankYou = new Paragraph("Thank you for your purchase!")
                    .setFontSize(16)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);

            document.add(thankYou);

            Paragraph footer = new Paragraph(
                    "This document serves as an official proof of payment."
            )
                    .setFontSize(11)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(new DeviceRgb(100,100,100));

            document.add(footer);

            document.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addStyledRow(Table table, String key, String value) {

        Cell keyCell = new Cell()
                .add(new Paragraph(key).setBold())
                .setBackgroundColor(new DeviceRgb(245,245,245))
                .setPadding(8);

        Cell valueCell = new Cell()
                .add(new Paragraph(value))
                .setPadding(8);

        table.addCell(keyCell);
        table.addCell(valueCell);
    }
}