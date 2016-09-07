package wbs.applications.imchat.api;

import static wbs.framework.utils.etc.LogicUtils.booleanEqual;
import static wbs.framework.utils.etc.LogicUtils.referenceNotEqualWithClass;
import static wbs.framework.utils.etc.Misc.isNotNull;
import static wbs.framework.utils.etc.NumberUtils.parseIntegerRequired;
import static wbs.framework.utils.etc.OptionalUtils.optionalIsNotPresent;
import static wbs.framework.utils.etc.StringUtils.hyphenToUnderscore;

import javax.inject.Provider;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.google.common.base.Optional;

import lombok.Cleanup;
import wbs.applications.imchat.model.ImChatConversationObjectHelper;
import wbs.applications.imchat.model.ImChatConversationRec;
import wbs.applications.imchat.model.ImChatCustomerObjectHelper;
import wbs.applications.imchat.model.ImChatCustomerRec;
import wbs.applications.imchat.model.ImChatObjectHelper;
import wbs.applications.imchat.model.ImChatProfileObjectHelper;
import wbs.applications.imchat.model.ImChatProfileRec;
import wbs.applications.imchat.model.ImChatRec;
import wbs.applications.imchat.model.ImChatSessionObjectHelper;
import wbs.applications.imchat.model.ImChatSessionRec;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.data.tools.DataFromJson;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.web.Action;
import wbs.framework.web.JsonResponder;
import wbs.framework.web.RequestContext;
import wbs.framework.web.Responder;

@PrototypeComponent ("imChatConversationStartAction")
public
class ImChatConversationStartAction
	implements Action {

	// dependencies

	@SingletonDependency
	Database database;

	@SingletonDependency
	ImChatApiLogic imChatApiLogic;

	@SingletonDependency
	ImChatConversationObjectHelper imChatConversationHelper;

	@SingletonDependency
	ImChatCustomerObjectHelper imChatCustomerHelper;

	@SingletonDependency
	ImChatObjectHelper imChatHelper;

	@SingletonDependency
	ImChatProfileObjectHelper imChatProfileHelper;

	@SingletonDependency
	ImChatSessionObjectHelper imChatSessionHelper;

	@SingletonDependency
	RequestContext requestContext;

	// prototype dependencies

	@PrototypeDependency
	Provider <JsonResponder> jsonResponderProvider;

	// implementation

	@Override
	public
	Responder handle () {

		DataFromJson dataFromJson =
			new DataFromJson ();

		// decode request

		JSONObject jsonValue =
			(JSONObject)
			JSONValue.parse (
				requestContext.reader ());

		ImChatConversationStartRequest startRequest =
			dataFromJson.fromJson (
				ImChatConversationStartRequest.class,
				jsonValue);

		// begin transaction

		@Cleanup
		Transaction transaction =
			database.beginReadWrite (
				"ImChatConversationStartAction.handle ()",
				this);

		ImChatRec imChat =
			imChatHelper.findRequired (
				parseIntegerRequired (
					requestContext.requestStringRequired (
						"imChatId")));

		// lookup session and customer

		ImChatSessionRec session =
			imChatSessionHelper.findBySecret (
				startRequest.sessionSecret ());

		if (
			session == null
			|| ! session.getActive ()
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

		ImChatCustomerRec customer =
			session.getImChatCustomer ();

		// lookup profile

		Optional <ImChatProfileRec> profileOptional =
			imChatProfileHelper.findByCode (
				imChat,
				hyphenToUnderscore (
					startRequest.profileCode ()));

		if (

			optionalIsNotPresent (
				profileOptional)

			|| booleanEqual (
				profileOptional.get ().getDeleted (),
				true)

			|| referenceNotEqualWithClass (
				ImChatRec.class,
				profileOptional.get ().getImChat (),
				imChat)

		) {

			ImChatFailure failureResponse =
				new ImChatFailure ()

				.reason (
					"profile-invalid")

				.message (
					"The profile id is invalid");

			return jsonResponderProvider.get ()

				.value (
					failureResponse);

		}

		ImChatProfileRec profile =
			profileOptional.get ();

		// check state

		if (
			isNotNull (
				customer.getCurrentConversation ())
		) {

			ImChatFailure failureResponse =
				new ImChatFailure ()

				.reason (
					"conversation-already")

				.message (
					"There is already a conversation in progres");

			return jsonResponderProvider.get ()

				.value (
					failureResponse);

		}

		// create conversation

		ImChatConversationRec conversation =
			imChatConversationHelper.insert (
				imChatConversationHelper.createInstance ()

			.setImChatCustomer (
				customer)

			.setImChatProfile (
				profile)

			.setIndex (
				customer.getNumConversations ())

			.setStartTime (
				transaction.now ())

			.setPendingReply (
				false)

		);

		// update customer

		customer

			.setNumConversations (
				customer.getNumConversations () + 1)

			.setCurrentConversation (
				conversation);

		// create response

		ImChatConversationStartSuccess successResponse =
			new ImChatConversationStartSuccess ()

			.customer (
				imChatApiLogic.customerData (
					customer))

			.profile (
				imChatApiLogic.profileData (
					profile))

			.conversation (
				imChatApiLogic.conversationData (
					conversation));

		// commit and return

		transaction.commit ();

		return jsonResponderProvider.get ()

			.value (
				successResponse);

	}

}
