package wbs.platform.core.console;

import static wbs.utils.collection.CollectionUtils.collectionIsEmpty;
import static wbs.utils.collection.IterableUtils.iterableFilterToList;
import static wbs.utils.etc.EnumUtils.enumName;
import static wbs.utils.etc.EnumUtils.enumNameHyphens;
import static wbs.utils.etc.OptionalUtils.optionalFromNullable;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsNotPresent;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.string.StringUtils.stringFormatLazy;
import static wbs.utils.time.TimeUtils.earlierThan;
import static wbs.web.utils.HtmlAttributeUtils.htmlClassAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlColumnSpanAttribute;
import static wbs.web.utils.HtmlBlockUtils.htmlHeadingTwoWrite;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphWriteFormat;
import static wbs.web.utils.HtmlFormUtils.htmlFormClose;
import static wbs.web.utils.HtmlFormUtils.htmlFormOpenPost;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellWriteHtml;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableHeaderRowWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenDetails;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowOpen;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowSeparatorWrite;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import lombok.NonNull;

import org.apache.commons.lang3.tuple.Pair;

import org.joda.time.Duration;
import org.joda.time.Instant;

import wbs.console.html.HtmlLink;
import wbs.console.part.AbstractPagePart;
import wbs.console.priv.UserPrivChecker;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.platform.deployment.console.ApiDeploymentConsoleHelper;
import wbs.platform.deployment.console.ConsoleDeploymentConsoleHelper;
import wbs.platform.deployment.console.DaemonDeploymentConsoleHelper;
import wbs.platform.deployment.model.ApiDeploymentRec;
import wbs.platform.deployment.model.ConsoleDeploymentRec;
import wbs.platform.deployment.model.DaemonDeploymentRec;
import wbs.platform.deployment.model.DeploymentState;

import wbs.utils.string.FormatWriter;

