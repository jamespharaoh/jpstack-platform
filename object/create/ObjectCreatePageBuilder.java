package wbs.platform.object.create;

import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.string.StringUtils.capitalise;
import static wbs.utils.string.StringUtils.hyphenToCamel;
import static wbs.utils.string.StringUtils.hyphenToCamelCapitalise;
import static wbs.utils.string.StringUtils.stringFormat;

import lombok.NonNull;

import wbs.console.context.ConsoleContextBuilderContainer;
import wbs.console.context.ResolvedConsoleContextExtensionPoint;
import wbs.console.forms.core.ConsoleFormBuilder;
import wbs.console.forms.core.ConsoleFormManager;
import wbs.console.forms.core.ConsoleFormType;
import wbs.console.helper.core.ConsoleHelper;
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
import wbs.framework.component.manager.ComponentManager;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.web.mvc.WebAction;
import wbs.web.responder.WebResponder;

@PrototypeComponent ("objectCreatePageBuilder")
public
class ObjectCreatePageBuilder <
	ObjectType extends Record <ObjectType>,
	ParentType extends Record <ParentType>
>
	implements ConsoleModuleBuilderComponent {

	// singleton dependences

	@SingletonDependency
	ComponentManager componentManager;

	@SingletonDependency
	ConsoleMetaManager consoleMetaManager;

	@SingletonDependency
	ConsoleFormBuilder consoleFormBuilder;

	@SingletonDependency
	ConsoleFormManager formContextManager;

	@ClassSingletonDependency
	LogContext logContext;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <ConsoleFile> consoleFileProvider;

	@PrototypeDependency
	ComponentProvider <ConsoleContextTab> contextTabProvider;

	@PrototypeDependency
	ComponentProvider <ObjectCreateAction <ObjectType, ParentType>>
		objectCreateActionProvider;

	@PrototypeDependency
	ComponentProvider <ObjectCreatePart <ObjectType, ParentType>>
		objectCreatePartProvider;

	@PrototypeDependency
	ComponentProvider <TabContextResponder> tabContextResponderProvider;

	// builder

	@BuilderParent
	ConsoleContextBuilderContainer<ObjectType> container;

	@BuilderSource
	ObjectCreatePageSpec spec;

	@BuilderTarget
	ConsoleModuleImplementation consoleModule;

	// state

	ConsoleHelper <ObjectType> consoleHelper;

	String name;
	String typeCode;
	String tabName;
	String tabLabel;
	String localFile;
	String targetContextTypeName;
	//FieldsProvider <ObjectType, ParentType> fieldsProvider;
	ConsoleFormType <ObjectType> formType;
	String createTimeFieldName;
	String createUserFieldName;
	String createPrivDelegate;
	String createPrivCode;
	String privKey;

	ComponentProvider <WebResponder> responderProvider;
	ComponentProvider <WebResponder> targetResponderProvider;

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

			for (
				ResolvedConsoleContextExtensionPoint extensionPoint
					: consoleMetaManager.resolveExtensionPoint (
						taskLogger,
						container.extensionPointName ())
			) {

				buildTab (
					taskLogger,
					extensionPoint);

				buildFile (
					taskLogger,
					extensionPoint);

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

				contextTabProvider.provide (
					taskLogger)

					.name (
						tabName)

					.defaultLabel (
						tabLabel)

					.localFile (
						localFile)

					/*.privKeys (
					 * 	Collections.singletonList (privKey))*/,

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

			ComponentProvider <WebAction> createActionProvider =
				taskLoggerNested ->
					objectCreateActionProvider.provide (
						taskLoggerNested)

				.consoleHelper (
					consoleHelper)

				.typeCode (
					typeCode)

				.responderProvider (
					responderProvider)

				.targetContextTypeName (
					targetContextTypeName)

				.targetResponderProvider (
					targetResponderProvider)

				.createPrivDelegate (
					createPrivDelegate)

				.createPrivCode (
					createPrivCode)

				.formType (
					formType)

				.createTimeFieldName (
					createTimeFieldName)

				.createUserFieldName (
					createUserFieldName);

			consoleModule.addContextFile (
				localFile,
				consoleFileProvider.provide (
					taskLogger)

					.getResponderProvider (
						responderProvider)

					.postActionProvider (
						createActionProvider)

					/*.privKeys (
						Collections.singletonList (privKey)*/,

				extensionPoint.contextTypeNames ()
			);

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
				container.consoleHelper ();

			name =
				ifNull (
					spec.name (),
					"create");

			typeCode =
				spec.typeCode ();

			tabName =
				ifNull (
					spec.tabName (),
					stringFormat (
						"%s.%s",
						container.pathPrefix (),
						name));

			tabLabel =
				capitalise (
					name);

			localFile =
				ifNull (
					spec.localFile (),
					stringFormat (
						"%s.%s",
						container.pathPrefix (),
						name));

			responderProvider =
				componentManager.getComponentProviderRequired (
					taskLogger,
					ifNull (
						spec.responderName (),
						stringFormat (
							"%s%sResponder",
							container.newBeanNamePrefix (),
							capitalise (
								name))),
					WebResponder.class);

			targetContextTypeName =
				ifNull (
					spec.targetContextTypeName (),
					consoleHelper.objectName () + ":combo");

			targetResponderProvider =
				componentManager.getComponentProviderRequired (
					taskLogger,
					ifNull (
						spec.targetResponderName (),
						stringFormat (
							"%sSettingsResponder",
							consoleHelper.objectName ())),
					WebResponder.class);

			createTimeFieldName =
				spec.createTimeFieldName ();

			createUserFieldName =
				spec.createUserFieldName ();

			privKey =
				spec.privKey ();

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
