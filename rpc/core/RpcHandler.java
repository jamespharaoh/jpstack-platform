package wbs.platform.rpc.core;

import wbs.framework.logging.TaskLogger;

public
interface RpcHandler {

	RpcResult handle (
			TaskLogger taskLogger,
			RpcSource source);

}
