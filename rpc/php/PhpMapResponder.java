package wbs.platform.rpc.php;

import java.io.OutputStream;
import java.util.Map;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.platform.php.PhpSerializer;

import wbs.web.context.RequestContext;
import wbs.web.misc.HttpStatus;
import wbs.web.responder.BufferedResponder;

@Accessors (fluent = true)
@PrototypeComponent ("phpMapResponder")
public
class PhpMapResponder
	extends BufferedResponder {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	RequestContext requestContext;

	// properties

	@Getter @Setter
	Map <?,?> map;

	@Getter @Setter
	long status =
		HttpStatus.httpOk;

	// protected implementation

	@Override
	protected
	void prepare (
			@NonNull Transaction parentTransaction) {

	}

	@Override
	protected
	void render (
			@NonNull Transaction parentTransaction,
			@NonNull OutputStream outputStream) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"render");

		) {

			PhpSerializer.serialize (
				outputStream,
				map,
				"utf-8");

		}

	}

	@Override
	protected
	void headers (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"headers");

		) {

			requestContext.status (
				status);

			requestContext.contentType (
				"application/vnd.php.serialized",
				"utf-8");

		}

	}

}
