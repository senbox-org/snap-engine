CREATE TABLE versions(
	number SMALLINT PRIMARY KEY NOT NULL
)
;

CREATE TABLE local_repositories(
	id INTEGER NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
	path VARCHAR(1024) UNIQUE NOT NULL
)
;

CREATE TABLE remote_repositories(
	id INTEGER NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
	name VARCHAR(256) UNIQUE NOT NULL
)
;

CREATE TABLE remote_repository_missions(
	id INTEGER NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
	remote_repository_id INTEGER NOT NULL,
	mission VARCHAR(256) NOT NULL,
	UNIQUE (remote_repository_id, mission),
	FOREIGN KEY (remote_repository_id) REFERENCES remote_repositories(id)
)
;

CREATE TABLE products(
	id INTEGER NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
	name VARCHAR(256) NOT NULL,
	remote_repository_mission_id INTEGER NOT NULL,
	local_repository_id INTEGER NOT NULL,
	type VARCHAR(256) NOT NULL,
	path VARCHAR(1024) NOT NULL,
	metadata_filename VARCHAR(256) NOT NULL,
	size_in_bytes BIGINT NOT NULL,
	acquisition_date TIMESTAMP NOT NULL,
	last_modified_date TIMESTAMP NOT NULL,
	first_near_latitude DOUBLE NOT NULL,
	first_near_longitude DOUBLE NOT NULL,
	first_far_latitude DOUBLE NOT NULL,
	first_far_longitude DOUBLE NOT NULL,
	last_near_latitude DOUBLE NOT NULL,
	last_near_longitude DOUBLE NOT NULL,
	last_far_latitude DOUBLE NOT NULL,
	last_far_longitude DOUBLE NOT NULL,
	geometric_boundary VARCHAR(1500) NOT NULL,
    FOREIGN KEY (remote_repository_mission_id) REFERENCES remote_repository_missions(id),
    FOREIGN KEY (local_repository_id) REFERENCES local_repositories(id)
)
;

CREATE TABLE product_remote_attributes(
	id INTEGER NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
	product_id INTEGER NOT NULL,
	name VARCHAR(256) NOT NULL,
	value VARCHAR(256) NOT NULL,
	UNIQUE (product_id, name),
	FOREIGN KEY (product_id) REFERENCES products(id)
)
;
