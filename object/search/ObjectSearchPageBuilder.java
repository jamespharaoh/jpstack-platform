package wbs.platform.object.search;

import static wbs.utils.collection.CollectionUtils.collectionSize;
import static wbs.utils.etc.LogicUtils.ifNotNullThenElse;
import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.etc.NullUtils.isNotNull;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.OptionalUtils.optionalIsNotPresent;
import static wbs.utils.etc.TypeUtils.classForName;
import static wbs.utils.etc.TypeUtils.classForNameRequired;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.string.StringUtils.capitalise;
import static wbs.utils.string.StringUtils.hyphenToCamel;
import static wbs.utils.string.StringUtils.hyphenToCamelCapitalise;
import static wbs.utils.string.StringUtils.stringFormat;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.google.common.base.Optional;

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
import wbs.framework.entity.record.IdObject;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.web.mvc.WebAction;
import wbs.web.responder.WebResponder;

@PrototypeComponent ("objectSearchPageBuilder")
public
class ObjectSearchPageBuilder <
	ObjectType extends Record <ObjectType>,
	SearchType extends Serializable,
	ResultType extends IdObject
> implements ConsoleModuleBuilderComponent {

	// singleton dependencies

	@SingletonDependency
	ComponentManager componentManager;

	@SingletonDependency
	ConsoleMetaManager consoleMetaManager;

	@SingletonDependency
	ConsoleFormManager formContextManager;

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
	ComponentProvider <TabContextResponder> tabContextResponderProvider;

	@PrototypeDependency
	ComponentProvider <ObjectSearchGetAction>
		objectSearchGetActionProvider;

	@PrototypeDependency
	ComponentProvider <ObjectSearchPart <ObjectType, SearchType>>
		objectSearchPartProvider;

	@PrototypeDependency
	ComponentProvider <
		ObjectSearchPostAction <
			ObjectType,
			SearchType,
			ResultType
		>
	> objectSearchPostActionProvider;

	@PrototypeDependency
	ComponentProvider <ObjectSearchResultsPart <ObjectType, ResultType>>
		objectSearchResultsPartProvider;

	// builder

	@BuilderParent
	ConsoleContextBuilderContainer <ObjectType> container;

	@BuilderSource
	ObjectSearchPageSpec spec;

	@BuilderTarget
	ConsoleModuleImplementation consoleModule;

	// state

	ConsoleHelper <ObjectType> consoleHelper;

	Class <SearchType> searchClass;
	Class <ResultType> resultClass;

	ConsoleFormType <SearchType> searchFormType;

	Map <String, ObjectSearchResultsMode <ResultType>> resultsModes;

	String name;
	String sessionKey;
	String privKey;
	String parentIdKey;
	String parentIdName;
	String tabName;
	String tabLabel;
	String fileName;

	ComponentProvider <WebResponder> searchResponderProvider;
	ComponentProvider <WebResponder> resultsResponderProvider;

	ComponentProvider <WebAction> searchGetActionProvider;
	ComponentProvider <WebAction> searchPostActionProvider;

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
					"buildConsoleModule");

		) {

			setDefaults (
				taskLogger);

			buildGetAction (
				taskLogger);

			buildPostAction (
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
						privKey != null
							? Collections.singletonList (privKey)
							: Collections.<String>emptyList ()),

				extensionPoint.contextTypeNames ());

		}

	}

	private
	void buildGetAction (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"buildGetAction");

		) {

			searchGetActionProvider =
				taskLoggerNested ->
					objectSearchGetActionProvider.provide (
						taskLoggerNested)

				.searchResponderProvider (
					searchResponderProvider)

				.resultsResponderProvider (
					resultsResponderProvider)

				.sessionKey (
					sessionKey);

		}

	}

	private
	void buildPostAction (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"buildPostAction");

		) {

			searchPostActionProvider =
				taskLoggerNested ->
					objectSearchPostActionProvider.provide (
						taskLoggerNested)

				.consoleHelper (
					consoleHelper)

				.searchClass (
					searchClass)

				.searchDaoMethodName (
					spec.searchDaoMethodName ())

				.resultsDaoMethodName (
					spec.resultsDaoMethodName ())

				.sessionKey (
					sessionKey)

				.parentIdKey (
					parentIdKey)

				.parentIdName (
					parentIdName)

				.searchFormType (
					searchFormType)

				.resultsModes (
					resultsModes)

				.searchResponderProvider (
					searchResponderProvider)

				.fileName (
					fileName)

			;

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

					.getActionProvider (
						searchGetActionProvider)

					.postActionProvider (
						searchPostActionProvider)

					.privKeys (
						taskLogger,
						privKey != null
							? Collections.singletonList (privKey)
							: Collections.<String>emptyList ()),

				extensionPoint.contextTypeNames ()
			);

		}

	}

	private
	void setDefaults (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"setDefaults");

		) {

			name =
				ifNull (
					spec.name (),
					"search");

			if (
				isNotNull (
					spec.objectTypeName ())
			) {

				consoleHelper =
					genericCastUnchecked (
						objectManager.consoleHelperForNameRequired (
							spec.objectTypeName ()));

			} else {

				consoleHelper =
					container.consoleHelper ();

			}

			String searchClassName =
				ifNull (
					spec.searchClassName (),
					stringFormat (
						"%s.%sSearch",
						consoleHelper
							.objectClass ()
							.getPackage ()
							.getName (),
						capitalise (
							consoleHelper.objectName ())));

			Optional <Class <?>> searchClassOptional =
				classForName (
					searchClassName);

			if (
				optionalIsNotPresent (
					searchClassOptional)
			) {

				throw new RuntimeException (
					stringFormat (
						"Search class not found: %s",
						searchClassName));

			}

			searchClass =
				genericCastUnchecked (
					searchClassOptional.get ());

			resultClass =
				genericCastUnchecked (
					ifNotNullThenElse (
						spec.resultsClassName (),
						() -> classForNameRequired (
							spec.resultsClassName ()),
						() -> consoleHelper.objectClass ()));

			privKey =
				spec.privKey ();

			parentIdKey =
				spec.parentIdKey ();

			parentIdName =
				spec.parentIdName ();

			sessionKey =
				stringFormat (
					"%s.%s",
					container.pathPrefix (),
					name);

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
					"Search");

			fileName =
				ifNull (
					spec.fileName (),
					stringFormat (
						"%s.%s",
						container.pathPrefix (),
						name));

			searchResponderProvider =
				componentManager.getComponentProviderRequired (
					taskLogger,
					stringFormat (
						"%s%sResponder",
						container.newBeanNamePrefix (),
						capitalise (
							name)),
					WebResponder.class);

			resultsResponderProvider =
				componentManager.getComponentProviderRequired (
					taskLogger,
					stringFormat (
						"%s%sResultsResponder",
						container.newBeanNamePrefix (),
						capitalise (
							name)),
					WebResponder.class);

			searchFormType =
				genericCastUnchecked (
					componentManager.getComponentRequired (
						taskLogger,
						stringFormat (
							"%s%sFormType",
							hyphenToCamel (
								container.consoleModule ().name ()),
							hyphenToCamelCapitalise (
								spec.searchFormTypeName ())),
						ConsoleFormType.class));

			long numResultsModes =
				ifNotNullThenElse (
					spec.resultsFormTypeName (),
					() -> 1l,
					() -> collectionSize (
						spec.resultsModes ()));

			resultsModes =
				LongStream.range (
					0l,
					numResultsModes)

				.mapToObj (
					index ->
						componentManager.getComponentRequired (
							taskLogger,
							stringFormat (
								"%s%sResultsMode%s",
								container.newBeanNamePrefix (),
								capitalise (
									name),
								integerToDecimalString (
									index)),
							ObjectSearchResultsMode.class))

				.collect (
					Collectors.toMap (
						resultsMode ->
							resultsMode.name (),
						resultsMode ->
							genericCastUnchecked (
								resultsMode)))

			;

		}

	}

}
