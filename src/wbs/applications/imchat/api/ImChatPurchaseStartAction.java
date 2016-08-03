package wbs.applications.imchat.api;

import static wbs.framework.utils.etc.Misc.notEqual;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import lombok.Cleanup;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.google.common.base.Optional;

import wbs.applications.imchat.model.ImChatCustomerObjectHelper;
import wbs.applications.imchat.model.ImChatCustomerRec;
import wbs.applications.imchat.model.ImChatObjectHelper;
import wbs.applications.imchat.model.ImChatPricePointObjectHelper;
import wbs.applications.imchat.model.ImChatPricePointRec;
import wbs.applications.imchat.model.ImChatPurchaseObjectHelper;
import wbs.applications.imchat.model.ImChatPurchaseRec;
import wbs.applications.imchat.model.ImChatPurchaseState;
import wbs.applications.imchat.model.ImChatRec;
import wbs.applications.imchat.model.ImChatSessionObjectHelper;
import wbs.applications.imchat.model.ImChatSessionRec;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.data.tools.DataFromJson;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.utils.RandomLogic;

import static wbs.framework.utils.etc.OptionalUtils.isNotPresent;
import static wbs.framework.utils.etc.OptionalUtils.isPresent;
import static wbs.framework.utils.etc.StringUtils.hyphenToUnderscore;

import wbs.framework.web.Action;
import wbs.framework.web.JsonResponder;
import wbs.framework.web.RequestContext;
import wbs.framework.web.Responder;
import wbs.integrations.paypal.logic.PaypalApi;
import wbs.integrations.paypal.logic.PaypalLogic;
import wbs.integrations.paypal.model.PaypalAccountRec;
import wbs.integrations.paypal.model.PaypalPaymentObjectHelper;
import wbs.integrations.paypal.model.PaypalPaymentRec;
import wbs.integrations.paypal.model.PaypalPaymentState;
import wbs.platform.currency.logic.CurrencyLogic;

