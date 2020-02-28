CREATE TABLE city (
  id INTEGER PRIMARY KEY,
  parent INTEGER,
  text TEXT
);


INSERT INTO city VALUES (1,0,'Asia');
INSERT INTO city VALUES (2,0,'Africa');
INSERT INTO city VALUES (3,0,'North America');
INSERT INTO city VALUES (4,0,'South America');
INSERT INTO city VALUES (5,0,'Antarctica');
INSERT INTO city VALUES (6,0,'Europe');
INSERT INTO city VALUES (7,0,'Australia');


INSERT INTO city VALUES (8,1,'Afghanistan');
INSERT INTO city VALUES (9,1,'Armenia');
INSERT INTO city VALUES (10,1,'Azerbaijan');
INSERT INTO city VALUES (11,1,'Bahrain');
INSERT INTO city VALUES (12,1,'China');
INSERT INTO city VALUES (13,1,'India');
INSERT INTO city VALUES (14,1,'Japan');
INSERT INTO city VALUES (15,1,'Malaysia');
INSERT INTO city VALUES (16,1,'Philippines');
INSERT INTO city VALUES (17,1,'Russia');
INSERT INTO city VALUES (18,1,'Singapore');
INSERT INTO city VALUES (19,1,'South Korea');
INSERT INTO city VALUES (20,1,'Thailand');
INSERT INTO city VALUES (21,1,'Turkey');
INSERT INTO city VALUES (22,1,'United Arab Emirates');


INSERT INTO city VALUES (23,2,'Madagascar');
INSERT INTO city VALUES (24,2,'Angola');
INSERT INTO city VALUES (25,2,'Cameroon');
INSERT INTO city VALUES (26,2,'Chad');
INSERT INTO city VALUES (27,2,'Algeria');
INSERT INTO city VALUES (28,2,'Egypt');
INSERT INTO city VALUES (29,2,'Libya');
INSERT INTO city VALUES (30,2,'Morocco');
INSERT INTO city VALUES (31,2,'Sudan');

