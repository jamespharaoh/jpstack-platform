package wbs.platform.feature.console;

import lombok.NonNull;

import wbs.console.feature.FeatureChecker;
import wbs.console.priv.UserPrivChecker;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.GlobalId;
import wbs.framework.logging.LogContext;

import wbs.platform.feature.model.FeatureRec;

@SingletonComponent ("featureChecker")
public
class FeatureCheckerImplementation
	implements FeatureChecker {

	// singleton dependencies

	@SingletonDependency
	FeatureConsoleHelper featureHelper;

	@ClassSingletonDependency
	LogContext logContext;

	// public implementation

	@Override
	public
	boolean checkFeatureAccess (
			@NonNull Transaction parentTransaction,
			@NonNull UserPrivChecker privChecker,
			@NonNull String featureCode) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"checkFeatureAccess");

		) {

			FeatureRec feature =
				featureHelper.findByCodeRequired (
					transaction,
					GlobalId.root,
					featureCode);

			return privChecker.canRecursive (
				transaction,
				feature,
				"view");

		}

	}


}
