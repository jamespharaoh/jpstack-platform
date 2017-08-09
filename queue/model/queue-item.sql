CREATE OR REPLACE VIEW queue_item_view AS

	SELECT

		queue_item.id AS queue_item_id,
		queue_item.queue_id AS queue_id,
		queue_item.created_time AS timestamp,
		queue_item.processed_user_id AS processed_by_user_id,

		1 AS num_created,
		0 AS num_processed,
		0 AS num_preferred,
		0 AS num_not_preferred

	FROM queue_item

UNION

	SELECT

		queue_item.id AS queue_item_id,
		queue_item.queue_id AS queue_id,
		queue_item.processed_time AS timestamp,
		queue_item.processed_user_id AS processed_by_user_id,

		0 AS num_created,
		1 AS num_processed,

		CASE
			WHEN queue_item.processed_by_preferred_user THEN 1
			ELSE 0
		END AS num_preferred,

		CASE
			WHEN queue_item.processed_by_preferred_user THEN 0
			ELSE 1
		END AS num_not_preferred

	FROM queue_item

	WHERE queue_item.processed_user_id IS NOT NULL

;
