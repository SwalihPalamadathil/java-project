-- ====================================================================
-- SHOP MANAGEMENT SYSTEM - DATABASE SCHEMA (SQLite / ANSI SQL)
-- ====================================================================

-- 1. USERS & AUTHENTICATION TABLE
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    full_name TEXT NOT NULL,
    role TEXT NOT NULL CHECK(role IN ('ADMIN', 'STAFF')),
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'LOCKED', 'INACTIVE')),
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    must_change_password INTEGER NOT NULL DEFAULT 0, -- 1 for True, 0 for False
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. CATEGORIES TABLE
CREATE TABLE IF NOT EXISTS categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    description TEXT
);

-- 3. SUPPLIERS TABLE
CREATE TABLE IF NOT EXISTS suppliers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    contact_person TEXT,
    phone TEXT,
    email TEXT,
    address TEXT
);

-- 4. PRODUCTS TABLE
CREATE TABLE IF NOT EXISTS products (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    sku TEXT NOT NULL UNIQUE,
    category_id INTEGER,
    brand TEXT,
    supplier_id INTEGER,
    unit TEXT NOT NULL DEFAULT 'pcs', -- pcs, kg, litre, pack
    purchase_price REAL NOT NULL CHECK(purchase_price >= 0),
    selling_price REAL NOT NULL CHECK(selling_price >= 0),
    tax_rate REAL NOT NULL DEFAULT 0.0 CHECK(tax_rate >= 0), -- in percent, e.g., 5.0, 18.0
    current_stock INTEGER NOT NULL DEFAULT 0 CHECK(current_stock >= 0),
    reorder_level INTEGER NOT NULL DEFAULT 5 CHECK(reorder_level >= 0),
    expiry_date TEXT, -- YYYY-MM-DD
    description TEXT,
    is_active INTEGER NOT NULL DEFAULT 1, -- 1 = active, 0 = soft deleted
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL
);

-- 5. STOCK MOVEMENTS (AUDIT TRAIL)
CREATE TABLE IF NOT EXISTS stock_movements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id INTEGER NOT NULL,
    type TEXT NOT NULL CHECK(type IN ('PURCHASE_IN', 'SALE_OUT', 'DAMAGE_LOSS', 'RETURN_IN', 'MANUAL_ADJUSTMENT')),
    quantity INTEGER NOT NULL,
    stock_before INTEGER NOT NULL,
    stock_after INTEGER NOT NULL,
    reason TEXT,
    reference_number TEXT, -- Invoice # or Supplier Bill #
    created_by TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- 6. SALES (BILLING INVOICES)
CREATE TABLE IF NOT EXISTS sales (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_number TEXT NOT NULL UNIQUE,
    customer_name TEXT,
    customer_phone TEXT,
    subtotal REAL NOT NULL,
    tax_amount REAL NOT NULL,
    discount_amount REAL NOT NULL DEFAULT 0.0,
    grand_total REAL NOT NULL,
    payment_method TEXT NOT NULL CHECK(payment_method IN ('CASH', 'CARD', 'UPI')),
    amount_tendered REAL NOT NULL,
    change_due REAL NOT NULL DEFAULT 0.0,
    created_by TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status TEXT NOT NULL DEFAULT 'COMPLETED' CHECK(status IN ('COMPLETED', 'REFUNDED'))
);

-- 7. SALE ITEMS
CREATE TABLE IF NOT EXISTS sale_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sale_id INTEGER NOT NULL,
    product_id INTEGER NOT NULL,
    product_name TEXT NOT NULL,
    sku TEXT NOT NULL,
    unit_price REAL NOT NULL,
    quantity INTEGER NOT NULL CHECK(quantity > 0),
    tax_rate REAL NOT NULL DEFAULT 0.0,
    tax_amount REAL NOT NULL DEFAULT 0.0,
    discount REAL NOT NULL DEFAULT 0.0,
    total_price REAL NOT NULL,
    FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- 8. EMPLOYEES
CREATE TABLE IF NOT EXISTS employees (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    phone TEXT NOT NULL,
    address TEXT,
    designation TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'STAFF',
    joining_date TEXT NOT NULL, -- YYYY-MM-DD
    base_salary REAL NOT NULL CHECK(base_salary >= 0),
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'INACTIVE'))
);

