package wbs.applications.imchat.console;

import static wbs.framework.utils.etc.OptionalUtils.ifNotPresent;
import static wbs.framework.utils.etc.OptionalUtils.optionalCast;

import javax.inject.Named;
import javax.servlet.ServletException;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import lombok.Cleanup;
import wbs.console.action.ConsoleAction;
import wbs.console.forms.FormFieldLogic;
import wbs.console.forms.FormFieldLogic.UpdateResultSet;
import wbs.console.forms.FormFieldSet;
import wbs.console.module.ConsoleModule;
import wbs.console.request.ConsoleRequestContext;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.web.Responder;
import wbs.platform.user.console.UserConsoleHelper;
import wbs.platform.user.console.UserConsoleLogic;

@PrototypeComponent ("imChatCustomerCreditAction")
public
class ImChatCustomerCreditAction
	extends ConsoleAction {

	// implementation

	@SingletonDependency
	Database database;

	@SingletonDependency
	FormFieldLogic formFieldLogic;

	@SingletonDependency
	@Named
	ConsoleModule imChatCustomerConsoleModule;

	@SingletonDependency
	ImChatCustomerCreditConsoleHelper imChatCustomerCreditHelper;

	@SingletonDependency
	ImChatCustomerConsoleHelper imChatCustomerHelper;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	@SingletonDependency
	UserConsoleHelper userHelper;

	// details

	@Override
	protected
	Responder backupResponder () {

		return responder (
			"imChatCustomerCreditResponder");

	}

	// implementation

	@Override
	protected
	Responder goReal ()
		throws ServletException {

		// begin transaction

		@Cleanup
		Transaction transaction =
			database.beginReadWrite (
				"ImChatCustomerCreditAction.goReal ()",
				this);

		// process form fields

		FormFieldSet formFields =
			imChatCustomerConsoleModule.formFieldSets ().get (
				"credit-request");

		ImChatCustomerCreditRequest request =
			ifNotPresent (

			optionalCast (
				ImChatCustomerCreditRequest.class,
				requestContext.request (
					"imChatCustomerCreditRequest")),

			Optional.of (
				new ImChatCustomerCreditRequest ())

		);

		request.customer (
			imChatCustomerHelper.findRequired (
				requestContext.stuffInteger (
					"imChatCustomerId")));

		UpdateResultSet updateResultSet =
			formFieldLogic.update (
				requestContext,
				formFields,
				request,
				ImmutableMap.of (),
				"credit");

		if (updateResultSet.errorCount () > 0) {

			requestContext.request (
				"imChatCustomerCreditUpdateResults",
				updateResultSet);

			formFieldLogic.reportErrors (
				requestContext,
				updateResultSet,
				"credit");

			return null;

		}

		// create credit log

		imChatCustomerCreditHelper.insert (
			imChatCustomerCreditHelper.createInstance ()

			.setImChatCustomer (
				request.customer ())

			.setIndex (
				request.customer ().getNumCredits ())

			.setTimestamp (
				transaction.now ())

			.setUser (
				userConsoleLogic.userRequired ())

			.setReason (
				request.reason ())

			.setCreditAmount (
				request.creditAmount ())

			.setCreditBalanceBefore (
				request.customer ().getBalance ())

			.setCreditBalanceAfter (
				+ request.customer ().getBalance ()
				+ request.creditAmount ())

			.setBillAmount (
				request.billAmount ())

		);

		// update customer

		request.customer ()

			.setNumCredits (
				request.customer.getNumCredits () + 1)

			.setBalance (
				+ request.customer ().getBalance ()
				+ request.creditAmount ())

			.setTotalPurchaseValue (
				+ request.customer ().getTotalPurchaseValue ()
				+ request.creditAmount ())

			.setTotalPurchasePrice (
				+ request.customer ().getTotalPurchasePrice ()
				+ request.billAmount ());

		// complete transaction

		transaction.commit ();

		requestContext.addNotice (
			"Customer credit applied");

		return null;

	}

}
