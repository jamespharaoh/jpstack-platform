package wbs.platform.postgresql.hibernate;

import java.sql.Types;

import wbs.framework.hibernate.EnumUserType;

import wbs.platform.postgresql.model.PostgresqlMaintenanceFrequency;

public
class PostgresqlMaintenanceFrequencyType
	extends EnumUserType<String,PostgresqlMaintenanceFrequency> {

	{

		sqlType (Types.VARCHAR);
		enumClass (PostgresqlMaintenanceFrequency.class);

		add ("m", PostgresqlMaintenanceFrequency.monthly);
		add ("w", PostgresqlMaintenanceFrequency.weekly);
		add ("d", PostgresqlMaintenanceFrequency.daily);
		add ("h", PostgresqlMaintenanceFrequency.hourly);
		add ("5", PostgresqlMaintenanceFrequency.fiveMinutes);
		add ("1", PostgresqlMaintenanceFrequency.oneMinute);

	}

}
