package wbs.platform.object.settings;

import static wbs.utils.etc.LogicUtils.ifNotNullThenElse;
import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.string.StringUtils.camelToHyphen;
import static wbs.utils.string.StringUtils.camelToSpaces;
import static wbs.utils.string.StringUtils.capitalise;
import static wbs.utils.string.StringUtils.hyphenToCamel;
import static wbs.utils.string.StringUtils.hyphenToCamelCapitalise;
import static wbs.utils.string.StringUtils.stringFormat;

import java.util.Collections;

import lombok.NonNull;

import wbs.console.context.ConsoleContextBuilderContainer;
import wbs.console.context.ResolvedConsoleContextExtensionPoint;
import wbs.console.forms.core.ConsoleFormBuilder;
import wbs.console.forms.core.ConsoleFormManager;
import wbs.console.forms.core.ConsoleFormType;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.module.ConsoleManager;
import wbs.console.module.ConsoleMetaManager;
import wbs.console.module.ConsoleModuleBuilderComponent;
import wbs.console.module.ConsoleModuleImplementation;
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
import wbs.framework.component.annotations.WeakSingletonDependency;
import wbs.framework.component.manager.ComponentManager;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.web.mvc.WebAction;
import wbs.web.responder.WebResponder;

@PrototypeComponent ("objectSettingsPageBuilder")
public
class ObjectSettingsPageBuilder <
	ObjectType extends Record <ObjectType>,
	ParentType extends Record <ParentType>
