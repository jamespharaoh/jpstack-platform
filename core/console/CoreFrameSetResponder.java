package wbs.platform.core.console;

import static wbs.utils.etc.Misc.doNothing;
import static wbs.web.utils.HtmlScriptUtils.htmlScriptBlockClose;
import static wbs.web.utils.HtmlScriptUtils.htmlScriptBlockOpen;
import static wbs.web.utils.HttpTimeUtils.httpTimestampString;

import lombok.NonNull;

import org.joda.time.Instant;

import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.config.WbsConfig;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.utils.string.FormatWriter;
import wbs.web.responder.BufferedTextResponder;

@PrototypeComponent ("coreFrameSetResponder")
public
class CoreFrameSetResponder
	extends BufferedTextResponder {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	WbsConfig wbsConfig;

	// implementation

	@Override
	protected
	void prepare (
			@NonNull Transaction parentTransaction) {

		doNothing ();

	}

	@Override
	public
	void render (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"render");

		) {

			goDocType (
				transaction,
				formatWriter);

			goHtml (
				transaction,
				formatWriter);

		}

	}

	@Override
	public
	void headers (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"setHtmlHeaders");

		) {

			requestContext.contentType (
				"text/html",
				"utf-8");

			requestContext.setHeader (
				"Cache-Control",
				"no-cache");

			requestContext.setHeader (
				"Expiry",
				httpTimestampString (
					Instant.now ()));

		}

	}

	public
	void goDocType (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"goDocType");

		) {

			formatWriter.writeLineFormat (
				"<!DOCTYPE html>");

		}

	}

	public
	void goHtml (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"goHtml");

		) {

			formatWriter.writeLineFormat (
				"<html>");

			goHtmlStuff (
				transaction,
				formatWriter);

			formatWriter.writeLineFormat (
				"</html>");

		}

	}

	public
	void goHtmlStuff (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"goHtmlStuff");

		) {

			goHead (
				transaction,
				formatWriter);

			goFrameset (
				transaction,
				formatWriter);

		}

	}

	public
	void goHead (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"goHead");

		) {

			formatWriter.writeLineFormatIncreaseIndent (
				"<head>");

			goHeadStuff (
				transaction,
				formatWriter);

			formatWriter.writeLineFormatDecreaseIndent (
				"</head>");

		}

	}

	public
	void goHeadStuff (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"goHeadStuff");

		) {

			goTitle (
				transaction,
				formatWriter);

			goScripts (
				transaction,
				formatWriter);

			formatWriter.writeLineFormat (
				"<link",
				" rel=\"shortcut icon\"",
				" href=\"%h\"",
				requestContext.resolveApplicationUrl (
					"/favicon.ico"),
				">");

			formatWriter.writeLineFormat (
				"<link",
				" rel=\"icon\"",
				" href=\"%h\"",
				requestContext.resolveApplicationUrl (
					"/favicon.ico"),
				">");

		}

	}

	public
	void goTitle (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"goTitle");

		) {

			formatWriter.writeLineFormatIncreaseIndent (
				"<title>");

			formatWriter.writeLineFormat (
				"%h",
				wbsConfig.consoleTitle ());

			formatWriter.writeLineFormatDecreaseIndent (
				"</title>");

		}

	}

	public
	void goScripts (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"goScripts");

		) {

			htmlScriptBlockOpen (
				formatWriter);

			formatWriter.writeLineFormatIncreaseIndent (
				"function show_inbox (show) {");

			formatWriter.writeLineFormat (
				"document.getElementById ('right_frameset').rows =");

			formatWriter.writeLineFormat (
				"  show ? '2*,1*' : '*,0';");

			formatWriter.writeLineFormatDecreaseIndent (
				"}");

			htmlScriptBlockClose (
				formatWriter);

		}

	}

	public
	void goFrameset (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"goFrameset");

		) {

			formatWriter.writeLineFormatIncreaseIndent (
				"<frameset cols=\"1*,4*\">");

			formatWriter.writeLineFormatIncreaseIndent (
				"<frameset rows=\"2*,1*\">");

			formatWriter.writeLineFormat (
				"<frame name=\"sidebar\" src=\"%h\">",
				requestContext.resolveApplicationUrl (
					"/sidebar"));

			formatWriter.writeLineFormat (
				"<frame name=\"status\" src=\"%h\">",
				requestContext.resolveApplicationUrl (
					"/status"));

			formatWriter.writeLineFormatDecreaseIndent (
				"</frameset>");

			formatWriter.writeLineFormat (
				"<frameset rows=\"1*,0\" id=\"right_frameset\">");

			formatWriter.writeLineFormatIncreaseIndent (
				"<frame name=\"main\" src=\"%h\">",
				requestContext.resolveApplicationUrl (
					"/home"));

			formatWriter.writeLineFormat (
				"<frame name=\"inbox\" src=\"%h\">",
				requestContext.resolveApplicationUrl (
					"/queues/queue.home"));

			formatWriter.writeLineFormatDecreaseIndent (
				"</frameset>");

			formatWriter.writeLineFormatDecreaseIndent (
				"</frameset>");

		}

	}

}
