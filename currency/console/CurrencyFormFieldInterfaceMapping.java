package wbs.platform.currency.console;

import static wbs.utils.etc.NumberUtils.parseIntegerRequired;
import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.etc.OptionalUtils.optionalCast;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsNotPresent;
import static wbs.utils.etc.OptionalUtils.optionalIsPresent;
import static wbs.utils.etc.ResultUtils.errorResult;
import static wbs.utils.etc.ResultUtils.successResult;
import static wbs.utils.etc.ResultUtils.successResultAbsent;
import static wbs.utils.etc.ResultUtils.successResultPresent;
import static wbs.utils.string.StringUtils.stringFormat;

import java.util.Map;

import com.google.common.base.Optional;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.forms.types.FormFieldInterfaceMapping;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;
import wbs.framework.object.ObjectManager;

import wbs.platform.currency.logic.CurrencyLogic;
import wbs.platform.currency.model.CurrencyRec;

import fj.data.Either;

@Accessors (fluent = true)
@PrototypeComponent ("currencyFormFieldInterfaceMapping")
public
class CurrencyFormFieldInterfaceMapping <Container>
	implements FormFieldInterfaceMapping <Container, Long, String> {

	// singleton dependencies

	@SingletonDependency
	CurrencyLogic currencyLogic;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ObjectManager objectManager;

	// properties

	@Getter @Setter
	String currencyPath;

	@Getter @Setter
	Boolean blankIfZero = false;

	// implementation

	@Override
	public
	Either <Optional <Long>, String> interfaceToGeneric (
			@NonNull Transaction parentTransaction,
			@NonNull Container container,
			@NonNull Map <String, Object> hints,
			@NonNull Optional <String> interfaceValue) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"interfaceToGeneric");

		) {

			if (! interfaceValue.isPresent ()) {

				return successResult (
					optionalAbsent ());

			}

			if (interfaceValue.get ().isEmpty ()) {

				return successResult (
					optionalAbsent ());

			}

			Optional <CurrencyRec> currencyOptional =
				optionalCast (
					CurrencyRec.class,
					objectManager.dereference (
						transaction,
						container,
						currencyPath,
						hints));

			if (
				optionalIsNotPresent (
					currencyOptional)
			) {

				try {

					return successResultPresent (
						parseIntegerRequired (
							interfaceValue.get ()));

				} catch (NumberFormatException exception) {

					return errorResult (
						"This currency value must be a whole number");

				}

			}

			CurrencyRec currency =
				optionalGetRequired (
					currencyOptional);

			Optional <Long> parseResult =
				currencyLogic.parseText (
					currency,
					interfaceValue.get ());

			if (
				optionalIsNotPresent (
					parseResult)
			) {

				return errorResult (
					stringFormat (
						"A currency value must be numeric and include the ",
						"appropriate decimal places"));

			}

			return successResult (
				Optional.of (
					currencyLogic.parseTextRequired (
						currency,
						interfaceValue.get ())));

		}

	}

	@Override
	public
	Either <Optional <String>, String> genericToInterface (
			@NonNull Transaction parentTransaction,
			@NonNull Container container,
			@NonNull Map <String, Object> hints,
			@NonNull Optional <Long> genericValue) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"genericToInterface");

		) {

			if (
				optionalIsNotPresent (
					genericValue)
			) {

				return successResultAbsent ();

			}

			if (genericValue.get () == 0 && blankIfZero) {

				return successResult (
					Optional.of (
						""));

			}

			Optional <CurrencyRec> currencyOptional =
				optionalCast (
					CurrencyRec.class,
					objectManager.dereference (
						transaction,
						container,
						currencyPath,
						hints));

			if (
				optionalIsPresent (
					currencyOptional)
			) {

				return successResultPresent (
					currencyLogic.formatText (
						optionalGetRequired (
							currencyOptional),
						genericValue.get ()));

			} else {

				return successResult (
					Optional.of (
						Long.toString (
							genericValue.get ())));

			}

		}

	}

}
