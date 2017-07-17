package wbs.platform.scaffold.fixture;

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
import wbs.framework.fixtures.FixtureProvider;
import wbs.framework.fixtures.TestAccounts;
import wbs.framework.logging.LogContext;

import wbs.platform.event.logic.EventFixtureLogic;
import wbs.platform.scaffold.model.RootObjectHelper;
import wbs.platform.scaffold.model.SliceObjectHelper;

@PrototypeComponent ("sliceFixtureProvider")
public
class SliceFixtureProvider
	implements FixtureProvider {

	// singleton dependencies

	@SingletonDependency
	EventFixtureLogic eventFixtureLogic;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	RootObjectHelper rootHelper;

	@SingletonDependency
	SliceObjectHelper sliceHelper;

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

			testAccounts.forEach (
				"slice",
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
					"Slice",
					sliceHelper,
					rootHelper.findRequired (
						transaction,
						0l),
					allParams,
					emptySet ());

			});

		}

	}

}
