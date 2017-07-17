package wbs.platform.deployment.fixture;

import static wbs.utils.collection.MapUtils.mapItemForKeyRequired;
import static wbs.utils.collection.SetUtils.emptySet;
import static wbs.utils.string.CodeUtils.simplifyToCodeRequired;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.config.WbsConfig;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.GlobalId;
import wbs.framework.fixtures.FixtureProvider;
import wbs.framework.fixtures.FixturesLogic;
import wbs.framework.fixtures.TestAccounts;
import wbs.framework.logging.LogContext;

import wbs.platform.deployment.model.ConsoleDeploymentObjectHelper;
import wbs.platform.event.logic.EventFixtureLogic;
import wbs.platform.menu.model.MenuGroupObjectHelper;
import wbs.platform.menu.model.MenuItemObjectHelper;
import wbs.platform.scaffold.model.RootObjectHelper;

@PrototypeComponent ("deploymentFixtureProvider")
public
class DeploymentFixtureProvider
	implements FixtureProvider {

	// singleton dependencies

	@SingletonDependency
	ConsoleDeploymentObjectHelper consoleDeploymentHelper;

	@SingletonDependency
	EventFixtureLogic eventFixtureLogic;

	@SingletonDependency
	FixturesLogic fixturesLogic;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	MenuGroupObjectHelper menuGroupHelper;

	@SingletonDependency
	MenuItemObjectHelper menuItemHelper;

	@SingletonDependency
	RootObjectHelper rootHelper;

	@SingletonDependency
	TestAccounts testAccounts;

	@SingletonDependency
	WbsConfig wbsConfig;

	// implementation

	@Override
	public
	void createFixtures (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createFixtures");

		) {

			createMenuItems (
				transaction);

			createConsoleDeployments (
				transaction);

		}

	}

	private
	void createMenuItems (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createMenuItems");

		) {

			menuItemHelper.insert (
				transaction,
				menuItemHelper.createInstance ()

				.setMenuGroup (
					menuGroupHelper.findByCodeRequired (
						transaction,
						GlobalId.root,
						wbsConfig.defaultSlice (),
						"internal"))

				.setCode (
					"deployment")

				.setName (
					"Deployment")

				.setDescription (
					"")

				.setLabel (
					"Deployments")

				.setTargetPath (
					"/deployment")

				.setTargetFrame (
					"main")

			);

		}

	}

	private
	void createConsoleDeployments (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createConsoleDeployments");

		) {

			testAccounts.forEach (
				"console-deployment",
				suppliedParams -> {

				Map <String, String> allParams =
					ImmutableMap.<String, String> builder ()

					.putAll (
						suppliedParams)

					.put (
						"code",
						simplifyToCodeRequired (
							mapItemForKeyRequired (
								suppliedParams,
								"name")))

					.build ()

				;

				eventFixtureLogic.createRecordAndEvents (
					transaction,
					"Deployment",
					consoleDeploymentHelper,
					rootHelper.findRequired (
						transaction,
						0l),
					allParams,
					emptySet ());

			});

		}

	}

}
