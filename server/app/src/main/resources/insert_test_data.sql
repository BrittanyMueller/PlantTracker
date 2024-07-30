DO $$
DECLARE
    pid INTEGER;
    device_id INTEGER;
    plant1_id INTEGER;
    plant2_id INTEGER;
    plant3_id INTEGER;
BEGIN
    -- Insert test pi, generating serial id
    INSERT INTO pi (uuid, name) 
    VALUES ('a6817b8d-e833-419c-a055-c72e85527cb8', 'Dev Pi')
    RETURNING id INTO pid;

    -- Insert test moisture device for test pi
    INSERT INTO moisture_devices (name, num_sensors, pid)
    VALUES ('DEVTEST', 7, pid)
    RETURNING id INTO device_id;

    -- Insert test plants using generated pi and moisture device ids, storing ids
    INSERT INTO plants (name, image_url, light_level, min_moisture, min_humidity, pid)
        VALUES ('Snake Plant', 'test', 0, 5, 50, pid) RETURNING id INTO plant1_id;
    INSERT INTO plants (name, image_url, light_level, min_moisture, min_humidity, pid)
        VALUES ('Money Tree', 'test', 2, 6, 40, pid) RETURNING id INTO plant2_id;
    INSERT INTO plants (name, image_url, light_level, min_moisture, min_humidity, pid)
        VALUES ('Wobbly Boi', 'test', 1, 4, 60, pid) RETURNING id INTO plant3_id;

    -- Associate plants with device sensor ports 
    INSERT INTO sensors (moisture_device_id, sensor_port, plant_id)
    VALUES
        (device_id, 1, plant1_id),
        (device_id, 2, plant2_id),
        (device_id, 3, plant3_id),
        (device_id, 4, NULL), -- Unassigned sensor
        (device_id, 5, NULL); -- Unassigned sensor
    COMMIT;
END $$;
