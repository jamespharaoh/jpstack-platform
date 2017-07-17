package wbs.platform.object.summary;

import lombok.NonNull;

import wbs.console.module.ConsoleModuleBuilderComponent;

import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

@PrototypeComponent ("objectSummaryBeanPartBuilder")
public
class ObjectSummaryBeanPartBuilder <
	ObjectType extends Record <ObjectType>,
	ParentType extends Record <ParentType>
> implements ConsoleModuleBuilderComponent {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <ObjectSummaryFieldsPart <ObjectType, ParentType>>
		summaryFieldsPartProvider;

	// builder

	@BuilderParent
	ObjectSummaryPageSpec objectSummaryPageSpec;

	@BuilderSource
	ObjectSummaryBeanPartSpec objectSummaryBeanPartSpec;

	@BuilderTarget
	ObjectSummaryPageBuilder <ObjectType, ParentType> objectSummaryPageBuilder;

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

			objectSummaryPageBuilder.addPart (
				objectSummaryBeanPartSpec.beanName ());

		}

	}

}
