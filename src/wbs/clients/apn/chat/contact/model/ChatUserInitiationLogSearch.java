package wbs.clients.apn.chat.contact.model;

import java.io.Serializable;
import java.util.Collection;

import lombok.Data;
import lombok.experimental.Accessors;

import org.joda.time.Interval;

@Accessors (fluent = true)
@Data
public
class ChatUserInitiationLogSearch
	implements Serializable {

	Integer chatId;

	Interval timestamp;

	ChatUserInitiationReason reason;

	Integer monitorUserId;

	boolean filter;

	Collection<Integer> filterChatIds;

}
