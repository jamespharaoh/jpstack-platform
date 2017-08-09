package wbs.platform.rpc.xml;

import static wbs.utils.etc.BinaryUtils.bytesFromHex;
import static wbs.utils.etc.EnumUtils.enumName;
import static wbs.utils.etc.NumberUtils.parseIntegerRequired;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.string.StringUtils.stringEqualSafe;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.string.StringUtils.stringNotEqualSafe;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.base.Optional;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.joda.time.LocalDate;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentManager;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.rpc.core.Rpc;
import wbs.platform.rpc.core.RpcDefinition;
import wbs.platform.rpc.core.RpcException;
import wbs.platform.rpc.core.RpcHandler;
import wbs.platform.rpc.core.RpcResult;
import wbs.platform.rpc.core.RpcSource;

import wbs.utils.io.RuntimeIoException;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import nu.xom.ValidityException;
import wbs.web.context.RequestContext;
import wbs.web.mvc.WebAction;
import wbs.web.responder.WebResponder;

@Accessors (fluent = true)
@PrototypeComponent ("xmlRpcAction")
public
class XmlRpcAction
	implements WebAction {

	// singleton dependencies

	@SingletonDependency
	ComponentManager componentManager;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	RequestContext requestContext;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <XmlResponder> xmlResponderProvider;

	// properties

	@Getter @Setter
	ComponentProvider <? extends RpcHandler> rpcHandlerProvider;

	// property setters

	public
	XmlRpcAction rpcHandlerName (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull String name) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"rpcHandlerName");

		) {

			return rpcHandlerProvider (
				componentManager.getComponentProviderRequired (
					parentTaskLogger,
					name,
					RpcHandler.class));

		}

	}

	// implementation

	public
	WebResponder xmlInternalErrorResponder (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"xmlInternalErrorResponder");

		) {

			return makeRpcResponder (
				taskLogger,
				Rpc.rpcError (
					"FIXME",
					Rpc.stInternalError,
					"internal-error",
					"An internal error has occurred"));

		}

	}

	@Override
	public
	Optional <WebResponder> defaultResponder (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"defaultResponder");

		) {

			return optionalOf (
				xmlInternalErrorResponder (
					taskLogger));

		}

	}

	private
	WebResponder makeRpcResponder (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull RpcResult result) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"makeRpcResponder");

		) {

			return xmlResponderProvider.provide (
				taskLogger,
				xmlResponder ->
					xmlResponder

				.data (
					result.getStruct ())

				.status (
					result.getHttpStatus ())

			);

		}

	}

	@Override
	public
	WebResponder handle (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"handle");

		) {

			RpcResult ret =
				realGo (
					parentTaskLogger);

			return ret != null
				? makeRpcResponder (
					taskLogger,
					ret)
				: null;

		}

	}

	public
	XmlRpcSource parse (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull InputStream in) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"parse");

		) {

			Builder parser =
				new Builder ();

			Document document =
				parser.build (in);

			XmlRpcSource source =
				new XmlRpcSource (
					document.getRootElement ());

			if (taskLogger.debugEnabled ()) {

				Serializer serializer =
					new Serializer (System.out, "utf-8");

				serializer.setIndent (4);

				serializer.write (document);

			}

			return source;

		} catch (IOException ioException) {

			throw new RuntimeIoException (
				ioException);

		} catch (ValidityException exception) {

			throw new RpcException (
				"error-response",
				Rpc.stRequestInvalid,
				"request-invalid",
				exception.getMessage ());

		} catch (ParsingException exception) {

			throw new RpcException (
				"error-response",
				Rpc.stRequestInvalid,
				"request-invalid",
				exception.getMessage ());

		}

	}

	public
	RpcResult realGo (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"realGo");

		) {

			XmlRpcSource source =
				parse (
					taskLogger,
					requestContext.inputStream ());

			RpcHandler rpcHandler =
				rpcHandlerProvider.provide (
					taskLogger);

			return rpcHandler.handle (
				taskLogger,
				source);

		} catch (RpcException exception) {

			return exception.getRpcResult ();

		} catch (Exception exception) {

			throw new RuntimeException (
				exception);

		}

	}

	private
	class XmlRpcSource
		implements RpcSource {

		private
		Element element;

		private
		XmlRpcSource (
				Element newElem) {

			element =
				newElem;

		}

		@Override
		public
		Object obtain (
				RpcDefinition def,
				List <String> errors,
				boolean checkRequires) {

			return xmlToObject (
				def,
				element,
				errors,
				checkRequires);

		}

	}

	private
	Object xmlToObject (
			RpcDefinition rpcDefinition,
			Element element,
			List<String> errors,
			boolean checkRequires) {

		Object object =
			xmlToObjectInternal (
				rpcDefinition,
				element,
				errors,
				checkRequires);

		if (rpcDefinition.checker () != null) {

			object =
				rpcDefinition.checker ().check (
					rpcDefinition,
					object,
					errors);

		}

		return object;

	}

	private
	Object xmlToObjectInternal (
			RpcDefinition rpcDefinition,
			Element element,
			List<String> errors,
			boolean checkRequires) {

		// check the element type is correct

		if (
			stringNotEqualSafe (
				rpcDefinition.name (),
				element.getLocalName ())
		) {

			errors.add (
				stringFormat (
					"Unexpected element: %s, expected: %s",
					element.getLocalName (),
					rpcDefinition.name ()));

			return null;

		}

		// delegate depending on type

		switch (rpcDefinition.type ()) {

		case rBoolean:

			return xmlToBoolean (
				rpcDefinition,
				element,
				errors);

		case rInteger:

			return xmlToInteger (
				rpcDefinition,
				element,
				errors);

		case rString:

			return xmlToString (
				rpcDefinition,
				element,
				errors);

		case rFloat:

			return xmlToFloat (
				rpcDefinition,
				element,
				errors);

		case rStructure:

			return xmlToStructure (
				rpcDefinition,
				element,
				errors,
				checkRequires);

		case rList:

			return xmlToList (
				rpcDefinition,
				element,
				errors,
				checkRequires);

		case rSource:

			return xmlToSource (
				rpcDefinition,
				element,
				errors);

		case rBinary:

			return xmlToBinary (
				rpcDefinition,
				element,
				errors);

		case rDate:

			return xmlToDate (
				rpcDefinition,
				element,
				errors);

		}

		throw new RuntimeException (
			stringFormat (
				"Can't decode %s",
				enumName (
					rpcDefinition.type ())));

	}

	private
	Object xmlToBoolean (
			RpcDefinition rpcDefinition,
			Element element,
			List<String> errors) {

		if (element.getChildElements ().size () != 0) {

			errors.add (
				stringFormat (
					"Node should not contain elements: %s",
					rpcDefinition.name ()));

			return null;

		}

		String text =
			element.getValue ();

		if (
			stringEqualSafe (
				text,
				"true")
		) {
			return true;
		}

		if (
			stringEqualSafe (
				text,
				"false")
		) {
			return false;
		}

		errors.add (
			stringFormat (
				"Node should contain true or false: %s",
				rpcDefinition.name ()));

		return null;

	}

	private final static
	Pattern integerPattern =
		Pattern.compile (
			"0|-?[123456789][0123456789]*");

	private
	Object xmlToInteger (
			RpcDefinition rpcDefinition,
			Element element,
			List <String> errors) {

		if (element.getChildElements ().size () > 0) {

			errors.add (
				stringFormat (
					"Node should not contain children: %s",
					rpcDefinition.name ()));

			return null;

		}

		String text =
			element.getValue ();

		if (! integerPattern.matcher (text).matches ()) {

			errors.add (
				stringFormat (
					"Node should be an integer value: %s",
					rpcDefinition.name ()));

			return null;

		}

		return parseIntegerRequired (
			text);

	}

	private
	Object xmlToString (
			RpcDefinition def,
			Element elem,
			List<String> errors) {

		if (elem.getChildElements ().size () > 0) {

			errors.add (
				stringFormat (
					"Node should not contain children: %s",
					def.name ()));

			return null;

		}

		return elem.getValue ();

	}

	private
	Object xmlToDate (
			RpcDefinition rpcDefinition,
			Element element,
			List<String> errors) {

		if (element.getChildElements ().size () > 0) {

			errors.add (
				stringFormat (
					"Node should not contain children: %s",
					rpcDefinition.name ()));

			return null;

		}

		String string =
			element.getValue ();

		try {

			return LocalDate.parse (
				string);

		} catch (Exception exception) {

			errors.add (
				stringFormat (
					"Node contains invalid date: %s",
					rpcDefinition.name ()));

			return null;

		}

	}

	private
	Object xmlToFloat (
			RpcDefinition rpcDefinition,
			Element element,
			List<String> errors) {

		if (element.getChildElements ().size () > 0) {

			errors.add (
				stringFormat (
					"Node should not contain children: %s",
					rpcDefinition.name ()));

			return null;

		}

		String text =
			element.getValue ();

		return Double.parseDouble (
			text);

	}

	private
	Object xmlToStructure (
			RpcDefinition rpcDefinition,
			Element element,
			List<String> errors,
			boolean checkRequires) {

		Map<String,Object> outMap =
			new HashMap<String,Object> ();

		Map<String,RpcDefinition> definitionMap =
			rpcDefinition.membersByName ();

		Elements children =
			element.getChildElements ();

		// iterate provided members

		for (
			int index = 0;
			index < children.size ();
			index ++
		) {

			Element child =
				children.get (
					index);

			// check it is defined

			if (! definitionMap.containsKey (
					child.getLocalName ())) {

				if (checkRequires) {

					errors.add (
						stringFormat (
							"Structure member not recognised: %s",
							child.getLocalName ()));

				}

				continue;

			}

			// check it isn't a dupe

			if (outMap.containsKey (
					child.getLocalName ())) {

				errors.add (
					stringFormat (
						"Duplicated structure member: %s",
						child.getLocalName ()));

				continue;

			}

			// store it

			Object outValue =
				xmlToObject (
					definitionMap.get (
						child.getLocalName ()),
					child,
					errors,
					checkRequires);

			outMap.put (
				child.getLocalName (),
				outValue);

		}

		// check all required parameters are present

		for (RpcDefinition member
				: definitionMap.values ()) {

			if (outMap.containsKey (
					member.name ()))
				continue;

			if (member.required ()) {

				errors.add (
					stringFormat (
						"Structure missing required member: %s",
						member.name ()));

			} else {

				outMap.put (
					member.name (),
					member.defaultValue ());

			}

		}

		// return

		return outMap;

	}

	private
	Object xmlToList (
			RpcDefinition rpcDefinition,
			Element element,
			List<String> errors,
			boolean checkRequires) {

		// iterate members

		List<Object> ret =
			new ArrayList<Object> ();

		RpcDefinition memberDefinition =
			rpcDefinition.members () [0];

		Elements children =
			element.getChildElements ();

		for (
			int index = 0;
			index < children.size ();
			index ++
		) {

			Element child =
				children.get (
					index);

			Object out =
				xmlToObject (
					memberDefinition,
					child,
					errors,
					checkRequires);

			ret.add (
				out);

		}

		// return

		return ret;

	}

	private
	Object xmlToSource (
			RpcDefinition rpcDefinition,
			Element element,
			List<String> errors) {

		return new XmlRpcSource (
			element);

	}

	private final static
	Pattern hexPattern =
		Pattern.compile (
			"[0123456789abcdefABCDEF]*");

	private
	Object xmlToBinary (
			RpcDefinition rpcDefinition,
			Element element,
			List<String> errors) {

		if (element.getChildElements ().size () > 0) {

			errors.add (
				stringFormat (
					"Elementshould not contain children: %s",
					rpcDefinition.name ()));

			return null;

		}

		String text =
			element.getValue ().trim ();

		if (! hexPattern.matcher (text).matches ()) {

			errors.add (
				stringFormat (
					"Element should contain hex: %s",
					rpcDefinition.name ()));

			return null;

		}

		return bytesFromHex (
			text);

	}

}
