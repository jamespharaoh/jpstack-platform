package wbs.platform.media.console;

import static wbs.utils.etc.LogicUtils.ifThenElse;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.string.StringUtils.bytesToString;
import static wbs.utils.string.StringUtils.joinWithSemicolonAndSpace;
import static wbs.utils.string.StringUtils.stringEqualSafe;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.string.StringUtils.stringIsEmpty;
import static wbs.utils.string.StringUtils.stringIsNotEmpty;
import static wbs.utils.string.StringUtils.utf8ToString;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import lombok.NonNull;

import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.platform.media.logic.MediaLogic;
import wbs.platform.media.model.MediaRec;
import wbs.platform.media.model.MediaTypeRec;

import wbs.utils.string.FormatWriter;

@SingletonComponent ("mediaConsoleLogic")
public
class MediaConsoleLogicImplementation
	implements MediaConsoleLogic {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	MediaConsoleHelper mediaHelper;

	@SingletonDependency
	MediaLogic mediaLogic;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	// constants

	public final static
	String PLAYER_COUNT_KEY =
		"MEDIA_CONSOLE_MODULE_PLAYER_COUNT";

	// implementation

	@Override
	public
	String mediaUrl (
			@NonNull Transaction parentTransaction,
			@NonNull MediaRec media) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"mediaUrl");

		) {

			if (
				mediaLogic.isImage (
					media)
			) {

				return stringFormat (
					"%s",
					mediaHelper.getDefaultContextPath (
						transaction,
						media),
					"/media.image");

			} else {

				throw new RuntimeException (
					stringFormat (
						"Don't know how to create media URL for %s",
						media.getMediaType ().getMimeType ()));

			}

		}

	}

	@Override
	public
	void writeMediaContent (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter,
			@NonNull MediaRec media,
			@NonNull String rotate) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"writeMediaContent");

		) {

			String mimeType =
				media.getMediaType ().getMimeType ();

			if (
				mediaLogic.isText (
					mimeType)
			) {

				formatWriter.writeFormat (
					"%h",
					bytesToString (
						media.getContent ().getData (),
						media.getEncoding ()));

			} else if (
				mediaLogic.isVideo (
					mimeType)
			) {

				Long playerCount =
					requestContext.requestIntegerRequired (
						PLAYER_COUNT_KEY);

				if (playerCount == null) {

					formatWriter.writeLineFormat (
						"<script",
						" src=\"/flowplayer-3.1.2.min.js\"",
						"></script>");

					playerCount =
						0l;

				}

				formatWriter.writeLineFormat (
					"<a",

					" href=\"%h\"",
					stringFormat (
						"%s",
						mediaHelper.getDefaultContextPath (
							transaction,
							media),
						"/media.video"),

					" style=\"%h\"",
					joinWithSemicolonAndSpace (
						"display: block",
						"width: 300px",
						"height: 225px;"),

					" id=\"player%h\"",
					integerToDecimalString (
						playerCount),

					"></a>");

				formatWriter.writeLineFormat (
					"<script",
					" type=\"text/javascript\"",
					">%s</script>\n",
					stringFormat (
						"flowplayer ('%j', '%j', {});\n",
						stringFormat (
							"player%s'",
							integerToDecimalString (
								playerCount)),
						"/flowplayer-3.1.2.swf"));

				requestContext.request (
					PLAYER_COUNT_KEY,
					playerCount + 1);

			} else if (
				mediaLogic.isAudio (mimeType)
			) {

				Long playerCount =
					requestContext.requestIntegerRequired (
						PLAYER_COUNT_KEY);

				if (playerCount == null) {

					formatWriter.writeLineFormat (
						"<script",
						" src=\"/flowplayer-3.1.2.min.js\"",
						"></script>\n");

					playerCount = 0l;

				}

				formatWriter.writeLineFormat (
					"<a",

					" href=\"%h\"",
					stringFormat (
						"%s",
						mediaHelper.getDefaultContextPath (
							transaction,
							media),
						"/media.audio.mp3"),

					" style=\"%h\"",
					joinWithSemicolonAndSpace (
						"display: block",
						"width: 300px",
						"height: 60px;"),

					" id=\"player%s\"",
					integerToDecimalString (
						playerCount),

					"></a>");

				formatWriter.writeFormat (
					"<script type=\"text/javascript\">\n",
					"  flowplayer ('player%s', '/flowplayer-3.1.2.swf', {\n",
					integerToDecimalString (
						playerCount),
					"    plugins: {\n",
					"      audio: {\n",
					"        url: '/flowplayer.audio-3.1.2.swf'\n",
					"      },\n",
					"      controls: {\n",
					"        autoHide: false,\n",
					"      },\n",
					"    },\n",
					"    clip: {\n",
					"      type: 'audio',\n",
					"    }\n,",
					"  });\n",
					"</script>\n");

				requestContext.request (
					PLAYER_COUNT_KEY,
					playerCount + 1);

			} else if (
				mediaLogic.isImage (
					mimeType)
			) {

				formatWriter.writeLineFormat (
					"<img",

					" src=\"%h\"",
					stringFormat (
						"%s",
						mediaHelper.getDefaultContextPath (
							transaction,
							media),
						"/media.image",
						ifThenElse (
							stringIsEmpty (
								rotate),
							() -> "?rotate=" + rotate,
							() -> "")),

					" alt=\"%h\"",
					media.getFilename (),

					">");

			} else {

				formatWriter.writeLineFormat (
					"(unable to display %h)",
					mimeType);

			}

		}

	}

	@Override
	public
	String mediaUrlScaled (
			@NonNull Transaction parentTransaction,
			@NonNull MediaRec media,
			@NonNull Integer width,
			@NonNull Integer height) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"mediaUrlScaled");

		) {

			MediaTypeRec mediaType =
				media.getMediaType ();

			if (
				! mediaLogic.isImage (
					media)
			) {

				throw new RuntimeException (
					stringFormat (
						"Unable to created scaled url for %s",
						mediaType.getMimeType ()));

			}

			return stringFormat (
				"%s",
				mediaHelper.getDefaultContextPath (
					transaction,
					media),
				"/media.imageScale",
				"?width=%u",
				integerToDecimalString (
					width),
				"&height=%u",
				integerToDecimalString (
					height));

		}

	}

	@Override
	public
	void writeMediaContentScaled (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter,
			@NonNull MediaRec media,
			@NonNull Integer width,
			@NonNull Integer height) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"writeMediaContentScaled");

		) {

			MediaTypeRec mediaType =
				media.getMediaType ();

			if (
				mediaLogic.isTextual (
					media)
			) {

				formatWriter.writeLineFormat (
					"<pre",
					" style=\"margin: 0\"",
					">%h</pre>",
					utf8ToString (
						media.getContent ().getData ()));

			} else if (
				mediaLogic.isImage (
					media)
			) {

				formatWriter.writeLineFormat (
					"<img src=\"%h\">",
					mediaUrlScaled (
						transaction,
						media,
						width,
						height));

			} else {

				formatWriter.writeLineFormat (
					"(unable to display %s)",
					mediaType.getMimeType ());

			}

		}

	}

	@Override
	public
	void writeMediaThumb100 (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter,
			@NonNull MediaRec media,
			@NonNull String rotate) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"writeMediaThumb100");

		) {

			if (
				mediaLogic.isText (
					media)
			) {

				formatWriter.writeFormat (
					"%h",
					bytesToString (
						media.getContent ().getData (),
						media.getEncoding ()));

			} else if (
				media.getThumb100Content () == null
			) {

				formatWriter.writeFormat (
					"[%h]",
					media.getMediaType ().getMimeType ());

			} else {

				formatWriter.writeFormat (
					"<img",

					" src=\"%h\"",
					stringFormat (
						"%s",
						mediaHelper.getDefaultContextPath (
							transaction,
							media),
						"/media.thumb100",
						ifThenElse (
							stringIsNotEmpty (
								rotate),
							() -> "?rotate=" + rotate,
							() -> "")),

					" alt=\"%h\"",
					media.getFilename (),

					">");

			}

		}

	}

	@Override
	public
	void writeMediaThumb100OrText (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter,
			@NonNull MediaRec media) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"writeMediaThumb100OrText");

		) {

			if (
				stringEqualSafe (
					media.getMediaType ().getMimeType (),
					"text/plain")
			) {

				formatWriter.writeLineFormat (
					"%h",
					bytesToString (
						media.getContent ().getData (),
						media.getEncoding ()));

			} else {

				writeMediaThumb100 (
					transaction,
					formatWriter,
					media,
					"");

			}

		}

	}

	@Override
	public
	void writeMediaThumb100Rot90 (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter,
			@NonNull MediaRec media) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"writeMediaThumb100Rot90");

		) {

			if (media.getThumb100Content () == null) {

				formatWriter.writeLineFormat (
					"[%h]",
					media.getMediaType ().getMimeType ());

			} else {

				formatWriter.writeLineFormat (
					"<img",

					" src=\"%h\"",
					stringFormat (
						"%s",
						mediaHelper.getDefaultContextPath (
							transaction,
							media),
						"/media.thumb100Rot90"),

					" alt=\"%h\"",
					media.getFilename (),

					">\n");

			}

		}

	}

	@Override
	public
	void writeMediaThumb32 (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter,
			@NonNull MediaRec media) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"writeMediaThumb32");

		) {

			if (media.getThumb32Content () == null) {

				formatWriter.writeLineFormat (
					"[%h]",
					media.getMediaType ().getMimeType ());

			} else {

				formatWriter.writeLineFormat (
					"<img",

					" src=\"%h\"",
					stringFormat (
						"%s",
						mediaHelper.getDefaultContextPath (
							transaction,
							media),
						"/media.thumb32"),

					" alt=\"%h\"",
					media.getFilename (),

					">");

			}

		}

	}

	@Override
	public
	void writeMediaThumb32OrText (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter,
			@NonNull MediaRec media) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"writeMediaThumb32OrText");

		) {

			if (
				stringEqualSafe (
					media.getMediaType ().getMimeType (),
					"text/plain")
			) {

				formatWriter.writeFormat (
					"%h",
					bytesToString (
						media.getContent ().getData (),
						media.getEncoding ()));

			} else {

				writeMediaThumb32 (
					transaction,
					formatWriter,
					media);

			}

		}

	}

	Map <String, String> iconNames =
		ImmutableMap.<String, String> builder ()

		.put (
			"text/plain",
			"txt.png")

		.put (
			"application/smil",
			"xml6.png")

		.build ();

}
