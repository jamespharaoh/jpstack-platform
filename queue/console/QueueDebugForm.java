package wbs.platform.queue.console;

import lombok.Data;
import lombok.experimental.Accessors;

@Accessors (fluent = true)
@Data
public
class QueueDebugForm {

	Long userId;
	Long sliceId;

	Boolean allItems = false;
	Boolean claimedItems = false;

}
