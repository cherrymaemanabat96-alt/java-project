# Kainan Food Ordering System (Java + JavaFX + CSS)

## Requirements
- JDK 17 or newer
- Maven 3.8+

## Run
    mvn javafx:run

## Project layout
    pom.xml
    src/main/java/foodorder/FoodOrderingApp.java   <- app logic + UI
    src/main/resources/foodorder/style.css         <- all styling

## Features
- Menu grouped by category with filter buttons
- Add items, adjust quantities (+ / -), clear cart
- Live subtotal, 12% VAT and total
- Customer name + order type (Dine-in / Takeout / Delivery)
- Receipt dialog on checkout

## Customizing
- Edit the `menu` list in FoodOrderingApp.java to change items/prices.
- Change colors in style.css (main orange is #e8590c).