> implements ConsoleModuleBuilderComponent {

	// singleton dependencies

	@SingletonDependency
	ComponentManager componentManager;

	@SingletonDependency
	ConsoleFormBuilder consoleFormBuilder;

	@WeakSingletonDependency
	ConsoleManager consoleManager;

	@SingletonDependency
	ConsoleMetaManager consoleMetaManager;

	@SingletonDependency
	ConsoleFormManager consoleFormManager;

	@ClassSingletonDependency
	LogContext logContext;

	@WeakSingletonDependency
	ConsoleObjectManager objectManager;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <ConsoleFile> consoleFile;

	@PrototypeDependency
	ComponentProvider <ConsoleContextTab> contextTab;

	@PrototypeDependency
	ComponentProvider <ObjectRemoveAction> objectRemoveAction;

	@PrototypeDependency
	ComponentProvider <ObjectSettingsAction <ObjectType, ParentType>>
		objectSettingsActionProvider;

	@PrototypeDependency
	ComponentProvider <ObjectSettingsPart <ObjectType, ParentType>>
		objectSettingsPartProvider;

	@PrototypeDependency
	ComponentProvider <TabContextResponder> tabContextResponder;

	// builder

	@BuilderParent
	ConsoleContextBuilderContainer <ObjectType> container;

	@BuilderSource
	ObjectSettingsPageSpec spec;

	@BuilderTarget
	ConsoleModuleImplementation consoleModule;

	// state

	ConsoleHelper <ObjectType> consoleHelper;

	ConsoleFormType <ObjectType> formType;

	String privKey;
	String name;
	String shortName;
	String longName;
	String friendlyLongName;
	String friendlyShortName;
	String fileName;
	String tabName;
	String tabLocation;

	ComponentProvider <WebResponder> responderProvider;
	ComponentProvider <WebAction> settingsActionProvider;

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

			buildAction (
				taskLogger);

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
				"end",
				contextTab.provide (
					taskLogger)

					.name (
						tabName)

					.defaultLabel (
						capitalise (
							friendlyShortName))

					.localFile (
						fileName)

					.privKeys (
						Collections.singletonList (
							privKey)),

				extensionPoint.contextTypeNames ());

		}

	}

	void buildAction (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"buildAction");

		) {

			if (consoleHelper.ephemeral ()) {

				settingsActionProvider =
					taskLoggerNested ->
						objectSettingsActionProvider.provide (
							taskLoggerNested)

					.detailsResponderProvider (
						responderProvider)

					.accessDeniedResponderProvider (
						responderProvider)

					.editPrivKey (
						privKey)

					.objectLookup (
						consoleHelper)

					.consoleHelper (
						consoleHelper)

					.formType (
						formType)

					.objectRefName (
						consoleHelper.codeExists ()
							? consoleHelper.codeFieldName ()
							: "id")

					.objectType (
						consoleHelper.objectTypeCode ())

				;

			} else {

				settingsActionProvider =
					taskLoggerNested ->
						objectSettingsActionProvider.provide (
							taskLoggerNested)

					.detailsResponderProvider (
						responderProvider)

					.accessDeniedResponderProvider (
						responderProvider)

					.editPrivKey (
						privKey)

					.objectLookup (
						consoleHelper)

					.consoleHelper (
							consoleHelper)

					.formType (
						formType)

				;

			}

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

			consoleModule.addContextFile (
				fileName,
				consoleFile.provide (
					taskLogger)

					.getResponderProvider (
						responderProvider)

					.postActionProvider (
						settingsActionProvider)

					.privName (
						taskLogger,
						privKey),

				extensionPoint.contextTypeNames ()
			);

			if (consoleHelper.ephemeral ()) {

				ComponentProvider <WebAction> removeActionProvider =
					taskLoggerNested ->
						objectRemoveAction.provide (
							taskLoggerNested)

					.objectHelper (
						consoleHelper)

					.settingsResponderProvider (
						responderProvider)

					.listResponderProvider (
						componentManager.getComponentProviderRequired (
							taskLogger,
							stringFormat (
								"%sListResponder",
								container.newBeanNamePrefix ()),
							WebResponder.class))

					.nextContextTypeName (
						ifNull (
							spec.listContextTypeName (),
						consoleHelper.objectTypeCamel () + ":list"))

					.editPrivKey (
						privKey);

				consoleModule.addContextFile (

					stringFormat (
						"%s.remove",
						container.structuralName ()),

					consoleFile.provide (
						taskLogger)

						.postActionProvider (
							removeActionProvider)

						.privName (
							taskLogger,
							privKey),

					extensionPoint.contextTypeNames ());

			}

		}

	}

	void setDefaults (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"setDefaults");

		) {

			consoleHelper =
				genericCastUnchecked (
					ifNotNullThenElse (
						spec.objectName (),
						() -> objectManager.consoleHelperForNameRequired (
							camelToHyphen (
								spec.objectName ())),
						() -> container.consoleHelper ()));

			name =
				spec.name ();

			shortName =
				ifNull (
					spec.shortName (),
					"settings");

			longName =
				ifNull (
					spec.longName (),
					"settings");

			friendlyShortName =
				ifNull (
					spec.friendlyShortName (),
					camelToSpaces (
						shortName));

			friendlyLongName =
				ifNull (
					spec.friendlyLongName (),
					stringFormat (
						"%s %s",
						consoleHelper.friendlyNameSingular (),
						camelToSpaces (
							longName)));

			responderProvider =
				componentManager.getComponentProviderRequired (
					taskLogger,
					ifNull (
						spec.responderName (),
						stringFormat (
							"%s%s%s",
							container.newBeanNamePrefix (),
							capitalise (shortName),
							"Responder")),
					WebResponder.class);

			fileName =
				ifNull (
					spec.fileName (),
					stringFormat (
						"%s.%s",
						container.pathPrefix (),
						shortName));

			tabName =
				ifNull (
					spec.tabName (),
					stringFormat (
						"%s.%s",
						container.pathPrefix (),
						shortName));

			privKey =
				ifNull (
					spec.privKey (),
					stringFormat (
						"%s.manage",
						consoleHelper.objectTypeCamel ()));

			formType =
				genericCastUnchecked (
					componentManager.getComponentRequired (
						taskLogger,
						stringFormat (
							"%s%sFormType",
							hyphenToCamel (
								container.consoleModule ().name ()),
							hyphenToCamelCapitalise (
								spec.formTypeName ())),
						ConsoleFormType.class));

		}

	}

}
