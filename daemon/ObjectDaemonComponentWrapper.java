package wbs.platform.daemon;

import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsNotPresent;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.string.StringUtils.stringExtract;
import static wbs.utils.string.StringUtils.stringNotEqualSafe;

import com.google.common.base.Optional;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.registry.ComponentDefinition;
import wbs.framework.component.registry.ComponentRegistryBuilder;
import wbs.framework.component.tools.ComponentWrapper;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

@SingletonComponent ("objectDaemonComponentWrapper")
public
class ObjectDaemonComponentWrapper
	implements ComponentWrapper <ObjectDaemon <Long>> {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// details

	@Override
	public
	Class <ObjectDaemon <Long>> componentClass () {

		return genericCastUnchecked (
			ObjectDaemon.class);

	}

	// public implementation

	@Override
	public
	void wrapComponent (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ComponentRegistryBuilder componentRegistry,
			@NonNull ComponentDefinition componentDefinition) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"wrapComponent");

		) {

			// sanity check

			if (
				stringNotEqualSafe (
					componentDefinition.scope (),
					"singleton")
			) {

				taskLogger.errorFormat (
					"Removal daemon component %s ",
					componentDefinition.name (),
					"has non-prototype scope: %s",
					componentDefinition.scope ());

				return;

			}

			Optional <String> baseNameOptional =
				stringExtract (
					"",
					"ObjectDaemonHelper",
					componentDefinition.name ());

			if (
				optionalIsNotPresent (
					baseNameOptional)
			) {

				taskLogger.errorFormat (
					"Removal daemon component has invalid name: %s",
					componentDefinition.name ());

				return;

			}

			String baseName =
				optionalGetRequired (
					baseNameOptional);

			componentRegistry.registerDefinition (
				taskLogger,
				new ComponentDefinition ()

				.nameFormat (
					"%sObjectDaemon",
					baseName)

				.scope (
					"singleton")

				.componentClass (
					ObjectDaemonWrapper.class)

				.addReferenceProperty (
					"objectDaemon",
					"singleton",
					componentDefinition.name ())

			);

		}

	}

}
