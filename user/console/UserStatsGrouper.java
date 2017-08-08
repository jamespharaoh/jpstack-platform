package wbs.platform.user.console;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.NonNull;

import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.priv.UserPrivChecker;
import wbs.console.reporting.StatsDataSet;
import wbs.console.reporting.StatsGrouper;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.platform.user.model.UserObjectHelper;
import wbs.platform.user.model.UserRec;

import wbs.utils.string.FormatWriter;

@SingletonComponent ("userStatsGrouper")
public
class UserStatsGrouper
	implements StatsGrouper {

	// singleton dependencies

	@SingletonDependency
	ConsoleObjectManager consoleObjectManager;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	UserPrivChecker privChecker;

	@SingletonDependency
	UserObjectHelper userHelper;

	// implementation

	@Override
	public
	Set <Object> getGroups (
			@NonNull StatsDataSet dataSet) {

		return new HashSet <Object> (
			dataSet.indexValues ().get (
				"userId"));

	}

	@Override
	public
	void writeTdForGroup (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter,
			@NonNull Object group) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"writeTdForGroup");

		) {

			UserRec user =
				userHelper.findRequired (
					transaction,
					(Long)
					group);

			consoleObjectManager.writeTdForObjectMiniLink (
				transaction,
				formatWriter,
				privChecker,
				user);

		}

	}

	@Override
	public
	List <Object> sortGroups (
			@NonNull Transaction parentTransaction,
			@NonNull Set <Object> groups) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"sortGroups");

		) {

			List <UserRec> users =
				new ArrayList<> (
					groups.size ());

			for (
				Object group
					: groups
			) {

				users.add (
					userHelper.findRequired (
						transaction,
						(Long)
						group));

			}

			Collections.sort (
				users);

			ArrayList <Object> ret =
				new ArrayList<> (
					users.size ());

			for (
				UserRec user
					: users
			) {

				ret.add (
					user.getId ());

			}

			return ret;

		}

	}

}
