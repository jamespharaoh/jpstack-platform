package wbs.platform.text.console;

import static wbs.utils.etc.OptionalUtils.optionalOfFormat;

import com.google.common.base.Optional;

import lombok.NonNull;

import wbs.console.helper.core.ConsoleHooks;

import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.database.Transaction;

import wbs.platform.text.model.TextRec;

import wbs.web.utils.HtmlUtils;

@SingletonComponent ("textConsoleHooks")
public
class TextConsoleHooks
	implements ConsoleHooks <TextRec> {

	// implemntation

	@Override
	public
	Optional <String> getHtml (
			@NonNull Transaction parentTransaction,
			@NonNull TextRec text,
			@NonNull Boolean mini) {

		return optionalOfFormat (
			HtmlUtils.htmlEncode (
				"\"" + text.getText () + "\""));

	}

}
