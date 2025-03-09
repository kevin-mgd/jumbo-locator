CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE stores (
                        id BIGSERIAL PRIMARY KEY,
                        store_id BIGINT NOT NULL UNIQUE,
                        city VARCHAR(255) NOT NULL,
                        postal_code VARCHAR(50) NOT NULL,
                        street VARCHAR(255) NOT NULL,
                        street2 VARCHAR(255),
                        street3 VARCHAR(255),
                        address_name VARCHAR(255) NOT NULL,
                        longitude DOUBLE PRECISION NOT NULL,
                        latitude DOUBLE PRECISION NOT NULL,
                        location GEOMETRY(Point, 4326) NOT NULL,
                        complex_number VARCHAR(100) NOT NULL,
                        show_warning_message BOOLEAN NOT NULL DEFAULT FALSE,
                        today_open VARCHAR(50) NOT NULL,
                        today_close VARCHAR(50) NOT NULL,
                        location_type VARCHAR(100) NOT NULL,
                        collection_point BOOLEAN NOT NULL DEFAULT FALSE,
                        sap_store_id VARCHAR(100) NOT NULL,
                        uuid VARCHAR(100) NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stores_location ON stores USING GIST (location);
CREATE INDEX idx_stores_city ON stores (LOWER(city));
CREATE INDEX idx_stores_postal_code ON stores (postal_code);
CREATE INDEX idx_stores_location_type ON stores (location_type);
CREATE INDEX idx_stores_collection_point ON stores (collection_point) WHERE collection_point = TRUE;
