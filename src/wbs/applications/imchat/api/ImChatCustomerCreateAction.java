package wbs.applications.imchat.api;

import static wbs.framework.utils.etc.Misc.doesNotContain;
import static wbs.framework.utils.etc.Misc.underscoreToHyphen;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Provider;

import lombok.Cleanup;
import lombok.SneakyThrows;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import wbs.applications.imchat.model.ImChatCustomerDetailTypeRec;
import wbs.applications.imchat.model.ImChatCustomerDetailValueObjectHelper;
import wbs.applications.imchat.model.ImChatCustomerDetailValueRec;
import wbs.applications.imchat.model.ImChatCustomerObjectHelper;
import wbs.applications.imchat.model.ImChatCustomerRec;
import wbs.applications.imchat.model.ImChatObjectHelper;
import wbs.applications.imchat.model.ImChatRec;
import wbs.applications.imchat.model.ImChatSessionObjectHelper;
import wbs.applications.imchat.model.ImChatSessionRec;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.data.tools.DataFromJson;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.utils.RandomLogic;
import wbs.framework.web.Action;
import wbs.framework.web.JsonResponder;
import wbs.framework.web.RequestContext;
import wbs.framework.web.Responder;
import wbs.platform.event.logic.EventLogic;

@PrototypeComponent ("imChatCustomerCreateAction")
public
class ImChatCustomerCreateAction
	implements Action {

	// dependencies

	@Inject
	Database database;

	@Inject
	EventLogic eventLogic;

	@Inject
	ImChatApiLogic imChatApiLogic;

	@Inject
	ImChatCustomerDetailValueObjectHelper imChatCustomerDetailValueHelper;

	@Inject
	ImChatCustomerObjectHelper imChatCustomerHelper;

	@Inject
	ImChatObjectHelper imChatHelper;

	@Inject
	ImChatSessionObjectHelper imChatSessionHelper;

	@Inject
	RandomLogic randomLogic;

	@Inject
	RequestContext requestContext;

	// prototype dependencies

	@Inject
	Provider<JsonResponder> jsonResponderProvider;

	// implementation

	@Override
	@SneakyThrows (IOException.class)
	public
	Responder handle () {

		DataFromJson dataFromJson =
			new DataFromJson ();

		// decode request

		JSONObject jsonValue =
			(JSONObject)
			JSONValue.parse (
				requestContext.reader ());

		ImChatCustomerCreateRequest createRequest =
			dataFromJson.fromJson (
				ImChatCustomerCreateRequest.class,
				jsonValue);

		// begin transaction

		@Cleanup
		Transaction transaction =
			database.beginReadWrite (
				this);

		ImChatRec imChat =
			imChatHelper.find (
				Integer.parseInt (
					(String)
					requestContext.request (
						"imChatId")));

		// check for existing

		ImChatCustomerRec existingCustomer =
			imChatCustomerHelper.findByEmail (
				imChat,
				createRequest.email ());

		if (existingCustomer != null) {

			ImChatFailure failureResponse =
				new ImChatFailure ()

				.reason (
					"email-already-exists")

				.message (
					"A customer with that email address already exists");

			return jsonResponderProvider.get ()
				.value (failureResponse);

		}

		// check email looks ok

		Matcher emailMatcher =
			emailPattern.matcher (
				createRequest.email ());

		if (! emailMatcher.matches ()) {

			ImChatFailure failureResponse =
				new ImChatFailure ()

				.reason (
					"email-invalid")

				.message (
					"Please enter a valid email address");

			return jsonResponderProvider.get ()
				.value (failureResponse);

		}

		// check password looks ok

		if (createRequest.password ().length () < 6) {

			ImChatFailure failureResponse =
				new ImChatFailure ()

				.reason (
					"password-invalid")

				.message (
					"Please enter a longer password");

			return jsonResponderProvider.get ()
				.value (failureResponse);

		}

		// create new

		ImChatCustomerRec newCustomer =
			imChatCustomerHelper.insert (
				imChatCustomerHelper.createInstance ()

			.setImChat (
				imChat)

			.setCode (
				randomLogic.generateNumericNoZero (8))

			.setEmail (
				createRequest.email ())

			.setPassword (
				createRequest.password ())

		);

		// update details

		for (
			ImChatCustomerDetailTypeRec detailType
				: imChat.getCustomerDetailTypes ()
		) {

			if (
				doesNotContain (
					createRequest.details ().keySet (),
					underscoreToHyphen (
						detailType.getCode ()))
			) {
				continue;
			}

			String stringValue =
				createRequest.details ().get (
					underscoreToHyphen (
						detailType.getCode ()));

			ImChatCustomerDetailValueRec detailValue =
				imChatCustomerDetailValueHelper.insert (
					imChatCustomerDetailValueHelper.createInstance ()

				.setImChatCustomer (
					newCustomer)

				.setImChatCustomerDetailType (
					detailType)

				.setValue (
					stringValue)

			);

			newCustomer.getDetails ().put (
				detailType.getId (),
				detailValue);

			eventLogic.createEvent (
				"im_chat_customer_detail_updated",
				newCustomer,
				detailType,
				stringValue);

		}

		// create session

		ImChatSessionRec session =
			imChatSessionHelper.insert (
				imChatSessionHelper.createInstance ()

			.setImChatCustomer (
				newCustomer)

			.setSecret (
				randomLogic.generateLowercase (20))

			.setActive (
				true)

			.setStartTime (
				transaction.now ())

			.setUpdateTime (
				transaction.now ())

		);

		newCustomer

			.setActiveSession (
				session);

		// create response

		ImChatCustomerCreateSuccess successResponse =
			new ImChatCustomerCreateSuccess ()

			.sessionSecret (
				session.getSecret ())

			.customer (
				imChatApiLogic.customerData (
					newCustomer));

		// commit and return

		transaction.commit ();

		return jsonResponderProvider.get ()
			.value (successResponse);

	}

	final static
	Pattern emailPattern =
		Pattern.compile (
			"[^@]+@[^@]+");

}
