package wbs.platform.deployment.model;

import java.util.List;

import wbs.framework.database.Transaction;

public
interface DaemonDeploymentDaoMethods {

	List <DaemonDeploymentRec> findByHostNotDeleted (
			Transaction parentTransaction,
			String host);

}
