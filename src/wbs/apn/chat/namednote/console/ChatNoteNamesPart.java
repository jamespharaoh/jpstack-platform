package wbs.apn.chat.namednote.console;

import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphClose;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphOpen;
import static wbs.web.utils.HtmlFormUtils.htmlFormClose;
import static wbs.web.utils.HtmlFormUtils.htmlFormOpenPostAction;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellOpen;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableHeaderRowWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenList;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowOpen;

import java.util.List;

import lombok.NonNull;

import wbs.console.part.AbstractPagePart;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.apn.chat.core.console.ChatConsoleHelper;
import wbs.apn.chat.core.model.ChatRec;
import wbs.apn.chat.namednote.model.ChatNoteNameRec;

@PrototypeComponent ("chatNoteNamesPart")
public
class ChatNoteNamesPart
	extends AbstractPagePart {

	// singleton dependencies

	@SingletonDependency
	ChatConsoleHelper chatHelper;

	@SingletonDependency
	ChatNoteNameConsoleHelper chatNoteNameHelper;

	@ClassSingletonDependency
	LogContext logContext;

	// state

	List <ChatNoteNameRec> noteNames;

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

			ChatRec chat =
				chatHelper.findFromContextRequired (
					transaction);

			noteNames =
				chatNoteNameHelper.findNotDeleted (
					transaction,
					chat);

		}

	}

	@Override
	public
	void renderHtmlBodyContent (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderHtmlBodyContent");

		) {

			// form open

			htmlFormOpenPostAction (
				"chat.settings.noteNames");

			// form controls

			htmlParagraphOpen ();

			formatWriter.writeFormat (
				"<input",
				" type=\"submit\"",
				" name=\"saveChanges\"",
				" value=\"save changes\"",
				">");

			htmlParagraphClose ();

			// table open

			htmlTableOpenList ();

			htmlTableHeaderRowWrite (
				"Name",
				"Options");

			// table contents

			for (
				ChatNoteNameRec noteName
					: noteNames
			) {

				htmlTableRowOpen ();

				htmlTableCellOpen ();

				formatWriter.writeLineFormat (
					"<input",
					" type=\"text\"",
					" name=\"noteName%s\"",
					integerToDecimalString (
						noteName.getId ()),
					" value=\"%h\"",
					requestContext.formOrDefault (
						stringFormat (
							"noteName%s",
							integerToDecimalString (
								noteName.getId ())),
						noteName.getName ()),
					">");

				htmlTableCellClose ();

				htmlTableCellOpen ();

				formatWriter.writeLineFormat (
					"<input",
					" type=\"submit\"",
					" name=\"noteMoveUp%s\"",
					integerToDecimalString (
						noteName.getId ()),
					" value=\"&uarr;\"",
					">");

				formatWriter.writeLineFormat (
					"<input",
					" type=\"submit\"",
					" name=\"noteMoveDown%s\"",
					integerToDecimalString (
						noteName.getId ()),
					" value=\"&darr;\"",
					">");

				formatWriter.writeLineFormat (
					"<input",
					" type=\"submit\"",
					" name=\"noteDelete%s\"",
					integerToDecimalString (
						noteName.getId ()),
					" value=\"&times;\"",
					">");

				htmlTableCellClose ();

				htmlTableRowClose ();

			}

			// new note

			htmlTableRowOpen ();

			htmlTableCellOpen ();

			formatWriter.writeLineFormat (
				"<input",
				" type=\"text\"",
				" name=\"noteNameNew\"",
				" value=\"%h\"",
				requestContext.formOrEmptyString (
					"noteNameNew"),
				">");

			htmlTableCellClose ();

			htmlTableCellOpen ();

			formatWriter.writeLineFormat (
				"<input",
				" type=\"submit\"",
				" name=\"saveChanges\"",
				" value=\"add new\"",
				">");

			htmlTableCellClose ();

			htmlTableRowClose ();

			// table close

			htmlTableClose ();

			// form controls again

			htmlParagraphOpen ();

			formatWriter.writeLineFormat (
				"<input",
				" type=\"submit\"",
				" name=\"saveChanges\"",
				" value=\"save changes\"",
				">");

			htmlParagraphClose ();

			// form close

			htmlFormClose ();

		}

	}

}
