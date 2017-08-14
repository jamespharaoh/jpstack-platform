package wbs.platform.object.verify.console;

import static wbs.utils.etc.LogicUtils.booleanToString;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.string.StringUtils.capitalise;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.web.utils.HtmlAttributeUtils.htmlNameAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlValueAttribute;
import static wbs.web.utils.HtmlBlockUtils.htmlHeadingTwoWriteFormat;
import static wbs.web.utils.HtmlFormUtils.htmlFormClose;
import static wbs.web.utils.HtmlFormUtils.htmlFormOpenPostAction;
import static wbs.web.utils.HtmlInputUtils.htmlInputSubmitWrite;
import static wbs.web.utils.HtmlScriptUtils.htmlScriptBlockWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableDetailsRowWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableDetailsRowWriteHtml;
import static wbs.web.utils.HtmlTableUtils.htmlTableDetailsRowWriteRaw;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenDetails;

import lombok.NonNull;

import wbs.console.helper.core.ConsoleHelper;
import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;
import wbs.console.responder.ConsoleHtmlResponder;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;

import wbs.platform.object.core.model.ObjectTypeObjectHelper;
import wbs.platform.object.core.model.ObjectTypeRec;
import wbs.platform.object.verify.model.ObjectVerificationRec;
import wbs.platform.user.console.UserConsoleLogic;

import wbs.utils.string.FormatWriter;
import wbs.utils.time.core.DefaultTimeFormatter;

@PrototypeComponent ("objectVerificationPendingFormResponder")
public
class ObjectVerificationPendingFormResponder <
	ObjectType extends Record <ObjectType>
>
	extends ConsoleHtmlResponder {

	// singleton dependencies

	@SingletonDependency
	DefaultTimeFormatter defaulTimeFormatter;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	ObjectTypeObjectHelper objectTypeHelper;

	@SingletonDependency
	UserPrivChecker privChecker;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	@SingletonDependency
	ObjectVerificationConsoleHelper verificationHelper;

	// state

	ObjectVerificationRec verification;
	ObjectType object;
	ObjectTypeRec objectType;

	ConsoleHelper <ObjectType> consoleHelper;

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

			verification =
				verificationHelper.findFromContextRequired (
					transaction);

			object =
				genericCastUnchecked (
					objectManager.getParentRequired (
						transaction,
						verification));

			consoleHelper =
				objectManager.consoleHelperForObjectRequired (
					object);

			objectType =
				objectTypeHelper.findRequired (
					transaction,
					consoleHelper.objectTypeId ());

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

			htmlScriptBlockWrite (
				formatWriter,
				"top.show_inbox (true);",
				stringFormat (
					"top.frames ['main'].location = '%j';",
					requestContext.resolveApplicationUrlFormat (
						consoleHelper.getDefaultContextPath (
							transaction,
							object))));

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

			htmlHeadingTwoWriteFormat (
				formatWriter,
				"%s data verification",
				capitalise (
					consoleHelper.friendlyNameSingular ()));

			requestContext.flushNotices (
				formatWriter);

			htmlFormOpenPostAction (
				formatWriter,
				requestContext.resolveLocalUrl (
					"/objectVerification.pendingForm"));

			htmlTableOpenDetails (
				formatWriter);

			htmlTableDetailsRowWrite (
				formatWriter,
				"Object type",
				consoleHelper.friendlyNameSingular ());

			htmlTableDetailsRowWriteRaw (
				formatWriter,
				"Object",
				() -> objectManager.writeTdForObject (
					transaction,
					formatWriter,
					privChecker,
					object));

			htmlTableDetailsRowWriteHtml (
				formatWriter,
				"Last run",
				defaulTimeFormatter.timestampTimezoneSecondShortString (
					userConsoleLogic.timezone (
						transaction),
					verification.getLastRun ()));

			htmlTableDetailsRowWrite (
				formatWriter,
				"Result",
				booleanToString (
					verification.getValid (),
					"Passed",
					"Failed"));

			htmlTableDetailsRowWriteHtml (
				formatWriter,
				"Actions",
				() -> {

				if (verification.getValid ()) {

					htmlInputSubmitWrite (
						formatWriter,
						htmlNameAttribute (
							"verify"),
						htmlValueAttribute (
							"confirm"));

				} else {

					htmlInputSubmitWrite (
						formatWriter,
						htmlNameAttribute (
							"verify"),
						htmlValueAttribute (
							"verify"));

					htmlInputSubmitWrite (
						formatWriter,
						htmlNameAttribute (
							"skip"),
						htmlValueAttribute (
							"skip"));

				}

			});

			htmlTableClose (
				formatWriter);

			htmlFormClose (
				formatWriter);

		}

	}

}
