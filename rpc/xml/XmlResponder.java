package wbs.platform.rpc.xml;

import static wbs.utils.etc.BinaryUtils.bytesToHex;

import java.io.IOException;
import java.util.Map;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.rpc.core.RpcElem;
import wbs.platform.rpc.core.RpcList;
import wbs.platform.rpc.core.RpcPrimitive;
import wbs.platform.rpc.core.RpcStructure;
import wbs.platform.rpc.core.RpcType;

import wbs.utils.io.BorrowedOutputStream;
import wbs.utils.io.RuntimeIoException;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;
import wbs.web.context.RequestContext;
import wbs.web.misc.HttpStatus;
import wbs.web.responder.WebResponder;

@Accessors (fluent = true)
@PrototypeComponent ("xmlResponder")
public
class XmlResponder
	implements WebResponder {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	RequestContext requestContext;

	// properties

	@Getter @Setter
	RpcElem data;

	@Getter @Setter
	long status =
		HttpStatus.httpOk;

	@Override
	public
	void execute (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"execute");

		) {

			requestContext.status (
				status);

			requestContext.setHeader (
				"Content-Type",
				"application/xml");

			try (

				BorrowedOutputStream outputStream =
					requestContext.outputStream ();

			) {

				Element node =
					toNode (
						data);

				Document doc =
					new Document (
						node);

				Serializer serializer =
					new Serializer (
						outputStream,
						"utf-8");

				serializer.setIndent (
					4);

				serializer.write (
					doc);

				if (taskLogger.debugEnabled ()) {

					serializer =
						new Serializer (
							System.out,
							"utf-8");

					serializer.setIndent (4);
					serializer.write (doc);

				}

			}

		} catch (IOException ioException) {

			throw new RuntimeIoException (
				ioException);

		}

	}

	public
	static Element toNode (
			RpcStructure data) {

		Element elem =
			new Element (data.getName ());

		for (Map.Entry<String,RpcElem> ent
				: data.getValue ().entrySet ()) {

			elem.appendChild (
				toNode (ent.getValue ()));

		}

		return elem;

	}

	public
	static Element toNode (
			RpcList data) {

		Element elem =
			new Element (data.getName ());

		for (RpcElem ent
				: data.getValue ()) {

			elem.appendChild (
				toNode (ent));

		}

		return elem;

	}

	public
	static Element toNode (
			RpcPrimitive data) {

		Element elem =
			new Element (data.getName ());

		if (data.getType () == RpcType.rBinary) {

			byte[] bytes =
				(byte[]) data.getValue ();

			elem.appendChild (
				bytesToHex (
					bytes));

		} else {

			elem.appendChild (
				data.getValue ().toString ());

		}

		return elem;

	}

	public
	static Element toNode (
			RpcElem data) {

		switch (data.getType ()) {

		case rStructure:

			return toNode ((RpcStructure) data);

		case rList:

			return toNode ((RpcList) data);

		case rString:
		case rBoolean:
		case rInteger:
		case rDate:
		case rFloat:
		case rBinary:

			return toNode (
				(RpcPrimitive) data);

		default:

			throw new RuntimeException (
				"Unable to process " + data.getType ());

		}

	}

}
