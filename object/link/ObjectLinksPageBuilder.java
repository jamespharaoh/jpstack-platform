package wbs.platform.object.link;

import static wbs.utils.string.StringUtils.camelToSpaces;
import static wbs.utils.string.StringUtils.capitalise;
import static wbs.utils.string.StringUtils.stringFormat;

import lombok.NonNull;

import wbs.console.context.ConsoleContextBuilderContainer;
import wbs.console.context.ResolvedConsoleContextExtensionPoint;
import wbs.console.forms.core.ConsoleFormManager;
import wbs.console.forms.core.ConsoleFormType;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.helper.manager.ConsoleObjectManager;
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
import wbs.framework.database.NestedTransaction;
import wbs.framework.entity.model.ModelField;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.web.mvc.WebAction;
import wbs.web.responder.WebResponder;

@PrototypeComponent ("objectLinksPageBuilder")
public
class ObjectLinksPageBuilder <
	ObjectType extends Record <ObjectType>,
	TargetType extends Record <TargetType>
> implements ConsoleModuleBuilderComponent {

	// singleton dependencies

	@SingletonDependency
	ConsoleFormManager consoleFormManager;

	@SingletonDependency
	ConsoleMetaManager consoleMetaManager;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <ConsoleFile> consoleFileProvider;

	@PrototypeDependency
	ComponentProvider <ConsoleContextTab> contextTabProvider;

	@PrototypeDependency
	ComponentProvider <ObjectLinksPart <ObjectType, TargetType>>
		objectLinksPartProvider;

	@PrototypeDependency
	ComponentProvider <ObjectLinksAction> objectLinksActionProvider;

	@PrototypeDependency
	ComponentProvider <TabContextResponder> tabContextResponderProvider;

	// builder

	@BuilderParent
	ConsoleContextBuilderContainer <ObjectType> container;

	@BuilderSource
	ObjectLinksPageSpec spec;

	@BuilderTarget
	ConsoleModuleImplementation consoleModule;

	// state

	ConsoleHelper <ObjectType> consoleHelper;

	String name;
	String tabName;
	String tabLabel;
	String localFile;
	String privKey;

	ComponentProvider <WebResponder> responderProvider;

	String pageTitle;
	ModelField linksField;
	ConsoleHelper <TargetType> targetConsoleHelper;
	ModelField targetLinksField;
	String addEventName;
	String removeEventName;
	ObjectLinksAction.EventOrder eventOrder;
	String updateSignalName;
	String targetUpdateSignalName;
	String successNotice;
	ConsoleFormType <TargetType> targetFormType;

	// build

	@Override
	@BuildMethod
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

			buildResponder ();

			for (
				ResolvedConsoleContextExtensionPoint resolvedExtensionPoint
					: consoleMetaManager.resolveExtensionPoint (
						taskLogger,
						container.extensionPointName ())
			) {

				buildTab (
					taskLogger,
					resolvedExtensionPoint);

				buildFile (
					taskLogger,
					resolvedExtensionPoint);

			}

		}

	}

	void buildTab (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ResolvedConsoleContextExtensionPoint extensionPoint) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"buildTab");

		) {

			consoleModule.addContextTab (
				taskLogger,
				container.tabLocation (),
				contextTabProvider.provide (
					taskLogger)

					.name (
						tabName)

					.defaultLabel (
						tabLabel)

					.localFile (
						localFile)

					.privKeys (
						privKey),

				extensionPoint.contextTypeNames ()
			);

		}

	}

	void buildFile (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ResolvedConsoleContextExtensionPoint extensionPoint) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"buildFile");

		) {

			ComponentProvider <WebAction> actionProvider =
				taskLoggerNested ->
					objectLinksActionProvider.provide (
						taskLoggerNested)

				.responderProvider (
					responderProvider)

				.contextHelper (
					consoleHelper)

				.contextLinkField (
					linksField.name ())

				.targetHelper (
					targetConsoleHelper)

				.targetLinkField (
					targetLinksField.name ())

				.addEventName (
					addEventName)

				.removeEventName (
					removeEventName)

				.eventOrder (
					eventOrder)

				.contextUpdateSignalName (
					updateSignalName)

				.targetUpdateSignalName (
					targetUpdateSignalName)

				.successNotice (
					successNotice);

			consoleModule.addContextFile (
				localFile,
				consoleFileProvider.provide (
					taskLogger)

					.getResponderProvider (
						responderProvider)

					.postActionProvider (
						actionProvider)

					.privName (
						taskLogger,
						privKey),

				extensionPoint.contextTypeNames ()
			);

		}

	}

	void buildResponder () {

		PagePartFactory partFactory =
			parentTransaction -> {

			try (

				NestedTransaction transaction =
					parentTransaction.nestTransaction (
						logContext,
						"buildResponder");

			) {

				return objectLinksPartProvider.provide (
					transaction)

					.consoleHelper (
						container.consoleHelper ())

					.contextLinksField (
						linksField.name ())

					.targetHelper (
						targetConsoleHelper)

					.targetFormType (
						targetFormType)

					.localFile (
						localFile)

				;

			}

		};

		responderProvider =
			taskLogger ->
				tabContextResponderProvider.provide (
					taskLogger)

			.tab (
				tabName)

			.title (
				pageTitle)

			.pagePartFactory (
				partFactory)

		;

	}

	// defaults

	private
	void setDefaults (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"setDefaults");
		) {

			consoleHelper =
				container.consoleHelper ();

			// general stuff

			name =
				spec.name ();

			tabName =
				stringFormat (
					"%s.%s",
					container.pathPrefix (),
					name);

			tabLabel =
				capitalise (
					camelToSpaces (
						name));

			localFile =
				stringFormat (
					"%s.%s",
					container.pathPrefix (),
					name);

			privKey =
				stringFormat (
					"%s.manage",
					container.pathPrefix ());

			pageTitle =
				stringFormat (
					"%s %s",
					container.consoleHelper ().friendlyName (),
					camelToSpaces (
						name));

			linksField =
				container.consoleHelper ().field (
					spec.linksFieldName ());

			targetConsoleHelper =
				objectManager.consoleHelperForClassRequired (
					(Class <?>)
					linksField.collectionValueType ());

			targetLinksField =
				targetConsoleHelper.field (
					spec.targetLinksFieldName ());

			addEventName =
				spec.addEventName ();

			removeEventName =
				spec.removeEventName ();

			eventOrder =
				spec.eventOrder ();

			updateSignalName =
				spec.updateSignalName ();

			targetUpdateSignalName =
				spec.targetUpdateSignalName ();

			successNotice =
				spec.successNotice ();

			targetFormType =
				consoleFormManager.getFormTypeRequired (
					taskLogger,
					consoleModule.name (),
					spec.formTypeName (),
					targetConsoleHelper.objectClass ());

		}

	}

}
