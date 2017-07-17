package wbs.platform.text.console;

import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.etc.TypeUtils.classEqualSafe;

import com.google.common.base.Optional;

import lombok.NonNull;

import wbs.console.forms.core.ConsoleFormBuilderContext;
import wbs.console.forms.types.FormFieldPluginProvider;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.text.model.TextRec;

@SingletonComponent ("textFormFieldPluginProvider")
@SuppressWarnings ({ "rawtypes", "unchecked" })
public
class TextFormFieldPluginProvider
	implements FormFieldPluginProvider {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <TextFormFieldNativeMapping>
		textFormFieldNativeMappingProvider;

	// implementation

	@Override
	public
	Optional getNativeMapping (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ConsoleFormBuilderContext context,
			@NonNull Class containerClass,
			@NonNull String fieldName,
			@NonNull Class genericClass,
			@NonNull Class nativeClass) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"getNativeMapping");

		) {

			if (

				classEqualSafe (
					genericClass,
					String.class)

				&& classEqualSafe (
					nativeClass,
					TextRec.class)

			) {

				return optionalOf (
					textFormFieldNativeMappingProvider.provide (
						taskLogger));

			} else {

				return optionalAbsent ();

			}

		}

	}

}
