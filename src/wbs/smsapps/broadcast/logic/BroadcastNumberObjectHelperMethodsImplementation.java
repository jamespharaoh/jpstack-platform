package wbs.smsapps.broadcast.logic;

import static wbs.utils.collection.CollectionUtils.collectionSize;
import static wbs.utils.collection.CollectionUtils.listItemAtIndexRequired;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsPresent;

import java.util.List;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import lombok.NonNull;

import wbs.framework.component.annotations.WeakSingletonDependency;

import wbs.sms.number.core.model.NumberRec;

import wbs.smsapps.broadcast.model.BroadcastNumberObjectHelper;
import wbs.smsapps.broadcast.model.BroadcastNumberObjectHelperMethods;
import wbs.smsapps.broadcast.model.BroadcastNumberRec;
import wbs.smsapps.broadcast.model.BroadcastNumberState;
import wbs.smsapps.broadcast.model.BroadcastRec;

public
class BroadcastNumberObjectHelperMethodsImplementation
	implements BroadcastNumberObjectHelperMethods {

	// singleton dependencies

	@WeakSingletonDependency
	BroadcastNumberObjectHelper broadcastNumberHelper;

	// implementation

	@Override
	public
	BroadcastNumberRec findOrCreate (
			@NonNull BroadcastRec broadcast,
			@NonNull NumberRec number) {

		// find existing

		BroadcastNumberRec broadcastNumber =
			broadcastNumberHelper.find (
				broadcast,
				number);

		if (broadcastNumber != null)
			return broadcastNumber;

		// create new

		broadcastNumber =
			broadcastNumberHelper.insert (
				broadcastNumberHelper.createInstance ()

			.setBroadcast (
				broadcast)

			.setNumber (
				number)

			.setState (
				BroadcastNumberState.removed)

		);

		// update broadcast

		broadcast

			.setNumRemoved (
				broadcast.getNumRemoved () + 1);

		// return

		return broadcastNumber;

	}

	@Override
	public
	List <BroadcastNumberRec> findOrCreateMany (
			@NonNull BroadcastRec broadcast,
			@NonNull List <NumberRec> numbers) {

		ImmutableList.Builder <BroadcastNumberRec> broadcastNumbersBuilder =
			ImmutableList.builder ();

		// find existing

		List <Optional <BroadcastNumberRec>> broadcastNumbers =
			broadcastNumberHelper.findMany (
				broadcast,
				numbers);

		// create new

		for (
			long index = 0;
			index < collectionSize (numbers);
			index ++
		) {

			Optional <BroadcastNumberRec> broadcastNumberOptional =
				listItemAtIndexRequired (
					broadcastNumbers,
					index);

			if (
				optionalIsPresent (
					broadcastNumberOptional)
			) {

				broadcastNumbersBuilder.add (
					optionalGetRequired (
						broadcastNumberOptional));

			} else {

				NumberRec number =
					listItemAtIndexRequired (
						numbers,
						index);

				broadcastNumbersBuilder.add (
					broadcastNumberHelper.insert (
						broadcastNumberHelper.createInstance ()

					.setBroadcast (
						broadcast)

					.setNumber (
						number)

					.setState (
						BroadcastNumberState.removed)

				));

				broadcast

					.setNumRemoved (
						broadcast.getNumRemoved () + 1);

			}

		}

		// return

		return broadcastNumbersBuilder.build ();

	}

}