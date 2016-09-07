package wbs.console.context;

import javax.inject.Provider;

import com.google.common.collect.ImmutableList;

import wbs.console.annotations.ConsoleMetaModuleBuilderHandler;
import wbs.console.module.ConsoleMetaModuleImplementation;
import wbs.framework.builder.Builder;
import wbs.framework.builder.Builder.MissingBuilderBehaviour;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;

@PrototypeComponent ("simpleConsoleContextMetaBuilder")
@ConsoleMetaModuleBuilderHandler
public
class SimpleConsoleContextMetaBuilder {

	// prototype dependencies

	@PrototypeDependency
	Provider <ConsoleContextRootExtensionPoint> rootExtensionPointProvider;

	// builder

	@BuilderParent
	ConsoleContextMetaBuilderContainer container;

	@BuilderSource
	SimpleConsoleContextSpec spec;

	@BuilderTarget
	ConsoleMetaModuleImplementation metaModule;

	// state

	String contextTypeName;

	// build

	@BuildMethod
	public
	void build (
			Builder builder) {

		setDefaults ();

		// extension point

		metaModule.addExtensionPoint (
			rootExtensionPointProvider.get ()

			.name (
				contextTypeName)

			.contextTypeNames (
				ImmutableList.<String>of (
					contextTypeName))

			.contextLinkNames (
				ImmutableList.<String>of (
					contextTypeName))

			.parentContextNames (
				ImmutableList.<String>of (
					contextTypeName)));

		// context hints

		metaModule.addContextHint (
			new ConsoleContextHint ()

			.linkName (
				contextTypeName)

			.singular (
				true)

			.plural (
				false)

		);

		// descend

		ConsoleContextMetaBuilderContainer nextContainer =
			new ConsoleContextMetaBuilderContainer ()

			.structuralName (
				contextTypeName)

			.extensionPointName (
				contextTypeName);

		builder.descend (
			nextContainer,
			spec.children (),
			metaModule,
			MissingBuilderBehaviour.ignore);

	}

	void setDefaults () {

		contextTypeName =
			spec.name ();

	}

}
