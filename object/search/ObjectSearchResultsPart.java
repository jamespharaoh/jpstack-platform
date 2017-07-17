package wbs.platform.object.search;

import static wbs.utils.collection.CollectionUtils.collectionHasOneItem;
import static wbs.utils.collection.CollectionUtils.emptyList;
import static wbs.utils.collection.CollectionUtils.listSlice;
import static wbs.utils.collection.IterableUtils.iterableIsNotEmpty;
import static wbs.utils.collection.IterableUtils.iterableMapToList;
import static wbs.utils.collection.MapUtils.emptyMap;
import static wbs.utils.collection.MapUtils.mapItemForKeyRequired;
import static wbs.utils.etc.NullUtils.isNotNull;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.NumberUtils.parseIntegerRequired;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIf;
import static wbs.utils.etc.OptionalUtils.optionalIsNotPresent;
import static wbs.utils.etc.OptionalUtils.optionalIsPresent;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.etc.OptionalUtils.presentInstances;
import static wbs.utils.etc.ReflectionUtils.methodGetRequired;
import static wbs.utils.etc.ReflectionUtils.methodInvoke;
import static wbs.utils.etc.TypeUtils.classEqualSafe;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.etc.TypeUtils.isNotInstanceOf;
import static wbs.utils.string.StringUtils.capitalise;
import static wbs.utils.string.StringUtils.hyphenToSpaces;
import static wbs.utils.string.StringUtils.objectToString;
import static wbs.utils.string.StringUtils.stringEqualSafe;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.string.StringUtils.stringSplitComma;
import static wbs.utils.time.TimeUtils.localDateNotEqual;
import static wbs.web.utils.HtmlAttributeUtils.htmlClassAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlColumnSpanAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlDataAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlDataAttributeFormat;
import static wbs.web.utils.HtmlAttributeUtils.htmlStyleAttribute;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphClose;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphOpen;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphWriteFormat;
import static wbs.web.utils.HtmlFormUtils.htmlFormClose;
import static wbs.web.utils.HtmlFormUtils.htmlFormOpenPost;
import static wbs.web.utils.HtmlStyleUtils.htmlStyleRuleEntry;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenList;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowOpen;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowSeparatorWrite;
import static wbs.web.utils.HtmlUtils.htmlLinkWrite;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.joda.time.Instant;
import org.joda.time.LocalDate;

import wbs.console.context.ConsoleContext;
import wbs.console.context.ConsoleContextType;
import wbs.console.forms.core.ConsoleForm;
import wbs.console.forms.types.FormField;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.helper.core.ConsoleHooks;
import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.html.MagicTableScriptRef;
import wbs.console.html.ScriptRef;
import wbs.console.misc.JqueryScriptRef;
import wbs.console.module.ConsoleManager;
import wbs.console.part.AbstractPagePart;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.IdObject;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;

import wbs.platform.user.console.UserConsoleLogic;
import wbs.platform.user.console.UserSessionLogic;

import wbs.utils.etc.NumberUtils;
import wbs.utils.etc.PropertyUtils;
import wbs.utils.string.FormatWriter;

@Accessors (fluent = true)
@PrototypeComponent ("objectSearchResultsPart")
public
class ObjectSearchResultsPart <
	ObjectType extends Record <ObjectType>,
	ResultType extends IdObject
