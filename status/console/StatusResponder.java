package wbs.platform.status.console;

import static wbs.web.utils.HtmlAttributeUtils.htmlAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlIdAttribute;
import static wbs.web.utils.HtmlFormUtils.htmlFormClose;
import static wbs.web.utils.HtmlFormUtils.htmlFormOpenPostAction;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellOpen;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableHeaderCellWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenList;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowOpen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import lombok.NonNull;

import wbs.console.context.ConsoleApplicationScriptRef;
import wbs.console.html.HtmlLink;
import wbs.console.html.ScriptRef;
import wbs.console.misc.JqueryScriptRef;
import wbs.console.part.PagePart;
import wbs.console.request.ConsoleRequestContext;
import wbs.console.responder.ConsoleHtmlResponder;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.utils.string.FormatWriter;

@PrototypeComponent ("statusResponder")
public
class StatusResponder
	extends ConsoleHtmlResponder {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	StatusLineManager statusLineManager;

	// state

	List <PagePart> pageParts =
		new ArrayList<> ();

	// details

	@Override
	protected
	Set <ScriptRef> myScriptRefs () {

		return ImmutableSet.<ScriptRef> builder ()

			.add (
				JqueryScriptRef.instance)

			.add (
				ConsoleApplicationScriptRef.javascript (
					"/js/js-cookie-2.0.4.js"))

			.add (
				ConsoleApplicationScriptRef.javascript (
					"/js/async.js"))

			.add (
				ConsoleApplicationScriptRef.javascript (
					"/js/status.js"))

			.addAll (
				pageParts.stream ()

				.map (
					PagePart::scriptRefs)

				.flatMap (
					Set::stream)

				.iterator ())

			.build ();

	}

	@Override
	protected
	Set <HtmlLink> myHtmlLinks () {

		return ImmutableSet.<HtmlLink> of (

			HtmlLink.applicationCssStyle (
				"/style/status.css")

		);

	}

	// implementation

	@Override
	protected
	void prepare (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"prepare");

		) {

			for (
				StatusLine statusLine
					: statusLineManager.getStatusLines ()
			) {

				PagePart pagePart =
					statusLine.createPagePart (
						transaction);

				pagePart.prepare (
					transaction);

				pageParts.add (
					pagePart);

			}

		}

	}

	@Override
	protected
	void renderHtmlHeadContents (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderHtmlHeadContents");

		) {

			super.renderHtmlHeadContents (
				transaction,
				formatWriter);

			pageParts.forEach (
				pagePart ->
					pagePart.renderHtmlHeadContent (
						transaction,
						formatWriter));

		}

	}

	@Override
	protected
	void renderHtmlBody (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderHtmlBody");

		) {

			formatWriter.writeLineFormatIncreaseIndent (
				"<body>");

			renderHtmlBodyContents (
				transaction,
				formatWriter);

			formatWriter.writeLineFormatDecreaseIndent (
				"</body>");

		}

	}

	@Override
	protected
	void renderHtmlBodyContents (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderHtmlBodyContents");

		) {

			// table open

			htmlTableOpenList (
				formatWriter,

				htmlIdAttribute (
					"statusTable"),

				htmlAttribute (
					"width",
					"100%")

			);

			// heading row

			htmlTableRowOpen (
				formatWriter);

			htmlTableHeaderCellWrite (
				formatWriter,
				"Status",
				htmlIdAttribute (
					"headerCell"));

			htmlTableRowClose (
				formatWriter);

			// loading row

			htmlTableRowOpen (
				formatWriter,
				htmlIdAttribute (
					"loadingRow"));

			htmlTableCellWrite (
				formatWriter,
				"Loading...",
				htmlIdAttribute (
					"loadingCell"));

			htmlTableRowClose (
				formatWriter);

			// notice row

			htmlTableRowOpen (
				formatWriter,
				htmlIdAttribute (
					"noticeRow"));

			htmlTableCellWrite (
				formatWriter,
				"—",
				htmlIdAttribute (
					"noticeCell"));

			htmlTableRowClose (
				formatWriter);

			// time row

			htmlTableRowOpen (
				formatWriter,
				htmlIdAttribute (
					"timeRow"));

			htmlTableCellWrite (
				formatWriter,
				"—",
				htmlIdAttribute (
					"timeCell"));

			htmlTableRowClose (
				formatWriter);

			// parts

			pageParts.forEach (
				pagePart ->
					pagePart.renderHtmlBodyContent (
						transaction,
						formatWriter));

			// log out row

			htmlTableRowOpen (
				formatWriter);

			htmlTableCellOpen (
				formatWriter);

			htmlFormOpenPostAction (
				formatWriter,
				"logoff");

			formatWriter.writeLineFormat (
				"<input",
				" type=\"submit\"",
				" value=\"log out\"",
				">");

			htmlFormClose (
				formatWriter);

			htmlTableCellClose (
				formatWriter);

			htmlTableRowClose (
				formatWriter);

			// table close

			htmlTableClose (
				formatWriter);

		}

	}

}
