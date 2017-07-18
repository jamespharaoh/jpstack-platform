SELECT postgresql_maintenance_insert ('m', 0, 'VACUUM VERBOSE ANALYZE');
SELECT postgresql_maintenance_insert ('w', 0, 'VACUUM VERBOSE ANALYZE');
SELECT postgresql_maintenance_insert ('d', 0, 'VACUUM VERBOSE ANALYZE');

SELECT postgresql_maintenance_insert (
	'1',
	0,
	'SELECT message_stats_update_log_process ()');

SELECT postgresql_maintenance_insert (
	'1',
	0,
	'SELECT sms_number_message_stats_update_log_process ()');
	