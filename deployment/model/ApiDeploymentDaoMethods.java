package wbs.platform.deployment.model;

import java.util.List;

import wbs.framework.database.Transaction;

public
interface ApiDeploymentDaoMethods {

	List <ApiDeploymentRec> findByHostNotDeleted (
			Transaction parentTransaction,
			String host);

}
