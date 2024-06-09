CREATE TABLE versions(
	id SMALLINT PRIMARY KEY NOT NULL
)
;

CREATE TABLE products(
	id INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT,
	stac_id VARCHAR(512),
	self_href VARCHAR(2048) NOT NULL,
	description VARCHAR(512) NOT NULL,
	acquisition_date TIMESTAMP,
    geometry GEOMETRY NOT NULL,
	UNIQUE (stac_id, self_href)
)
;