-- 9. ATTENDANCE
CREATE TABLE IF NOT EXISTS attendance (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    employee_id INTEGER NOT NULL,
    date TEXT NOT NULL, -- YYYY-MM-DD
    status TEXT NOT NULL CHECK(status IN ('PRESENT', 'ABSENT', 'HALF_DAY', 'LEAVE')),
    remarks TEXT,
    UNIQUE(employee_id, date),
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

-- 10. SALARY RECORDS
CREATE TABLE IF NOT EXISTS salary_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    employee_id INTEGER NOT NULL,
    month_year TEXT NOT NULL, -- YYYY-MM
    base_salary REAL NOT NULL,
    absence_deduction REAL NOT NULL DEFAULT 0.0,
    overtime_pay REAL NOT NULL DEFAULT 0.0,
    bonus REAL NOT NULL DEFAULT 0.0,
    advance_deduction REAL NOT NULL DEFAULT 0.0,
    net_salary REAL NOT NULL,
    payment_date TEXT, -- YYYY-MM-DD
    payment_status TEXT NOT NULL DEFAULT 'UNPAID' CHECK(payment_status IN ('PAID', 'UNPAID')),
    payment_method TEXT DEFAULT 'CASH',
    remarks TEXT,
    UNIQUE(employee_id, month_year),
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

-- 11. SHOP SETTINGS
CREATE TABLE IF NOT EXISTS settings (
    setting_key TEXT PRIMARY KEY,
    setting_value TEXT NOT NULL,
    description TEXT
);

-- INDEXES FOR PERFORMANCE
CREATE INDEX IF NOT EXISTS idx_products_sku ON products(sku);
CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_sales_invoice ON sales(invoice_number);
CREATE INDEX IF NOT EXISTS idx_sales_created_at ON sales(created_at);
CREATE INDEX IF NOT EXISTS idx_stock_movements_prod ON stock_movements(product_id);
CREATE INDEX IF NOT EXISTS idx_attendance_emp_date ON attendance(employee_id, date);
CREATE INDEX IF NOT EXISTS idx_salary_emp_month ON salary_records(employee_id, month_year);

-- ====================================================================
-- SEED DATA
-- ====================================================================

-- Default Shop Settings
INSERT OR IGNORE INTO settings (setting_key, setting_value, description) VALUES
('shop_name', 'Apex Supermarket & General Store', 'Retail Shop Business Name'),
('shop_address', '104 Market Road, City Centre, Metro 560001', 'Physical Shop Address'),
('shop_phone', '+1 (555) 019-2834', 'Contact Telephone Number'),
('shop_gst', '29ABCDE1234F1Z5', 'Tax / GST Identification Number'),
('shop_currency', '$', 'Currency symbol used across UI and invoices'),
('tax_rate_default', '5.0', 'Default tax percentage for new items');

-- Default Categories (5 categories)
INSERT OR IGNORE INTO categories (id, name, description) VALUES
(1, 'Groceries & Staples', 'Rice, flour, grains, sugar, edible oils, and pulses'),
(2, 'Beverages & Drinks', 'Soft drinks, bottled water, tea, coffee, and packaged juices'),
(3, 'Snacks & Confectionery', 'Biscuits, chips, chocolate, dry snacks, and confectionery'),
(4, 'Dairy & Breakfast', 'Milk, butter, cheese, yogurt, bread, and breakfast cereals'),
(5, 'Personal & Home Care', 'Soaps, shampoos, detergents, oral care, and cleaning essentials');

-- Default Suppliers (3 suppliers)
INSERT OR IGNORE INTO suppliers (id, name, contact_person, phone, email, address) VALUES
(1, 'Global Metro Wholesalers', 'Robert Jenkins', '+1-555-111-2222', 'orders@metrowholesale.com', 'Warehouse 12, Logistics Park'),
(2, 'Fresh Valley Agro Suppliers', 'Sarah Miller', '+1-555-333-4444', 'contact@freshvalley.com', 'Farm Route 4, Green Valley'),
(3, 'Apex Consumer Distributing', 'David Zhao', '+1-555-555-6666', 'sales@apexconsumer.com', 'Industrial Area Phase 2');

-- Default Employees (3 employees)
INSERT OR IGNORE INTO employees (id, name, phone, address, designation, role, joining_date, base_salary, status) VALUES
(1, 'Johnathan Miller', '+1-555-777-8888', '12 Oak Ridge Lane', 'Store Manager', 'ADMIN', '2023-01-15', 3500.00, 'ACTIVE'),
(2, 'Emily Watson', '+1-555-888-9999', '45 Maple Boulevard', 'Senior Cashier', 'STAFF', '2023-06-01', 2200.00, 'ACTIVE'),
(3, 'Michael Chang', '+1-555-999-0000', '78 Pinecrest Drive', 'Sales & Inventory Clerk', 'STAFF', '2024-02-10', 1800.00, 'ACTIVE');

-- Default Products (20 Sample Products)
INSERT OR IGNORE INTO products (id, name, sku, category_id, brand, supplier_id, unit, purchase_price, selling_price, tax_rate, current_stock, reorder_level, expiry_date, description, is_active) VALUES
(1, 'Basmati Rice Premium 5kg', '890103000101', 1, 'Royal Harvest', 1, 'pcs', 12.50, 16.99, 5.0, 45, 10, '2026-12-31', 'Aged long-grain aromatic basmati rice', 1),
(2, 'Whole Wheat Flour 10kg', '890103000102', 1, 'Golden Grain', 1, 'pcs', 8.20, 11.50, 5.0, 30, 8, '2026-11-30', '100% stone ground whole wheat flour', 1),
(3, 'Refined Sunflower Oil 1L', '890103000103', 1, 'SunPure', 1, 'pcs', 2.10, 3.25, 5.0, 60, 15, '2026-10-15', 'Enriched with Vitamin A and D', 1),
(4, 'Organic Raw Sugar 1kg', '890103000104', 1, 'SweetCrest', 1, 'pcs', 1.10, 1.85, 5.0, 50, 12, '2027-01-01', 'Pure unrefined cane sugar crystals', 1),
(5, 'Red Lentils (Masoor) 1kg', '890103000105', 1, 'NatureFresh', 2, 'pcs', 1.80, 2.65, 5.0, 25, 10, '2026-09-30', 'High protein split red lentils', 1),

(6, 'Classic Roast Coffee 200g', '890203000201', 2, 'CaféAroma', 3, 'pcs', 4.50, 6.99, 12.0, 35, 10, '2026-08-31', 'Rich blend roasted ground coffee', 1),
(7, 'English Breakfast Tea 100 bags', '890203000202', 2, 'TwinCrown', 3, 'pcs', 3.20, 5.40, 12.0, 40, 12, '2026-07-20', 'Traditional full-bodied black tea bags', 1),
(8, 'Sparkling Mineral Water 1L', '890203000203', 2, 'AquaPure', 1, 'pcs', 0.60, 1.25, 18.0, 80, 20, '2027-05-10', 'Naturally carbonated mineral water', 1),
(9, 'Organic Orange Juice 1L', '890203000204', 2, 'OrchardBliss', 2, 'pcs', 1.90, 3.10, 12.0, 18, 10, '2026-05-15', '100% freshly squeezed no added sugar', 1),
(10, 'Cola Refresh Can 330ml', '890203000205', 2, 'SparkCola', 3, 'pcs', 0.45, 0.99, 18.0, 120, 24, '2026-12-01', 'Crisp chilled carbonated soft drink', 1),

(11, 'Salted Potato Crisps 150g', '890303000301', 3, 'CrunchTime', 3, 'pcs', 0.85, 1.75, 12.0, 65, 15, '2026-04-18', 'Kettle-cooked salted crispy chips', 1),
(12, 'Dark Chocolate Bar 100g 70%', '890303000302', 3, 'VelvetCacao', 3, 'pcs', 1.40, 2.95, 18.0, 40, 10, '2026-10-30', 'Rich single-origin 70% cacao chocolate', 1),
(13, 'Butter Cookies Tin 400g', '890303000303', 3, 'RoyalDan', 3, 'pcs', 3.10, 5.20, 12.0, 22, 8, '2026-08-15', 'Traditional rich Danish butter cookies', 1),
(14, 'Roasted Almonds 200g', '890303000304', 3, 'NutriBite', 2, 'pcs', 3.80, 5.90, 5.0, 14, 8, '2026-09-01', 'Lightly salted California roasted almonds', 1),

(15, 'Full Cream Fresh Milk 1L', '890403000401', 4, 'MeadowPure', 2, 'pcs', 0.95, 1.45, 5.0, 4, 15, '2026-03-25', 'Pasteurized homogenized whole milk', 1),
(16, 'Salted Dairy Butter 250g', '890403000402', 4, 'MeadowPure', 2, 'pcs', 1.60, 2.50, 12.0, 15, 8, '2026-06-20', 'Traditional churned salted table butter', 1),
(17, 'Cheddar Cheese Block 400g', '890403000403', 4, 'AlpineDairy', 2, 'pcs', 2.90, 4.80, 12.0, 12, 6, '2026-07-10', 'Naturally aged sharp cheddar block', 1),
(18, 'Farm Fresh Eggs (Pack of 12)', '890403000404', 4, 'SunHens', 2, 'pcs', 1.80, 2.80, 5.0, 3, 10, '2026-03-30', 'Grade A large free-range brown eggs', 1),

(19, 'Moisturizing Bath Soap 125g', '890503000501', 5, 'SilkTouch', 1, 'pcs', 0.70, 1.35, 18.0, 55, 15, '2027-08-01', 'Enriched with almond oil and shea butter', 1),
(20, 'Dishwashing Gel 500ml', '890503000502', 5, 'CleanSpark', 1, 'pcs', 1.20, 2.15, 18.0, 30, 10, '2027-02-15', 'Lemon power degreaser liquid formula', 1);

-- Note: Users are seeded dynamically by DatabaseInitializer with BCrypt hashing:
-- Admin: username='admin', password='Admin@123', role='ADMIN', must_change_password=1
-- Cashier: username='cashier', password='Cashier@123', role='STAFF', must_change_password=0
