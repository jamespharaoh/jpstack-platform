package wbs.platform.currency.fixture;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.config.WbsConfig;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.GlobalId;
import wbs.framework.fixtures.FixtureProvider;
import wbs.framework.logging.LogContext;

import wbs.platform.currency.model.CurrencyObjectHelper;
import wbs.platform.menu.model.MenuGroupObjectHelper;
import wbs.platform.menu.model.MenuItemObjectHelper;
import wbs.platform.scaffold.model.SliceObjectHelper;

@PrototypeComponent ("currencyFixtureProvider")
public
class CurrencyFixtureProvider
	implements FixtureProvider {

	// singleton dependencies

	@SingletonDependency
	CurrencyObjectHelper currencyHelper;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	MenuGroupObjectHelper menuGroupHelper;

	@SingletonDependency
	MenuItemObjectHelper menuItemHelper;

	@SingletonDependency
	SliceObjectHelper sliceHelper;

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

			menuItemHelper.insert (
				transaction,
				menuItemHelper.createInstance ()

				.setMenuGroup (
					menuGroupHelper.findByCodeRequired (
						transaction,
						GlobalId.root,
						wbsConfig.defaultSlice (),
						"system"))

				.setCode (
					"currency")

				.setName (
					"Currency")

				.setDescription (
					"")

				.setLabel (
					"Currencies")

				.setTargetPath (
					"/currencys")

				.setTargetFrame (
					"main")

			);

			currencyHelper.insert (
				transaction,
				currencyHelper.createInstance ()

				.setSlice (
					sliceHelper.findByCodeRequired (
						transaction,
						GlobalId.root,
						wbsConfig.defaultSlice ()))

				.setCode (
					"gbp")

				.setName (
					"GBP")

				.setDescription (
					"Pounds sterling")

				.setDivisions (
					100l)

				.setPrefix (
					"£")

				.setSingularSuffix (
					"")

				.setPluralSuffix (
					"")

			);

			currencyHelper.insert (
				transaction,
				currencyHelper.createInstance ()

				.setSlice (
					sliceHelper.findByCodeRequired (
						transaction,
						GlobalId.root,
						wbsConfig.defaultSlice ()))

				.setCode (
					"usd")

				.setName (
					"USD")

				.setDescription (
					"US Dollars")

				.setDivisions (
					100l)

				.setPrefix (
					"$")

				.setSingularSuffix (
					"")

				.setPluralSuffix (
					"")

			);

			currencyHelper.insert (
				transaction,
				currencyHelper.createInstance ()

				.setSlice (
					sliceHelper.findByCodeRequired (
						transaction,
						GlobalId.root,
						wbsConfig.defaultSlice ()))

				.setCode (
					"credit")

				.setName (
					"Credits")

				.setDescription (
					"Arbitrary credits")

				.setDivisions (
					1l)

				.setPrefix (
					"")

				.setSingularSuffix (
					" credit")

				.setPluralSuffix (
					" credits")

			);

		}

	}

}
