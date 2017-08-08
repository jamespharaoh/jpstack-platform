
CREATE OR REPLACE VIEW pg_my_tables_summary
AS SELECT

	rel.oid,

	rel.relname
	AS name,

	reltuples::bigint
	AS tuples,

	pg_relation_size (rel.oid)
	AS size,

	coalesce (
		sum (pg_relation_size (idx.indexrelid)),
		0)
	AS index_size,

	pg_relation_size (rel.oid)
	+ coalesce (
		sum (pg_relation_size (idx.indexrelid)),
		0)
	AS total_size

FROM pg_class
AS rel

LEFT JOIN pg_index
AS idx
ON rel.oid = idx.indrelid

WHERE rel.relkind = 'r'

GROUP BY
	rel.oid,
	rel.relname,
	rel.reltuples,
	pg_relation_size (rel.oid);
