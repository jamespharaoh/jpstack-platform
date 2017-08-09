package wbs.platform.object.search;

import static wbs.utils.collection.CollectionUtils.collectionSize;
import static wbs.utils.collection.IterableUtils.iterableMap;
import static wbs.utils.collection.MapUtils.emptyMap;
import static wbs.utils.etc.NullUtils.isNotNull;
import static wbs.utils.etc.NullUtils.isNull;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.OptionalUtils.optionalIsPresent;
import static wbs.utils.etc.OptionalUtils.optionalOrElseRequired;
import static wbs.utils.etc.PropertyUtils.propertySetAuto;
import static wbs.utils.etc.ReflectionUtils.methodGetRequired;
import static wbs.utils.etc.ReflectionUtils.methodInvoke;
import static wbs.utils.etc.TypeUtils.classInstantiate;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.string.StringUtils.joinWithComma;
import static wbs.utils.string.StringUtils.stringFormat;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.action.ConsoleAction;
import wbs.console.context.ConsoleContext;
import wbs.console.context.ConsoleContextType;
import wbs.console.forms.core.ConsoleForm;
import wbs.console.forms.core.ConsoleFormType;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.module.ConsoleManager;
import wbs.console.request.ConsoleRequestContext;
import wbs.console.responder.RedirectResponder;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.TaskLogger;

import wbs.platform.user.console.UserConsoleLogic;
import wbs.platform.user.console.UserSessionLogic;
import wbs.platform.user.model.UserRec;

import wbs.utils.etc.NumberUtils;

import wbs.web.responder.WebResponder;

@Accessors (fluent = true)
@PrototypeComponent ("objectSearchAction")
public
class ObjectSearchPostAction <
	ObjectType extends Record <ObjectType>,
	SearchType extends Serializable,
	ResultType
