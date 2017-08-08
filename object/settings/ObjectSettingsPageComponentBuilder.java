package wbs.platform.object.settings;

import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.string.StringUtils.camelToSpaces;
import static wbs.utils.string.StringUtils.capitalise;
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

@PrototypeComponent ("objectSettingsPageComponentBuilder")
public
class ObjectSettingsPageComponentBuilder
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
	ObjectSettingsPageSpec spec;

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

			String shortName =
				ifNull (
					spec.shortName (),
					"settings");

			String partFactoryName =
				stringFormat (
					"%s%sPartFactory",
					context.newComponentNamePrefix (),
					capitalise (
						shortName));

			String responderName =
				stringFormat (
					"%s%sResponder",
					context.newComponentNamePrefix (),
					capitalise (
						shortName));

			String consoleHelperName =
				stringFormat (
					"%sConsoleHelper",
					hyphenToCamel (
						context.objectType ()));

			String privKey =
				ifNull (
					spec.privKey (),
					stringFormat (
						"%s.manage",
						hyphenToCamel (
							context.objectType ())));

			String fileName =
				ifNull (
					spec.fileName (),
					stringFormat (
						"%s.%s",
						context.pathPrefix (),
						shortName));

			String tabName =
				ifNull (
					spec.tabName (),
					stringFormat (
						"%s.%s",
						context.pathPrefix (),
						shortName));

			String title =
				ifNull (
					spec.friendlyLongName (),
					stringFormat (
						"%s %s",
						context.friendlyName (),
						camelToSpaces (
							ifNull (
								spec.longName (),
								"settings"))));

			target.registerDefinition (
				taskLogger,
				new ComponentDefinition ()

				.name (
					partFactoryName)

				.componentClass (
					ObjectSettingsPartFactory.class)

				.scope (
					"singleton")

				.hide (
					true)

				.addReferenceProperty (
					"objectLookup",
					"singleton",
					consoleHelperName)

				.addReferenceProperty (
					"consoleHelper",
					"singleton",
					consoleHelperName)

				.addValueProperty (
					"editPrivKey",
					optionalOf (
						privKey))

				.addValuePropertyFormat (
					"localName",
					"/%s",
					fileName)

				.addReferencePropertyFormat (
					"formType",
					"singleton",
					"%s%sFormType",
					hyphenToCamel (
						context.consoleModule ().name ()),
					hyphenToCamelCapitalise (
						spec.formTypeName ()))

				.addValuePropertyFormat (
					"removeLocalName",
					"/%s.remove",
					context.pathPrefix ())

			);

			target.registerDefinition (
				taskLogger,
				new ComponentDefinition ()

				.name (
					responderName)

				.componentClass (
					TabContextResponder.class)

				.scope (
					"prototype")

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
