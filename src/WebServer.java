import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import models.Employee;
import services.IntelligenceService;
import utils.FileHandler;

public final class WebServer {

    private static final int PORT = 8765;
    private static final Path WEB_ROOT = Paths.get("web").toAbsolutePath().normalize();
    private static final Object DATA_LOCK = new Object();

    private WebServer() {}

    public static void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
        server.createContext("/api/", WebServer::handleApi);
        server.createContext("/", WebServer::serveStatic);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("ERM web app running at http://127.0.0.1:" + PORT);
    }

    private static void handleApi(HttpExchange exchange) throws IOException {
        try {
            synchronized (DATA_LOCK) {
                routeApi(exchange);
            }
        } catch (IllegalArgumentException exception) {
            sendJson(exchange, 400, "{\"error\":" + quote(exception.getMessage()) + "}");
        } catch (Exception exception) {
            sendJson(exchange, 500, "{\"error\":\"Unable to read or save the project data.\"}");
        }
    }

    private static void routeApi(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("/api/dashboard".equals(path) && "GET".equals(method)) {
            sendJson(exchange, 200, dashboardJson());
        } else if ("/api/intelligence".equals(path) && "GET".equals(method)) {
            sendJson(exchange, 200, IntelligenceService.dashboardJson());
        } else if ("/api/employees".equals(path) && "GET".equals(method)) {
            sendJson(exchange, 200, employeesJson(FileHandler.loadEmployees()));
        } else if ("/api/employees".equals(path) && "POST".equals(method)) {
            addEmployee(exchange);
        } else if (path.startsWith("/api/employees/") && "DELETE".equals(method)) {
            deleteEmployee(exchange, parseId(path, "/api/employees/"));
        } else if ("/api/inventory".equals(path) && "GET".equals(method)) {
            sendJson(exchange, 200, rowsJson(FileHandler.loadInventory(), "name", "quantity", true));
        } else if ("/api/inventory".equals(path) && "POST".equals(method)) {
            addInventoryItem(exchange);
        } else if (path.startsWith("/api/inventory/") && "DELETE".equals(method)) {
            deleteRow(exchange, FileHandler.loadInventory(), "/api/inventory/", true);
        } else if ("/api/sales".equals(path) && "GET".equals(method)) {
            sendJson(exchange, 200, rowsJson(FileHandler.loadSales(), "product", "amount", false));
        } else if ("/api/sales".equals(path) && "POST".equals(method)) {
            addSale(exchange);
        } else if (path.startsWith("/api/sales/") && "DELETE".equals(method)) {
            deleteRow(exchange, FileHandler.loadSales(), "/api/sales/", false);
        } else {
            sendJson(exchange, 404, "{\"error\":\"API route not found.\"}");
        }
    }

    private static void addEmployee(HttpExchange exchange) throws IOException {
        Map<String, String> form = readForm(exchange);
        String name = requiredName(form.get("name"));
        double salary = parsePositiveAmount(form.get("salary"), "Salary");
        ArrayList<Employee> employees = FileHandler.loadEmployees();
        int nextId = employees.stream().mapToInt(Employee::getId).max().orElse(0) + 1;
        employees.add(new Employee(nextId, name, salary));
        FileHandler.saveEmployees(employees);
        sendJson(exchange, 201, "{\"ok\":true}");
    }

    private static void deleteEmployee(HttpExchange exchange, int id) throws IOException {
        ArrayList<Employee> employees = FileHandler.loadEmployees();
        boolean deleted = employees.removeIf(employee -> employee.getId() == id);
        if (!deleted) {
            sendJson(exchange, 404, "{\"error\":\"Employee not found.\"}");
            return;
        }
        FileHandler.saveEmployees(employees);
        sendJson(exchange, 200, "{\"ok\":true}");
    }

    private static void addInventoryItem(HttpExchange exchange) throws IOException {
        Map<String, String> form = readForm(exchange);
        String name = requiredName(form.get("name"));
        int quantity = parsePositiveInteger(form.get("quantity"), "Quantity");
        List<String[]> items = FileHandler.loadInventory();
        items.add(new String[]{String.valueOf(nextRowId(items)), name, String.valueOf(quantity)});
        FileHandler.saveInventory(items);
        sendJson(exchange, 201, "{\"ok\":true}");
    }

    private static void addSale(HttpExchange exchange) throws IOException {
        Map<String, String> form = readForm(exchange);
        String product = requiredName(form.get("product"));
        double amount = parsePositiveAmount(form.get("amount"), "Sale amount");
        List<String[]> sales = FileHandler.loadSales();
        sales.add(new String[]{String.valueOf(nextRowId(sales)), product, String.valueOf(amount)});
        FileHandler.saveSales(sales);
        sendJson(exchange, 201, "{\"ok\":true}");
    }

    private static void deleteRow(HttpExchange exchange, List<String[]> rows, String prefix, boolean inventory)
            throws IOException {
        int id = parseId(exchange.getRequestURI().getPath(), prefix);
        boolean deleted = rows.removeIf(row -> row.length > 0 && String.valueOf(id).equals(row[0]));
        if (!deleted) {
            sendJson(exchange, 404, "{\"error\":\"Record not found.\"}");
            return;
        }
        if (inventory) {
            FileHandler.saveInventory(rows);
        } else {
            FileHandler.saveSales(rows);
        }
        sendJson(exchange, 200, "{\"ok\":true}");
    }

    private static String dashboardJson() {
        ArrayList<Employee> employees = FileHandler.loadEmployees();
        List<String[]> inventory = FileHandler.loadInventory();
        List<String[]> sales = FileHandler.loadSales();
        int units = 0;
        int lowStock = 0;
        for (String[] item : inventory) {
            int quantity = parseRowInteger(item, 2);
            units += quantity;
            if (quantity <= 5) lowStock++;
        }
        double revenue = 0;
        for (String[] sale : sales) revenue += parseRowAmount(sale, 2);
        return "{\"employeeCount\":" + employees.size()
                + ",\"productCount\":" + inventory.size()
                + ",\"salesCount\":" + sales.size()
                + ",\"stockUnits\":" + units
                + ",\"lowStockCount\":" + lowStock
                + ",\"revenue\":" + revenue + "}";
    }

    private static String employeesJson(List<Employee> employees) {
        StringBuilder json = new StringBuilder("[");
        for (int index = 0; index < employees.size(); index++) {
            Employee employee = employees.get(index);
            if (index > 0) json.append(',');
            json.append("{\"id\":").append(employee.getId())
                    .append(",\"name\":").append(quote(employee.getName()))
                    .append(",\"salary\":").append(employee.getSalary()).append('}');
        }
        return json.append(']').toString();
    }

    private static String rowsJson(List<String[]> rows, String nameKey, String valueKey, boolean integerValue) {
        StringBuilder json = new StringBuilder("[");
        for (int index = 0; index < rows.size(); index++) {
            String[] row = rows.get(index);
            if (index > 0) json.append(',');
            json.append("{\"id\":").append(quote(valueAt(row, 0)))
                    .append(",\"").append(nameKey).append("\":").append(quote(valueAt(row, 1)))
                    .append(",\"").append(valueKey).append("\":")
                    .append(integerValue ? parseRowInteger(row, 2) : parseRowAmount(row, 2)).append('}');
        }
        return json.append(']').toString();
    }

    private static void serveStatic(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod()) && !"HEAD".equals(exchange.getRequestMethod())) {
            sendText(exchange, 405, "text/plain; charset=utf-8", "Method not allowed.");
            return;
        }
        String requestPath = exchange.getRequestURI().getPath();
        if ("/".equals(requestPath)) requestPath = "/index.html";
        Path file = WEB_ROOT.resolve(requestPath.substring(1)).normalize();
        if (!file.startsWith(WEB_ROOT) || !Files.isRegularFile(file)) {
            sendText(exchange, 404, "text/plain; charset=utf-8", "Not found.");
            return;
        }
        String contentType = contentType(file);
        byte[] bytes = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        if ("HEAD".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
        } else {
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    private static Map<String, String> readForm(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        if (body.length > 8192) throw new IllegalArgumentException("Submitted form is too large.");
        Map<String, String> values = new HashMap<>();
        String encoded = new String(body, StandardCharsets.UTF_8);
        for (String pair : encoded.split("&")) {
            int separator = pair.indexOf('=');
            if (separator < 0) continue;
            String key = URLDecoder.decode(pair.substring(0, separator), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8);
            values.put(key, value);
        }
        return values;
    }

    private static String requiredName(String value) {
        String name = value == null ? "" : value.trim();
        if (name.isEmpty() || name.length() > 80 || name.contains(",")
                || name.contains("\n") || name.contains("\r")) {
            throw new IllegalArgumentException("Enter a name of up to 80 characters without commas.");
        }
        return name;
    }

    private static double parsePositiveAmount(String value, String label) {
        try {
            double amount = Double.parseDouble(value);
            if (!Double.isFinite(amount) || amount <= 0) throw new NumberFormatException();
            return amount;
        } catch (Exception exception) {
            throw new IllegalArgumentException(label + " must be a positive number.");
        }
    }

    private static int parsePositiveInteger(String value, String label) {
        try {
            int number = Integer.parseInt(value);
            if (number <= 0) throw new NumberFormatException();
            return number;
        } catch (Exception exception) {
            throw new IllegalArgumentException(label + " must be a positive whole number.");
        }
    }

    private static int parseId(String path, String prefix) {
        try {
            int id = Integer.parseInt(path.substring(prefix.length()));
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Record ID is invalid.");
        }
    }

    private static int nextRowId(List<String[]> rows) {
        int maxId = 0;
        for (String[] row : rows) {
            try {
                maxId = Math.max(maxId, Integer.parseInt(valueAt(row, 0)));
            } catch (NumberFormatException ignored) {}
        }
        return maxId + 1;
    }

    private static int parseRowInteger(String[] row, int index) {
        try {
            return Integer.parseInt(valueAt(row, index));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static double parseRowAmount(String[] row, int index) {
        try {
            return Double.parseDouble(valueAt(row, index));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static String valueAt(String[] row, int index) {
        return index < row.length ? row[index] : "";
    }

    private static String quote(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (char character : value.toCharArray()) {
            switch (character) {
                case '"': escaped.append("\\\""); break;
                case '\\': escaped.append("\\\\"); break;
                case '\b': escaped.append("\\b"); break;
                case '\f': escaped.append("\\f"); break;
                case '\n': escaped.append("\\n"); break;
                case '\r': escaped.append("\\r"); break;
                case '\t': escaped.append("\\t"); break;
                default:
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
            }
        }
        return escaped.append('"').toString();
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".js")) return "text/javascript; charset=utf-8";
        if (name.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    private static void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        sendText(exchange, status, "application/json; charset=utf-8", body);
    }

    private static void sendText(HttpExchange exchange, int status, String contentType, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}