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

public class PdfTicketService {

    public void generateReservationPdf(Reservation reservation) {

        try {

            // ===== Unique code =====
            String uniqueCode = UUID.randomUUID().toString().substring(0, 8);

            String fileName = "Reservation_" + reservation.getId() + ".pdf";
            String path = System.getProperty("user.home") + "/Desktop/" + fileName;

            PdfWriter writer = new PdfWriter(path);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(40, 40, 40, 40);

            // ===== Logo =====
            String logoPath = "src/main/resources/icons/blue.png"; // put your logo here
            Image logo = new Image(ImageDataFactory.create(logoPath))
                    .scaleToFit(120, 120);
            logo.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            document.add(logo);

            // ===== Title =====
            Paragraph title = new Paragraph("TRAVEL RESERVATION TICKET")
                    .setBold()
                    .setFontSize(20)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.BLUE);

            document.add(title);
            document.add(new Paragraph("\n"));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            // ===== Reservation Info =====
            document.add(new Paragraph("Reservation ID: " + reservation.getId()));
            document.add(new Paragraph("Start Date: " + reservation.getDateDebut().toLocalDate().format(formatter)));
            document.add(new Paragraph("End Date: " + reservation.getDateFin().toLocalDate().format(formatter)));
            document.add(new Paragraph("Status: " + reservation.getStatut()));
            document.add(new Paragraph("Total Cost: " + reservation.getCoutTotal() + " TND"));
            document.add(new Paragraph("Unique Code: " + uniqueCode));
            document.add(new Paragraph("\n"));

            // ===== QR CODE =====
            String qrText = "ReservationID:" + reservation.getId() +
                    "|Code:" + uniqueCode;

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            var bitMatrix = qrCodeWriter.encode(qrText, BarcodeFormat.QR_CODE, 200, 200);

            Path qrPath = Path.of("qr_temp.png");
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", qrPath);

            Image qrImage = new Image(ImageDataFactory.create(qrPath.toAbsolutePath().toString()));
            qrImage.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            document.add(qrImage);

            document.close();

            System.out.println("PDF Generated: " + path);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}