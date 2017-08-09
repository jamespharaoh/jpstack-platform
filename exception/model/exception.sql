CREATE INDEX exception_timestamp
ON exception (timestamp, id);

CREATE INDEX exception_alert_timestamp
ON exception (timestamp, id)
WHERE alert;

---------------------------------------------------------- TABLE exception_type

CREATE OR REPLACE FUNCTION exception_type (text) RETURNS int AS '
	DECLARE
		the_code ALIAS FOR $1;
		the_id int;
	BEGIN
		SELECT INTO the_id id FROM exception_type WHERE code = the_code;
		IF NOT FOUND THEN
			RETURN 0;
		END IF;
		RETURN the_id;
	END;
' LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION exception_type_insert (text, text)
RETURNS int AS $$

	INSERT INTO exception_type (id, code, description)
	VALUES (nextval ('exception_type_id_seq'), $1, $2);

	SELECT currval ('exception_type_id_seq')::int;

$$ LANGUAGE SQL;

---------------------------------------- FUNCTION exception_clear_alerts

CREATE OR REPLACE FUNCTION exception_clear_alerts (text)
RETURNS void AS $$
	DECLARE
		the_exception_type_code ALIAS FOR $1;
		the_exception_type_id int;
	BEGIN

		SELECT INTO the_exception_type_id id
		FROM exception_type
		WHERE code = the_exception_type_code;

		IF NOT FOUND THEN

			RAISE EXCEPTION 'Exception type not found: %',
				the_exception_type_code;

		END IF;

		UPDATE exception
		SET alert = false
		WHERE type_id = the_exception_type_id
			AND alert;

		RETURN;

	END;
$$ LANGUAGE 'plpgsql';

----------------------------------------------------- FUNCTION exception_insert

CREATE OR REPLACE FUNCTION exception_insert (text, text, text, int, text)
RETURNS int AS $$
	DECLARE
		the_type_code ALIAS FOR $1;
		new_source ALIAS FOR $2;
		new_summary ALIAS FOR $3;
		new_user_id ALIAS FOR $4;
		new_dump ALIAS FOR $5;

		new_id int;
	BEGIN

		new_id := nextval ('exception_id_seq');

		INSERT INTO exception (id, type_id, source, summary, user_id, dump)
		VALUES (new_id, exception_type (the_type_code), new_source, new_summary,
			new_user_id, new_dump);

		RETURN new_id;

	END;
$$ LANGUAGE 'plpgsql';
