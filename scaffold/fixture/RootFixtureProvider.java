package wbs.platform.scaffold.fixture;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.fixtures.FixtureProvider;
import wbs.framework.logging.LogContext;

import wbs.platform.scaffold.model.RootObjectHelper;

import wbs.utils.random.RandomLogic;

@PrototypeComponent ("rootFixtureProvider")
public
class RootFixtureProvider
	implements FixtureProvider {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	RandomLogic randomLogic;

	@SingletonDependency
	RootObjectHelper rootHelper;

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

			rootHelper.insert (
				transaction,
				rootHelper.createInstance ()

				.setId (
					0l)

				.setCode (
					"root")

				.setFixturesSeed (
					randomLogic.generateLowercase (
						20))

				.setInstallationId (
					randomLogic.randomInteger ())

			);

		}

	}

}
