package wbs.platform.object.summary;

import static wbs.utils.collection.CollectionUtils.collectionIsEmpty;
import static wbs.utils.etc.DebugUtils.debugFormat;
import static wbs.utils.etc.LogicUtils.referenceEqualWithClass;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.string.StringUtils.capitalise;
import static wbs.web.utils.HtmlAttributeUtils.htmlColumnSpanAttribute;
import static wbs.web.utils.HtmlBlockUtils.htmlHeadingThreeWrite;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphWriteFormat;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableHeaderCellWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenList;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowOpen;

import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.helper.core.ConsoleHelper;
import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.part.AbstractPagePart;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;

import wbs.utils.data.Pair;
import wbs.utils.string.FormatWriter;

@Accessors (fluent = true)
@PrototypeComponent ("objectSummaryErrorsPart")
public
class ObjectSummaryErrorsPart <
	ObjectType extends Record <ObjectType>,
	ParentType extends Record <ParentType>
>
	extends AbstractPagePart {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	UserPrivChecker privChecker;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	// properties

	@Getter @Setter
	ConsoleHelper <ObjectType> consoleHelper;

	// state

	ObjectType object;

	List <Pair <Record <?>, String>> errors;

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

			object =
				consoleHelper.lookupObject (
					transaction,
					requestContext.consoleContextStuffRequired ());

debugFormat ("CALL VERIFY");

			errors =
				consoleHelper.hooks ().verifyData (
					transaction,
					object,
					true,
					false);

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

			if (
				collectionIsEmpty (
					errors)
			) {
				return;
			}

			htmlHeadingThreeWrite (
				formatWriter,
				"Validation errors");

			htmlParagraphWriteFormat (
				formatWriter,
				"This object's data is not valid. Following is a list of ",
				"errors which must be resolved.");

			htmlTableOpenList (
				formatWriter);

			htmlTableRowOpen (
				formatWriter);

			htmlTableHeaderCellWrite (
				formatWriter,
				"Object",
				htmlColumnSpanAttribute (2l));

			htmlTableHeaderCellWrite (
				formatWriter,
				"Error");

			htmlTableRowClose (
				formatWriter);

			for (
				Pair <Record <?>, String> error
					: errors
			) {

				Record <?> errorObject =
					error.left ();

				ConsoleHelper <?> errorObjectHelper =
					objectManager.consoleHelperForObjectRequired (
						genericCastUnchecked (
							errorObject));

				String errorMessage =
					error.right ();

				htmlTableRowOpen (
					formatWriter);

				htmlTableCellWrite (
					formatWriter,
					capitalise (
						errorObjectHelper.friendlyNameSingular ()));

				if (
					referenceEqualWithClass (
						Record.class,
						errorObject,
						object)
				) {

					htmlTableCellWrite (
						formatWriter,
						"—");

				} else {

					objectManager.writeTdForObjectLink (
						transaction,
						formatWriter,
						privChecker,
						errorObject,
						object);

				}

				htmlTableCellWrite (
					formatWriter,
					errorMessage);

				htmlTableRowClose (
					formatWriter);

			}

			htmlTableClose (
				formatWriter);

		}

	}

}
