CREATE TABLE version(
	number SMALLINT PRIMARY KEY NOT NULL
)
;

CREATE TABLE component(
	id SMALLINT PRIMARY KEY NOT NULL,
	name VARCHAR(256) NOT NULL,
	description VARCHAR(512),
	parent_component_id SMALLINT,

	UNIQUE (name, parent_component_id),
	FOREIGN KEY (parent_component_id) REFERENCES component(id)
)
;
