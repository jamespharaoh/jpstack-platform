
------------------------------------------------------------ TABLE lock

CREATE OR REPLACE FUNCTION create_locks (int) RETURNS void AS '
	DECLARE
		max_id ALIAS FOR $1;
		this_id int;
	BEGIN
		LOCK TABLE lock;
		DELETE FROM lock;
		FOR this_id in 0..max_id-1 LOOP
			INSERT INTO lock (id) VALUES (this_id);
		END LOOP;
		RETURN;
	END;
' LANGUAGE 'plpgsql';

SELECT create_locks (16384);
