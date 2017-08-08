package wbs.platform.object.summary;

import static wbs.utils.etc.NumberUtils.integerToDecimalString;

import lombok.NonNull;

import wbs.console.component.ConsoleComponentBuilderComponent;
import wbs.console.part.TextPartFactory;

import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentManager;
import wbs.framework.component.registry.ComponentDefinition;
import wbs.framework.component.registry.ComponentRegistryBuilder;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.object.summary.ObjectSummaryPageComponentBuilder.ObjectSummaryComponentContext;

@PrototypeComponent ("objectSummaryHeadingComponentBuilder")
public
class ObjectSummaryHeadingComponentBuilder
	implements ConsoleComponentBuilderComponent {

	// singleton dependencies

	@SingletonDependency
	ComponentManager componentManager;

	@ClassSingletonDependency
	LogContext logContext;

	// builder

	@BuilderParent
	ObjectSummaryComponentContext context;

	@BuilderSource
	ObjectSummaryHeadingSpec spec;

	@BuilderTarget
	ComponentRegistryBuilder target;

	// build

	@BuildMethod
	@Override
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

			target.registerDefinition (
				taskLogger,
				new ComponentDefinition ()

				.nameFormat (
					"%sSummaryPartFactory%s",
					context.newComponentNamePrefix (),
					integerToDecimalString (
						context.numParts ()))

				.scope (
					"singleton")

				.componentClass (
					TextPartFactory.class)

				.addValuePropertyFormat (
					"text",
					"<h2>%h</h2>\n",
					spec.label ())

			);

			context.numParts (
				context.numParts () + 1);

		}

	}

}