>
	extends ConsoleAction {

	// singleton dependencies

	@SingletonDependency
	ConsoleManager consoleManager;

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	@SingletonDependency
	UserSessionLogic userSessionLogic;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <RedirectResponder> redirectResponderProvider;

	@PrototypeDependency
	ComponentProvider <ObjectSearchCsvResponder <ResultType>>
		objectSearchCsvResponderProvider;

	// properties

	@Getter @Setter
	ConsoleHelper <ObjectType> consoleHelper;

	@Getter @Setter
	Class <SearchType> searchClass;

	@Getter @Setter
	String searchDaoMethodName;

	@Getter @Setter
	String resultsDaoMethodName;

	@Getter @Setter
	String sessionKey;

	@Getter @Setter
	String parentIdKey;

	@Getter @Setter
	String parentIdName;

	@Getter @Setter
	ConsoleFormType <SearchType> searchFormType;

	@Getter @Setter
	Map <String, ObjectSearchResultsMode <ResultType>> resultsModes;

	@Getter @Setter
	ComponentProvider <WebResponder> searchResponderProvider;

	@Getter @Setter
	String fileName;

	// implementation

	@Override
	protected
	WebResponder goReal (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"goReal");

		) {

			UserRec user =
				userConsoleLogic.userRequired (
					transaction);

			// handle new/repeat search buttons

			if (
				optionalIsPresent (
					requestContext.parameter (
						"new-search"))
			) {

				transaction.debugFormat (
					"New search");

				userSessionLogic.userDataRemove (
					transaction,
					user,
					stringFormat (
						"object_search_%s_results",
						sessionKey));

				transaction.commit ();

				return redirectResponderProvider.provide (
					transaction,
					redirectResponder ->
						redirectResponder

					.targetUrl (
						requestContext.resolveLocalUrl (
							"/" + fileName))

				);

			}

			if (
				optionalIsPresent (
					requestContext.parameter (
						"repeat-search"))
			) {

				transaction.debugFormat (
					"Repeat search");

				userSessionLogic.userDataRemove (
					transaction,
					user,
					stringFormat (
						"object_search_%s_results",
						sessionKey));

				transaction.flush ();

			}

			if (
				optionalIsPresent (
					requestContext.parameter (
						"download-csv"))
			) {

				transaction.debugFormat (
					"Download CSV");

				transaction.commit ();

				return objectSearchCsvResponderProvider.provide (
					transaction,
					objectSearchCsvResponder ->
						objectSearchCsvResponder

					.consoleHelper (
						consoleHelper)

					.resultsModes (
						resultsModes)

					.resultsDaoMethodName (
						resultsDaoMethodName)

					.sessionKey (
						sessionKey)

				);

			}

			// perform search

			transaction.debugFormat (
				"Process search form");

			SearchType search =
				genericCastUnchecked (
					optionalOrElseRequired (
						userSessionLogic.userDataObject (
							transaction,
							user,
							stringFormat (
								"object_search_%s_fields",
								sessionKey)),
						() -> classInstantiate (
							searchClass)));

			ConsoleForm <SearchType> searchFormContext =
				searchFormType.buildAction (
					transaction,
					emptyMap (),
					search);

			searchFormContext.implicit (
				transaction);

			consoleHelper.consoleHooks ().applySearchFilter (
				transaction,
				search);

			if (
				parentIdKey != null
				|| parentIdName != null
			) {

				if (
					parentIdKey == null
					|| parentIdName == null
				) {
					throw new RuntimeException ();
				}

				Object parentId =
					requestContext.stuff (
						parentIdKey);

				if (parentId == null) {
					throw new RuntimeException ();
				}

				propertySetAuto (
					search,
					parentIdName,
					parentId);

			}

			// update search details

			searchFormContext.update (
				transaction);

			userSessionLogic.userDataObjectStore (
				transaction,
				user,
				stringFormat (
					"object_search_%s_fields",
					sessionKey),
				search);

			if (searchFormContext.errors ()) {

				searchFormContext.reportErrors (
					transaction);

				transaction.commit ();

				return searchResponderProvider.provide (
					transaction);

			}

			// perform search

			transaction.debugFormat (
				"Perform search");

			List <Long> objectIds;

			if (
				isNotNull (
					searchDaoMethodName)
			) {

				Method method =
					methodGetRequired (
						consoleHelper.getClass (),
						searchDaoMethodName,
						ImmutableList.<Class <?>> of (
							Transaction.class,
							searchClass));

				objectIds =
					genericCastUnchecked (
						methodInvoke (
							method,
							consoleHelper,
							transaction,
							search));

			} else {

				objectIds =
					consoleHelper.searchIds (
						transaction,
						search);

			}

			if (objectIds.isEmpty ()) {

				// no results

				transaction.debugFormat (
					"Search returned no results");

				requestContext.addError (
					"No results returned from search");

				transaction.commit ();

				return searchResponderProvider.provide (
					transaction);

			} else if (

				objectIds.size () == 1

				&& isNull (
					resultsDaoMethodName)

			) {

				// single result

				transaction.debugFormat (
					"Search returned single result");

				ConsoleContextType targetContextType =
					consoleManager.contextType (
						consoleHelper.objectName () + ":combo",
						true);

				Optional <ConsoleContext> targetContext =
					consoleManager.relatedContext (
						transaction,
						requestContext.consoleContextRequired (),
						targetContextType);

				if (
					optionalIsPresent (
						targetContext)
				) {

					transaction.commit ();

					return redirectResponderProvider.provide (
						transaction,
						redirectResponder ->
							redirectResponder

						.targetUrl (
							requestContext.resolveContextUrlFormat (
								"%s",
								targetContext.get ().pathPrefix (),
								"/%s",
								consoleHelper.getPathId (
									transaction,
									objectIds.get (
										0))))

					);

				} else {

					Record <?> object =
						consoleHelper.findRequired (
							transaction,
							objectIds.get (
								0));

					transaction.commit ();

					return redirectResponderProvider.provide (
						transaction,
						redirectResponder ->
							redirectResponder

						.targetUrl (
							requestContext.resolveLocalUrl (
								consoleHelper.getDefaultLocalPathGeneric (
									transaction,
									object)))

					);

				}

			} else {

				// multiple results

				transaction.debugFormat (
					"Search returned %s results",
					integerToDecimalString (
						collectionSize (
							objectIds)));

				userSessionLogic.userDataStringStore (
					transaction,
					user,
					stringFormat (
						"object_search_%s_results",
						sessionKey),
					joinWithComma (
						iterableMap (
							objectIds,
							NumberUtils::integerToDecimalString)));

				transaction.commit ();

				return redirectResponderProvider.provide (
					transaction,
					redirectResponder ->
						redirectResponder

					.targetUrl (
						requestContext.resolveLocalUrl (
							"/" + fileName))

				);

			}

		}

	}

}
