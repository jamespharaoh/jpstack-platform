package wbs.platform.queue.hibernate;

import java.sql.Types;

import wbs.framework.hibernate.EnumUserType;

import wbs.platform.queue.model.QueueItemState;

public
class QueueItemStateType
	extends EnumUserType<String,QueueItemState> {

	{

		sqlType (Types.VARCHAR);
		enumClass (QueueItemState.class);

		add ("w", QueueItemState.waiting);
		add ("p", QueueItemState.pending);
		add ("c", QueueItemState.claimed);
		add ("x", QueueItemState.cancelled);
		add ("z", QueueItemState.processed);

	}

}
