package wbs.platform.event.fixture;

import static wbs.utils.etc.NullUtils.isNull;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsNotPresent;
import static wbs.utils.etc.TypeUtils.classpathResource;
import static wbs.utils.etc.TypeUtils.dynamicCastRequired;
import static wbs.utils.string.CodeUtils.simplifyToCodeRequired;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.string.StringUtils.stringReplaceAllSimple;

import com.google.common.base.Optional;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.component.scaffold.PluginManager;
import wbs.framework.component.scaffold.PluginSpec;
import wbs.framework.data.tools.DataFromXml;
import wbs.framework.data.tools.DataFromXmlBuilder;
import wbs.framework.database.Database;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.fixtures.MetaFixtures;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.TaskLogger;

import wbs.platform.event.metamodel.EventTypeSpec;
import wbs.platform.event.metamodel.EventTypesSpec;
import wbs.platform.event.model.EventTypeObjectHelper;

import wbs.utils.io.SafeInputStream;

public
class EventTypesMetaFixtures
	implements MetaFixtures {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@SingletonDependency
	EventTypeObjectHelper eventTypeHelper;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	PluginManager pluginManager;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <DataFromXmlBuilder> dataFromXmlBuilderProvider;

	// public implementation

	@Override
	public
	void createFixtures (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"createFixtures");

		) {

			DataFromXml dataFromXml =
				dataFromXmlBuilderProvider.provide (
					transaction)

				.registerBuilderClasses (
					transaction,
					EventTypesSpec.class,
					EventTypeSpec.class)

				.build (
					transaction)

			;

			for (
				PluginSpec pluginSpec
					: pluginManager.plugins ()
			) {

				String filename =
					stringFormat (
						"/%s",
						stringReplaceAllSimple (
							".",
							"/",
							pluginSpec.packageName ()),
						"/model",
						"/%s-event-types.xml",
						pluginSpec.name ());

				Optional <SafeInputStream> inputStreamOptional =
					classpathResource (
						getClass ().getClassLoader (),
							filename);

				if (
					optionalIsNotPresent (
						inputStreamOptional)
				) {
					continue;
				}

				try (

					SafeInputStream inputStream =
						optionalGetRequired (
							inputStreamOptional);

				) {

					if (
						isNull (
							inputStream)
					) {
						continue;
					}

					EventTypesSpec eventTypesSpec =
						dynamicCastRequired (
							EventTypesSpec.class,
							dataFromXml.readInputStreamRequired (
								transaction,
								inputStream,
								filename));

					for (
						EventTypeSpec eventTypeSpec
							: eventTypesSpec.eventTypes ()
					) {

						createEventType (
							transaction,
							eventTypeSpec);

					}

				}

			}

			transaction.commit ();

		}

	}

	// private implementation

	private
	void createEventType (
			@NonNull Transaction parentTransaction,
			@NonNull EventTypeSpec spec) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createEventType");

		) {

			// create event type

			eventTypeHelper.insert (
				transaction,
				eventTypeHelper.createInstance ()

				.setCode (
					simplifyToCodeRequired (
						spec.name ()))

				.setDescription (
					spec.text ())

				.setAdmin (
					spec.admin ())

			);

			transaction.noticeFormat (
				"Created event type: %s",
				spec.name ());

		}

	}

}
