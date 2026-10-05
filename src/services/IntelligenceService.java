package services;

import java.util.ArrayList;
import java.util.List;
import utils.FileHandler;

public class IntelligenceService {

    public static String dashboardJson() {
        List<String[]> inventory = FileHandler.loadInventory();
        List<String[]> sales = FileHandler.loadSales();

        List<String> alerts = new ArrayList<>();
        for (String[] item : inventory) {
            if (item == null || item.length < 3) {
                continue;
            }
            String name = item[1];
            int quantity = parseInteger(item[2]);
            String level;
            String description;
            if (quantity <= 5) {
                level = "critical";
                description = "Low stock signal";
            } else if (quantity >= 50) {
                level = "warning";
                description = "Overstock risk";
            } else {
                level = "info";
                description = "Healthy stock";
            }
            alerts.add("{\"name\":" + quote(name)
                    + ",\"quantity\":" + quantity
                    + ",\"level\":" + quote(level)
                    + ",\"description\":" + quote(description)
                    + "}");
        }

        List<String> actions = new ArrayList<>();
        if (inventory.size() > 0) {
            String[] mostCritical = null;
            int lowestQuantity = Integer.MAX_VALUE;
            for (String[] item : inventory) {
                if (item == null || item.length < 3) {
                    continue;
                }
                int qty = parseInteger(item[2]);
                if (qty < lowestQuantity) {
                    lowestQuantity = qty;
                    mostCritical = item;
                }
            }
            if (mostCritical != null) {
                actions.add("{\"priority\":\"URGENT\",\"level\":\"critical\",\"source\":\"Inventory\",\"title\":\"Low stock alert\",\"reason\":\"Current stock for "
                        + escape(mostCritical[1]) + " is only " + parseInteger(mostCritical[2]) + " units.\",\"action\":\"Review stock levels\",\"route\":\"inventory\"}");
            }
        }

        if (sales.size() > 0) {
            String topProduct = "Unassigned";
            double topRevenue = 0;
            for (String[] sale : sales) {
                if (sale == null || sale.length < 3) {
                    continue;
                }
                double amount = parseAmount(sale[2]);
                if (amount > topRevenue) {
                    topRevenue = amount;
                    topProduct = sale[1];
                }
            }
            actions.add("{\"priority\":\"RECOMMENDED\",\"level\":\"warning\",\"source\":\"Sales\",\"title\":\"Demand momentum\",\"reason\":\"" + escape(topProduct)
                    + " is contributing the highest recorded revenue in the current ledger.\",\"action\":\"View sales performance\",\"route\":\"sales\"}");
        }

        if (actions.isEmpty()) {
            actions.add("{\"priority\":\"OPTIONAL\",\"level\":\"info\",\"source\":\"People\",\"title\":\"Workspace status\",\"reason\":\"The current dataset is still developing; active monitoring is recommended.\",\"action\":\"Review overview\",\"route\":\"overview\"}");
        }

        return "{\"inventoryAlerts\": [" + join(alerts) + "], \"actionCenter\": [" + join(actions) + "]}";
    }

    private static int parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static double parseAmount(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    private static String quote(String value) {
        return "\"" + escape(value) + "\"";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
