CREATE TABLE IF NOT EXISTS pi (
    id SERIAL PRIMARY KEY,
    uuid VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(32)
);

CREATE TABLE IF NOT EXISTS moisture_devices (
    id SERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL,
    num_sensors INT NOT NULL,
    pid INT,
    FOREIGN KEY (pid) REFERENCES pi(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS plants (
    id SERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL,
    image_url VARCHAR(128),
    light_level INT, -- checked value 0, 1, 2? enum?
    min_moisture INT, -- checked value 1-10
    min_humidity INT, -- checked value 0-100
    pid INT,
    FOREIGN KEY (pid) REFERENCES pi(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS sensors (
    moisture_device_id INT,
    sensor_port INT,
    plant_id INT NULL,
    FOREIGN KEY (moisture_device_id) REFERENCES moisture_devices(id) ON DELETE CASCADE,
    FOREIGN KEY (plant_id) REFERENCES plants(id) ON DELETE CASCADE,
    PRIMARY KEY (moisture_device_id, sensor_port)
);

CREATE TABLE IF NOT EXISTS plant_sensor_data (
    plant_id INT,
    moisture REAL,
    light REAL, -- lumens? lux? what actually is this
    temp REAL,
    humidity REAL,
    ts TIMESTAMP NOT NULL,
    FOREIGN KEY (plant_id) REFERENCES plants(id),
    PRIMARY KEY (plant_id, ts)
);
