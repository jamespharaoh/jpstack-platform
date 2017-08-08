
CREATE OR REPLACE FUNCTION menu_group (text)
RETURNS integer AS $$

	SELECT id
	FROM menu_group
	WHERE code = $1;

$$ LANGUAGE SQL STABLE;
