package wbs.console.supervisor;

import java.util.Map;

import javax.inject.Provider;

import lombok.NonNull;

import wbs.console.annotations.ConsoleModuleBuilderHandler;
import wbs.console.reporting.IntegerStatsFormatter;

import wbs.framework.builder.Builder;
import wbs.framework.builder.BuilderComponent;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

@PrototypeComponent ("supervisorIntegerStatsFormatterBuilder")
@ConsoleModuleBuilderHandler
public
class SupervisorIntegerStatsFormatterBuilder
	implements BuilderComponent {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// prototype dependencies

	@PrototypeDependency
	Provider <IntegerStatsFormatter> integerStatsFormatterProvider;

	// builder

	@BuilderParent
	SupervisorConfigSpec container;

	@BuilderSource
	SupervisorIntegerStatsFormatterSpec spec;

	@BuilderTarget
	SupervisorConfigBuilder supervisorConfigBuilder;

	// build

	@Override
	@BuildMethod
	public
	void build (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull Builder <TaskLogger> builder) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"build");

		) {

			String name =
				spec.name ();

			String targetBase =
				spec.targetBase ();

			Map<String,String> targetParams =
				spec.targetParams ();

			supervisorConfigBuilder.statsFormattersByName.put (
				name,
				integerStatsFormatterProvider.get ()

				.targetBase (
					targetBase)

				.targetParams (
					targetParams)

			);

		}

	}

}
