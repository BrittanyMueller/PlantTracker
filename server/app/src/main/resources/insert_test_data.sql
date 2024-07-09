DO $$
DECLARE
    pid INTEGER;
    moisture_id INTEGER;
BEGIN
    -- Insert test pi, generating serial id
    INSERT INTO pi (mac, location) 
    VALUES ('85:b6:e8:7e:45:d2', 'Dev Pi')
    RETURNING id INTO pid;

    -- Insert test moisture device for test pi
    INSERT INTO moisture_devices (name, num_sensors, pid)
    VALUES ('DEVTEST', 8, pid)
    RETURNING id INTO moisture_id;

    -- Insert test plants using generated pi and moisture device ids
    INSERT INTO plants (name, img_path, moisture_sensor_device_id, moisture_sensor_port, light_level, min_moisture, min_humidity, pid)
    VALUES 
        ('Snake Plant', 'test', moisture_id, 1, 0, 5, 50, pid),
        ('Money Tree', 'test', moisture_id, 2, 2, 6, 40, pid),
        ('Wobbly Boi', 'test', moisture_id, 3, 1, 4, 60, pid);
    COMMIT;
END $$;
