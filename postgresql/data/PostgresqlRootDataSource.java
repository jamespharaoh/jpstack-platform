package wbs.platform.postgresql.data;

import javax.sql.DataSource;

import lombok.NonNull;

import org.postgresql.ds.PGSimpleDataSource;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.HiddenComponent;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.config.WbsConfig;
import wbs.framework.component.tools.ComponentFactory;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

@SingletonComponent ("rootDataSource")
@HiddenComponent
public
class PostgresqlRootDataSource
	implements ComponentFactory <DataSource> {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	WbsConfig wbsConfig;

	// implementation

	@Override
	public
	DataSource makeComponent (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"makeComponent");

		) {

			//@SuppressWarnings ("unused")
			//Class <?> postgresqlDriverClass =
			//	org.postgresql.Driver.class;

			PGSimpleDataSource rootDataSource =
				new PGSimpleDataSource ();

			rootDataSource.setServerName (
				wbsConfig.database ().hostname ());

			rootDataSource.setDatabaseName (
				wbsConfig.database ().databaseName ());

			rootDataSource.setUser (
				wbsConfig.database ().username ());

			rootDataSource.setPassword (
				wbsConfig.database ().password ());

			return rootDataSource;

		}

	}

}
