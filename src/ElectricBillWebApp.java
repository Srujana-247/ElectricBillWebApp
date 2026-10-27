import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ElectricBillWebApp {
    private static final double FIXED_CHARGE = 100.0;
    private static final double TAX_RATE = 0.05;
    private static final double LATE_FEE = 50.0;
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, "text/plain; charset=UTF-8",
                        "Method not allowed".getBytes(StandardCharsets.UTF_8));
                return;
            }
            byte[] page = java.nio.file.Files.readAllBytes(
                    java.nio.file.Paths.get("public", "index.html"));
            send(exchange, 200, "text/html; charset=UTF-8", page);
        });

        server.createContext("/style.css", exchange -> {
            byte[] css = java.nio.file.Files.readAllBytes(
                    java.nio.file.Paths.get("public", "style.css"));
            send(exchange, 200, "text/css; charset=UTF-8", css);
        });

        server.createContext("/bill", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, "text/plain; charset=UTF-8",
                        "Method not allowed".getBytes(StandardCharsets.UTF_8));
                return;
            }

            try {
                String body = new String(exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8);
                Map<String, String> form = parseForm(body);

                String consumerNo = required(form, "consumerNo", "Consumer number");
                String name = required(form, "name", "Consumer name");
                String address = required(form, "address", "Address");
                String meterNo = required(form, "meterNo", "Meter number");

                int previousReading = parseNonNegativeInt(
                        required(form, "previousReading", "Previous reading"),
                        "Previous reading");
                int currentReading = parseNonNegativeInt(
                        required(form, "currentReading", "Current reading"),
                        "Current reading");

                if (currentReading < previousReading) {
                    throw new IllegalArgumentException(
                            "Current reading cannot be less than previous reading.");
                }

                String lateValue = required(form, "late", "Late payment selection");
                if (!"0".equals(lateValue) && !"1".equals(lateValue)) {
                    throw new IllegalArgumentException(
                            "Please select whether the bill is paid late.");
                }

                int units = currentReading - previousReading;
                double energyCharge;

                // Original ElectricBill.java slab calculations are preserved.
                if (units <= 100) {
                    energyCharge = units * 2.00;
                } else if (units <= 300) {
                    energyCharge = 100 * 2.00 + (units - 100) * 3.00;
                } else {
                    energyCharge = 100 * 2.00 + 200 * 3.00 + (units - 300) * 5.00;
                }

                double tax = energyCharge * TAX_RATE;
                double lateFee = "1".equals(lateValue) ? LATE_FEE : 0.0;
                double totalAmount = energyCharge + FIXED_CHARGE + tax + lateFee;

                LocalDate today = LocalDate.now();
                LocalDate dueDate = today.plusDays(15);

                String html = buildBillPage(consumerNo, name, address, meterNo,
                        previousReading, currentReading, units, energyCharge,
                        tax, lateFee, totalAmount, today, dueDate);
                send(exchange, 200, "text/html; charset=UTF-8",
                        html.getBytes(StandardCharsets.UTF_8));
            } catch (IllegalArgumentException ex) {
                String errorPage = buildErrorPage(ex.getMessage());
                send(exchange, 400, "text/html; charset=UTF-8",
                        errorPage.getBytes(StandardCharsets.UTF_8));
            } catch (Exception ex) {
                String errorPage = buildErrorPage(
                        "Please check your entries and try again. Meter readings must be whole numbers.");
                send(exchange, 400, "text/html; charset=UTF-8",
                        errorPage.getBytes(StandardCharsets.UTF_8));
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Electricity Bill Web App is running.");
        System.out.println("Open http://localhost:8080 in your browser.");
        System.out.println("Press Ctrl+C here to stop the server.");
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> values = new HashMap<>();
        if (body == null || body.isEmpty()) return values;

        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1
                    ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            values.put(key, value);
        }
        return values;
    }

    private static String required(Map<String, String> form, String key, String label) {
        String value = form.get(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }

    private static int parseNonNegativeInt(String value, String label) {
        try {
            int number = Integer.parseInt(value);
            if (number < 0) {
                throw new IllegalArgumentException(label + " cannot be negative.");
            }
            return number;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " must be a whole number.");
        }
    }

    private static String buildBillPage(
            String consumerNo, String name, String address, String meterNo,
            int previousReading, int currentReading, int units,
            double energyCharge, double tax, double lateFee, double totalAmount,
            LocalDate today, LocalDate dueDate) {

        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Your Electricity Bill</title>
                  <link rel="stylesheet" href="/style.css">
                </head>
                <body>
                  <main class="page bill-page">
                    <a class="back-link" href="/">← Back to calculator</a>
                    <section class="bill-card">
                      <div class="bill-top">
                        <div class="brand-mark">⚡</div>
                        <div>
                          <p class="eyebrow">CONSUMER BILLING STATEMENT</p>
                          <h1>Electricity Bill</h1>
                          <p class="muted">A summary of your electricity usage and charges.</p>
                        </div>
                        <div class="bill-status">BILL GENERATED</div>
                      </div>

                      <div class="total-panel">
                        <span>Total amount payable</span>
                        <strong>₹ %s</strong>
                        <small>Bill date: %s &nbsp; • &nbsp; Due date: %s</small>
                      </div>

                      <div class="section-heading"><h2>Consumer details</h2></div>
                      <div class="details-grid">
                        <div><span>Consumer number</span><strong>%s</strong></div>
                        <div><span>Consumer name</span><strong>%s</strong></div>
                        <div><span>Meter number</span><strong>%s</strong></div>
                        <div><span>Address</span><strong>%s</strong></div>
                      </div>

                      <div class="section-heading"><h2>Meter readings</h2></div>
                      <div class="reading-grid">
                        <div class="reading"><span>Previous reading</span><strong>%d</strong></div>
                        <div class="reading"><span>Current reading</span><strong>%d</strong></div>
                        <div class="reading accent"><span>Units consumed</span><strong>%d</strong></div>
                      </div>

                      <div class="section-heading"><h2>Charge breakdown</h2></div>
                      <table>
                        <tbody>
                          <tr><td>Energy charges</td><td>₹ %s</td></tr>
                          <tr><td>Fixed charges</td><td>₹ %s</td></tr>
                          <tr><td>Tax (5%% of energy charges)</td><td>₹ %s</td></tr>
                          <tr><td>Late fee</td><td>₹ %s</td></tr>
                          <tr class="grand-total"><th>Total amount</th><th>₹ %s</th></tr>
                        </tbody>
                      </table>
                      <p class="notice">Please pay your bill before the due date.</p>
                      <div class="actions">
                        <button type="button" class="button secondary" onclick="window.print()">Print bill</button>
                        <a class="button primary" href="/">Calculate another bill</a>
                      </div>
                    </section>
                    <p class="footer-note">Demo project • Charges are calculated using the configured sample tariff.</p>
                  </main>
                </body>
                </html>
                """.formatted(
                money(totalAmount), escape(today.format(DATE_FORMAT)),
                escape(dueDate.format(DATE_FORMAT)), escape(consumerNo), escape(name),
                escape(meterNo), escape(address), previousReading, currentReading, units,
                money(energyCharge), money(FIXED_CHARGE), money(tax), money(lateFee),
                money(totalAmount));
    }

    private static String buildErrorPage(String message) {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Check your entries</title>
                  <link rel="stylesheet" href="/style.css">
                </head>
                <body>
                  <main class="page error-page">
                    <section class="error-card">
                      <div class="error-icon">!</div>
                      <p class="eyebrow">COULD NOT GENERATE BILL</p>
                      <h1>Check your entries</h1>
                      <p class="error-message">%s</p>
                      <a class="button primary" href="/">Return to form</a>
                    </section>
                  </main>
                </body>
                </html>
                """.formatted(escape(message));
    }

    private static String money(double amount) {
        return String.format(java.util.Locale.US, "%,.2f", amount);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static void send(HttpExchange exchange, int status, String contentType,
                             byte[] response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, response.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(response);
        }
    }
}
