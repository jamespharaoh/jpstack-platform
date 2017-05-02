package wbs.apn.chat.infosite.api;

import static wbs.web.utils.HtmlBlockUtils.htmlParagraphWrite;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.web.context.RequestContext;
import wbs.web.responder.PrintResponder;

@PrototypeComponent ("chatInfoSiteMessageSentResponder")
public
class ChatInfoSiteMessageSentResponder
	extends PrintResponder {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	RequestContext requestContext;

	// implementation

	@Override
	protected
	void goHeaders (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"goHeaders");

		) {

			requestContext.setHeader (
				"Content-Type",
				"text/html");

		}

	}

	@Override
	protected
	void goContent (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"goContent");

		) {

			htmlParagraphWrite (
				"Your message has been sent.");

		}

	}

}
