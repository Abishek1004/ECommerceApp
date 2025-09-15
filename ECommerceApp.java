import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;



public class ECommerceApp {

    // ---------- Model Classes ----------
    static class Product {
        private final int id;
        private String name;
        private String category;
        private double price;
        private int stock;

        public Product(int id, String name, String category, double price, int stock) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.price = price;
            this.stock = stock;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public double getPrice() { return price; }
        public int getStock() { return stock; }

        public void reduceStock(int q) { stock = Math.max(0, stock - q); }
        public void increaseStock(int q) { stock += q; }

        @Override
        public String toString() {
            return String.format("[%d] %s - %s - ₹%.2f (Stock: %d)", id, name, category, price, stock);
        }
    }

    static class CartItem {
        private final Product product;
        private int qty;

        public CartItem(Product product, int qty) {
            this.product = product;
            this.qty = qty;
        }

        public Product getProduct() { return product; }
        public int getQty() { return qty; }
        public void setQty(int q) { qty = q; }
        public double getTotal() { return qty * product.getPrice(); }

        @Override
        public String toString() {
            return String.format("%s x %d = ₹%.2f", product.getName(), qty, getTotal());
        }
    }

    // ---------- In-memory "DB" (Collections) ----------
    private final Map<Integer, Product> productById = new HashMap<>();
    private final Map<String, List<Product>> categoryMap = new TreeMap<>();
    private final Map<String, String> users = new HashMap<>(); // username -> password (plaintext for demo)
    private final List<CartItem> cart = new ArrayList<>();
    private String loggedInUser = null;

    // For generating product IDs
    private int nextProductId = 1001;

    // ---------- Swing UI ----------
    private final JFrame frame = new JFrame("E-Commerce (Swing/AWT Demo)");
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainPanel = new JPanel(cardLayout);

    // Panels
    private final LoginPanel loginPanel = new LoginPanel();
    private final ShopPanel shopPanel = new ShopPanel();
    private final CartPanel cartPanel = new CartPanel();
    private final AdminPanel adminPanel = new AdminPanel();

    public ECommerceApp() {
        seedData();
        buildUI();
    }

    private void seedData() {
        // sample users
        users.put("user1", "pass1");
        users.put("admin", "admin"); // admin password

        // sample products
        addProductInternal("Laptop", "Electronics", 55000, 7);
        addProductInternal("Smartphone", "Electronics", 15000, 25);
        addProductInternal("Headphones", "Electronics", 1200, 50);
        addProductInternal("Shirt", "Clothing", 799, 60);
        addProductInternal("Jeans", "Clothing", 1299, 40);
        addProductInternal("Running Shoes", "Footwear", 2499, 20);
        addProductInternal("Washing Machine", "Home Appliances", 25999, 5);
    }

    private void addProductInternal(String name, String category, double price, int stock) {
        Product p = new Product(nextProductId++, name, category, price, stock);
        productById.put(p.getId(), p);
        categoryMap.computeIfAbsent(category, k -> new ArrayList<>()).add(p);
    }

