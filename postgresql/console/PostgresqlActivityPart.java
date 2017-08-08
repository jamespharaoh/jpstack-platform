package wbs.platform.postgresql.console;

import static wbs.utils.collection.CollectionUtils.collectionIsEmpty;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.string.StringUtils.stringEqualSafe;
import static wbs.web.utils.HtmlBlockUtils.htmlHeadingTwoWrite;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableHeaderRowWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenList;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowOpen;

import java.util.ArrayList;
import java.util.List;

import lombok.NonNull;

import wbs.console.part.AbstractPagePart;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.platform.postgresql.model.PostgresqlStatActivityObjectHelper;
import wbs.platform.postgresql.model.PostgresqlStatActivityRec;

import wbs.utils.string.FormatWriter;

@PrototypeComponent ("postgresqlActivityPart")
public
class PostgresqlActivityPart
	extends AbstractPagePart {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	PostgresqlStatActivityObjectHelper postgresqlStatActivityHelper;

	// state

	List <PostgresqlStatActivityRec> activeStatActivities;

	List <PostgresqlStatActivityRec> idleStatActivities;

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

			List <PostgresqlStatActivityRec> allStatActivities =
				postgresqlStatActivityHelper.findAll (
					transaction);

			activeStatActivities =
				new ArrayList<PostgresqlStatActivityRec> ();

			idleStatActivities =
				new ArrayList<PostgresqlStatActivityRec> ();

			for (
				PostgresqlStatActivityRec statActivity
					: allStatActivities
			) {

				if (
					stringEqualSafe (
						statActivity.getCurrentQuery (),
						"<IDLE>")
				) {

					idleStatActivities.add (
						statActivity);

				} else {

					activeStatActivities.add (
						statActivity);

				}

			}

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

			doList (
				transaction,
				formatWriter,
				activeStatActivities);

			htmlHeadingTwoWrite (
				formatWriter,
				"Idle");

			doList (
				transaction,
				formatWriter,
				idleStatActivities);

		}

	}

	void doList (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter,
			@NonNull List <PostgresqlStatActivityRec> statActivities) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"doList");

		) {

			if (
				collectionIsEmpty (
					statActivities)
			) {

				htmlParagraphWrite (
					formatWriter,
					"(none)");

				return;

			}

			htmlTableOpenList (
				formatWriter);

			htmlTableHeaderRowWrite (
				formatWriter,
				"PID",
				"Database",
				"User",
				"Query");

			for (
				PostgresqlStatActivityRec statActivity
					: statActivities
			) {

				htmlTableRowOpen (
					formatWriter);

				htmlTableCellWrite (
					formatWriter,
					integerToDecimalString (
						statActivity.getId ()));

				htmlTableCellWrite (
					formatWriter,
					statActivity.getDatabaseName ());

				htmlTableCellWrite (
					formatWriter,
					statActivity.getUserName ());

				htmlTableCellWrite (
					formatWriter,
					statActivity.getCurrentQuery ());

				htmlTableRowClose (
					formatWriter);

			}

			htmlTableClose (
				formatWriter);

		}

	}

}
