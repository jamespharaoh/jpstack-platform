package wbs.platform.object.search;

import static wbs.utils.collection.CollectionUtils.emptyList;
import static wbs.utils.collection.IterableUtils.iterableMapToList;
import static wbs.utils.collection.MapUtils.emptyMap;
import static wbs.utils.collection.MapUtils.mapItemForKeyRequired;
import static wbs.utils.etc.NullUtils.isNotNull;
import static wbs.utils.etc.OptionalUtils.presentInstances;
import static wbs.utils.etc.ReflectionUtils.methodGetRequired;
import static wbs.utils.etc.ReflectionUtils.methodInvoke;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.string.StringUtils.stringSplitComma;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.forms.core.ConsoleForm;
import wbs.console.forms.core.FormFieldSet;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.platform.user.console.UserConsoleLogic;
import wbs.platform.user.console.UserSessionLogic;

import wbs.utils.etc.NumberUtils;
import wbs.utils.string.FormatWriter;

import wbs.web.responder.BufferedTextResponder;

@Accessors (fluent = true)
@PrototypeComponent ("objectSearchCsvResponder")
public
class ObjectSearchCsvResponder <ResultType>
	extends BufferedTextResponder {

	// singleton dependencies

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

	// properties

	@Getter @Setter
	ConsoleHelper <?> consoleHelper;

	@Getter @Setter
	Map <String, ObjectSearchResultsMode <ResultType>> resultsModes;

	@Getter @Setter
	String resultsDaoMethodName;

	@Getter @Setter
	String sessionKey;

	// state

	FormatWriter formatWriter;

	List <FormFieldSet <ResultType>> formFieldSets =
		new ArrayList<> ();

	Object searchObject;
	List <Long> objectIds;

	ConsoleForm <ResultType> formContext;

	// implementation

	@Override
	public
	void prepare (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"prepare");

		) {

			// set search object

			searchObject =
				userSessionLogic.userDataObjectRequired (
					transaction,
					userConsoleLogic.userRequired (
						transaction),
					stringFormat (
						"object_search_%s_fields",
						sessionKey));

			// get object ids

			List <Long> objectIdsTemp =
				iterableMapToList (
					stringSplitComma (
						userSessionLogic.userDataStringRequired (
							transaction,
							userConsoleLogic.userRequired (
								transaction),
							stringFormat (
								"object_search_%s_results",
								sessionKey))),
					NumberUtils::parseIntegerRequired);

			objectIds =
				objectIdsTemp;

			// results mode

			String resultsModeName =
				requestContext.parameterOrDefault (
					"mode",
					resultsModes.keySet ().iterator ().next ());

			ObjectSearchResultsMode <ResultType> resultsMode =
				mapItemForKeyRequired (
					resultsModes,
					resultsModeName);

			// form context

			formContext =
				resultsMode.formType ().buildResponse (
					transaction,
					emptyMap (),
					emptyList ());

		}

	}


	@Override
	protected
	void headers (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"headers");

		) {

			requestContext.contentType (
				"text/csv",
				"utf-8");

			requestContext.setHeader (
				"Content-Disposition",
				stringFormat (
					"attachment;filename=%s-search.csv",
					consoleHelper.objectTypeHyphen ()));

		}

	}

	@Override
	protected
	void render (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"render");

		) {

			// write headers

			formContext.outputCsvHeadings (
				transaction,
				formatWriter);

			// iterate through objects

			int batchesSinceFlush = 0;

			for (
				List <Long> batch
					: Lists.partition (
						objectIds,
						64)
			) {

				List <Optional <ResultType>> objects;

				if (
					isNotNull (
						resultsDaoMethodName)
				) {

					Method method =
						methodGetRequired (
							consoleHelper.getClass (),
							resultsDaoMethodName,
							ImmutableList.<Class<?>> of (
								searchObject.getClass (),
								List.class));

					objects =
						genericCastUnchecked (
							methodInvoke (
								method,
								consoleHelper,
								searchObject,
								batch));

				} else {

					objects =
						genericCastUnchecked (
							consoleHelper.findMany (
								transaction,
								batch));

				}

				for (
					ResultType object
						: presentInstances (
							objects)
				) {

					// write object

					formContext.outputCsvRow (
						transaction,
						formatWriter,
						object);

				}

				// flush regularly

				if (++ batchesSinceFlush == 64) {

					transaction.flush ();

					batchesSinceFlush = 0;

				}

			}

		}

	}

}
