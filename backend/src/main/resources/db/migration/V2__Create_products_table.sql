-- Create products table
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    price DECIMAL(12, 2) NOT NULL,
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    image_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT true,
    category_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_category 
        FOREIGN KEY (category_id) 
        REFERENCES categories(id) 
        ON DELETE RESTRICT
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);
CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_is_active ON products(is_active);
CREATE INDEX IF NOT EXISTS idx_products_price ON products(price);

-- Create composite index for common queries
CREATE INDEX IF NOT EXISTS idx_products_category_active ON products(category_id, is_active);

-- Insert sample products
INSERT INTO products (name, description, price, stock_quantity, category_id, is_active) VALUES 
('Laptop Pro 15', 'High-performance laptop with 16GB RAM and 512GB SSD', 1299.99, 50, 1, true),
('Gaming Mouse', 'RGB gaming mouse with 12000 DPI and customizable buttons', 79.99, 100, 1, true),
('Mechanical Keyboard', 'Mechanical keyboard with Cherry MX switches', 149.99, 75, 1, true),
('4K Monitor', '27-inch 4K UHD monitor with HDR support', 399.99, 30, 1, true),
('Wireless Headphones', 'Noise-cancelling wireless headphones with 30h battery', 199.99, 60, 1, true),
('USB-C Hub', 'Multi-port USB-C hub with HDMI and USB 3.0', 49.99, 200, 1, true),
('SSD 1TB', 'Internal SSD 1TB NVMe M.2', 129.99, 150, 1, true),
('Webcam HD', '1080p HD webcam with auto-focus', 89.99, 80, 1, true),
('Desk Lamp', 'LED desk lamp with USB charging port', 39.99, 120, 1, true),
('Monitor Stand', 'Adjustable monitor stand with cable management', 59.99, 90, 1, true)
ON CONFLICT DO NOTHING;