@PrototypeComponent ("imChatPurchaseStartAction")
public
class ImChatPurchaseStartAction
	implements Action {

	// dependencies

	@Inject
	CurrencyLogic currencyLogic;

	@Inject
	Database database;

	@Inject
	ImChatApiLogic imChatApiLogic;

	@Inject
	ImChatObjectHelper imChatHelper;

	@Inject
	ImChatCustomerObjectHelper imChatCustomerHelper;

	@Inject
	ImChatPricePointObjectHelper imChatPricePointHelper;

	@Inject
	ImChatPurchaseObjectHelper imChatPurchaseHelper;

	@Inject
	ImChatSessionObjectHelper imChatSessionHelper;

	@Inject
	PaypalApi paypalApi;

	@Inject
	PaypalLogic paypalLogic;

	@Inject
	PaypalPaymentObjectHelper paypalPaymentHelper;

	@Inject
	RandomLogic randomLogic;

	@Inject
	RequestContext requestContext;

	// prototype dependencies

	@Inject
	Provider<JsonResponder> jsonResponderProvider;

	// state

	ImChatPurchaseStartRequest purchaseRequest;

	Boolean imChatDevelopmentMode;

	Integer customerId;
	ImChatCustomerData customerData;

	Map<String,String> paypalExpressCheckoutProperties;

	Integer purchaseId;
	String purchasePriceString;
	String purchaseSuccessUrl;
	String purchaseFailureUrl;
	String purchaseCheckoutUrl;

	Optional<String> redirectUrl;

	// implementation

	@Override
	public
	Responder handle () {

		decodeRequest ();

		Optional<Responder> createPurchaseResult =
			createPurchase ();

		if (
			isPresent (
				createPurchaseResult)
		) {
			return createPurchaseResult.get ();
		}

		makeApiCall ();

		updatePurchase ();

		return createResponse ();

	}

	void decodeRequest () {

		DataFromJson dataFromJson =
			new DataFromJson ();

		JSONObject jsonValue =
			(JSONObject)
			JSONValue.parse (
				requestContext.reader ());

		purchaseRequest =
			dataFromJson.fromJson (
				ImChatPurchaseStartRequest.class,
				jsonValue);

	}

	Optional<Responder> createPurchase () {

		// begin transaction

		@Cleanup
		Transaction transaction =
			database.beginReadWrite (
				"ImChatPurchaseStartAction.createPurchase ()",
				this);

		ImChatRec imChat =
			imChatHelper.findRequired (
				Integer.parseInt (
					requestContext.requestStringRequired (
						"imChatId")));

		imChatDevelopmentMode =
			imChat.getDevelopmentMode ();

		// lookup session and customer

		ImChatSessionRec session =
			imChatSessionHelper.findBySecret (
				purchaseRequest.sessionSecret ());

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

			return Optional.of (
				jsonResponderProvider.get ()

				.value (
					failureResponse)

			);

		}

		// get customer

		ImChatCustomerRec customer =
			session.getImChatCustomer ();

		customerId =
			customer.getId ();

		// lookup price point

		Optional<ImChatPricePointRec> pricePointOptional =
			imChatPricePointHelper.findByCode (
				imChat,
				hyphenToUnderscore (
					purchaseRequest.pricePointCode ()));

		if (

			isNotPresent (
				pricePointOptional)

			|| notEqual (
				pricePointOptional.get ().getImChat (),
				imChat)

			|| pricePointOptional.get ().getDeleted ()

		) {

			ImChatFailure failureResponse =
				new ImChatFailure ()

				.reason (
					"price-point-invalid")

				.message (
					"The price point id is invalid");

			return Optional.of (
				jsonResponderProvider.get ()

				.value (
					failureResponse)

			);

		}

		ImChatPricePointRec pricePoint =
			pricePointOptional.get ();

		// get paypal account

		PaypalAccountRec paypalAccount =
			imChat.getPaypalAccount ();

		paypalExpressCheckoutProperties =
			paypalLogic.expressCheckoutProperties (
				paypalAccount);

		// create paypal payment

		PaypalPaymentRec paypalPayment;

		if (imChat.getDevelopmentMode ()) {

			paypalPayment = null;

		} else {

			paypalPayment =
				paypalPaymentHelper.insert (
					paypalPaymentHelper.createInstance ()

				.setPaypalAccount (
					paypalAccount)

				.setTimestamp (
					transaction.now ())

				.setValue (
					customer.getDeveloperMode ()
						? 1
						: pricePoint.getPrice ())

				.setState (
					PaypalPaymentState.started)

			);

		}

		// create purchase

		ImChatPurchaseRec purchase =
			imChatPurchaseHelper.insert (
				imChatPurchaseHelper.createInstance ()

			.setImChatCustomer (
				customer)

			.setIndex (
				(int) (long)
				customer.getNumPurchases ())

			.setToken (
				randomLogic.generateLowercase (20))

			.setImChatSession (
				session)

			.setImChatPricePoint (
				pricePoint)

			.setState (
				ImChatPurchaseState.creating)

			.setPrice (
				customer.getDeveloperMode ()
					? 1
					: pricePoint.getPrice ())

			.setValue (
				pricePoint.getValue ())

			.setCreatedTime (
				transaction.now ())

			.setCompletedTime (
				imChat.getDevelopmentMode ()
					? transaction.now ()
					: null)

			.setPaypalPayment (
				paypalPayment)

		);

		purchaseId =
			purchase.getId ();

		purchasePriceString =
			currencyLogic.formatSimple (
				imChat.getBillingCurrency (),
				purchase.getPrice ());

		purchaseSuccessUrl =
			purchaseRequest.successUrl ().replace (
				"{token}",
				purchase.getToken ());

		purchaseFailureUrl =
			purchaseRequest.failureUrl ().replace (
				"{token}",
				purchase.getToken ());

		purchaseCheckoutUrl =
			paypalAccount.getCheckoutUrl ();

		// update customer

		customer

			.setNumPurchases (
				customer.getNumPurchases () + 1)

			.setBalance (
				imChat.getDevelopmentMode ()
					? customer.getBalance ()
						+ pricePoint.getValue ()
					: customer.getBalance ());

		// commit and return

		transaction.commit ();

		return Optional.absent ();

	}

	void makeApiCall () {

		// update paypal

		if (imChatDevelopmentMode) {

			redirectUrl =
				Optional.of (
					"development-mode");

		} else {

			redirectUrl =
				paypalApi.setExpressCheckout (
					purchasePriceString,
					purchaseSuccessUrl,
					purchaseFailureUrl,
					purchaseCheckoutUrl,
					paypalExpressCheckoutProperties);

		}

	}

	void updatePurchase () {

		// begin transaction

		@Cleanup
		Transaction transaction =
			database.beginReadWrite (
				"ImChatPurchaseStartAction.updatePurchase",
				this);

		// update purchase

		ImChatPurchaseRec purchase =
			imChatPurchaseHelper.findRequired (
				purchaseId);

		purchase

			.setState (
				redirectUrl.isPresent ()
					? ImChatPurchaseState.created
					: ImChatPurchaseState.createFailed);

		// process customer

		ImChatCustomerRec customer =
			imChatCustomerHelper.findRequired (
				customerId);

		customerData =
			imChatApiLogic.customerData (
				customer);

		// commit transaction

		transaction.commit ();

	}

	Responder createResponse () {

		if (
			isPresent (
				redirectUrl)
		) {

			// create response

			ImChatPurchaseStartSuccess successResponse =
				new ImChatPurchaseStartSuccess ()

				.redirectUrl (
					redirectUrl.get ())

				.customer (
					customerData);

			// return

			return jsonResponderProvider.get ()

				.value (
					successResponse);

		} else {

			ImChatFailure failureResponse =
				new ImChatFailure ()

				.reason (
					"unknown-error")

				.message (
					"The payment attempt failed for an unknown reason");

			return jsonResponderProvider.get ()

				.value (
					failureResponse);

		}

	}

}
