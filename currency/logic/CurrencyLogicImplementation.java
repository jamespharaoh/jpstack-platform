package wbs.platform.currency.logic;

import static wbs.utils.etc.Misc.doNothing;
import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.etc.NumberUtils.parseIntegerRequired;
import static wbs.utils.etc.OptionalUtils.optionalOrThrow;
import static wbs.utils.etc.OptionalUtils.presentInstances;
import static wbs.utils.string.StringUtils.joinWithPipe;
import static wbs.utils.string.StringUtils.joinWithoutSeparator;
import static wbs.utils.string.StringUtils.nullIfEmptyString;
import static wbs.utils.string.StringUtils.stringEqualSafe;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.string.StringUtils.stringIsNotEmpty;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Optional;

import lombok.NonNull;

import wbs.framework.component.annotations.SingletonComponent;

import wbs.platform.currency.model.CurrencyRec;

@SingletonComponent ("currencyLogic")
public
class CurrencyLogicImplementation
	implements CurrencyLogic {

	@Override
	public
	String formatSimple (
			@NonNull CurrencyRec currency,
			Long amount) {

		StringBuilder stringBuilder =
			new StringBuilder ();

		stringBuilder.append (
			amount / currency.getDivisions ());

		stringBuilder.append (
			".");

		if (currency.getDivisions () == 100) {

			Long remainder =
				Math.abs (amount % 100);

			if (remainder == 0) {

				stringBuilder.append (
					"00");

			} else if (remainder < 10) {

				stringBuilder.append (
					"0");

				stringBuilder.append (
					remainder);

			} else {

				stringBuilder.append (
					remainder);

			}

		} else {

			throw new RuntimeException ();

		}

		return stringBuilder.toString ();

	}

	@Override
	public
	String formatText (
			@NonNull CurrencyRec currency,
			@NonNull Long amount) {

		StringBuilder stringBuilder =
			new StringBuilder ();

		stringBuilder.append (
			currency.getPrefix ());

		if (amount < 0) {

			stringBuilder.append (
				"-");

		}

		stringBuilder.append (
			Math.abs (
				amount / currency.getDivisions ()));

		if (currency.getDivisions () == 1) {

			doNothing ();

		} else if (currency.getDivisions () == 10) {

			stringBuilder.append (
				".");

			Long remainder =
				Math.abs (amount % 10);

			if (remainder == 0) {

				stringBuilder.append (
					"0");

			} else {

				stringBuilder.append (
					remainder);

			}

		} else if (currency.getDivisions () == 100) {

			stringBuilder.append (
				".");

			Long remainder =
				Math.abs (amount % 100);

			if (remainder == 0) {

				stringBuilder.append (
					"00");

			} else if (remainder < 10) {

				stringBuilder.append (
					"0");

				stringBuilder.append (
					remainder);

			} else {

				stringBuilder.append (
					remainder);

			}

		} else {

			throw new RuntimeException ();

		}

		if (amount == 1 || amount == -1) {

			stringBuilder.append (
				currency.getSingularSuffix ());

		} else {

			stringBuilder.append (
				currency.getPluralSuffix ());

		}


		return stringBuilder.toString ();

	}

	@Override
	public
	String formatHtml (
			@NonNull CurrencyRec currency,
			Long amountRaw) {

		String amountString =
			formatText (
				currency,
				amountRaw);

		return amountRaw < 0

			? stringFormat (
				"<span style=\"color: red\">%h</span>",
				amountString)

			: stringFormat (
				"%h",
				amountString);

	}

	@Override
	public
	String formatHtmlTd (
			@NonNull CurrencyRec currency,
			Long credit) {

		return stringFormat (
			"<td style=\"text-align: right\">%s</td>",
			formatHtml (
				currency,
				credit));

	}

	@Override
	public
	Optional<Long> parseText (
			@NonNull CurrencyRec currency,
			@NonNull String text) {

		// build pattern

		int decimalPlaces;

		if (currency.getDivisions () == 1) {
			decimalPlaces = 0;
		} else if (currency.getDivisions () == 10) {
			decimalPlaces = 1;
		} else if (currency.getDivisions () == 100) {
			decimalPlaces = 2;
		} else {
			throw new RuntimeException ();
		}

		StringBuilder patternBuilder =
			new StringBuilder ();

		patternBuilder.append (
			joinWithoutSeparator (
				"\\s*",
				"(?:",
				Pattern.quote (
					currency.getPrefix ()),
				")?",
				"\\s*"));

		patternBuilder.append (
			joinWithoutSeparator (
				"([-+])?"));

		patternBuilder.append (
			joinWithoutSeparator (
				"(\\d+)"));

		if (decimalPlaces > 0) {

			patternBuilder.append (
				joinWithoutSeparator (
					"\\.",
					"(\\d{" + decimalPlaces + "})"));

		}

		if (

			stringIsNotEmpty (
				currency.getSingularSuffix ().trim ())

			|| stringIsNotEmpty (
				currency.getPluralSuffix ().trim ())

		) {

			patternBuilder.append (
				joinWithoutSeparator (

				"\\s*",
				"(?:",

				joinWithPipe (
					presentInstances (

					Optional.fromNullable (
						nullIfEmptyString (
							Pattern.quote (
								currency.getSingularSuffix ().trim ()))),

					Optional.fromNullable (
						nullIfEmptyString (
							Pattern.quote (
								currency.getPluralSuffix ().trim ())))

				)),

				")\\s*"

			));

		}

		patternBuilder.append (
			"\\s*");

		Pattern pattern =
			Pattern.compile (
				patternBuilder.toString ());

		// perform match

		Matcher matcher =
			pattern.matcher (
				text);

		if (! matcher.matches ()) {

			return Optional.absent ();

		}

		// return the result

		boolean positive =
			stringEqualSafe (
				ifNull (
					matcher.group (1),
					"+"),
				"+");

		long units =
			parseIntegerRequired (
				matcher.group (2));

		long subDivisions =
			decimalPlaces > 0
				? parseIntegerRequired (
					matcher.group (3))
				: 0;

		if (positive) {

			return Optional.of (
				+ units * currency.getDivisions ()
				+ subDivisions);

		} else {

			return Optional.of (
				- units * currency.getDivisions ()
				- subDivisions);

		}

	}

	@Override
	public
	Long parseTextRequired (
			@NonNull CurrencyRec currency,
			@NonNull String text) {

		return optionalOrThrow (
			parseText (
				currency,
				text),
			() -> new IllegalArgumentException (
				stringFormat (
					"Cannot parse \"%s\" as currency \"%s.%s\"",
					text,
					currency.getSlice ().getCode (),
					currency.getCode ())));

	}

	@Override
	public
	Double toFloat (
			@NonNull CurrencyRec currency,
			@NonNull Long amount) {

		return (double) amount
			/ (double) currency.getDivisions ();

	}

}
