CREATE TABLE product_local_attributes(
	id INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT,
	product_id INTEGER NOT NULL,
	name VARCHAR(256) NOT NULL,
	value VARCHAR(1024) NOT NULL,
	UNIQUE (product_id, name),
	FOREIGN KEY (product_id) REFERENCES products(id)
)
;
