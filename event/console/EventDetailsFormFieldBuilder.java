package wbs.platform.event.console;

import lombok.NonNull;

import wbs.console.forms.basic.IdentityFormFieldAccessor;
import wbs.console.forms.basic.IdentityFormFieldNativeMapping;
import wbs.console.forms.core.ConsoleFormBuilderComponent;
import wbs.console.forms.core.ConsoleFormBuilderContext;
import wbs.console.forms.core.ConsoleFormPluginManagerImplementation;
import wbs.console.forms.core.FormFieldSetImplementation;
import wbs.console.forms.core.ReadOnlyFormField;
import wbs.console.forms.text.HtmlFormFieldRenderer;
import wbs.console.forms.types.ConsoleFormNativeMapping;
import wbs.console.forms.types.FormFieldAccessor;
import wbs.console.forms.types.FormFieldInterfaceMapping;
import wbs.console.forms.types.FormFieldRenderer;

import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

@SuppressWarnings ({ "rawtypes", "unchecked" })
@PrototypeComponent ("eventDetailsFormFieldBuilder")
public
class EventDetailsFormFieldBuilder
	implements ConsoleFormBuilderComponent {

	// singleton dependencies

	@SingletonDependency
	ConsoleFormPluginManagerImplementation formFieldPluginManager;

	@ClassSingletonDependency
	LogContext logContext;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <EventDetailsFormFieldInterfaceMapping>
		eventDetailsFormFieldInterfaceMappingProvider;

	@PrototypeDependency
	ComponentProvider <HtmlFormFieldRenderer> htmlFormFieldRendererProvider;

	@PrototypeDependency
	ComponentProvider <IdentityFormFieldAccessor>
		identityFormFieldAccessorProvider;

	@PrototypeDependency
	ComponentProvider <IdentityFormFieldNativeMapping>
		identityFormFieldNativeMappingProvider;

	@PrototypeDependency
	ComponentProvider <ReadOnlyFormField> readOnlyFormFieldProvider;

	// builder

	@BuilderParent
	ConsoleFormBuilderContext context;

	@BuilderSource
	EventDetailsFormFieldSpec spec;

	@BuilderTarget
	FormFieldSetImplementation formFieldSet;

	// build

	@Override
	@BuildMethod
	public
	void build (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull Builder builder) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"build");

		) {

			String name =
				"details";

			String label =
				"Details";

			// accessor

			FormFieldAccessor accessor =
				identityFormFieldAccessorProvider.provide (
					taskLogger,
					identityFormFieldAccessor ->
						identityFormFieldAccessor

				.containerClass (
					context.containerClass ())

			);

			// native mapping

			ConsoleFormNativeMapping nativeMapping =
				identityFormFieldNativeMappingProvider.provide (
					taskLogger);

			// interface mapping

			FormFieldInterfaceMapping interfaceMapping =
				eventDetailsFormFieldInterfaceMappingProvider.provide (
					taskLogger);

			// renderer

			FormFieldRenderer renderer =
				htmlFormFieldRendererProvider.provide (
					taskLogger,
					nestedRenderer ->
						nestedRenderer

				.name (
					name)

				.label (
					label)

			);

			// form field

			formFieldSet.addFormItem (
				readOnlyFormFieldProvider.provide (
					taskLogger,
					formItem ->
						formItem

				.name (
					name)

				.label (
					label)

				.accessor (
					accessor)

				.nativeMapping (
					nativeMapping)

				.interfaceMapping (
					interfaceMapping)

				.renderer (
					renderer)

			));

		}

	}

}
