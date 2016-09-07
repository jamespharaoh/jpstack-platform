package wbs.applications.imchat.api;

import static wbs.framework.utils.etc.Misc.isNull;

import java.util.List;

import javax.inject.Provider;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.google.common.collect.Lists;

import lombok.Cleanup;
import wbs.applications.imchat.model.ImChatCustomerObjectHelper;
import wbs.applications.imchat.model.ImChatCustomerRec;
import wbs.applications.imchat.model.ImChatPurchaseObjectHelper;
import wbs.applications.imchat.model.ImChatPurchaseRec;
import wbs.applications.imchat.model.ImChatSessionObjectHelper;
import wbs.applications.imchat.model.ImChatSessionRec;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.data.tools.DataFromJson;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.utils.TimeFormatter;
import wbs.framework.web.Action;
import wbs.framework.web.JsonResponder;
import wbs.framework.web.RequestContext;
import wbs.framework.web.Responder;

@PrototypeComponent ("imChatPurchaseHistoryAction")
public
class ImChatPurchaseHistoryAction
	implements Action {

	// dependencies

	@SingletonDependency
	Database database;

	@SingletonDependency
	ImChatApiLogic imChatApiLogic;

	@SingletonDependency
	ImChatCustomerObjectHelper imChatCustomerHelper;

	@SingletonDependency
	ImChatPurchaseObjectHelper imChatPurchaseHelper;

	@SingletonDependency
	ImChatSessionObjectHelper imChatSessionHelper;

	@SingletonDependency
	RequestContext requestContext;

	@SingletonDependency
	TimeFormatter timeFormatter;

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

		ImChatPurchaseHistoryRequest purchaseHistoryRequest =
			dataFromJson.fromJson (
					ImChatPurchaseHistoryRequest.class,
				jsonValue);

		// begin transaction

		@Cleanup
		Transaction transaction =
			database.beginReadOnly (
				"ImChatPurchaseHistoryAction.handle ()",
				this);

		// lookup session and customer

		ImChatSessionRec session =
				imChatSessionHelper.findBySecret (
						purchaseHistoryRequest.sessionSecret ());

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

		// retrieve purchases

		List<ImChatPurchaseRec> purchases =
			Lists.reverse (
				customer.getPurchases ());

		// create response

		ImChatPurchaseHistorySuccess purchaseHistoryResponse =
			new ImChatPurchaseHistorySuccess ()

			.customer (
				imChatApiLogic.customerData (
					customer));

		for (
			ImChatPurchaseRec purchase
				: purchases
		) {

			if (
				isNull (
					purchase.getCompletedTime ())
			) {
				continue;
			}

			purchaseHistoryResponse.purchases.add (
				imChatApiLogic.purchaseHistoryData (
					purchase));

		}

		return jsonResponderProvider.get ()

			.value (
				purchaseHistoryResponse);

	}

}