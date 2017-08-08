package wbs.platform.currency.console;

import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.etc.TypeUtils.classNameSimple;
import static wbs.utils.string.StringUtils.camelToSpaces;
import static wbs.utils.string.StringUtils.capitalise;
import static wbs.utils.string.StringUtils.stringFormat;

import java.util.ArrayList;
import java.util.List;

import lombok.NonNull;

import org.apache.commons.lang3.Range;

import wbs.console.forms.basic.IdentityFormFieldNativeMapping;
import wbs.console.forms.basic.IntegerFormFieldInterfaceMapping;
import wbs.console.forms.basic.IntegerFormFieldValueValidator;
import wbs.console.forms.basic.NullFormFieldConstraintValidator;
import wbs.console.forms.basic.RangeFormFieldInterfaceMapping;
import wbs.console.forms.basic.RequiredFormFieldValueValidator;
import wbs.console.forms.core.ConsoleFormBuilderComponent;
import wbs.console.forms.core.ConsoleFormBuilderContext;
import wbs.console.forms.core.ConsoleFormPluginManagerImplementation;
import wbs.console.forms.core.FormFieldSetImplementation;
import wbs.console.forms.core.ReadOnlyFormField;
import wbs.console.forms.core.UpdatableFormField;
import wbs.console.forms.object.DereferenceFormFieldAccessor;
import wbs.console.forms.text.TextFormFieldRenderer;
import wbs.console.forms.text.TextualRangeFormFieldInterfaceMapping;
import wbs.console.forms.types.ConsoleFormNativeMapping;
import wbs.console.forms.types.FormField;
import wbs.console.forms.types.FormFieldAccessor;
import wbs.console.forms.types.FormFieldConstraintValidator;
import wbs.console.forms.types.FormFieldInterfaceMapping;
import wbs.console.forms.types.FormFieldRenderer;
import wbs.console.forms.types.FormFieldUpdateHook;
import wbs.console.forms.types.FormFieldValueValidator;
import wbs.console.helper.manager.ConsoleObjectManager;

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
@PrototypeComponent ("currencyFormFieldBuilder")
public
class CurrencyFormFieldBuilder
	implements ConsoleFormBuilderComponent {

	// singleton dependencies

	@SingletonDependency
	ConsoleFormPluginManagerImplementation formFieldPluginManager;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <CurrencyFormFieldInterfaceMapping>
		currencyFormFieldInterfaceMappingProvider;

	@PrototypeDependency
	ComponentProvider <DereferenceFormFieldAccessor>
		dereferenceFormFieldAccessorProvider;

	@PrototypeDependency
	ComponentProvider <IdentityFormFieldNativeMapping>
		identityFormFieldNativeMappingProvider;

	@PrototypeDependency
	ComponentProvider <IntegerFormFieldInterfaceMapping>
		integerFormFieldInterfaceMappingProvider;

	@PrototypeDependency
	ComponentProvider <IntegerFormFieldValueValidator>
		integerFormFieldValueValidatorProvider;

	@PrototypeDependency
	ComponentProvider <NullFormFieldConstraintValidator>
		nullFormFieldValueConstraintValidatorProvider;

	@PrototypeDependency
	ComponentProvider <RangeFormFieldInterfaceMapping>
		rangeFormFieldInterfaceMappingProvider;

	@PrototypeDependency
	ComponentProvider <ReadOnlyFormField> readOnlyFormFieldProvider;

	@PrototypeDependency
	ComponentProvider <RequiredFormFieldValueValidator>
		requiredFormFieldValueValidatorProvider;

	@PrototypeDependency
	ComponentProvider <TextFormFieldRenderer> textFormFieldRendererProvider;

	@PrototypeDependency
	ComponentProvider <TextualRangeFormFieldInterfaceMapping>
		textualRangeFormFieldInterfaceMappingProvider;

	@PrototypeDependency
	ComponentProvider <UpdatableFormField> updatableFormFieldProvider;

	// builder

	@BuilderParent
	ConsoleFormBuilderContext context;

	@BuilderSource
	CurrencyFormFieldSpec spec;

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

			// resolve properties from spec

			String name =
				spec.name ();

			String fieldName =
				ifNull (
					spec.fieldName (),
					name);

			String label =
				ifNull (
					spec.label (),
					capitalise (
						camelToSpaces (
							name)));

			Boolean readOnly =
				ifNull (
					spec.readOnly (),
					false);

			Boolean nullable =
				ifNull (
					spec.nullable (),
					false);

			Long minimum =
				ifNull (
					spec.minimum (),
					Long.MIN_VALUE);

			Long maximum =
				ifNull (
					spec.maximum (),
					Long.MAX_VALUE);

			Class <?> propertyClass =
				optionalGetRequired (
					objectManager.dereferenceType (
						taskLogger,
						optionalOf (
							context.containerClass ()),
						optionalOf (
							fieldName)));

			Boolean blankIfZero =
				ifNull (
					spec.blankIfZero (),
					false);

			// accessor

			FormFieldAccessor accessor =
				dereferenceFormFieldAccessorProvider.provide (
					taskLogger)

				.path (
					fieldName)

				.nativeClass (
					propertyClass);

			// native mapping

			ConsoleFormNativeMapping nativeMapping;

			boolean range;

			if (propertyClass == Long.class) {

				nativeMapping =
					identityFormFieldNativeMappingProvider.provide (
						taskLogger);

				range = false;

			} else if (propertyClass == Range.class) {

				nativeMapping =
					identityFormFieldNativeMappingProvider.provide (
						taskLogger);

				range = true;

			} else {

				throw new RuntimeException (
					stringFormat (
						"Don't know how to map %s as integer for %s.%s",
						classNameSimple (
							propertyClass),
						classNameSimple (
							context.containerClass ()),
						name));

			}

			// value validator

			List <FormFieldValueValidator> valueValidators =
				new ArrayList<> ();

			if (! nullable) {

				valueValidators.add (
					requiredFormFieldValueValidatorProvider.provide (
						taskLogger));

			}

			valueValidators.add (
				integerFormFieldValueValidatorProvider.provide (
					taskLogger)

				.label (
					label)

				.minimum (
					minimum)

				.maximum (
					maximum)

			);

			// constraint validator

			FormFieldConstraintValidator constraintValidator =
				nullFormFieldValueConstraintValidatorProvider.provide (
					taskLogger);

			// interface mapping

			FormFieldInterfaceMapping interfaceMapping;

			if (range) {

				interfaceMapping =
					rangeFormFieldInterfaceMappingProvider.provide (
						taskLogger)

					.itemMapping (
						currencyFormFieldInterfaceMappingProvider.provide (
							taskLogger)

						.currencyPath (
							spec.currencyPath ())

						.blankIfZero (
							blankIfZero)
					)

				;

			} else {

				interfaceMapping =
					currencyFormFieldInterfaceMappingProvider.provide (
						taskLogger)

					.currencyPath (
						spec.currencyPath ())

					.blankIfZero (
						blankIfZero)

				;

			}

			// renderer

			FormFieldRenderer renderer =
				textFormFieldRendererProvider.provide (
					taskLogger)

				.name (
					name)

				.label (
					label)

				.nullable (
					ifNull (
						spec.nullable (),
						false))

				.listAlign (
					FormField.Align.right)

			;

			// update hook

			FormFieldUpdateHook updateHook =
				formFieldPluginManager.getUpdateHook (
					taskLogger,
					context,
					context.containerClass (),
					name);

			// field

			if (! readOnly) {

				formFieldSet.addFormItem (
					updatableFormFieldProvider.provide (
						taskLogger)

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
						interfaceMapping)

					.renderer (
						renderer)

					.updateHook (
						updateHook)

				);

			} else {

				formFieldSet.addFormItem (
					readOnlyFormFieldProvider.provide (
						taskLogger)

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

					.csvMapping (
						interfaceMapping)

					.renderer (
						renderer)

				);

			}

		}

	}

}
