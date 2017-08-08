package wbs.platform.rpc.core;

import java.util.List;

public
class RpcException
	extends RuntimeException {

	private final
	RpcResult result;

	public
	RpcException (
			RpcResult newResult) {

		super (
			newResult.getStatusMessage ());

		result = newResult;

	}

	public
	RpcException (
			String name,
			int status,
			String statusCode,
			List<String> statusMessages,
			RpcElem... members) {

		this (
			new RpcResult (
				name,
				status,
				statusCode,
				statusMessages,
				members));

	}

	public
	RpcException (
			String name,
			int status,
			String statusCode,
			String statusMessage) {

		this (
			new RpcResult (
				name,
				status,
				statusCode,
				statusMessage));

	}

	public
	RpcResult getRpcResult () {

		return result;

	}

}
