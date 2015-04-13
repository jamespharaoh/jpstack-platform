package wbs.platform.object.core.fixture;

import javax.inject.Inject;

import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.fixtures.FixtureProvider;
import wbs.framework.record.GlobalId;
import wbs.platform.menu.model.MenuGroupObjectHelper;
import wbs.platform.menu.model.MenuItemObjectHelper;
import wbs.platform.menu.model.MenuItemRec;

@PrototypeComponent ("objectTypeFixtureProvider")
public
class ObjectTypeFixtureProvider
	implements FixtureProvider {

	// dependencies

	@Inject
	MenuGroupObjectHelper menuGroupHelper;

	@Inject
	MenuItemObjectHelper menuItemHelper;

	// implementation

	@Override
	public
	void createFixtures () {

		menuItemHelper.insert (
			new MenuItemRec ()

			.setMenuGroup (
				menuGroupHelper.findByCode (
					GlobalId.root,
					"test",
					"system"))

			.setCode (
				"object_type")

			.setName (
				"Object Type")

			.setDescription (
				"Manage database object types")

			.setLabel (
				"Object type")

			.setTargetPath (
				"/objectTypes")

			.setTargetFrame (
				"main")

		);

	}

}