@PrototypeComponent ("coreSystemRestartPart")
public
class CoreSystemRestartPart
	extends AbstractPagePart {

	// singleton dependencies

	@SingletonDependency
	ApiDeploymentConsoleHelper apiDeploymentHelper;

	@SingletonDependency
	ConsoleDeploymentConsoleHelper consoleDeploymentHelper;

	@SingletonDependency
	DaemonDeploymentConsoleHelper daemonDeploymentHelper;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	UserPrivChecker userPrivChecker;

	// state

	List <ApiDeploymentRec> apiDeployments;
	List <ConsoleDeploymentRec> consoleDeployments;
	List <DaemonDeploymentRec> daemonDeployments;

	// details

	@Override
	public
	Set <HtmlLink> links () {

		return ImmutableSet.<HtmlLink> of (

			HtmlLink.applicationCssStyle (
				"/style/core-system-restart.css")

		);

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

			apiDeployments =
				iterableFilterToList (
					apiDeploymentHelper.findAllNotDeletedEntities (
						transaction),
					apiDeployment ->
						userPrivChecker.canRecursive (
							transaction,
							apiDeployment,
							"restart"));

			Collections.sort (
				apiDeployments);

			consoleDeployments =
				iterableFilterToList (
					consoleDeploymentHelper.findAllNotDeletedEntities (
						transaction),
					consoleDeployment ->
						userPrivChecker.canRecursive (
							transaction,
							consoleDeployment,
							"restart"));

			Collections.sort (
				consoleDeployments);

			daemonDeployments =
				iterableFilterToList (
					daemonDeploymentHelper.findAllNotDeletedEntities (
						transaction),
					consoleDeployment ->
						userPrivChecker.canRecursive (
							transaction,
							consoleDeployment,
							"restart"));

			Collections.sort (
				daemonDeployments);

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

			htmlHeadingTwoWrite (
				formatWriter,
				"Restart server components");

			htmlParagraphWriteFormat (
				formatWriter,
				"Use these controls to restart server compoments. This may be ",
				"necessary from time to time if they encounter certain types ",
				"of problem.");

			htmlFormOpenPost (
				formatWriter);

			htmlTableOpenDetails (
				formatWriter);

			htmlTableHeaderRowWrite (
				formatWriter,
				"Name",
				"Description",
				"Status",
				"Restart");

			htmlTableRowSeparatorWrite (
				formatWriter);

			renderApiDeployments (
				transaction,
				formatWriter);

			htmlTableRowSeparatorWrite (
				formatWriter);

			renderDaemonDeployments (
				transaction,
				formatWriter);

			htmlTableRowSeparatorWrite (
				formatWriter);

			renderConsoleDeployments (
				transaction,
				formatWriter);

			htmlTableClose (
				formatWriter);

			htmlFormClose (
				formatWriter);

		}

	}

	private
	void renderApiDeployments (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderApiDeployments");

		) {

			if (
				collectionIsEmpty (
					apiDeployments)
			) {

				htmlTableRowOpen (
					formatWriter);

				htmlTableCellWrite (
					formatWriter,
					stringFormatLazy (
						"(you do not have permission to restart any API ",
						"deployments)"),
					htmlColumnSpanAttribute (
						4l));

				htmlTableRowClose (
					formatWriter);

			}

			for (
				ApiDeploymentRec apiDeployment
					: apiDeployments
			) {

				htmlTableRowOpen (
					formatWriter);

				htmlTableCellWrite (
					formatWriter,
					apiDeployment.getName ());

				htmlTableCellWrite (
					formatWriter,
					apiDeployment.getDescription ());

				writeDeploymentState (
					formatWriter,
					optionalFromNullable (
						apiDeployment.getState ()),
					optionalFromNullable (
						apiDeployment.getStateTimestamp ()));

				htmlTableCellWriteHtml (
					formatWriter,
					() -> formatWriter.writeLineFormat (
						"<input",
						" type=\"submit\"",
						" name=\"api/%h\"",
						apiDeployment.getCode (),
						" value=\"restart\"",
						">"));

				htmlTableRowClose (
					formatWriter);

			}

		}

	}

	private
	void renderConsoleDeployments (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderConsoleDeployments");

		) {

			if (
				collectionIsEmpty (
					consoleDeployments)
			) {

				htmlTableRowOpen (
					formatWriter);

				htmlTableCellWrite (
					formatWriter,
					stringFormatLazy (
						"(you do not have permission to restart any console ",
						"deployments)"),
					htmlColumnSpanAttribute (
						4l));

				htmlTableRowClose (
					formatWriter);

			}

			for (
				ConsoleDeploymentRec consoleDeployment
					: consoleDeployments
			) {

				htmlTableRowOpen (
					formatWriter);

				htmlTableCellWrite (
					formatWriter,
					consoleDeployment.getName ());

				htmlTableCellWrite (
					formatWriter,
					consoleDeployment.getDescription ());

				writeDeploymentState (
					formatWriter,
					optionalFromNullable (
						consoleDeployment.getState ()),
					optionalFromNullable (
						consoleDeployment.getStateTimestamp ()));

				htmlTableCellWriteHtml (
					formatWriter,
					() -> formatWriter.writeLineFormat (
						"<input",
						" type=\"submit\"",
						" name=\"console/%h\"",
						consoleDeployment.getCode (),
						" value=\"restart\"",
						">"));

				htmlTableRowClose (
					formatWriter);

			}

		}

	}

	private
	void renderDaemonDeployments (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderDaemonDeployments");

		) {

			if (
				collectionIsEmpty (
					daemonDeployments)
			) {

				htmlTableRowOpen (
					formatWriter);

				htmlTableCellWrite (
					formatWriter,
					stringFormatLazy (
						"(you do not have permission to restart any daemon ",
						"deployments)"),
					htmlColumnSpanAttribute (
						4l));

				htmlTableRowClose (
					formatWriter);

			}

			for (
				DaemonDeploymentRec daemonDeployment
					: daemonDeployments
			) {

				htmlTableRowOpen (
					formatWriter);

				htmlTableCellWrite (
					formatWriter,
					daemonDeployment.getName ());

				htmlTableCellWrite (
					formatWriter,
					daemonDeployment.getDescription ());

				writeDeploymentState (
					formatWriter,
					optionalFromNullable (
						daemonDeployment.getState ()),
					optionalFromNullable (
						daemonDeployment.getStateTimestamp ()));

				htmlTableCellWriteHtml (
					formatWriter,
					() -> formatWriter.writeLineFormat (
						"<input",
						" type=\"submit\"",
						" name=\"daemon/%h\"",
						daemonDeployment.getCode (),
						" value=\"restart\"",
						">"));

				htmlTableRowClose (
					formatWriter);

			}

		}

	}

	private
	void writeDeploymentState (
			@NonNull FormatWriter formatWriter,
			@NonNull Optional <DeploymentState> deploymentState,
			@NonNull Optional <Instant> deploymentStateTimestamp) {

		Pair <String, String> deploymentStatePair =
			deploymentStateClassAndLabel (
				deploymentState,
				deploymentStateTimestamp);

		htmlTableCellWrite (
			formatWriter,
			deploymentStatePair.getLeft (),
			htmlClassAttribute (
				deploymentStatePair.getRight ()));

	}

	private
	Pair <String, String> deploymentStateClassAndLabel (
			@NonNull Optional <DeploymentState> deploymentState,
			@NonNull Optional <Instant> deploymentStateTimestamp) {

		if (

			optionalIsNotPresent (
				deploymentState)

			|| optionalIsNotPresent (
				deploymentStateTimestamp)

			|| earlierThan (
				optionalGetRequired (
					deploymentStateTimestamp),
				Instant.now ().minus (
					Duration.standardSeconds (
						5l)))

		) {

			return Pair.of (
				"—",
				"core-system-restart-state-unknown");

		}

		return Pair.of (
			enumName (
				optionalGetRequired (
					deploymentState)),
			stringFormat (
				"core-system-restart-state-%s",
				enumNameHyphens (
					optionalGetRequired (
						deploymentState))));


	}

}
