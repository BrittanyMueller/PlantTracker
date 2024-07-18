BEGIN; 

-- Delete test plant
DELETE FROM plants
WHERE img_path = 'test';

-- Delete test moisture device
DELETE FROM moisture_devices
WHERE name = 'DEVTEST';

DELETE FROM pi
WHERE mac = '85:b6:e8:7e:45:d2';

COMMIT;
