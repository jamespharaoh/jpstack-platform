package wbs.platform.deployment.model;

import java.util.List;

import wbs.framework.database.Transaction;

public
interface ConsoleDeploymentDaoMethods {

	List <ConsoleDeploymentRec> findByHostNotDeleted (
			Transaction parentTransaction,
			String host);

}
