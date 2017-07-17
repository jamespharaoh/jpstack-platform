package wbs.platform.media.console;

import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.etc.NullUtils.isNotNull;
import static wbs.utils.string.StringUtils.camelToSpaces;
import static wbs.utils.string.StringUtils.capitalise;

import java.util.ArrayList;
import java.util.List;

import lombok.NonNull;

import wbs.console.forms.basic.IdentityFormFieldInterfaceMapping;
import wbs.console.forms.basic.IdentityFormFieldNativeMapping;
import wbs.console.forms.basic.NullFormFieldConstraintValidator;
import wbs.console.forms.basic.RequiredFormFieldValueValidator;
import wbs.console.forms.basic.SimpleFormFieldAccessor;
import wbs.console.forms.core.ConsoleFormBuilderComponent;
import wbs.console.forms.core.ConsoleFormBuilderContext;
import wbs.console.forms.core.ConsoleFormPluginManagerImplementation;
import wbs.console.forms.core.FormFieldSetImplementation;
import wbs.console.forms.core.ReadOnlyFormField;
import wbs.console.forms.core.UpdatableFormField;
import wbs.console.forms.object.DereferenceFormFieldAccessor;
import wbs.console.forms.types.ConsoleFormNativeMapping;
import wbs.console.forms.types.FormFieldAccessor;
import wbs.console.forms.types.FormFieldConstraintValidator;
import wbs.console.forms.types.FormFieldInterfaceMapping;
import wbs.console.forms.types.FormFieldRenderer;
import wbs.console.forms.types.FormFieldUpdateHook;
import wbs.console.forms.types.FormFieldValueValidator;

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

import wbs.platform.media.model.MediaRec;

@SuppressWarnings ({ "rawtypes", "unchecked" })
@PrototypeComponent ("imageFormFieldBuilder")
public
class ImageFormFieldBuilder
	implements ConsoleFormBuilderComponent {

	// singleton dependencies

	@SingletonDependency
	ConsoleFormPluginManagerImplementation formFieldPluginManager;

	@ClassSingletonDependency
	LogContext logContext;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <DereferenceFormFieldAccessor>
		dereferenceFormFieldAccessorProvider;

	@PrototypeDependency
	ComponentProvider <IdentityFormFieldInterfaceMapping>
		identityFormFieldInterfaceMappingProvider;

	@PrototypeDependency
	ComponentProvider <IdentityFormFieldNativeMapping>
		identityFormFieldNativeMappingProvider;

	@PrototypeDependency
	ComponentProvider <ImageCsvFormFieldInterfaceMapping>
		imageCsvFormFieldInterfaceMappingProvider;

	@PrototypeDependency
	ComponentProvider <NullFormFieldConstraintValidator>
		nullFormFieldValueConstraintValidatorProvider;

	@PrototypeDependency
	ComponentProvider <ReadOnlyFormField> readOnlyFormFieldProvider;

	@PrototypeDependency
	ComponentProvider <RequiredFormFieldValueValidator>
		requiredFormFieldValueValidatorProvider;

	@PrototypeDependency
	ComponentProvider <SimpleFormFieldAccessor> simpleFormFieldAccessorProvider;

	@PrototypeDependency
	ComponentProvider <ImageFormFieldRenderer> imageFormFieldRendererProvider;

	@PrototypeDependency
	ComponentProvider <UpdatableFormField> updatableFormFieldProvider;

	// builder

	@BuilderParent
	ConsoleFormBuilderContext context;

	@BuilderSource
	ImageFormFieldSpec spec;

	@BuilderTarget
	FormFieldSetImplementation formFieldSet;

	// state

	String name;
	String label;
	Boolean nullable;
	Boolean showFilename;

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

			setDefaults ();

			buildField (
				taskLogger);

		}

	}

	void setDefaults () {

		name =
			spec.name ();

		label =
			ifNull (
				spec.label (),
				capitalise (
					camelToSpaces (
						name)));

		nullable =
			ifNull (
				spec.nullable (),
				false);

		showFilename =
			ifNull (
				spec.showFilename (),
				true);

	}

	void buildField (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"buildField");

		) {

			// accessor

			FormFieldAccessor accessor;

			if (
				isNotNull (
					spec.fieldName ())
			) {

				accessor =
					dereferenceFormFieldAccessorProvider.provide (
						taskLogger,
						dereferenceFormFieldAccessor ->
							dereferenceFormFieldAccessor

					.path (
						spec.fieldName ())

				);

			} else {

				accessor =
					simpleFormFieldAccessorProvider.provide (
						taskLogger,
						simpleFormFieldAccessor ->
							simpleFormFieldAccessor

					.name (
						name)

					.nativeClass (
						MediaRec.class)

				);

			}

			// native mapping

			ConsoleFormNativeMapping nativeMapping =
				identityFormFieldNativeMappingProvider.provide (
					taskLogger);

			// value validator

			List <FormFieldValueValidator> valueValidators =
				new ArrayList<> ();

			if (! nullable) {

				valueValidators.add (
					requiredFormFieldValueValidatorProvider.provide (
						taskLogger));

			}

			// constraint validator

			FormFieldConstraintValidator constraintValidator =
				nullFormFieldValueConstraintValidatorProvider.provide (
					taskLogger);

			// interface mapping

			FormFieldInterfaceMapping interfaceMapping =
				identityFormFieldInterfaceMappingProvider.provide (
					taskLogger);

			// renderer

			FormFieldRenderer renderer =
				imageFormFieldRendererProvider.provide (
					taskLogger,
					imageFormFieldRenderer ->
						imageFormFieldRenderer

				.name (
					name)

				.label (
					label)

				.nullable (
					nullable)

				.showFilename (
					showFilename)

			);

			// update hook

			FormFieldUpdateHook updateHook =
				formFieldPluginManager.getUpdateHook (
					taskLogger,
					context,
					context.containerClass (),
					name);

			// csv mapping

			FormFieldInterfaceMapping csvMapping =
				imageCsvFormFieldInterfaceMappingProvider.provide (
					taskLogger);

			// form field

			formFieldSet.addFormItem (
				updatableFormFieldProvider.provide (
					taskLogger,
					updatableFormField ->
						updatableFormField

				.name (
					name)

				.label (
					label)

				.accessor (
					accessor)

				.nativeMapping (
					nativeMapping)

				.valueValidators (
					valueValidators)

				.constraintValidator (
					constraintValidator)

				.interfaceMapping (
					interfaceMapping)

				.csvMapping (
					csvMapping)

				.renderer (
					renderer)

				.updateHook (
					updateHook)

			));

		}

	}

}
