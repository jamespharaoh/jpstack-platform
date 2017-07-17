package wbs.platform.postgresql.data;

import javax.sql.DataSource;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NamedDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.annotations.StrongPrototypeDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.component.tools.ComponentFactory;
import wbs.framework.database.DbPool;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

@SingletonComponent ("dataSource")
public
class PostgresqlDataSource
	implements ComponentFactory <DataSource> {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	@NamedDependency
	DataSource rootDataSource;

	// prototype dependencies

	@StrongPrototypeDependency
	ComponentProvider <DbPool> dbPoolProvider;

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

			return dbPoolProvider.provide (
				taskLogger,
				dbPool ->
					dbPool

				.dataSource (
					rootDataSource)

			);

		}

	}

}
