package wbs.platform.core.console;

import static wbs.utils.etc.OptionalUtils.optionalOrEmptyString;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.web.utils.HtmlBlockUtils.htmlHeadingOneWrite;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphWriteFormat;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphWriteHtml;

import java.util.Set;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.html.ScriptRef;
import wbs.console.part.PagePart;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;
import wbs.console.responder.ConsoleHtmlResponder;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.GlobalId;
import wbs.framework.exception.ExceptionLogger;
import wbs.framework.exception.ExceptionUtils;
import wbs.framework.exception.GenericExceptionResolution;
import wbs.framework.logging.LogContext;

import wbs.platform.user.console.UserConsoleLogic;

import wbs.utils.string.FormatWriter;

@Accessors (fluent = true)
@PrototypeComponent ("coreTitledResponder")
public
class CoreTitledResponder
	extends ConsoleHtmlResponder {

	// singleton dependencies

	@SingletonDependency
	ExceptionLogger exceptionLogger;

	@SingletonDependency
	ExceptionUtils exceptionLogic;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	UserPrivChecker privChecker;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	// properties

	@Getter @Setter
	String title;

	@Getter @Setter
	PagePart pagePart;

	// state

	Throwable pagePartThrew;

	// details

	@Override
	protected
	Set <ScriptRef> scriptRefs () {
		return pagePart.scriptRefs ();
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

			if (pagePart != null) {

				try {

					pagePart.prepare (
						transaction);

				} catch (RuntimeException exception) {

					// record the exception

					String path =
						stringFormat (
							"%s%s",
							requestContext.servletPath (),
							optionalOrEmptyString (
								requestContext.pathInfo ()));

					exceptionLogger.logThrowable (
						transaction,
						"console",
						path,
						exception,
						userConsoleLogic.userId (),
						GenericExceptionResolution.ignoreWithUserWarning);

					// and remember we had a problem

					pagePartThrew =
						exception;

					requestContext.addError (
						"Internal error");

				}

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

			formatWriter.writeLineFormat (
				"<link",
				" rel=\"stylesheet\"",
				" href=\"%h\"",
				requestContext.resolveApplicationUrl (
					"/style/basic.css"),
				">");

			pagePart.renderHtmlHeadContent (
				transaction,
				formatWriter);

		}

	}

	protected
	void goTab () {
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

			htmlHeadingOneWrite (
				formatWriter,
				title);

			requestContext.flushNotices (
				formatWriter);

			if (pagePartThrew != null) {

				htmlParagraphWriteFormat (
					formatWriter,
					"Unable to show page contents.");

				if (
					privChecker.canRecursive (
						transaction,
						GlobalId.root,
						"debug")
				) {

					htmlParagraphWriteHtml (
						formatWriter,
						stringFormat (
							"<pre>%h</pre>",
							exceptionLogic.throwableDump (
								transaction,
								pagePartThrew)));

				}

			} else if (pagePart != null) {

				pagePart.renderHtmlBodyContent (
					transaction,
					formatWriter);

			}

		}

	}

}
