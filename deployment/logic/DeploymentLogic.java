package wbs.platform.deployment.logic;

import com.google.common.base.Optional;

import wbs.framework.database.Transaction;

import wbs.platform.deployment.model.ApiDeploymentRec;
import wbs.platform.deployment.model.ConsoleDeploymentRec;
import wbs.platform.deployment.model.DaemonDeploymentRec;

public
interface DeploymentLogic {

	//AgentDeploymentRec thisAgentDeployment ();

	Optional <ApiDeploymentRec> thisApiDeployment (
			Transaction parentTransaction);

	Optional <ConsoleDeploymentRec> thisConsoleDeployment (
			Transaction parentTransaction);

	Optional <DaemonDeploymentRec> thisDaemonDeployment (
			Transaction parentTransaction);

	String gitVersion ();

}