    private void buildUI() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 600);
        frame.setLocationRelativeTo(null);

        mainPanel.add(loginPanel, "LOGIN");
        mainPanel.add(shopPanel, "SHOP");
        mainPanel.add(cartPanel, "CART");
        mainPanel.add(adminPanel, "ADMIN");

        frame.getContentPane().add(mainPanel);
        cardLayout.show(mainPanel, "LOGIN");
        frame.setVisible(true);
    }

    // ---------- Panels ----------
    class LoginPanel extends JPanel {
        private final JTextField userField = new JTextField(30);
        private final JPasswordField passField = new JPasswordField(30);
        private final JButton loginButton = new JButton("Login");
        private final JButton registerButton = new JButton("Register");
        private final JLabel message = new JLabel(" ");


        public LoginPanel() {
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JLabel title = new JLabel("Welcome - Please Login or Register");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 21f));
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            add(title, gbc);

            gbc.gridwidth = 1;
            gbc.gridy++;
            add(new JLabel("Username:"), gbc);
            gbc.gridx = 1;
            add(userField, gbc);

            gbc.gridx = 0; gbc.gridy++;
            add(new JLabel("Password:"), gbc);
            gbc.gridx = 1;
            add(passField, gbc);

            gbc.gridx = 0; gbc.gridy++;
            add(loginButton, gbc);
            gbc.gridx = 1;
            add(registerButton, gbc);

            gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 8;
            message.setForeground(Color.RED);
            add(message, gbc);

            loginButton.addActionListener(e -> doLogin());
            registerButton.addActionListener(e -> doRegister());
        }

        private void doLogin() {
            String u = userField.getText().trim();
            String p = new String(passField.getPassword());
            if (u.isEmpty() || p.isEmpty()) {
                message.setText("Enter username and password.");
                return;
            }
            if (users.containsKey(u) && users.get(u).equals(p)) {
                loggedInUser = u;
                message.setForeground(Color.GREEN.darker());
                message.setText("Login successful. Welcome " + u + "!");
                // show shop
                shopPanel.refreshCategoryList();
                cardLayout.show(mainPanel, "SHOP");
            } else {
                message.setForeground(Color.RED);
                message.setText("Invalid credentials.");
            }
        }

        private void doRegister() {
            String u = userField.getText().trim();
            String p = new String(passField.getPassword());
            if (u.isEmpty() || p.isEmpty()) {
                message.setText("Enter username and password to register.");
                return;
            }
            if (users.containsKey(u)) {
                message.setText("Username already exists.");
                return;
            }
            users.put(u, p);
            message.setForeground(Color.GREEN.darker());
            message.setText("Registration successful. You can now login.");
        }
    }

    class ShopPanel extends JPanel {
        private final JList<String> categoryJList = new JList<>();
        private final DefaultListModel<String> catModel = new DefaultListModel<>();
        private final DefaultListModel<String> productListModel = new DefaultListModel<>();
        private final JList<String> productJList = new JList<>(productListModel);
        private final JButton addToCartBtn = new JButton("Add Selected to Cart");
        private final JButton viewCartBtn = new JButton("View Cart");
        private final JButton logoutBtn = new JButton("Logout");
        private final JButton adminBtn = new JButton("Admin Panel (admin only)");

        public ShopPanel() {
            setLayout(new BorderLayout(10, 10));
            setBorder(new EmptyBorder(10,10,10,10));

            // Left = categories
            JPanel left = new JPanel(new BorderLayout(5,5));
            left.add(new JLabel("Categories"), BorderLayout.NORTH);
            categoryJList.setModel(catModel);
            left.add(new JScrollPane(categoryJList), BorderLayout.CENTER);
            categoryJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            // Center = products
            JPanel center = new JPanel(new BorderLayout(5,5));
            center.add(new JLabel("Products"), BorderLayout.NORTH);
            productJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            center.add(new JScrollPane(productJList), BorderLayout.CENTER);

            // Bottom actions
            JPanel actions = new JPanel();
            actions.add(addToCartBtn);
            actions.add(viewCartBtn);
            actions.add(adminBtn);
            actions.add(logoutBtn);

            add(left, BorderLayout.WEST);
            add(center, BorderLayout.CENTER);
            add(actions, BorderLayout.SOUTH);

            // events
            categoryJList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) loadProductsForSelectedCategory();
            });

            addToCartBtn.addActionListener(e -> addSelectedProductToCart());
            viewCartBtn.addActionListener(e -> {
                cartPanel.refreshCart();
                cardLayout.show(mainPanel, "CART");
            });
            logoutBtn.addActionListener(e -> doLogout());
            adminBtn.addActionListener(e -> {
                if ("admin".equals(loggedInUser)) {
                    adminPanel.refreshUI();
                    cardLayout.show(mainPanel, "ADMIN");
                } else {
                    JOptionPane.showMessageDialog(frame, "Admin only. Login as 'admin' user.", "Access denied", JOptionPane.WARNING_MESSAGE);
                }
            });

            refreshCategoryList();
        }

        public void refreshCategoryList() {
            catModel.clear();
            for (String c : categoryMap.keySet()) catModel.addElement(c);
            if (!catModel.isEmpty()) categoryJList.setSelectedIndex(0);
            loadProductsForSelectedCategory();
        }

        private void loadProductsForSelectedCategory() {
            productListModel.clear();
            String cat = categoryJList.getSelectedValue();
            if (cat == null) return;
            List<Product> products = categoryMap.getOrDefault(cat, Collections.emptyList());
            for (Product p : products) productListModel.addElement(p.toString());
        }

        private void addSelectedProductToCart() {
            String sel = productJList.getSelectedValue();
            if (sel == null) {
                JOptionPane.showMessageDialog(frame, "Select a product first.", "No selection", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            // parse id from product string: starts with [id]
            int id = parseIdFromListString(sel);
            if (!productById.containsKey(id)) {
                JOptionPane.showMessageDialog(frame, "Product not found.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Product p = productById.get(id);
            if (p.getStock() <= 0) {
                JOptionPane.showMessageDialog(frame, "Product out of stock.", "Out of stock", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String qtyStr = JOptionPane.showInputDialog(frame, "Enter quantity (Available: " + p.getStock() + "):", "1");
            if (qtyStr == null) return;
            int qty;
            try {
                qty = Integer.parseInt(qtyStr.trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Invalid quantity.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (qty <= 0 || qty > p.getStock()) {
                JOptionPane.showMessageDialog(frame, "Quantity must be >0 and <= available stock.", "Invalid qty", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // If already in cart, increase qty
            for (CartItem ci : cart) {
                if (ci.getProduct().getId() == p.getId()) {
                    ci.setQty(ci.getQty() + qty);
                    JOptionPane.showMessageDialog(frame, "Updated cart: " + p.getName() + " x " + ci.getQty());
                    return;
                }
            }
            cart.add(new CartItem(p, qty));
            JOptionPane.showMessageDialog(frame, "Added to cart: " + p.getName() + " x " + qty);
        }

        private int parseIdFromListString(String s) {
            // string format: [id] name - category - ₹price (Stock: n)
            try {
                int start = s.indexOf('[');
                int end = s.indexOf(']');
                if (start >= 0 && end > start) {
                    return Integer.parseInt(s.substring(start + 1, end));
                }
            } catch (Exception ignored) {}
            return -1;
        }

        private void doLogout() {
            loggedInUser = null;
            cart.clear();
            cardLayout.show(mainPanel, "LOGIN");
        }
    }

    class CartPanel extends JPanel {
        private final DefaultListModel<String> cartListModel = new DefaultListModel<>();
        private final JList<String> cartList = new JList<>(cartListModel);
        private final JButton removeBtn = new JButton("Remove Selected");
        private final JButton checkoutBtn = new JButton("Checkout");
        private final JButton backBtn = new JButton("Back to Shop");

        public CartPanel() {
            setLayout(new BorderLayout(10, 10));
            setBorder(new EmptyBorder(10,10,10,10));

            add(new JLabel("Your Cart"), BorderLayout.NORTH);
            add(new JScrollPane(cartList), BorderLayout.CENTER);

            JPanel bottom = new JPanel();
            bottom.add(removeBtn);
            bottom.add(checkoutBtn);
            bottom.add(backBtn);
            add(bottom, BorderLayout.SOUTH);

            removeBtn.addActionListener(e -> removeSelected());
            checkoutBtn.addActionListener(e -> doCheckout());
            backBtn.addActionListener(e -> cardLayout.show(mainPanel, "SHOP"));
        }

        public void refreshCart() {
            cartListModel.clear();
            double total = 0;
            if (cart.isEmpty()) cartListModel.addElement("<Cart is empty>");
            else {
                for (CartItem ci : cart) {
                    cartListModel.addElement(ci.toString() + " [Product ID: " + ci.getProduct().getId() + "]");
                    total += ci.getTotal();
                }
            }
            cartListModel.addElement(String.format("----- TOTAL: ₹%.2f -----", total));
        }

        private void removeSelected() {
            int idx = cartList.getSelectedIndex();
            if (idx < 0 || idx >= cart.size()) {
                JOptionPane.showMessageDialog(frame, "Select an item (not the total line) to remove.", "No selection", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            CartItem ci = cart.remove(idx);
            JOptionPane.showMessageDialog(frame, "Removed: " + ci.getProduct().getName());
            refreshCart();
        }

        private void doCheckout() {
            if (cart.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Cart is empty.", "Empty", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            // check stock & finalize
            StringBuilder sb = new StringBuilder();
            for (CartItem ci : cart) {
                if (ci.getQty() > ci.getProduct().getStock()) {
                    sb.append(String.format("Not enough stock for %s. Available: %d%n", ci.getProduct().getName(), ci.getProduct().getStock()));
                }
            }
            if (sb.length() > 0) {
                JOptionPane.showMessageDialog(frame, sb.toString(), "Stock issues", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // reduce stock
            double total = 0;
            for (CartItem ci : cart) {
                ci.getProduct().reduceStock(ci.getQty());
                total += ci.getTotal();
            }
            cart.clear();
            JOptionPane.showMessageDialog(frame, String.format("Order placed successfully! Paid ₹%.2f", total), "Success", JOptionPane.INFORMATION_MESSAGE);
            shopPanel.refreshCategoryList();
            cardLayout.show(mainPanel, "SHOP");
        }
    }

    class AdminPanel extends JPanel {
        private final JTextField nameField = new JTextField(15);
        private final JTextField categoryField = new JTextField(10);
        private final JTextField priceField = new JTextField(8);
        private final JTextField stockField = new JTextField(5);
        private final JButton addBtn = new JButton("Add Product");
        private final JButton backBtn = new JButton("Back");

        private final DefaultListModel<String> prodModel = new DefaultListModel<>();
        private final JList<String> prodList = new JList<>(prodModel);
        private final JButton refreshBtn = new JButton("Refresh List");

        public AdminPanel() {
            setLayout(new BorderLayout(10,10));
            setBorder(new EmptyBorder(10,10,10,10));

            JPanel form = new JPanel();
            form.add(new JLabel("Name:")); form.add(nameField);
            form.add(new JLabel("Category:")); form.add(categoryField);
            form.add(new JLabel("Price:")); form.add(priceField);
            form.add(new JLabel("Stock:")); form.add(stockField);
            form.add(addBtn);
            form.add(backBtn);

            add(form, BorderLayout.NORTH);

            add(new JLabel("All Products:"), BorderLayout.CENTER);
            add(new JScrollPane(prodList), BorderLayout.CENTER);

            JPanel bottom = new JPanel();
            bottom.add(refreshBtn);
            add(bottom, BorderLayout.SOUTH);

            addBtn.addActionListener(e -> doAddProduct());
            backBtn.addActionListener(e -> cardLayout.show(mainPanel, "SHOP"));
            refreshBtn.addActionListener(e -> refreshUI());
            refreshUI();
        }

        public void refreshUI() {
            prodModel.clear();
            for (Product p : productById.values()) prodModel.addElement(p.toString());
        }

        private void doAddProduct() {
            String name = nameField.getText().trim();
            String cat = categoryField.getText().trim();
            double price;
            int stock;
            try {
                price = Double.parseDouble(priceField.getText().trim());
                stock = Integer.parseInt(stockField.getText().trim());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Invalid price or stock.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (name.isEmpty() || cat.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Name and category required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            addProductInternal(name, cat, price, stock);
            // update category map reference
            categoryMap.computeIfAbsent(cat, k -> new ArrayList<>());
            // add product reference to categoryMap last inserted
            categoryMap.get(cat).add(productById.get(nextProductId - 1));
            refreshUI();
            shopPanel.refreshCategoryList();
            JOptionPane.showMessageDialog(frame, "Product added.");
        }
    }

    // ---------- Run ----------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ECommerceApp());
    }
}
