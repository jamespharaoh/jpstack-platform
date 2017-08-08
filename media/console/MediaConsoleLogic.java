package wbs.platform.media.console;

import lombok.NonNull;

import wbs.framework.database.Transaction;

import wbs.platform.media.model.MediaRec;

import wbs.utils.string.FormatWriter;

public
interface MediaConsoleLogic {

	String mediaUrl (
			Transaction parentTransaction,
			MediaRec media);

	String mediaUrlScaled (
			Transaction parentTransaction,
			MediaRec media,
			Integer width,
			Integer height);

	/*
	String mediaThumb100Url (
			MediaRec media);

	String mediaThumb32Url (
			MediaRec media);
	*/

	void writeMediaContent (
			Transaction parentTransaction,
			FormatWriter formatWriter,
			MediaRec media,
			String rotate);

	default
	void writeMediaContent (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter,
			@NonNull MediaRec media) {

		writeMediaContent (
			parentTransaction,
			formatWriter,
			media,
			"");

	}

	void writeMediaContentScaled (
			Transaction parentTransaction,
			FormatWriter formatWriter,
			MediaRec media,
			Integer width,
			Integer height);

	void writeMediaThumb100 (
			Transaction parentTransaction,
			FormatWriter formatWriter,
			MediaRec media,
			String rotate);

	default
	void writeMediaThumb100 (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter,
			@NonNull MediaRec media) {

		writeMediaThumb100 (
			parentTransaction,
			formatWriter,
			media,
			"");

	}

	void writeMediaThumb100OrText (
			Transaction parentTransaction,
			FormatWriter formatWriter,
			MediaRec media);

	void writeMediaThumb100Rot90 (
			Transaction parentTransaction,
			FormatWriter formatWriter,
			MediaRec media);

	void writeMediaThumb32 (
			Transaction parentTransaction,
			FormatWriter formatWriter,
			MediaRec media);

	void writeMediaThumb32OrText (
			Transaction parentTransaction,
			FormatWriter formatWriter,
			MediaRec media);

}
