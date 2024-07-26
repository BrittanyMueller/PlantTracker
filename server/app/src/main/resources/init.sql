CREATE TABLE IF NOT EXISTS pi (
    id SERIAL PRIMARY KEY,
    mac VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(32)
);

CREATE TABLE IF NOT EXISTS moisture_devices (
    id SERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL,
    num_sensors INT NOT NULL,
    pid INT,
    FOREIGN KEY (pid) REFERENCES pi(id)
);

CREATE TABLE IF NOT EXISTS plants (
    id SERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL,
    image_url VARCHAR(50),
    light_level INT, -- checked value 0, 1, 2? enum?
    min_moisture INT, -- checked value 1-10
    min_humidity INT, -- checked value 0-100
    pid INT,
    FOREIGN KEY (pid) REFERENCES pi(id)
);

CREATE TABLE IF NOT EXISTS sensors (
    moisture_device_id INT,
    sensor_port INT,
    plant_id INT NULL,
    FOREIGN KEY (moisture_device_id) REFERENCES moisture_devices(id),
    FOREIGN KEY (plant_id) REFERENCES plants(id),
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
