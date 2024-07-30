BEGIN; 

-- Delete test plant
DELETE FROM plants
WHERE image_url = 'test';

-- Delete test moisture device
DELETE FROM moisture_devices
WHERE name = 'DEVTEST';

DELETE FROM pi
WHERE uuid = 'a6817b8d-e833-419c-a055-c72e85527cb8';

COMMIT;
