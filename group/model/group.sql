
CREATE TABLE user_group (

	group_id int
		NOT NULL
		REFERENCES "group",

	user_id int
		NOT NULL
		REFERENCES "user",

	PRIMARY KEY (
		group_id,
		user_id
	)

);

CREATE TABLE group_priv (

	priv_id int
		NOT NULL
		REFERENCES priv,

	group_id int
		NOT NULL
		REFERENCES "group",

	PRIMARY KEY (
		priv_id,
		group_id
	)

);
