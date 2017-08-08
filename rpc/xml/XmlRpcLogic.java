package wbs.platform.rpc.xml;

import wbs.framework.logging.TaskLogger;

import wbs.web.context.RequestContext;

public
interface XmlRpcLogic {

	void xmlRpcHeaders (
			TaskLogger parentTaskLogger,
			RequestContext requestContext);

}
