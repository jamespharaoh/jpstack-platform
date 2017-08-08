package wbs.platform.rpc.core;

import static wbs.utils.etc.NumberUtils.toJavaIntegerRequired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import wbs.web.misc.HttpStatus;

public
class Rpc {

	private
	Rpc () {
		// never instantiated
	}

	/*
	 * Status codes are blocked as follows:
	 *
	 * 0x0___ success
	 * 0x1___ server classErrors
	 * 0x2___ authentication classErrors
	 * 0x3___ authorisation classErrors
	 * 0x4___ invalid request
	 * 0x5___ specific classErrors
	 *
	 * 0x_0__ standard codes
	 * 0x_Z__ custom codes (Z > 0)
	 */

	public final static
	int
		stSuccess = 0x0000,
		stPartialSuccess = 0x0001,
		stInternalError = 0x1000,
		stAuthError = 0x2000,
		stForbidden = 0x3000,
		stRequestInvalid = 0x4000,
		stEncodingUnsupported = 0x4001,
		stFailed = 0x5000,
		stCancelled = 0x5001;

	public static
	long statusToHttpStatus (
			long status) {

		switch (
			toJavaIntegerRequired (
				status & 0xf000l)
		) {

		case 0x0000:
			return HttpStatus.httpOk;

		case 0x1000:
			return HttpStatus.httpInternalServerError;

		case 0x2000:
			return HttpStatus.httpUnauthorized;

		case 0x3000:
			return HttpStatus.httpForbidden;

		case 0x4000:
			return HttpStatus.httpBadRequest;

		case 0x5000:
			return HttpStatus.httpConflict;

		}

		throw new RuntimeException (
			"Invalid status: " + status);

	}

	public static
	RpcResult rpcError (
			String name,
			int status,
			String statusCode,
			String error) {

		return rpcError (
			name,
			status,
			statusCode,
			ImmutableList.<String>of (
				error));

	}

	public static
	RpcResult rpcError (
			String name,
			int status,
			String statusCode,
			List<String> messages) {

		return new RpcResult (
			name,
			status,
			statusCode,
			messages);

	}

	public static
	RpcResult rpcSuccess (
			String name,
			String message,
			RpcElem... members) {

		return new RpcResult (
			name,
			stSuccess,
			"success",
			ImmutableList.<String>of (
				message),
			members);

	}

	public static
	RpcDefinition rpcDefinition (
			String name,
			RpcType type,
			RpcDefinition... members) {

		return new RpcDefinition (
			name,
			true,
			null,
			type,
			null,
			members);

	}

	public static
	RpcDefinition rpcDefinition (
			String name,
			Object defaultValue,
			RpcType type,
			RpcDefinition... members) {

		return new RpcDefinition (
			name,
			false,
			defaultValue,
			type,
			null,
			members);

	}

	public static
	RpcDefinition rpcDefinition (
			String name,
			RpcType type,
			RpcChecker checker,
			RpcDefinition... members) {

		return new RpcDefinition (
			name,
			true,
			null,
			type,
			checker,
			members);

	}

	public static
	RpcDefinition rpcDefinition (
			String name,
			Object defaultValue,
			RpcType type,
			RpcChecker checker,
			RpcDefinition... members) {

		return new RpcDefinition (
			name,
			false,
			defaultValue,
			type,
			checker,
			members);

	}

	public static
	RpcChecker.EnumRpcChecker rpcEnumChecker (
			Map <String, ?> map) {

		return new RpcChecker.EnumRpcChecker (
			map);

	}

	public static
	RpcChecker.SetRpcChecker rpcSetChecker () {
		return new RpcChecker.SetRpcChecker ();
	}

	public static
	RpcStructure rpcStruct (
			String name,
			RpcElem... members) {

		return new RpcStructure (
			name,
			members);

	}

	public static
	RpcPrimitive rpcElem (
			String name,
			Object value) {

		return new RpcPrimitive (
			name,
			value);

	}

	public static
	RpcList rpcList (
			String name,
			String memberName,
			RpcType memberType,
			RpcElem... members) {

		return new RpcList (
			name,
			memberName,
			memberType,
			members);

	}

	public static
	RpcList rpcList (
			String name,
			RpcElem... members) {

		if (members.length < 1) {

			throw new IllegalArgumentException (
				"Must specify type for rpcList if no members are supplied");

		}

		return new RpcList (
			name,
			members [0].getName (),
			members [0].getType (),
			members);

	}

	public static
	RpcList rpcList (
			String name,
			Collection<RpcElem> members) {

		if (members.size () < 1) {

			throw new IllegalArgumentException (
				"Must specify type for rpcList if no members are supplied");

		}

		RpcElem firstMember =
			members.iterator ().next ();

		return new RpcList (
			name,
			firstMember.getName (),
			firstMember.getType (),
			members);

	}

	public static
	RpcList rpcList (
			String name,
			String memberName,
			Collection<String> memberStrings) {

		if (memberStrings.size () < 1) {

			throw new IllegalArgumentException (
				"Must specify type for rpcList if no members are supplied");

		}

		List<RpcElem> memberElements =
			new ArrayList<RpcElem> ();

		for (String member
				: memberStrings) {

			memberElements.add (
				rpcElem (
					memberName,
					member));

		}

		return new RpcList (
			name,
			memberName,
			RpcType.rString,
			memberElements);

	}

}
