package com.example.lumoo.infrastructure.email;

public final class EmailTemplates {

    private EmailTemplates() {}

    private static String wrap(String title, String body) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;background:#f9f9f9;padding:0">
              <div style="background:#1a2336;padding:24px 32px">
                <span style="color:white;font-size:22px;font-weight:900;letter-spacing:-1px">LUMOO</span>
              </div>
              <div style="background:white;padding:32px">
                <h2 style="color:#1a2336;font-size:18px;margin:0 0 16px">%s</h2>
                %s
              </div>
              <div style="padding:16px 32px;text-align:center">
                <p style="color:#aaa;font-size:11px;margin:0">LUMOO Gambia &mdash; Building Gambia's Future</p>
              </div>
            </div>
            """.formatted(title, body);
    }

    public static String orderPlaced(String name, String orderId, double total, String address) {
        return wrap("Order Received!", """
            <p style="color:#444;font-size:14px">Hi <strong>%s</strong>,</p>
            <p style="color:#444;font-size:14px">Your order <strong>#LMO-%s</strong> has been placed successfully.</p>
            <table style="width:100%%;border-collapse:collapse;margin:16px 0">
              <tr><td style="padding:8px;border-bottom:1px solid #eee;color:#888;font-size:12px">Order ID</td><td style="padding:8px;border-bottom:1px solid #eee;font-size:14px;font-weight:bold">#LMO-%s</td></tr>
              <tr><td style="padding:8px;border-bottom:1px solid #eee;color:#888;font-size:12px">Amount</td><td style="padding:8px;border-bottom:1px solid #eee;font-size:14px;font-weight:bold">GMD %.2f</td></tr>
              <tr><td style="padding:8px;color:#888;font-size:12px">Delivery to</td><td style="padding:8px;font-size:14px">%s</td></tr>
            </table>
            <p style="color:#444;font-size:13px">We'll notify you when your order is confirmed and shipped.</p>
            <a href="https://lumoo.my/track" style="display:inline-block;margin-top:16px;background:#4d78c0;color:white;padding:12px 24px;font-size:12px;font-weight:bold;text-decoration:none;border-radius:4px">Track Your Order →</a>
            """.formatted(name, orderId, orderId, total, address));
    }

    public static String orderPaid(String name, String orderId, double total) {
        return wrap("Payment Confirmed!", """
            <p style="color:#444;font-size:14px">Hi <strong>%s</strong>,</p>
            <p style="color:#444;font-size:14px">Great news! Payment for order <strong>#LMO-%s</strong> has been confirmed.</p>
            <p style="background:#dcfce7;color:#166534;padding:12px 16px;font-size:13px;font-weight:bold;border-radius:4px">
              ✓ Payment received: GMD %.2f
            </p>
            <p style="color:#444;font-size:13px">Your order is now being processed and will be shipped soon.</p>
            """.formatted(name, orderId, total));
    }

    public static String orderShipped(String name, String orderId, String trackingNumber) {
        return orderShipped(name, orderId, trackingNumber, null);
    }

    public static String orderShipped(String name, String orderId, String trackingNumber, String trackingUrl) {
        String trackingBlock = "";
        if (trackingNumber != null && !trackingNumber.isBlank()) {
            trackingBlock = "<div style='background:#f0f4ff;border:1px solid #d0deff;border-radius:6px;padding:14px 18px;margin:16px 0'>" +
                    "<p style='color:#888;font-size:11px;margin:0 0 4px;text-transform:uppercase;letter-spacing:1px'>Tracking Number</p>" +
                    "<p style='color:#1a2336;font-size:18px;font-weight:900;margin:0;letter-spacing:1px'>" + trackingNumber + "</p>";
            if (trackingUrl != null && !trackingUrl.isBlank()) {
                trackingBlock += "<a href='" + trackingUrl + "' style='display:inline-block;margin-top:10px;background:#4d78c0;color:white;padding:8px 18px;font-size:11px;font-weight:bold;text-decoration:none;border-radius:4px'>Track with Courier →</a>";
            }
            trackingBlock += "</div>";
        }
        return wrap("Your Order is On Its Way!", """
            <p style="color:#444;font-size:14px">Hi <strong>%s</strong>,</p>
            <p style="color:#444;font-size:14px">Order <strong>#LMO-%s</strong> has been dispatched!</p>
            %s
            <a href="https://lumoo.my/track" style="display:inline-block;margin-top:12px;background:#1a2336;color:white;padding:10px 20px;font-size:11px;font-weight:bold;text-decoration:none;border-radius:4px">Track on LUMOO →</a>
            """.formatted(name, orderId, trackingBlock));
    }

    public static String orderDelivered(String name, String orderId) {
        return wrap("Order Delivered!", """
            <p style="color:#444;font-size:14px">Hi <strong>%s</strong>,</p>
            <p style="color:#444;font-size:14px">Order <strong>#LMO-%s</strong> has been marked as delivered.</p>
            <p style="color:#444;font-size:13px">We hope you love your purchase! If you have any issues, please contact us.</p>
            <p style="color:#444;font-size:13px">Don't forget to leave a review — it helps other buyers.</p>
            """.formatted(name, orderId));
    }

    public static String orderCancelled(String name, String orderId, String reason) {
        String reasonLine = (reason != null && !reason.isBlank())
                ? "<p style='color:#444;font-size:13px'>Reason: " + reason + "</p>"
                : "";
        return wrap("Order Cancelled", """
            <p style="color:#444;font-size:14px">Hi <strong>%s</strong>,</p>
            <p style="color:#444;font-size:14px">Order <strong>#LMO-%s</strong> has been cancelled.</p>
            %s
            <p style="color:#444;font-size:13px">If you have any questions, please contact our support team.</p>
            """.formatted(name, orderId, reasonLine));
    }

    public static String paymentFailed(String name, String orderId) {
        return wrap("Payment Unsuccessful", """
            <p style="color:#444;font-size:14px">Hi <strong>%s</strong>,</p>
            <p style="color:#444;font-size:14px">Unfortunately, payment for order <strong>#LMO-%s</strong> could not be processed.</p>
            <p style="background:#fee2e2;color:#991b1b;padding:12px 16px;font-size:13px;font-weight:bold;border-radius:4px">
              ✕ Payment failed — your order has been cancelled and stock has been released.
            </p>
            <p style="color:#444;font-size:13px">Please try placing a new order. If you believe this is an error, contact us.</p>
            """.formatted(name, orderId));
    }

    public static String staleCancelled(String name, String orderId) {
        return wrap("Order Expired", """
            <p style="color:#444;font-size:14px">Hi <strong>%s</strong>,</p>
            <p style="color:#444;font-size:14px">Order <strong>#LMO-%s</strong> has been cancelled because payment proof was not uploaded within 7 days.</p>
            <p style="color:#444;font-size:13px">If you still wish to purchase, please place a new order.</p>
            """.formatted(name, orderId));
    }

    public static String inquiryReceived(String vendorName, String buyerName, String productName,
                                          String subject, String message, String replyTo) {
        return wrap("New Product Inquiry", """
            <p style="color:#444;font-size:14px">Hi <strong>%s</strong>,</p>
            <p style="color:#444;font-size:14px">You have received a new inquiry about <strong>%s</strong>.</p>
            <table style="width:100%%;border-collapse:collapse;margin:16px 0">
              <tr><td style="padding:8px;border-bottom:1px solid #eee;color:#888;font-size:12px">From</td><td style="padding:8px;border-bottom:1px solid #eee;font-size:14px">%s</td></tr>
              <tr><td style="padding:8px;border-bottom:1px solid #eee;color:#888;font-size:12px">Reply to</td><td style="padding:8px;border-bottom:1px solid #eee;font-size:14px">%s</td></tr>
              <tr><td style="padding:8px;border-bottom:1px solid #eee;color:#888;font-size:12px">Subject</td><td style="padding:8px;border-bottom:1px solid #eee;font-size:14px">%s</td></tr>
            </table>
            <p style="color:#888;font-size:12px;margin-bottom:4px">MESSAGE:</p>
            <div style="background:#f5f5f5;padding:12px 16px;font-size:14px;color:#333;border-left:3px solid #4d78c0">%s</div>
            """.formatted(vendorName, productName, buyerName, replyTo, subject, message));
    }

    public static String newCareerApplication(String adminEmail, String postingTitle,
                                               String applicantName, String applicantEmail) {
        return wrap("New Career Application", """
            <p style="color:#444;font-size:14px">A new application has been submitted for <strong>%s</strong>.</p>
            <table style="width:100%%;border-collapse:collapse;margin:16px 0">
              <tr><td style="padding:8px;border-bottom:1px solid #eee;color:#888;font-size:12px">Applicant</td><td style="padding:8px;border-bottom:1px solid #eee;font-size:14px">%s</td></tr>
              <tr><td style="padding:8px;color:#888;font-size:12px">Email</td><td style="padding:8px;font-size:14px">%s</td></tr>
            </table>
            <p style="color:#444;font-size:13px">Log into the admin panel to review this application.</p>
            """.formatted(postingTitle, applicantName, applicantEmail));
    }

    public static String newPartnershipApplication(String companyName, String contactName,
                                                    String email, String partnerType) {
        return wrap("New Partnership Application", """
            <p style="color:#444;font-size:14px">A new partnership application has been submitted.</p>
            <table style="width:100%%;border-collapse:collapse;margin:16px 0">
              <tr><td style="padding:8px;border-bottom:1px solid #eee;color:#888;font-size:12px">Company</td><td style="padding:8px;border-bottom:1px solid #eee;font-size:14px">%s</td></tr>
              <tr><td style="padding:8px;border-bottom:1px solid #eee;color:#888;font-size:12px">Contact</td><td style="padding:8px;border-bottom:1px solid #eee;font-size:14px">%s</td></tr>
              <tr><td style="padding:8px;border-bottom:1px solid #eee;color:#888;font-size:12px">Email</td><td style="padding:8px;border-bottom:1px solid #eee;font-size:14px">%s</td></tr>
              <tr><td style="padding:8px;color:#888;font-size:12px">Type</td><td style="padding:8px;font-size:14px">%s</td></tr>
            </table>
            <p style="color:#444;font-size:13px">Log into the admin panel to review this application.</p>
            """.formatted(companyName, contactName, email, partnerType));
    }

    public static String subscriberWelcome(String storeUrl) {
        return wrap("Welcome to LUMOO!", """
            <p style="color:#444;font-size:14px">Thank you for subscribing to LUMOO updates!</p>
            <p style="color:#444;font-size:13px">You'll be the first to hear about new products, deals, and industry news from Gambia's leading construction materials marketplace.</p>
            <a href="%s" style="display:inline-block;margin-top:16px;background:#1a2336;color:white;padding:12px 28px;font-size:12px;font-weight:bold;text-decoration:none;letter-spacing:1px">
                Shop Now →
            </a>
            <p style="color:#aaa;font-size:11px;margin-top:24px">You can unsubscribe at any time by visiting your account settings.</p>
            """.formatted(storeUrl));
    }

    public static String reviewRequest(String name, String orderId, String productName, String reviewUrl) {
        return wrap("How was your order?", """
            <p style="color:#444;font-size:14px">Hi <strong>%s</strong>,</p>
            <p style="color:#444;font-size:14px">Your order <strong>#LMO-%s</strong> has been delivered. We hope you love it!</p>
            <p style="color:#444;font-size:13px">Would you take a moment to review <strong>%s</strong>? Your feedback helps other buyers make better decisions.</p>
            <a href="%s" style="display:inline-block;margin-top:16px;background:#4d78c0;color:white;padding:12px 28px;font-size:12px;font-weight:bold;text-decoration:none;letter-spacing:1px">
                Leave a Review →
            </a>
            """.formatted(name, orderId, productName, reviewUrl));
    }

    public static String vendorApproved(String name) {
        return wrap("Your Vendor Application is Approved!", """
            <p style="color:#444;font-size:14px">Congratulations <strong>%s</strong>!</p>
            <p style="color:#444;font-size:14px">Your LUMOO vendor application has been <strong style="color:#166534">approved</strong>.</p>
            <p style="color:#444;font-size:13px">You can now log into your Vendor Hub to start listing your products.</p>
            <p style="background:#dcfce7;color:#166534;padding:12px 16px;font-size:13px;font-weight:bold;border-radius:4px">
                Please log out and log back in to access your Vendor Hub.
            </p>
            """.formatted(name));
    }

    public static String vendorRejected(String name, String reason) {
        String reasonLine = (reason != null && !reason.isBlank())
                ? "<p style='color:#444;font-size:13px'>Reason: <em>" + reason + "</em></p>"
                : "";
        return wrap("Vendor Application Update", """
            <p style="color:#444;font-size:14px">Hi <strong>%s</strong>,</p>
            <p style="color:#444;font-size:14px">We've reviewed your vendor application and unfortunately it was <strong style="color:#991b1b">not approved</strong> at this time.</p>
            %s
            <p style="color:#444;font-size:13px">You may re-apply after 12 hours. If you have any questions, please contact us at <a href="mailto:info@lumoo.gm">info@lumoo.gm</a>.</p>
            """.formatted(name, reasonLine));
    }

    public static String adminDailyDigest(String date, long newOrders, double revenue,
                                           long pendingProof, long returnRequests,
                                           long pendingVendorApps, long newSubscribers,
                                           String dashboardUrl) {
        return wrap("Daily Digest — " + date, """
            <p style="color:#444;font-size:13px">Here's your LUMOO summary for <strong>%s</strong>:</p>
            <table style="width:100%%;border-collapse:collapse;margin:16px 0">
              <tr style="background:#f8fafc">
                <td style="padding:10px 14px;font-size:12px;color:#888;font-weight:bold;border-bottom:1px solid #eee">New Orders</td>
                <td style="padding:10px 14px;font-size:16px;font-weight:900;color:#1a2336;border-bottom:1px solid #eee">%d</td>
              </tr>
              <tr>
                <td style="padding:10px 14px;font-size:12px;color:#888;font-weight:bold;border-bottom:1px solid #eee">Revenue (Today)</td>
                <td style="padding:10px 14px;font-size:16px;font-weight:900;color:#4d78c0;border-bottom:1px solid #eee">GMD %.2f</td>
              </tr>
              <tr style="background:#f8fafc">
                <td style="padding:10px 14px;font-size:12px;color:#888;font-weight:bold;border-bottom:1px solid #eee">Pending Proof Verification</td>
                <td style="padding:10px 14px;font-size:16px;font-weight:900;color:%s;border-bottom:1px solid #eee">%d</td>
              </tr>
              <tr>
                <td style="padding:10px 14px;font-size:12px;color:#888;font-weight:bold;border-bottom:1px solid #eee">Return Requests</td>
                <td style="padding:10px 14px;font-size:16px;font-weight:900;color:%s;border-bottom:1px solid #eee">%d</td>
              </tr>
              <tr style="background:#f8fafc">
                <td style="padding:10px 14px;font-size:12px;color:#888;font-weight:bold;border-bottom:1px solid #eee">Pending Vendor Applications</td>
                <td style="padding:10px 14px;font-size:16px;font-weight:900;color:%s;border-bottom:1px solid #eee">%d</td>
              </tr>
              <tr>
                <td style="padding:10px 14px;font-size:12px;color:#888;font-weight:bold">New Subscribers</td>
                <td style="padding:10px 14px;font-size:16px;font-weight:900;color:#1a2336">%d</td>
              </tr>
            </table>
            <a href="%s" style="display:inline-block;margin-top:8px;background:#1a2336;color:white;padding:12px 28px;font-size:12px;font-weight:bold;text-decoration:none;letter-spacing:1px">
                Open Dashboard →
            </a>
            """.formatted(
                date, newOrders, revenue,
                pendingProof > 0 ? "#d97706" : "#166534", pendingProof,
                returnRequests > 0 ? "#dc2626" : "#166534", returnRequests,
                pendingVendorApps > 0 ? "#d97706" : "#166534", pendingVendorApps,
                newSubscribers, dashboardUrl));
    }
}
