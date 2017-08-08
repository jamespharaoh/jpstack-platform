package wbs.platform.media.fixture;

import static wbs.utils.collection.IterableUtils.iterableMapToList;

import java.util.List;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.platform.media.model.MediaObjectHelper;
import wbs.platform.media.model.MediaRec;

@SingletonComponent ("mediaFixtureLogic")
public
class MediaFixtureLogicImplementation
	implements MediaFixtureLogic {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	MediaObjectHelper mediaHelper;

	// implementation

	@Override
	public
	List <MediaRec> testImages (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"testImages");

		) {

			return iterableMapToList (
				MediaTestImagesFixtureProvider.testMediaIds,
				mediaId ->
					mediaHelper.findRequired (
						transaction,
						mediaId));

		}

	}

}
