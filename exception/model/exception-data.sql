INSERT INTO exception_type (
	id,
	code,
	description)
VALUES (
	0,
	'unknown',
	'Unknown');

SELECT exception_type_insert (
	'agent',
	'Agent');

SELECT exception_type_insert (
	'daemon',
	'Daemon');

SELECT exception_type_insert (
	'console',
	'Console');

SELECT exception_type_insert (
	'webapi',
	'Web API');

SELECT exception_type_insert (
	'external',
	'External');
