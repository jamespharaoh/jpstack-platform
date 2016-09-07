package wbs.clients.apn.chat.infosite.api;

import static wbs.framework.utils.etc.NumberUtils.toJavaIntegerRequired;
import static wbs.framework.utils.etc.StringUtils.stringEqualSafe;
import static wbs.framework.utils.etc.StringUtils.stringNotEqualSafe;

import java.io.IOException;
import java.io.OutputStream;

import wbs.clients.apn.chat.infosite.model.ChatInfoSiteObjectHelper;
import wbs.clients.apn.chat.infosite.model.ChatInfoSiteRec;
import wbs.clients.apn.chat.user.core.model.ChatUserRec;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.web.AbstractResponder;
import wbs.framework.web.RequestContext;
import wbs.platform.media.model.MediaRec;

@PrototypeComponent ("chatInfoSiteImageResponder")
public
class ChatInfoSiteImageResponder
	extends AbstractResponder {

	// dependencies

	@SingletonDependency
	ChatInfoSiteObjectHelper chatInfoSiteHelper;

	@SingletonDependency
	RequestContext requestContext;

	// state

	ChatInfoSiteRec infoSite;
	MediaRec media;
	byte[] data;

	// implementation

	@Override
	protected
	void prepare () {

		infoSite =
			chatInfoSiteHelper.findRequired (
				requestContext.requestIntegerRequired (
					"chatInfoSiteId"));

		if (
			stringNotEqualSafe (
				infoSite.getToken (),
				requestContext.requestStringRequired (
					"chatInfoSiteToken"))
		) {

			throw new RuntimeException (
				"Token mismatch");

		}

		Long index =
			requestContext.requestIntegerRequired (
				"chatInfoSiteIndex");

		ChatUserRec chatUser =
			infoSite.getOtherChatUsers ().get (
				toJavaIntegerRequired (
					index));

		if (chatUser == null) {

			throw new RuntimeException (
				"Index out of bounds");

		}

		String mode =
			requestContext.requestStringRequired (
				"chatInfoSiteMode");

		if (
			stringEqualSafe (
				mode,
				"full")
		) {

			media =
				chatUser.getMainChatUserImage ().getFullMedia ();

		} else {

			media =
				chatUser.getMainChatUserImage ().getMedia ();

		}

		data =
			media.getContent ().getData ();

	}

	@Override
	protected
	void goHeaders () {

		requestContext.setHeader (
			"Content-Type",
			media.getMediaType ().getMimeType ());

		requestContext.setHeader (
			"Content-Length",
			Integer.toString (
				data.length));

	}

	@Override
	protected
	void goContent ()
		throws IOException {

		OutputStream out =
			requestContext.outputStream ();

		out.write (
			data);

	}

}