>
	extends AbstractPagePart {

	// singleton dependencies

	@SingletonDependency
	ConsoleManager consoleManager;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	@SingletonDependency
	UserSessionLogic userSessionLogic;

	// properties

	@Getter @Setter
	ConsoleHelper <ObjectType> consoleHelper;

	@Getter @Setter
	Map <String, ObjectSearchResultsMode <ResultType>> resultsModes;

	@Getter @Setter
	Class <ResultType> resultsClass;

	@Getter @Setter
	String resultsDaoMethodName;

	@Getter @Setter
	String sessionKey;

	@Getter @Setter
	Long itemsPerPage;

	@Getter @Setter
	String targetContextTypeName;

	// state

	IdObject currentObject;
	List <Optional <ResultType>> objects;
	Integer totalObjects;

	Boolean singlePage;
	Long pageNumber;
	Long pageCount;

	Optional <ConsoleContext> targetContext;

	ConsoleForm <ResultType> form;

	// details

	@Override
	public
	Set <ScriptRef> scriptRefs () {

		return ImmutableSet.<ScriptRef> builder ()

			.addAll (
				super.scriptRefs ())

			.add (
				JqueryScriptRef.instance)

			.add (
				MagicTableScriptRef.instance)

			.build ();

	}

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

			// form fields

			String resultsModeName =
				requestContext.parameterOrDefault (
					"mode",
					resultsModes.keySet ().iterator ().next ());

			ObjectSearchResultsMode <ResultType> resultsMode =
				mapItemForKeyRequired (
					resultsModes,
					resultsModeName);

			form =
				resultsMode.formType ().buildResponse (
					transaction,
					emptyMap (),
					emptyList ());

			// current object

			if (
				classEqualSafe (
					consoleHelper.objectClass (),
					resultsClass)
			) {

				Optional <Long> currentObjectIdOptional =
					requestContext.stuffInteger (
						consoleHelper.objectName () + "Id");

				if (
					optionalIsPresent (
						currentObjectIdOptional)
				) {

					currentObject =
						consoleHelper.findRequired (
							transaction,
							optionalGetRequired (
								currentObjectIdOptional));

				}

			}

			// set search object

			Object searchObject =
				userSessionLogic.userDataObjectRequired (
					transaction,
					userConsoleLogic.userRequired (
						transaction),
					stringFormat (
						"object_search_%s_fields",
						sessionKey));

			// get search results for page

			List <Long> allObjectIds =
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

			totalObjects =
				allObjectIds.size ();

			if (
				stringEqualSafe (
					requestContext.parameterOrDefault (
						"page",
						"0"),
					"all")
			) {

				singlePage = true;

			} else {

				singlePage = false;

				pageNumber =
					parseIntegerRequired (
						requestContext.parameterOrDefault (
							"page",
							"0"));

			}

			List <Long> pageObjectIds =
				singlePage

				? allObjectIds

				: listSlice (
					allObjectIds,
					pageNumber * itemsPerPage,
					Math.min (
						(pageNumber + 1) * itemsPerPage,
						allObjectIds.size ()));

			pageCount =
				(allObjectIds.size () - 1) / itemsPerPage + 1;

			// load objects

			if (
				isNotNull (
					resultsDaoMethodName)
			) {

				Method method =
					methodGetRequired (
						consoleHelper.getClass (),
						resultsDaoMethodName,
						ImmutableList.<Class <?>> of (
							Transaction.class,
							searchObject.getClass (),
							List.class));

				objects =
					genericCastUnchecked (
						methodInvoke (
							method,
							consoleHelper,
							transaction,
							searchObject,
							pageObjectIds));

			} else {

				objects =
					genericCastUnchecked (
						iterableMapToList (
							pageObjectIds,
							pageObjectId ->
								consoleHelper.find (
									transaction,
									pageObjectId)));

			}

			// other stuff

			prepareTargetContext (
				transaction);

		}

	}

	private
	void prepareTargetContext (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"prepareTargetContext");

		) {

			ConsoleContextType targetContextType =
				consoleManager.contextType (
					targetContextTypeName,
					true);

			targetContext =
				consoleManager.relatedContext (
					transaction,
					requestContext.consoleContextRequired (),
					targetContextType);

		}

	}

	@Override
	public
	void renderHtmlBodyContent (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderHtmlBodyContent");

		) {

			renderNewSearch (
				transaction,
				formatWriter);

			renderTotalObjects (
				transaction,
				formatWriter);

			renderPageNumbers (
				transaction,
				formatWriter);

			renderModeTabs (
				transaction,
				formatWriter);

			renderSearchResults (
				transaction,
				formatWriter);

			renderPageNumbers (
				transaction,
				formatWriter);

		}

	}

	private
	void renderNewSearch (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderNewSearch");

		) {

			htmlFormOpenPost (
				formatWriter);

			htmlParagraphOpen (
				formatWriter);

			formatWriter.writeLineFormat (
				"<input",
				" type=\"submit\"",
				" name=\"new-search\"",
				" value=\"new search\"",
				">");

			formatWriter.writeLineFormat (
				"<input",
				" type=\"submit\"",
				" name=\"repeat-search\"",
				" value=\"repeat search\"",
				">");

			formatWriter.writeLineFormat (
				"<input",
				" type=\"submit\"",
				" name=\"download-csv\"",
				" value=\"download csv\"",
				">");

			htmlParagraphClose (
				formatWriter);

			htmlFormClose (
				formatWriter);

		}

	}

	private
	void renderTotalObjects (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderTotalObjects");

		) {

			htmlParagraphWriteFormat (
				formatWriter,
				"Search returned %h items",
				integerToDecimalString (
					totalObjects));

		}

	}

	private
	void renderPageNumbers (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderPageNumbers");

		) {

			if (pageCount == 1)
				return;

			htmlParagraphOpen (
				formatWriter,
				htmlClassAttribute (
					"links"));

			formatWriter.writeLineFormat (
				"Select page");

			htmlLinkWrite (
				formatWriter,
				stringFormat (
					"?page=all&mode=%u",
					requestContext.parameterOrDefault (
						"mode",
						resultsModes.keySet ().iterator ().next ())),
				"All",
				presentInstances (
					optionalIf (
						singlePage,
						() -> htmlClassAttribute (
							"selected"))));

			for (
				long page = 0;
				page < pageCount;
				page ++
			) {

				htmlLinkWrite (
					formatWriter,
					stringFormat (
						"?page=%h&mode=%u",
						integerToDecimalString (
							page),
						requestContext.parameterOrDefault (
							"mode",
							resultsModes.keySet ().iterator ().next ())),
					integerToDecimalString (
						page + 1),
					presentInstances (
						optionalIf (
							! singlePage && page == pageNumber,
							() -> htmlClassAttribute (
								"selected"))));

			}

			htmlParagraphClose (
				formatWriter);

		}

	}

	private
	void renderModeTabs (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderModeTabs");

		) {

			if (
				collectionHasOneItem (
					resultsModes.entrySet ())
			) {
				return;
			}

			htmlParagraphOpen (
				formatWriter,
				htmlClassAttribute (
					"links"));

			for (
				ObjectSearchResultsMode <ResultType> resultsMode
					: resultsModes.values ()
			) {

				htmlLinkWrite (
					formatWriter,
					stringFormat (
						"?page=%u&mode=%u",
						requestContext.parameterOrDefault (
							"page",
							"0"),
						resultsMode.name ()),
					capitalise (
						hyphenToSpaces (
							resultsMode.name ())),
					presentInstances (
						optionalIf (
							stringEqualSafe (
								resultsMode.name (),
								requestContext.parameterOrDefault (
									"mode",
									resultsModes.keySet ().iterator ().next ())),
							() -> htmlClassAttribute (
								"selected"))));

			}

			htmlParagraphClose (
				formatWriter);

		}

	}

	void renderSearchResults (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderSearchResults");

		) {

			htmlTableOpenList (
				formatWriter);

			htmlTableRowOpen (
				formatWriter);

			form.outputTableHeadings (
				transaction,
				formatWriter);

			htmlTableRowClose (
				formatWriter);

			LocalDate currentDate =
				new LocalDate (0);

			for (
				Optional <ResultType> resultOptional
					: objects
			) {

				// handle deleted records

				if (
					optionalIsNotPresent (
						resultOptional)
				) {

					htmlTableRowOpen (
						formatWriter);

					htmlTableCellWrite (
						formatWriter,
						"(deleted)",
						htmlColumnSpanAttribute (
							form.columnFields ().columns ()));

					htmlTableRowClose (
						formatWriter);

					continue;

				}

				// handle restricted records

				ResultType result =
					resultOptional.get ();

				if (

					result instanceof Record

					&& ! consoleHelper.canView (
						transaction,
						genericCastUnchecked (
							result))

				)  {

					htmlTableRowOpen (
						formatWriter);

					htmlTableCellWrite (
						formatWriter,
						"(restricted)",
						htmlColumnSpanAttribute (
							form.columnFields ().columns ()));

					htmlTableRowClose (
						formatWriter);

					continue;

				}

				// output dates

				if (

					classEqualSafe (
						consoleHelper.objectClass (),
						resultsClass)

					&& consoleHelper.event ()

				) {

					Instant rowTimestamp =
						(Instant)
						PropertyUtils.propertyGetAuto (
							result,
							consoleHelper.timestampFieldName ());

					LocalDate rowDate =
						rowTimestamp.toDateTime ().toLocalDate ();

					if (
						localDateNotEqual (
							currentDate,
							rowDate)
					) {

						currentDate =
							rowDate;

						htmlTableRowSeparatorWrite (
							formatWriter);

						htmlTableRowOpen (
							formatWriter,
							htmlStyleAttribute (
								htmlStyleRuleEntry (
									"font-weight",
									"bold")));

						htmlTableCellWrite (
							formatWriter,
							userConsoleLogic.dateStringLong (
								transaction,
								rowTimestamp),
							htmlColumnSpanAttribute (
								form.columnFields ().columns ()));

						htmlTableRowClose (
							formatWriter);

					}

				}

				// output separator if there are row fields

				if (
					iterableIsNotEmpty (
						form.rowFields ().formFields ())
				) {

					htmlTableRowSeparatorWrite (
						formatWriter);

				}

				// output column fields

				if (result instanceof Record) {

					htmlTableRowOpen (
						formatWriter,

						htmlClassAttribute (
							presentInstances (

							Optional.of (
								"magic-table-row"),

							Optional.of (
								stringFormat (
									"search-result-%s",
									integerToDecimalString (
										result.getId ()))),

							optionalIf (
								result == currentObject,
								() -> "selected"),

							getListClass (
								transaction,
								result)

						)),

						htmlDataAttribute (
							"rows-class",
							stringFormat (
								"search-result-%s",
								integerToDecimalString (
									result.getId ()))),

						htmlDataAttribute (
							"target-href",
							objectUrl (
								transaction,
								genericCastUnchecked (
									result)))

					);

				} else {

					htmlTableRowOpen (
						formatWriter);

				}

				form.outputTableCellsList (
					transaction,
					formatWriter,
					result,
					false);

				htmlTableRowClose (
					formatWriter);

				// output row fields

				if (
					isNotNull (
						form.rowFields ())
				) {

					for (
						FormField <ResultType, ?, ?, ?> rowField
							: form.rowFields ().formFields ()
					) {

						htmlTableRowOpen (
							formatWriter,
							presentInstances (

							optionalOf (
								htmlClassAttribute (
									presentInstances (

								Optional.of (
									"magic-table-row"),

								Optional.of (
									stringFormat (
										"search-result-%s",
										integerToDecimalString (
											result.getId ()))),

								optionalIf (
									result == currentObject,
									() -> "selected"),

								getListClass (
									transaction,
									result)

							))),

							optionalOf (
								htmlDataAttributeFormat (
									"rows-class",
									"search-result-%s",
									integerToDecimalString (
										result.getId ()))),

							optionalIf (
								result instanceof Record,
								() -> htmlDataAttribute (
									"target-href",
									objectUrl (
										transaction,
										genericCastUnchecked (
											result))))

						));

						try {

							rowField.renderTableCellList (
								transaction,
								formatWriter,
								form,
								result,
								false,
								form.columnFields ().columns ());

						} catch (Exception exception) {

							throw new RuntimeException (
								stringFormat (
									"Error rendering field %s for %s",
									rowField.name (),
									objectToString (
										result)),
								exception);

						}

						htmlTableRowClose (
							formatWriter);

					}

				}

			}

			htmlTableClose (
				formatWriter);

		}

	}

	private
	String objectUrl (
			@NonNull Transaction parentTransaction,
			@NonNull Record <?> object) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"objectUrl");

		) {

			if (
				optionalIsPresent (
					targetContext)
			) {

				return requestContext.resolveContextUrl (
					stringFormat (
						"%s",
						targetContext.get ().pathPrefix (),
						"/%s",
						consoleHelper.getPathIdGeneric (
							transaction,
							object)));

			} else {

				return requestContext.resolveLocalUrl (
					consoleHelper.getDefaultLocalPathGeneric (
						transaction,
						object));

			}

		}

	}

	private
	Optional <String> getListClass (
			@NonNull Transaction parentTransaction,
			@NonNull IdObject object) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"getListClass");

		) {

			if (
				isNotInstanceOf (
					Record.class,
					object)
			) {
				return Optional.absent ();
			}

			ConsoleHooks <ObjectType> consoleHooks =
				consoleHelper.consoleHooks ();

			return consoleHooks.getListClass (
				transaction,
				genericCastUnchecked (
					object));

		}

	}

}
