package wbs.console.forms;

import static wbs.framework.utils.etc.LogicUtils.ifThenElse;
import static wbs.framework.utils.etc.NullUtils.ifNull;
import static wbs.framework.utils.etc.StringUtils.camelToSpaces;
import static wbs.framework.utils.etc.StringUtils.capitalise;
import static wbs.framework.utils.etc.TypeUtils.classEqualSafe;

import javax.inject.Provider;

import lombok.NonNull;

import wbs.console.annotations.ConsoleModuleBuilderHandler;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.application.annotations.PrototypeDependency;
import wbs.framework.application.annotations.SingletonDependency;
import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.utils.etc.BeanLogic;

@SuppressWarnings ({ "rawtypes", "unchecked" })
@PrototypeComponent ("jsonFormFieldBuilder")
@ConsoleModuleBuilderHandler
public
class JsonFormFieldBuilder {

	// singleton dependencies

	@SingletonDependency
	FormFieldPluginManagerImplementation formFieldPluginManager;

	// prototype dependencies

	@PrototypeDependency
	Provider <ChainedFormFieldNativeMapping>
	chainedFormFieldNativeMappingProvider;

	@PrototypeDependency
	Provider <HtmlFormFieldRenderer>
	htmlFormFieldRendererProvider;

	@PrototypeDependency
	Provider <JsonFormFieldNativeMapping>
	jsonFormFieldNativeMappingProvider;

	@PrototypeDependency
	Provider <JsonFormFieldInterfaceMapping>
	jsonFormFieldInterfaceMappingProvider;

	@PrototypeDependency
	Provider <ReadOnlyFormField>
	readOnlyFormFieldProvider;

	@PrototypeDependency
	Provider <SimpleFormFieldAccessor>
	simpleFormFieldAccessorProvider;

	@PrototypeDependency
	Provider <Utf8StringFormFieldNativeMapping>
	utf8StringFormFieldNativeMappingProvider;

	// builder

	@BuilderParent
	FormFieldBuilderContext context;

	@BuilderSource
	JsonFormFieldSpec spec;

	@BuilderTarget
	FormFieldSet formFieldSet;

	// build

	@BuildMethod
	public
	void build (
			@NonNull Builder builder) {

		String name =
			spec.name ();

		String label =
			ifNull (
				spec.label (),
				capitalise (
					camelToSpaces (
						name)));

		// field type

		Class<?> propertyClass =
			BeanLogic.propertyClassForClass (
				context.containerClass (),
				name);

		// accessor

		FormFieldAccessor accessor =
			simpleFormFieldAccessorProvider.get ()

			.name (
				name)

			.nativeClass (
				propertyClass);

		// native mapping

		FormFieldNativeMapping nativeMapping =
			ifThenElse (
				classEqualSafe (
					propertyClass,
					byte[].class),

			() ->
				chainedFormFieldNativeMappingProvider.get ()

				.previousMapping (
					jsonFormFieldNativeMappingProvider.get ())

				.nextMapping (
					utf8StringFormFieldNativeMappingProvider.get ()),

			() ->
				chainedFormFieldNativeMappingProvider.get ()

				.previousMapping (
					jsonFormFieldNativeMappingProvider.get ())

				.nextMapping (
					formFieldPluginManager.getNativeMappingRequired (
						context,
						context.containerClass (),
						name,
						String.class,
						propertyClass))

		);

		// interface mapping

		FormFieldInterfaceMapping interfaceMapping =
			jsonFormFieldInterfaceMappingProvider.get ();

		// renderer

		FormFieldRenderer renderer =
			htmlFormFieldRendererProvider.get ()

			.name (
				name)

			.label (
				label);

		// form field

		formFieldSet.addFormField (
			readOnlyFormFieldProvider.get ()

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

		);

	}

}
