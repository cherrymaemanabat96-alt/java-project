package foodorder;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FoodOrderingApp extends Application {

    // ---------- Model ----------
    record FoodItem(String name, String category, double price, String emoji, String description) {}

    static class CartLine {
        final FoodItem item;
        int qty = 1;
        CartLine(FoodItem item) { this.item = item; }
        double total() { return item.price() * qty; }
    }

    // ---------- Data ----------
    private static final double VAT_RATE = 0.12;

    private final List<FoodItem> menu = List.of(
        new FoodItem("Chicken Adobo", "Mains", 165, "🍗", "Classic soy-vinegar braised chicken with rice"),
        new FoodItem("Pork Sinigang", "Mains", 185, "🍲", "Sour tamarind soup with vegetables"),
        new FoodItem("Beef Kare-Kare", "Mains", 225, "🥘", "Oxtail stew in rich peanut sauce"),
        new FoodItem("Lechon Kawali", "Mains", 195, "🥓", "Crispy deep-fried pork belly"),
        new FoodItem("Pancit Canton", "Noodles", 120, "🍜", "Stir-fried noodles with veggies and meat"),
        new FoodItem("Spaghetti", "Noodles", 110, "🍝", "Sweet-style Filipino spaghetti"),
        new FoodItem("Lumpiang Shanghai", "Snacks", 90, "🥟", "8 pcs crispy pork spring rolls"),
        new FoodItem("Siomai", "Snacks", 80, "🥡", "6 pcs steamed pork dumplings"),
        new FoodItem("Halo-Halo", "Desserts", 95, "🍧", "Shaved ice with sweet beans, fruits & leche flan"),
        new FoodItem("Leche Flan", "Desserts", 70, "🍮", "Creamy caramel custard"),
        new FoodItem("Iced Tea", "Drinks", 45, "🧋", "House-brewed, refillable"),
        new FoodItem("Fresh Buko Juice", "Drinks", 60, "🥥", "Chilled young coconut juice")
    );

    private final Map<String, CartLine> cart = new LinkedHashMap<>();
    private String activeCategory = "All";
    private int orderCounter = 1001;
    private String css;

    // ---------- UI references ----------
    private FlowPane menuPane;
    private VBox cartRows;
    private Label subtotalLbl, vatLbl, totalLbl;
    private TextField nameField;
    private ComboBox<String> orderType;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        css = getClass().getResource("style.css").toExternalForm();

        BorderPane root = new BorderPane();
        root.setTop(buildHeader());
        root.setCenter(buildMenuArea());
        root.setRight(buildCartPanel());

        renderMenu();
        refreshCart();

        Scene scene = new Scene(root, 1150, 720);
        scene.getStylesheets().add(css);
        stage.setTitle("Kainan Food Ordering System");
        stage.setScene(scene);
        stage.show();
    }

    // ---------- Header ----------
    private HBox buildHeader() {
        Label title = new Label("🍽  Kainan");
        title.getStyleClass().add("app-title");
        Label subtitle = new Label("Fresh Filipino food, ordered fast");
        subtitle.getStyleClass().add("app-subtitle");
        VBox text = new VBox(2, title, subtitle);

        HBox header = new HBox(text);
        header.getStyleClass().add("header");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    // ---------- Menu ----------
    private VBox buildMenuArea() {
        HBox filters = new HBox(8);
        filters.setPadding(new Insets(0, 0, 4, 0));
        ToggleGroup group = new ToggleGroup();

        for (String cat : List.of("All", "Mains", "Noodles", "Snacks", "Desserts", "Drinks")) {
            ToggleButton btn = new ToggleButton(cat);
            btn.getStyleClass().add("category-btn");
            btn.setToggleGroup(group);
            btn.setSelected(cat.equals("All"));
            btn.setOnAction(e -> {
                activeCategory = cat;
                renderMenu();
            });
            filters.getChildren().add(btn);
        }
        // Don't allow deselecting the active category
        group.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null && oldT != null) oldT.setSelected(true);
        });

        menuPane = new FlowPane(16, 16);
        menuPane.getStyleClass().add("menu-pane");

        ScrollPane scroll = new ScrollPane(menuPane);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("menu-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox box = new VBox(12, filters, scroll);
        box.getStyleClass().add("menu-area");
        return box;
    }

    private void renderMenu() {
        menuPane.getChildren().clear();
        for (FoodItem item : menu) {
            if (activeCategory.equals("All") || item.category().equals(activeCategory)) {
                menuPane.getChildren().add(buildCard(item));
            }
        }
    }

    private VBox buildCard(FoodItem item) {
        Label emoji = new Label(item.emoji());
        emoji.getStyleClass().add("card-emoji");

        Label name = new Label(item.name());
        name.getStyleClass().add("card-name");

        Label desc = new Label(item.description());
        desc.getStyleClass().add("card-desc");
        desc.setWrapText(true);

        Label price = new Label(money(item.price()));
        price.getStyleClass().add("card-price");

        Button add = new Button("+ Add");
        add.getStyleClass().add("add-btn");
        add.setOnAction(e -> addToCart(item));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottom = new HBox(price, spacer, add);
        bottom.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(6, emoji, name, desc, bottom);
        card.getStyleClass().add("card");
        card.setPrefWidth(200);
        return card;
    }

    // ---------- Cart ----------
    private VBox buildCartPanel() {
        Label heading = new Label("Your Order");
        heading.getStyleClass().add("cart-heading");

        cartRows = new VBox(8);
        ScrollPane scroll = new ScrollPane(cartRows);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("cart-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        subtotalLbl = new Label();
        vatLbl = new Label();
        totalLbl = new Label();
        totalLbl.getStyleClass().add("total-value");

        GridPane totals = new GridPane();
        totals.setHgap(10);
        totals.setVgap(6);
        totals.add(new Label("Subtotal"), 0, 0);
        totals.add(subtotalLbl, 1, 0);
        totals.add(new Label("VAT (12%)"), 0, 1);
        totals.add(vatLbl, 1, 1);
        Label totalText = new Label("Total");
        totalText.getStyleClass().add("total-label");
        totals.add(totalText, 0, 2);
        totals.add(totalLbl, 1, 2);
        ColumnConstraints c0 = new ColumnConstraints();
        c0.setHgrow(Priority.ALWAYS);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHalignment(javafx.geometry.HPos.RIGHT);
        totals.getColumnConstraints().addAll(c0, c1);
        totals.getStyleClass().add("totals");

        nameField = new TextField();
        nameField.setPromptText("Customer name");
        nameField.getStyleClass().add("input");

        orderType = new ComboBox<>();
        orderType.getItems().addAll("Dine-in", "Takeout", "Delivery");
        orderType.setValue("Dine-in");
        orderType.setMaxWidth(Double.MAX_VALUE);
        orderType.getStyleClass().add("input");

        Button place = new Button("Place Order");
        place.getStyleClass().add("place-btn");
        place.setMaxWidth(Double.MAX_VALUE);
        place.setOnAction(e -> placeOrder());

        Button clear = new Button("Clear Cart");
        clear.getStyleClass().add("clear-btn");
        clear.setMaxWidth(Double.MAX_VALUE);
        clear.setOnAction(e -> {
            cart.clear();
            refreshCart();
        });

        VBox panel = new VBox(12, heading, scroll, totals, nameField, orderType, place, clear);
        panel.getStyleClass().add("cart-panel");
        panel.setPrefWidth(360);
        return panel;
    }

    private void addToCart(FoodItem item) {
        CartLine line = cart.get(item.name());
        if (line == null) {
            cart.put(item.name(), new CartLine(item));
        } else {
            line.qty++;
        }
        refreshCart();
    }

    private void changeQty(CartLine line, int delta) {
        line.qty += delta;
        if (line.qty <= 0) {
            cart.remove(line.item.name());
        }
        refreshCart();
    }

    private void refreshCart() {
        cartRows.getChildren().clear();

        if (cart.isEmpty()) {
            Label empty = new Label("Your cart is empty.\nAdd something delicious!");
            empty.getStyleClass().add("empty-label");
            cartRows.getChildren().add(empty);
        }

        double subtotal = 0;
        for (CartLine line : cart.values()) {
            subtotal += line.total();

            Label name = new Label(line.item.emoji() + " " + line.item.name());
            name.getStyleClass().add("row-name");
            Label unit = new Label(money(line.item.price()) + " each");
            unit.getStyleClass().add("row-unit");
            VBox info = new VBox(2, name, unit);
            HBox.setHgrow(info, Priority.ALWAYS);

            Button minus = new Button("−");
            minus.getStyleClass().add("qty-btn");
            minus.setOnAction(e -> changeQty(line, -1));
            Label qty = new Label(String.valueOf(line.qty));
            qty.getStyleClass().add("qty-label");
            Button plus = new Button("+");
            plus.getStyleClass().add("qty-btn");
            plus.setOnAction(e -> changeQty(line, 1));
            HBox qtyBox = new HBox(6, minus, qty, plus);
            qtyBox.setAlignment(Pos.CENTER);

            Label lineTotal = new Label(money(line.total()));
            lineTotal.getStyleClass().add("row-total");
            lineTotal.setMinWidth(70);
            lineTotal.setAlignment(Pos.CENTER_RIGHT);

            HBox row = new HBox(10, info, qtyBox, lineTotal);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("cart-row");
            cartRows.getChildren().add(row);
        }

        double vat = subtotal * VAT_RATE;
        subtotalLbl.setText(money(subtotal));
        vatLbl.setText(money(vat));
        totalLbl.setText(money(subtotal + vat));
    }

    // ---------- Checkout ----------
    private void placeOrder() {
        if (cart.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Empty cart", "Please add at least one item first.");
            return;
        }
        String customer = nameField.getText().trim();
        if (customer.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing name", "Please enter the customer name.");
            return;
        }

        double subtotal = cart.values().stream().mapToDouble(CartLine::total).sum();
        double vat = subtotal * VAT_RATE;

        StringBuilder sb = new StringBuilder();
        sb.append("Order #").append(orderCounter).append("\n");
        sb.append("Customer: ").append(customer).append("\n");
        sb.append("Type: ").append(orderType.getValue()).append("\n");
        sb.append("Time: ").append(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"))).append("\n");
        sb.append("--------------------------------\n");
        for (CartLine l : cart.values()) {
            sb.append(String.format("%dx %-20s %10s%n", l.qty, l.item.name(), money(l.total())));
        }
        sb.append("--------------------------------\n");
        sb.append(String.format("%-23s %10s%n", "Subtotal", money(subtotal)));
        sb.append(String.format("%-23s %10s%n", "VAT (12%)", money(vat)));
        sb.append(String.format("%-23s %10s%n", "TOTAL", money(subtotal + vat)));

        orderCounter++;
        showAlert(Alert.AlertType.INFORMATION, "Order placed!", sb.toString());

        cart.clear();
        nameField.clear();
        refreshCart();
    }

    private void showAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle("Kainan");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getDialogPane().getStylesheets().add(css);
        alert.getDialogPane().getStyleClass().add("dialog");
        alert.showAndWait();
    }

    private static String money(double v) {
        return String.format("₱%,.2f", v);
    }
}
