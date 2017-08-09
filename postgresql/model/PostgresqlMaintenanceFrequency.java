package wbs.platform.postgresql.model;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

public
enum PostgresqlMaintenanceFrequency {

	monthly (
		"m",
		"Monthly"),

	weekly (
		"w",
		"Weekly"),

	daily (
		"d",
		"Daily"),

	hourly (
		"h",
		"Hourly"),

	fiveMinutes (
		"5",
		"5 minutes"),

	oneMinute (
		"1",
		"1 minute");

	@Getter
	String code;

	@Getter
	String description;

	static
	Map<String,PostgresqlMaintenanceFrequency> byCode =
		new HashMap<String,PostgresqlMaintenanceFrequency> ();

	static {

		for (PostgresqlMaintenanceFrequency value
				: values ()) {

			byCode.put (
				value.getCode (),
				value);

		}

	}

	PostgresqlMaintenanceFrequency (
			String newCode,
			String newDescription) {

		code =
			newCode;

		description =
			newDescription;

	}

	public static
	PostgresqlMaintenanceFrequency forCode (
			String code) {

		PostgresqlMaintenanceFrequency value =
			byCode.get (code);

		if (value == null)
			throw new IllegalArgumentException ();

		return value;

	}
}
