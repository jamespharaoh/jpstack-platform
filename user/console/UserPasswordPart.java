package wbs.platform.user.console;

import static wbs.web.utils.HtmlBlockUtils.htmlParagraphClose;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphOpen;
import static wbs.web.utils.HtmlFormUtils.htmlFormClose;
import static wbs.web.utils.HtmlFormUtils.htmlFormOpenPostAction;

import lombok.NonNull;

import wbs.console.part.AbstractPagePart;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.utils.string.FormatWriter;

@PrototypeComponent ("userPasswordPart")
public
class UserPasswordPart
	extends AbstractPagePart {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	// implementation

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

			// form open

			htmlFormOpenPostAction (
				formatWriter,
				requestContext.resolveLocalUrl (
					"/user.password"));

			// password

			htmlParagraphOpen (
				formatWriter);

			formatWriter.writeLineFormat (
				"Enter new password<br>");

			formatWriter.writeLineFormat (
				"<input",
				" type=\"password\"",
				" name=\"password_1\"",
				" size=\"32\"",
				">");

			htmlParagraphClose (
				formatWriter);

			// password confirmation

			htmlParagraphOpen (
				formatWriter);

			formatWriter.writeLineFormat (
				"Enter again to confirm<br>");

			formatWriter.writeLineFormat (
				"<input",
				" type=\"password\"",
				" name=\"password_2\"",
				" size=\"32\"",
				">");

			htmlParagraphClose (
				formatWriter);

			// form controls

			htmlParagraphOpen (
				formatWriter);

			formatWriter.writeFormat (
				"<input",
				" type=\"submit\"",
				" value=\"save changes\"",
				">");

			htmlParagraphClose (
				formatWriter);

			// form close

			htmlFormClose (
				formatWriter);

		}

	}

}
