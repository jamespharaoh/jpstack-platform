package wbs.platform.event.console;

import static wbs.utils.etc.Misc.doesNotImplement;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.string.StringUtils.stringEqualSafe;

import com.google.common.base.Optional;

import lombok.NonNull;

import wbs.console.forms.core.ConsoleFormBuilderContext;
import wbs.console.forms.types.FormFieldPluginProvider;
import wbs.console.helper.core.ConsoleHelper;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

@SingletonComponent ("eventFormFieldPluginProvider")
@SuppressWarnings ({ "rawtypes", "unchecked" })
public
class EventFormFieldPluginProvider
	implements FormFieldPluginProvider {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <NameFormFieldUpdateHook>
		nameFormFieldUpdateHookProvider;

	@PrototypeDependency
	ComponentProvider <SimpleFormFieldUpdateHook>
		simpleFormFieldUpdateHookProvider;

	// implementation

	@Override
	public
	Optional getUpdateHook (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ConsoleFormBuilderContext context,
			@NonNull Class<?> containerClass,
			@NonNull String fieldName) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"getUpdateHook");

		) {

			// only operate on records

			if (
				doesNotImplement (
					containerClass,
					Record.class)
			) {
				return Optional.absent ();
			}

			// get console helper

			ConsoleHelper consoleHelper =
				context.consoleHelper ();

			// check for name field

			if (

				consoleHelper.nameExists ()

				&& stringEqualSafe (
					consoleHelper.nameFieldName (),
					fieldName)

			) {

				// name field has special update hook

				return optionalOf (
					nameFormFieldUpdateHookProvider.provide (
						taskLogger));

			} else {

				// other fields use simple update hook

				return optionalOf (
					simpleFormFieldUpdateHookProvider.provide (
						taskLogger)

					.fieldName (
						fieldName)

				);

			}

		}

	}

}
