package wbs.platform.deployment.logic;

import static wbs.utils.collection.CollectionUtils.collectionDoesNotHaveOneElement;
import static wbs.utils.collection.CollectionUtils.collectionIsNotEmpty;
import static wbs.utils.collection.CollectionUtils.listFirstElementRequired;
import static wbs.utils.string.StringUtils.hyphenToUnderscore;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.string.StringUtils.stringStartsWithSimple;
import static wbs.utils.string.StringUtils.substringFrom;

import java.lang.ProcessBuilder.Redirect;
import java.util.List;

import com.google.common.base.Optional;

import lombok.NonNull;

import org.apache.commons.io.IOUtils;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NormalLifecycleSetup;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.GlobalId;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.deployment.model.ApiDeploymentObjectHelper;
import wbs.platform.deployment.model.ApiDeploymentRec;
import wbs.platform.deployment.model.ConsoleDeploymentObjectHelper;
import wbs.platform.deployment.model.ConsoleDeploymentRec;
import wbs.platform.deployment.model.DaemonDeploymentObjectHelper;
import wbs.platform.deployment.model.DaemonDeploymentRec;

@SingletonComponent ("deploymentLogic")
public
class DeploymentLogicImplementation
	implements DeploymentLogic {

	// singleton dependencies

	@SingletonDependency
	ApiDeploymentObjectHelper apiDeploymentHelper;

	@SingletonDependency
	ConsoleDeploymentObjectHelper consoleDeploymentHelper;

	@SingletonDependency
	DaemonDeploymentObjectHelper daemonDeploymentHelper;

	@ClassSingletonDependency
	LogContext logContext;

	// state

	String gitVersion;

	// life cycle

	@NormalLifecycleSetup
	public
	void setup (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"setup");

		) {

			gitVersion =
				getGitVersion ();

			taskLogger.noticeFormat (
				"Got version from git: %s",
				gitVersion);

			if (
				stringStartsWithSimple (
					"version-",
					gitVersion)
			) {

				gitVersion =
					stringFormat (
						"v%s",
						substringFrom (
							gitVersion,
							8));

			}

		}

	}

	private
	String getGitVersion () {

		try {

			Process tagNameProcess =
				new ProcessBuilder (
					"git",
					"tag",
					"--points-at",
					"HEAD")

				.redirectOutput (
					Redirect.PIPE)

				.start ();

			if (tagNameProcess.waitFor () != 0) {
				return "unknown";
			}

			List <String> tags =
				IOUtils.readLines (
					tagNameProcess.getInputStream (),
					"utf-8");

			if (
				collectionIsNotEmpty (
					tags)
			) {

				return listFirstElementRequired (
					tags);

			}

			Process commitIdProcess =
				new ProcessBuilder (
					"git",
					"rev-parse",
					"--short",
					"HEAD")

				.redirectOutput (
					Redirect.PIPE)

				.start ();

			if (commitIdProcess.waitFor () != 0) {
				return "unknown";
			}

			List <String> commitIdLines =
				IOUtils.readLines (
					commitIdProcess.getInputStream (),
					"utf-8");

			if (
				collectionDoesNotHaveOneElement (
					commitIdLines)
			) {
				throw new RuntimeException ();
			}

			return listFirstElementRequired (
				commitIdLines);

		} catch (Exception exception) {

			throw new RuntimeException (
				exception);

		}

	}

	// accessors

	@Override
	public
	String gitVersion () {
		return gitVersion;
	}

	// implementation

	@Override
	public
	Optional <ApiDeploymentRec> thisApiDeployment (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"thisApiDeployment");

		) {

			return apiDeploymentHelper.findByCode (
				transaction,
				GlobalId.root,
				hyphenToUnderscore (
					System.getenv (
						"WBS_DEPLOYMENT_NAME")));

		}

	}

	@Override
	public
	Optional <ConsoleDeploymentRec> thisConsoleDeployment (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"thisConsoleDeployment");

		) {

			return consoleDeploymentHelper.findByCode (
				transaction,
				GlobalId.root,
				hyphenToUnderscore (
					System.getenv (
						"WBS_DEPLOYMENT_NAME")));

		}

	}

	@Override
	public
	Optional <DaemonDeploymentRec> thisDaemonDeployment (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"thisDaemonDeployment");

		) {

			return daemonDeploymentHelper.findByCode (
				transaction,
				GlobalId.root,
				hyphenToUnderscore (
					System.getenv (
						"WBS_DEPLOYMENT_NAME")));

		}

	}

}
