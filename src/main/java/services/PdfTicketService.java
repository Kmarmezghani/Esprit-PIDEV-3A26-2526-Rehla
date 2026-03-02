package services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.io.image.ImageDataFactory;

import models.Reservation;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import services.TicketService;
import models.Ticket;
import java.util.List;

public class PdfTicketService {

    public void generateReservationPdf(Reservation reservation) {

        try {

            String uniqueCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            String fileName = "Reservation_" + reservation.getId() + ".pdf";
            String path = System.getProperty("user.home") + "/Desktop/" + fileName;

            PdfWriter writer = new PdfWriter(path);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(40, 40, 40, 40);

            // ================= LOGO (SAFE LOADING) =================
            var logoStream = getClass()
                    .getClassLoader()
                    .getResourceAsStream("Backoffice/icons/blue.png");

            if (logoStream != null) {
                Image logo = new Image(ImageDataFactory.create(logoStream.readAllBytes()))
                        .scaleToFit(120, 120);
                logo.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                document.add(logo);
            }

            document.add(new Paragraph("\n"));

            // ================= HEADER BAND =================
            Paragraph header = new Paragraph("TRAVEL RESERVATION TICKET")
                    .setBold()
                    .setFontSize(22)
                    .setFontColor(ColorConstants.WHITE)
                    .setBackgroundColor(ColorConstants.BLUE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(10);

            document.add(header);
            document.add(new Paragraph("\n"));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            // ================= RESERVATION TABLE =================
            Table infoTable = new Table(new float[]{1, 2});
            infoTable.useAllAvailableWidth();

            addStyledCell(infoTable, "Reservation ID", true);
            addStyledCell(infoTable, "#" + reservation.getId(), false);

            addStyledCell(infoTable, "Start Date", true);
            addStyledCell(infoTable, reservation.getDateDebut().toLocalDate().format(formatter), false);

            addStyledCell(infoTable, "End Date", true);
            addStyledCell(infoTable, reservation.getDateFin().toLocalDate().format(formatter), false);

            addStyledCell(infoTable, "Status", true);
            addStyledCell(infoTable, reservation.getStatut(), false);

            addStyledCell(infoTable, "Total Cost", true);
            addStyledCell(infoTable, reservation.getCoutTotal() + " TND", false);

            addStyledCell(infoTable, "Unique Code", true);
            addStyledCell(infoTable, uniqueCode, false);

            document.add(infoTable);

            document.add(new Paragraph("\n\n"));

            Paragraph ticketsTitle = new Paragraph("TICKETS DETAILS")
                    .setBold()
                    .setFontSize(16)
                    .setFontColor(ColorConstants.BLUE);

            document.add(ticketsTitle);
            document.add(new Paragraph("\n"));

            Table ticketsTable = new Table(new float[]{3, 2});
            ticketsTable.useAllAvailableWidth();

// Header Row
            ticketsTable.addHeaderCell(createHeaderCell("Ticket"));
            ticketsTable.addHeaderCell(createHeaderCell("Price"));

            TicketService ticketService = new TicketService();
            List<Ticket> tickets = ticketService.getTicketsByReservation(reservation.getId());

            if (tickets.isEmpty()) {
                ticketsTable.addCell(new Cell(1, 2)
                        .add(new Paragraph("No tickets found for this reservation."))
                        .setTextAlignment(TextAlignment.CENTER));
            } else {

                for (Ticket ticket : tickets) {
                    ticketsTable.addCell(new Paragraph(ticket.getType()).setTextAlignment(TextAlignment.CENTER));
                    ticketsTable.addCell(new Paragraph(ticket.getPrix() + " TND").setTextAlignment(TextAlignment.CENTER));
                }
            }

            document.add(ticketsTable);

            document.add(new Paragraph("\n\n"));

            // ================= QR CODE =================
            String qrText = "ReservationID:" + reservation.getId() +
                    "|Code:" + uniqueCode;

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            var bitMatrix = qrCodeWriter.encode(qrText, BarcodeFormat.QR_CODE, 200, 200);

            Path qrPath = Path.of("qr_temp.png");
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", qrPath);

            Image qrImage = new Image(ImageDataFactory.create(qrPath.toAbsolutePath().toString()))
                    .scaleToFit(150, 150);

            qrImage.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            document.add(qrImage);

            document.add(new Paragraph("\n"));

            // ================= FOOTER =================
            Paragraph footer = new Paragraph(
                    "Please present this ticket at boarding.\nThis document is electronically generated."
            )
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10)
                    .setFontColor(ColorConstants.GRAY);

            document.add(footer);

            document.close();

            System.out.println("✅ Reservation PDF Generated: " + path);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void addStyledCell(Table table, String text, boolean isHeader) {
        Cell cell = new Cell().add(new Paragraph(text));
        cell.setPadding(8);

        if (isHeader) {
            cell.setBold();
            cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        }

        table.addCell(cell);
    }

    private Cell createHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(ColorConstants.BLUE)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(8);
    }

    /** Génère le PDF en mémoire (byte[]) => parfait pour pièce jointe email */
    public byte[] generateReservationPdfBytes(Reservation reservation) throws java.io.IOException {
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {

            // On réutilise EXACTEMENT la même logique que generateReservationPdf,
            // mais en écrivant dans baos au lieu d’un chemin Desktop.

            // ===== Unique code =====
            String uniqueCode = java.util.UUID.randomUUID().toString().substring(0, 8);

            com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(baos);
            com.itextpdf.kernel.pdf.PdfDocument pdf = new com.itextpdf.kernel.pdf.PdfDocument(writer);
            com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf, com.itextpdf.kernel.geom.PageSize.A4);
            document.setMargins(40, 40, 40, 40);

            // ===== Logo (recommandé: depuis resources, pas src/... en runtime) =====
            try (java.io.InputStream logoStream = getClass().getResourceAsStream("/Backoffice/icons/blue.png")) {
                if (logoStream != null) {
                    byte[] logoBytes = logoStream.readAllBytes();
                    com.itextpdf.layout.element.Image logo =
                            new com.itextpdf.layout.element.Image(com.itextpdf.io.image.ImageDataFactory.create(logoBytes))
                                    .scaleToFit(120, 120);
                    logo.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                    document.add(logo);
                }
            }

            // ===== Title =====
            com.itextpdf.layout.element.Paragraph title = new com.itextpdf.layout.element.Paragraph("TRAVEL RESERVATION TICKET")
                    .setBold()
                    .setFontSize(20)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.BLUE);

            document.add(title);
            document.add(new com.itextpdf.layout.element.Paragraph("\n"));

            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

            // ===== Reservation Info =====
            document.add(new com.itextpdf.layout.element.Paragraph("Reservation ID: " + reservation.getId()));
            document.add(new com.itextpdf.layout.element.Paragraph("Start Date: " + reservation.getDateDebut().toLocalDate().format(formatter)));
            document.add(new com.itextpdf.layout.element.Paragraph("End Date: " + reservation.getDateFin().toLocalDate().format(formatter)));
            document.add(new com.itextpdf.layout.element.Paragraph("Status: " + reservation.getStatut()));
            document.add(new com.itextpdf.layout.element.Paragraph("Total Cost: " + reservation.getCoutTotal() + " TND"));
            document.add(new com.itextpdf.layout.element.Paragraph("Unique Code: " + uniqueCode));
            document.add(new com.itextpdf.layout.element.Paragraph("\n"));

            // ===== QR CODE en mémoire (pas de fichier temp) =====
            String qrText = "ReservationID:" + reservation.getId() + "|Code:" + uniqueCode;

            com.google.zxing.qrcode.QRCodeWriter qrCodeWriter = new com.google.zxing.qrcode.QRCodeWriter();
            var bitMatrix = qrCodeWriter.encode(qrText, com.google.zxing.BarcodeFormat.QR_CODE, 200, 200);

            try (java.io.ByteArrayOutputStream qrOut = new java.io.ByteArrayOutputStream()) {
                com.google.zxing.client.j2se.MatrixToImageWriter.writeToStream(bitMatrix, "PNG", qrOut);
                com.itextpdf.layout.element.Image qrImage =
                        new com.itextpdf.layout.element.Image(com.itextpdf.io.image.ImageDataFactory.create(qrOut.toByteArray()));
                qrImage.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                document.add(qrImage);
            }

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new java.io.IOException("Erreur génération PDF (bytes)", e);
        }
    }

    public void savePdfBytesToFile(byte[] pdfBytes, String absolutePath) throws java.io.IOException {
        java.nio.file.Files.write(java.nio.file.Path.of(absolutePath), pdfBytes);
    }
}