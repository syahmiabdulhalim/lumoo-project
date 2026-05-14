package com.example.lumoo.controller;

import com.example.lumoo.model.Order;
import com.example.lumoo.model.User;
import com.example.lumoo.repository.OrderRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.security.Principal;

@Controller
public class InvoiceController {

    @Autowired private OrderRepository orderRepository;

    @GetMapping("/buyer/invoice/{id}")
    public void generateInvoice(@PathVariable Long id, HttpServletResponse response, Principal principal) throws IOException {
        Order order = orderRepository.findById(id).orElseThrow();

        if (principal == null || !order.getUser().getEmail().equals(principal.getName())) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "Access denied");
            return;
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=LUMOO_Invoice_" + id + ".pdf");

        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();
        
        // Header
        Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("LUMOO CONSTRUCTION MATERIALS", fontTitle);
        title.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph("Order ID: #LMO-" + order.getId()));
        document.add(new Paragraph("Date: " + order.getOrderDate()));
        User user = (User) order.getUser(); 
        document.add(new Paragraph("Customer: " + user.getUsername()));
        document.add(new Paragraph("------------------------------------------------------------------"));

        // Items Table
        document.add(new Paragraph("Items Purchased:"));
        order.getItems().forEach(item -> {
            try {
                document.add(new Paragraph("- " + item.getProductName() + " x " + item.getQuantity() + " : GMD " + (item.getPrice() * item.getQuantity())));
            } catch (DocumentException e) { e.printStackTrace(); }
        });

        document.add(new Paragraph("------------------------------------------------------------------"));
        Font fontTotal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        document.add(new Paragraph("TOTAL AMOUNT: GMD " + order.getTotalAmount(), fontTotal));
        
        document.add(new Paragraph("\nThank you for building with LUMOO Gambia."));
        
        document.close();
    }
}