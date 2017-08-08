------------------------------------------------------------ TABLE object_type

CREATE OR REPLACE FUNCTION object_type_id (text)
RETURNS int AS $$

	SELECT id
	FROM object_type
	WHERE code = $1;

$$ LANGUAGE SQL STABLE;

CREATE OR REPLACE FUNCTION object_type_code (int)
RETURNS text AS $$

	SELECT code
	FROM object_type
	WHERE id = $1;

$$ LANGUAGE SQL STABLE;
