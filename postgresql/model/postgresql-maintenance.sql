
CREATE OR REPLACE FUNCTION postgresql_maintenance_insert (
	text,
	int,
	text)
RETURNS int AS $$

DECLARE

	new_frequency ALIAS FOR $1;
	new_sequence ALIAS FOR $2;
	new_command ALIAS FOR $3;

	new_maint_sched_id int;

BEGIN

	new_maint_sched_id :=
		nextval ('postgresql_maintenance_id_seq');

	INSERT INTO postgresql_maintenance (
		id,
		command,
		frequency,
		sequence)
	VALUES (
		new_maint_sched_id,
		new_command,
		new_frequency,
		new_sequence);

	RETURN new_maint_sched_id;

END;

$$ LANGUAGE 'plpgsql';
