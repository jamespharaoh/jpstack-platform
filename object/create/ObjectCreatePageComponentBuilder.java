package wbs.platform.object.create;

import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.string.StringUtils.capitalise;
import static wbs.utils.string.StringUtils.capitaliseFormat;
import static wbs.utils.string.StringUtils.hyphenToCamel;
import static wbs.utils.string.StringUtils.hyphenToCamelCapitalise;
import static wbs.utils.string.StringUtils.stringFormat;

import lombok.NonNull;

import wbs.console.component.ConsoleComponentBuilderComponent;
import wbs.console.component.ConsoleComponentBuilderContext;
import wbs.console.tab.TabContextResponder;

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

@PrototypeComponent ("objectCreatePageComponentBuilder")
public
class ObjectCreatePageComponentBuilder
	implements ConsoleComponentBuilderComponent {

	// singleton dependencies

	@SingletonDependency
	ComponentManager componentManager;

	@ClassSingletonDependency
	LogContext logContext;

	// builder

	@BuilderParent
	ConsoleComponentBuilderContext context;

	@BuilderSource
	ObjectCreatePageSpec spec;

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

			String name =
				ifNull (
					spec.name (),
					"create");

			String tabName =
				ifNull (
					spec.tabName (),
					stringFormat (
						"%s.%s",
						context.pathPrefix (),
						name));

			String title =
				capitaliseFormat (
					"%s create",
					context.friendlyName ());

			String partFactoryName =
				ifNull (
					spec.responderName (),
					stringFormat (
						"%s%sPartFactory",
						context.newComponentNamePrefix (),
						capitalise (
							name)));

			String responderName =
				ifNull (
					spec.responderName (),
					stringFormat (
						"%s%sResponder",
						context.newComponentNamePrefix (),
						capitalise (
							name)));

			String localFile =
				ifNull (
					spec.localFile (),
					stringFormat (
						"%s.%s",
						context.pathPrefix (),
						name));

			target.registerDefinition (
				taskLogger,
				new ComponentDefinition ()

				.name (
					partFactoryName)

				.scope (
					"singleton")

				.componentClass (
					ObjectCreatePartFactory.class)

				.hide (
					true)

				.addReferencePropertyFormat (
					"consoleHelper",
					"singleton",
					"%sConsoleHelper",
					hyphenToCamel (
						context.objectType ()))

				.addReferencePropertyFormat (
					"formType",
					"singleton",
					"%s%sFormType",
					hyphenToCamel (
						context.consoleModule ().name ()),
					hyphenToCamelCapitalise (
						spec.formTypeName ()))

				.addValueProperty (
					"localFile",
					optionalOf (
						localFile))

			);

			target.registerDefinition (
				taskLogger,
				new ComponentDefinition ()

				.name (
					responderName)

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

				.addReferenceProperty (
					"pagePartFactory",
					"singleton",
					partFactoryName)

			);

		}

	}

}
