BEGIN; 

-- Delete test plant
DELETE FROM plants
WHERE image_url = 'test';

-- Delete test moisture device
DELETE FROM moisture_devices
WHERE name = 'DEVTEST';

DELETE FROM pi
WHERE uuid = 'e9a17b41-a512-489a-a958-a609fc43e0b1';

COMMIT;
