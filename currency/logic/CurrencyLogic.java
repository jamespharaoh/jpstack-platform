package wbs.platform.currency.logic;

import com.google.common.base.Optional;

import wbs.platform.currency.model.CurrencyRec;

public
interface CurrencyLogic {

	String formatSimple (
			CurrencyRec currency,
			Long amount);

	String formatText (
			CurrencyRec currency,
			Long amount);

	String formatHtml (
			CurrencyRec currency,
			Long amountRaw);

	String formatHtmlTd (
			CurrencyRec currency,
			Long credit);

	Optional <Long> parseText (
			CurrencyRec currency,
			String text);

	Long parseTextRequired (
			CurrencyRec currency,
			String text);

	Double toFloat (
			CurrencyRec currency,
			Long amount);

}
