package wbs.platform.priv.console;

import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.string.StringUtils.capitalise;
import static wbs.utils.string.StringUtils.stringFormat;

import com.google.common.collect.ImmutableList;

import lombok.NonNull;

import wbs.console.context.ConsoleContextBuilderContainer;
import wbs.console.context.ResolvedConsoleContextExtensionPoint;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.module.ConsoleMetaManager;
import wbs.console.module.ConsoleModuleBuilderComponent;
import wbs.console.module.ConsoleModuleImplementation;
import wbs.console.part.PagePartFactory;
import wbs.console.responder.ConsoleFile;
import wbs.console.tab.ConsoleContextTab;
import wbs.console.tab.TabContextResponder;

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
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.web.responder.WebResponder;

@PrototypeComponent ("objectPrivsPageBuilder")
public
class ObjectPrivsPageBuilder <
	ObjectType extends Record <ObjectType>
> implements ConsoleModuleBuilderComponent {

	// singleton dependencies

	@SingletonDependency
	ConsoleMetaManager consoleMetaManager;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <ConsoleFile> consoleFileProvider;

	@PrototypeDependency
	ComponentProvider <ConsoleContextTab> contextTabProvider;

	@ClassSingletonDependency
	LogContext logContext;

	@PrototypeDependency
	ComponentProvider <TabContextResponder> tabContextResponder;

	@PrototypeDependency
	ComponentProvider <ObjectPrivsPart <ObjectType>> objectPrivsPartProvider;

	// builder

	@BuilderParent
	ConsoleContextBuilderContainer <ObjectType> container;

	@BuilderSource
	ObjectPrivsPageSpec spec;

	@BuilderTarget
	ConsoleModuleImplementation consoleModule;

	// state

	ConsoleHelper <ObjectType> consoleHelper;

	String name;
	String tabName;
	String tabLabel;
	String fileName;

	ComponentProvider <WebResponder> responderProvider;

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

			setDefaults (
				taskLogger);

			buildResponder (
				taskLogger);

			for (
				ResolvedConsoleContextExtensionPoint resolvedExtensionPoint
					: consoleMetaManager.resolveExtensionPoint (
						taskLogger,
						container.extensionPointName ())
			) {

				buildContextTab (
					taskLogger,
					resolvedExtensionPoint);

				buildContextFile (
					taskLogger,
					resolvedExtensionPoint);

			}

		}

	}

	void buildContextTab (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ResolvedConsoleContextExtensionPoint extensionPoint) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"buildContextTab");

		) {

			consoleModule.addContextTab (
				taskLogger,
				"end",

				contextTabProvider.provide (
					taskLogger)

					.name (
						tabName)

					.defaultLabel (
						tabLabel)

					.localFile (
						fileName)

					.privKeys (
						ImmutableList.of (
							stringFormat (
								"%s.manage",
								consoleHelper.objectName ()))),

				extensionPoint.contextTypeNames ());

		}

	}

	void buildContextFile (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ResolvedConsoleContextExtensionPoint extensionPoint) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"buildContextFile");

		) {

			consoleModule.addContextFile (
				fileName,
				consoleFileProvider.provide (
					taskLogger)

					.getResponderProvider (
						responderProvider)

					.privKeys (
						taskLogger,
						ImmutableList.of (
							stringFormat (
								"%s.manage",
								consoleHelper.objectName ()))),

				extensionPoint.contextTypeNames ()
			);

		}

	}

	void buildResponder (
			@NonNull TaskLogger parentTaskLogger) {

		PagePartFactory privsPartFactory =
			taskLoggerNested ->
				objectPrivsPartProvider.provide (
					taskLoggerNested)

			.consoleHelper (
				consoleHelper)

		;

		responderProvider =
			taskLoggerNested ->
				tabContextResponder.provide (
					taskLoggerNested)

			.tab (
				tabName)

			.title (
				capitalise (
					stringFormat (
						"%s privs",
						consoleHelper.friendlyName ())))

			.pagePartFactory (
				privsPartFactory)

		;

	}

	void setDefaults (
			@NonNull TaskLogger parentTaskLogger) {

		consoleHelper =
			container.consoleHelper ();

		name =
			ifNull (
				spec.name (),
				"privs");

		tabName =
			ifNull (
				spec.tabName (),
				stringFormat (
					"%s.%s",
					container.pathPrefix (),
					name));

		tabLabel =
			ifNull (
				spec.tabLabel (),
				"Privs");

		fileName =
			ifNull (
				spec.fileName (),
				stringFormat (
					"%s.%s",
					container.pathPrefix (),
					name));

	}

}
