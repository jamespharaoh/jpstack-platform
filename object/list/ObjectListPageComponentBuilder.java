package wbs.platform.object.list;

import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.etc.OptionalUtils.optionalFromNullable;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.etc.OptionalUtils.optionalOfFormat;
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

@PrototypeComponent ("objectListPageComponentBuilder")
public
class ObjectListPageComponentBuilder
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
	ObjectListPageSpec spec;

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

			String targetContextTypeName =
				ifNull (
					spec.targetContextTypeName (),
					stringFormat (
						"%s:combo",
						hyphenToCamel (
							context.objectType ())));

			String tabName =
				stringFormat (
					"%s.list",
					context.pathPrefix ());

			String title =
				capitaliseFormat (
					"%s list",
					context.friendlyName ());

			target.registerDefinition (
				taskLogger,
				new ComponentDefinition ()

				.nameFormat (
					"%sListPartFactory",
					context.newComponentNamePrefix ())

				.scope (
					"singleton")

				.componentClass (
					ObjectListPartFactory.class)

				.hide (
					true)

				.addReferencePropertyFormat (
					"consoleHelper",
					"singleton",
					"%sConsoleHelper",
					hyphenToCamel (
						context.objectType ()))

				.addValueProperty (
					"typeCode",
					optionalFromNullable (
						spec.typeCode ()))

				.addValueProperty (
					"localName",
					optionalOfFormat (
						"%s.list",
						context.pathPrefix ()))

				.addReferencePropertyFormat (
					"formType",
					"singleton",
					"%s%sFormType",
					hyphenToCamel (
						context.consoleModule ().name ()),
					hyphenToCamelCapitalise (
						spec.formTypeName ()))

				.addValueProperty (
					"listTabSpecs",
					optionalOf (
						spec.listTabsByName ()))

				.addValueProperty (
					"listBrowserSpecs",
					optionalOf (
						spec.listBrowsersByFieldName ()))

				.addValueProperty (
					"targetContextTypeName",
					optionalOf (
						targetContextTypeName))

				.addValueProperty (
					"moveControls",
					optionalOf (
						ifNull (
							spec.moveControls (),
							false)))

				.addValueProperty (
					"editControl",
					optionalOf (
						ifNull (
							spec.editControl (),
							false)))

				.addValueProperty (
					"deleteControl",
					optionalOf (
						ifNull (
							spec.deleteControl (),
							false)))

			);

			target.registerDefinition (
				taskLogger,
				new ComponentDefinition ()

				.nameFormat (
					"%sListResponder",
					context.newComponentNamePrefix ())

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
					"%sListPartFactory",
					context.newComponentNamePrefix ())

			);

			target.registerDefinition (
				taskLogger,
				new ComponentDefinition ()

				.nameFormat (
					"%sListAction",
					context.newComponentNamePrefix ())

				.scope (
					"prototype")

				.componentClass (
					ObjectListAction.class)

				.hide (
					true)

				.addReferencePropertyFormat (
					"consoleHelper",
					"singleton",
					"%sConsoleHelper",
					hyphenToCamel (
						context.objectType ()))

				.addReferencePropertyFormat (
					"responderProvider",
					"prototype",
					"%sListResponder",
					context.newComponentNamePrefix ())

			);

		}

	}

}
