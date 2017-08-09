package wbs.platform.object.summary;

import static wbs.utils.etc.NullUtils.isNotNull;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.string.StringUtils.capitalise;
import static wbs.utils.string.StringUtils.hyphenToCamel;
import static wbs.utils.string.StringUtils.hyphenToCamelCapitalise;
import static wbs.utils.string.StringUtils.stringFormat;

import java.util.stream.Collectors;
import java.util.stream.LongStream;

import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

import wbs.console.component.ConsoleComponentBuilderComponent;
import wbs.console.component.ConsoleComponentBuilderContext;
import wbs.console.module.ConsoleModuleSpec;
import wbs.console.part.ProviderPagePartFactory;
import wbs.console.tab.TabContextResponder;

import wbs.framework.builder.Builder;
import wbs.framework.builder.Builder.MissingBuilderBehaviour;
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

@PrototypeComponent ("objectSummaryPageComponentBuilder")
public
class ObjectSummaryPageComponentBuilder
	implements ConsoleComponentBuilderComponent {

	// singleton dependencies

	@SingletonDependency
	ComponentManager componentManager;

	@ClassSingletonDependency
	LogContext logContext;

	// builder

	@BuilderParent
	ConsoleComponentBuilderContext parentContext;

	@BuilderSource
	ObjectSummaryPageSpec spec;

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

			String tabName =
				stringFormat (
					"%s.summary",
					parentContext.pathPrefix ());

			String title =
				capitalise (
					stringFormat (
						"%s summary",
						parentContext.friendlyName ()));

			ObjectSummaryComponentContext childContext =
				new ObjectSummaryComponentContext ()

				.consoleModule (
					parentContext.consoleModule ())

				.existingComponentNamePrefix (
					parentContext.existingComponentNamePrefix ())

				.newComponentNamePrefix (
					parentContext.newComponentNamePrefix ())

				.componentName (
					stringFormat (
						"%sSummary",
						parentContext.newComponentNamePrefix ()))

				.objectType (
					parentContext.objectType ())

				.numParts (
					0l)

			;

			// create summary errors part

			target.registerDefinition (
				taskLogger,
				new ComponentDefinition ()

				.nameFormat (
					"%sSummaryPart%s",
					parentContext.newComponentNamePrefix (),
					integerToDecimalString (
						childContext.numParts ()))

				.scope (
					"prototype")

				.componentClass (
					ObjectSummaryErrorsPart.class)

				.hide (
					true)

				.addReferencePropertyFormat (
					"consoleHelper",
					"singleton",
					"%sConsoleHelper",
					hyphenToCamel (
						parentContext.objectType ()))

			);

			target.registerDefinition (
				taskLogger,
				new ComponentDefinition ()

				.nameFormat (
					"%sSummaryPartFactory%s",
					parentContext.newComponentNamePrefix (),
					integerToDecimalString (
						childContext.numParts ()))

				.scope (
					"singleton")

				.componentClass (
					ProviderPagePartFactory.class)

				.addReferencePropertyFormat (
					"pagePartProvider",
					"prototype",
					"%sSummaryPart%s",
					parentContext.newComponentNamePrefix (),
					integerToDecimalString (
						childContext.numParts ()))

			);

			childContext.numParts (
				childContext.numParts () + 1);

			// add create summary details part

			if (
				isNotNull (
					spec.formTypeName ())
			) {

				target.registerDefinition (
					taskLogger,
					new ComponentDefinition ()

					.nameFormat (
						"%sSummaryPart%s",
						parentContext.newComponentNamePrefix (),
						integerToDecimalString (
							childContext.numParts ()))

					.scope (
						"prototype")

					.componentClass (
						ObjectSummaryFieldsPart.class)

					.hide (
						true)

					.addReferencePropertyFormat (
						"consoleHelper",
						"singleton",
						"%sConsoleHelper",
						hyphenToCamel (
							parentContext.objectType ()))

					.addReferencePropertyFormat (
						"formType",
						"singleton",
						"%s%sFormType",
						hyphenToCamel (
							parentContext.consoleModule ().name ()),
						hyphenToCamelCapitalise (
							spec.formTypeName ()))

					.addValuePropertyFormat (
						"heading",
						"%s details",
						capitalise (
							parentContext.friendlyName ()))

				);

				target.registerDefinition (
					taskLogger,
					new ComponentDefinition ()

					.nameFormat (
						"%sSummaryPartFactory%s",
						parentContext.newComponentNamePrefix (),
						integerToDecimalString (
							childContext.numParts ()))

					.scope (
						"singleton")

					.componentClass (
						ProviderPagePartFactory.class)

					.addReferencePropertyFormat (
						"pagePartProvider",
						"prototype",
						"%sSummaryPart%s",
						parentContext.newComponentNamePrefix (),
						integerToDecimalString (
							childContext.numParts ()))

				);

				childContext.numParts (
					childContext.numParts () + 1);

			}

			// add custom parts

			builder.descend (
				taskLogger,
				childContext,
				spec.builders (),
				target,
				MissingBuilderBehaviour.error);

			// create summary part

			target.registerDefinition (
				taskLogger,
				new ComponentDefinition ()

				.nameFormat (
					"%sSummaryPart",
					parentContext.newComponentNamePrefix ())

				.scope (
					"prototype")

				.componentClass (
					ObjectSummaryPart.class)

				.hide (
					true)

				.addReferenceListProperty (
					"partFactories",
					"singleton",
					LongStream.range (
						0l,
						childContext.numParts ())

						.mapToObj (
							index ->
								stringFormat (
									"%sSummaryPartFactory%s",
									parentContext.newComponentNamePrefix (),
									integerToDecimalString (
										index)))

						.collect (
							Collectors.toList ())

				)

			);

			target.registerDefinition (
				taskLogger,
				new ComponentDefinition ()

				.nameFormat (
					"%sSummaryPartFactory",
					parentContext.newComponentNamePrefix ())

				.scope (
					"singleton")

				.componentClass (
					ProviderPagePartFactory.class)

				.addReferencePropertyFormat (
					"pagePartProvider",
					"prototype",
					"%sSummaryPart",
					parentContext.newComponentNamePrefix ())

			);

			target.registerDefinition (
				taskLogger,
				new ComponentDefinition ()

				.nameFormat (
					"%sSummaryResponder",
					parentContext.newComponentNamePrefix ())

				.scope (
					"prototype")

				.componentClass (
					TabContextResponder.class)

				.hide (
					true)

				.addValueProperty (
					"tab",
					optionalOf (
						tabName))

				.addValueProperty (
					"title",
					optionalOf (
						title))

				.addReferencePropertyFormat (
					"pagePartFactory",
					"singleton",
					"%sSummaryPartFactory",
					parentContext.newComponentNamePrefix ())

			);

			/*
			builder.descend (
				taskLogger,
				childContext,
				spec.builders (),
				target,
				MissingBuilderBehaviour.error);

			target.registerDefinition (
				taskLogger,
				new ComponentDefinition ()

				.name (
					stringFormat (
						"

			PagePartFactory partFactory =
				parentTransaction -> {

				try (

					NestedTransaction transaction =
						parentTransaction.nestTransaction (
							logContext,
							"buildResponder");

				) {

					return objectSummaryPartProvider.get ().partFactories (
						pagePartFactories);

				}

			};

				tabContextResponder.get ()

					.tab (
						stringFormat (
							"%s.summary",
							container.pathPrefix ()))

					.title (
						capitalise (
							stringFormat (
								"%s summary",
								consoleHelper.friendlyName ())))

					.pagePartFactory (
						partFactory));
			*/

		}

	}

	@Accessors (fluent = true)
	@Data
	public static
	class ObjectSummaryComponentContext {

		ConsoleModuleSpec consoleModule;

		String newComponentNamePrefix;
		String existingComponentNamePrefix;

		String objectType;

		String componentName;
		Long numParts;

	}

}
