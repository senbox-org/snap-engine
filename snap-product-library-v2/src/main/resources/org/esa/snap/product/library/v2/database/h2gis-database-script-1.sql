CREATE TABLE versions(
	id SMALLINT PRIMARY KEY NOT NULL
)
;

CREATE TABLE sensor_types(
	id SMALLINT NOT NULL PRIMARY KEY,
	name VARCHAR(128) UNIQUE NOT NULL
)
;

CREATE TABLE pixel_types(
	id SMALLINT NOT NULL PRIMARY KEY,
	name VARCHAR(128) UNIQUE NOT NULL
)
;

CREATE TABLE data_format_types(
	id SMALLINT NOT NULL PRIMARY KEY,
	name VARCHAR(128) UNIQUE NOT NULL
)
;

CREATE TABLE local_repositories(
	id SMALLINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
	folder_path VARCHAR(2048) UNIQUE NOT NULL
)
;

CREATE TABLE remote_repositories(
	id SMALLINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
	name VARCHAR(256) UNIQUE NOT NULL
)
;

CREATE TABLE remote_missions(
	id SMALLINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
	name VARCHAR(256) NOT NULL,
	remote_repository_id SMALLINT NOT NULL,
	UNIQUE (remote_repository_id, name),
    FOREIGN KEY (remote_repository_id) REFERENCES remote_repositories(id)
)
;

CREATE TABLE remote_attributes(
	id SMALLINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
	name VARCHAR(256) NOT NULL,
	remote_mission_id SMALLINT NOT NULL,
	UNIQUE (remote_mission_id, name),
    FOREIGN KEY (remote_mission_id) REFERENCES remote_missions(id)
)
;

CREATE TABLE products(
	id INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT,
	name VARCHAR(512) NOT NULL,
	remote_mission_id SMALLINT,
	local_repository_id SMALLINT NOT NULL,
	relative_path VARCHAR(2048) NOT NULL,
	entry_point VARCHAR(512),
	size_in_bytes BIGINT NOT NULL,
	acquisition_date TIMESTAMP,
	last_modified_date TIMESTAMP NOT NULL,
    geometry GEOMETRY NOT NULL,
	data_format_type_id SMALLINT,
	pixel_type_id SMALLINT,
	sensor_type_id SMALLINT,
	UNIQUE (local_repository_id, relative_path),
    FOREIGN KEY (remote_mission_id) REFERENCES remote_missions(id),
    FOREIGN KEY (local_repository_id) REFERENCES local_repositories(id),
    FOREIGN KEY (data_format_type_id) REFERENCES data_format_types(id),
    FOREIGN KEY (pixel_type_id) REFERENCES pixel_types(id),
    FOREIGN KEY (sensor_type_id) REFERENCES sensor_types(id)
)
;

CREATE TABLE product_remote_attributes(
	id INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT,
	product_id INTEGER NOT NULL,
	name VARCHAR(256) NOT NULL,
	value VARCHAR(102400) NOT NULL,
	UNIQUE (product_id, name),
	FOREIGN KEY (product_id) REFERENCES products(id)
)
;

INSERT INTO sensor_types (id, name) VALUES (1, 'Optical')
;
INSERT INTO sensor_types (id, name) VALUES (2, 'Radar')
;
INSERT INTO sensor_types (id, name) VALUES (3, 'Altimetric')
;
INSERT INTO sensor_types (id, name) VALUES (4, 'Atmospheric')
;
INSERT INTO sensor_types (id, name) VALUES (5, 'Unknown')
;

INSERT INTO pixel_types (id, name) VALUES (1, 'Unsigned byte')
;
INSERT INTO pixel_types (id, name) VALUES (2, 'Signed byte')
;
INSERT INTO pixel_types (id, name) VALUES (3, 'Unsigned short')
;
INSERT INTO pixel_types (id, name) VALUES (4, 'Signed short')
;
INSERT INTO pixel_types (id, name) VALUES (5, 'Unsigned integer')
;
INSERT INTO pixel_types (id, name) VALUES (6, 'Signed integer')
;
INSERT INTO pixel_types (id, name) VALUES (7, 'Float')
;
INSERT INTO pixel_types (id, name) VALUES (8, 'Double')
;

INSERT INTO data_format_types (id, name) VALUES (1, 'Raster')
;
INSERT INTO data_format_types (id, name) VALUES (2, 'Vector')
;
INSERT INTO data_format_types (id, name) VALUES (3, 'Unknown')
;
