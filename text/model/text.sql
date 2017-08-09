---------------------------------------- TABLE text

CREATE UNIQUE INDEX text_text
ON text (text);

---------------------------------------- FUNCTION textl

CREATE OR REPLACE FUNCTION textl (int)
RETURNS text AS $$
	SELECT text
	FROM text
	WHERE id = $1;
$$ LANGUAGE SQL;

---------------------------------------- FUNCTION text_lookup_or_create

CREATE OR REPLACE FUNCTION text_lookup_or_create (text)
RETURNS int AS $$
	DECLARE
		the_text ALIAS FOR $1;
		the_id int;
	BEGIN

		IF the_text IS NULL THEN
			RETURN NULL;
		END IF;

		SELECT INTO the_id id FROM text WHERE text = the_text;
		IF NOT FOUND THEN
			the_id = nextval ('text_id_seq');
			INSERT INTO TEXT (id, text)
				VALUES (the_id, the_text);
		END IF;
		RETURN the_id;
	END;
$$ LANGUAGE 'plpgsql';

---------------------------------------- FUNCTION atom

CREATE OR REPLACE FUNCTION atom (int) RETURNS text AS $$
	SELECT text FROM text WHERE id = $1;
$$ LANGUAGE SQL;

---------------------------------------- FUNCTION text_before_update

CREATE OR REPLACE FUNCTION text_before_update ()
RETURNS trigger AS $$
	BEGIN
		RAISE EXCEPTION 'The text table should never be updated';
	END;
$$ LANGUAGE 'plpgsql';

---------------------------------------- TRIGGER text_before_update

CREATE TRIGGER text_before_update
BEFORE UPDATE ON text FOR EACH ROW
EXECUTE PROCEDURE text_before_update ();

---------------------------------------- FUNCTION text_before_delete

CREATE OR REPLACE FUNCTION text_before_delete () RETURNS trigger AS $$
	BEGIN
		RAISE EXCEPTION 'Rows in the text table should never be deleted';
	END;
$$ LANGUAGE 'plpgsql';

---------------------------------------- TRIGGER text_before_delete

CREATE TRIGGER text_before_delete
BEFORE DELETE ON text FOR EACH ROW
EXECUTE PROCEDURE text_before_delete ();
