package wbs.platform.text.model;

import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsPresent;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.string.StringUtils.stringFormatArray;

import com.google.common.base.Optional;

import lombok.NonNull;

import wbs.framework.database.Transaction;

public
interface TextObjectHelperMethods {

	TextRec findOrCreate (
			Transaction parentTransaction,
			String stringValue);

	default
	Optional <TextRec> findOrCreate (
			@NonNull Transaction parentTransaction,
			@NonNull Optional <String> stringValue) {

		if (
			optionalIsPresent (
				stringValue)
		) {

			return optionalOf (
				findOrCreate (
					parentTransaction,
					optionalGetRequired (
						stringValue)));

		} else {

			return optionalAbsent ();

		}

	}

	default
	TextRec findOrCreateFormat (
			@NonNull Transaction parentTransaction,
			@NonNull String ... arguments) {

		return findOrCreate (
			parentTransaction,
			stringFormatArray (
				arguments));

	}

}