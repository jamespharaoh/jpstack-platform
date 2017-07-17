package wbs.platform.currency.fixture;

import static wbs.utils.collection.MapUtils.mapItemForKeyRequired;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.TypeUtils.dynamicCastRequired;
import static wbs.utils.string.StringUtils.stringIsEmpty;

import java.util.Map;

import lombok.NonNull;

import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.fixtures.FixtureMappingPlugin;

import wbs.platform.currency.logic.CurrencyLogic;
import wbs.platform.currency.model.CurrencyRec;

@SingletonComponent ("currencyFixtureMappingPlugin")
public
class CurrencyFixtureMappingPlugin
	implements FixtureMappingPlugin {

	// singleton dependencies

	@SingletonDependency
	CurrencyLogic currencyLogic;

	// details

	@Override
	public
	String name () {
		return "currency";
	}

	// public implementation

	@Override
	public
	String map (
			@NonNull Map <String, Object> hints,
			@NonNull String inputValue) {

		CurrencyRec currency =
			dynamicCastRequired (
				CurrencyRec.class,
				mapItemForKeyRequired (
					hints,
					"currency"));

		if (
			stringIsEmpty (
				inputValue)
		) {
			return "";
		}

		return integerToDecimalString (
			currencyLogic.parseTextRequired (
				currency,
				inputValue));

	}

}
