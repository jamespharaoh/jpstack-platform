package wbs.platform.rpc.core;

import java.util.List;

public
interface RpcSource {

	Object obtain (
			RpcDefinition def,
			List <String> errors,
			boolean checkRequires);

}
