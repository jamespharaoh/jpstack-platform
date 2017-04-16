package wbs.imchat.api;

import static wbs.utils.etc.LogicUtils.not;
import static wbs.utils.etc.LogicUtils.referenceNotEqualWithClass;
import static wbs.utils.etc.Misc.isNull;
import static wbs.utils.etc.NumberUtils.parseIntegerRequired;

import javax.inject.Provider;

import lombok.NonNull;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.data.tools.DataFromJson;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.TaskLogger;

import wbs.platform.event.logic.EventLogic;

import wbs.imchat.model.ImChatCustomerRec;
import wbs.imchat.model.ImChatObjectHelper;
import wbs.imchat.model.ImChatRec;
import wbs.imchat.model.ImChatSessionObjectHelper;
import wbs.imchat.model.ImChatSessionRec;
import wbs.web.action.Action;
import wbs.web.context.RequestContext;
import wbs.web.responder.JsonResponder;
import wbs.web.responder.Responder;

@PrototypeComponent ("imChatConditionsAcceptAction")
public
class ImChatConditionsAcceptAction
	implements Action {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@SingletonDependency
	EventLogic eventLogic;

	@SingletonDependency
	ImChatApiLogic imChatApiLogic;

	@SingletonDependency
	ImChatObjectHelper imChatHelper;

	@SingletonDependency
	ImChatSessionObjectHelper imChatSessionHelper;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	RequestContext requestContext;

	// prototype dependencies

	@PrototypeDependency
	Provider <JsonResponder> jsonResponderProvider;

	// implementation

	@Override
	public
	Responder handle (
			@NonNull TaskLogger parentTaskLogger) {

		TaskLogger taskLogger =
			logContext.nestTaskLogger (
				parentTaskLogger,
				"handle");

		DataFromJson dataFromJson =
			new DataFromJson ();

		// decode request

		JSONObject jsonValue =
			(JSONObject)
			JSONValue.parse (
				requestContext.reader ());

		ImChatConditionsAcceptRequest conditionsAcceptRequest =
			dataFromJson.fromJson (
				ImChatConditionsAcceptRequest.class,
				jsonValue);

		// begin transaction

		try (

			Transaction transaction =
				database.beginReadWrite (
					taskLogger,
					"ImChatConditionsAcceptAction.handle ()",
					this);

		) {

			ImChatRec imChat =
				imChatHelper.findRequired (
					parseIntegerRequired (
						requestContext.requestStringRequired (
							"imChatId")));

			// lookup session

			ImChatSessionRec session =
				imChatSessionHelper.findBySecret (
					conditionsAcceptRequest.sessionSecret ());

			ImChatCustomerRec customer =
				session.getImChatCustomer ();

			if (

				isNull (
					session)

				|| not (
					session.getActive ())

				|| referenceNotEqualWithClass (
					ImChatRec.class,
					customer.getImChat (),
					imChat)

			) {

				ImChatFailure failureResponse =
					new ImChatFailure ()

					.reason (
						"session-invalid")

					.message (
						"The session secret is invalid or the session is no " +
						"longer active");

				return jsonResponderProvider.get ()

					.value (
						failureResponse);

			}

			if (! customer.getAcceptedTermsAndConditions ()) {

				// accept terms and conditions

				customer

					.setAcceptedTermsAndConditions (
						true);

				eventLogic.createEvent (
					taskLogger,
					"im_chat_customer_conditions_accepted",
					customer);

			}

			// create response

			ImChatConditionsAcceptSuccess successResponse =
				new ImChatConditionsAcceptSuccess ()

				.customer (
					imChatApiLogic.customerData (
						customer));

			// commit and return

			transaction.commit ();

			return jsonResponderProvider.get ()

				.value (
					successResponse);

		}

	}

}
