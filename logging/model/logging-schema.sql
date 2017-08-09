CREATE TABLE logging_global_rule_event (

	logging_global_rule_id int NOT NULL REFERENCES logging_global_rule,
	logging_event_id int NOT NULL REFERENCES logging_event,

	PRIMARY KEY (
		logging_global_rule_id,
		logging_event_id
	)

);

CREATE TABLE logging_dynamic_context_rule_event (

	logging_dynamic_context_rule_id int NOT NULL
	REFERENCES logging_dynamic_context_rule,

	logging_event_id int NOT NULL REFERENCES logging_event,

	PRIMARY KEY (
		logging_dynamic_context_rule_id,
		logging_event_id
	)

);

CREATE TABLE logging_static_context_rule_event (

	logging_static_context_rule_id int NOT NULL
	REFERENCES logging_static_context_rule,

	logging_event_id int NOT NULL REFERENCES logging_event,

	PRIMARY KEY (
		logging_static_context_rule_id,
		logging_event_id
	)

);

-- ex: noet ts=4 filetype=sql