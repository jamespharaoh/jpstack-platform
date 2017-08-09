package wbs.platform.queue.hibernate;

import java.sql.Types;

import org.hibernate.type.CustomType;
import org.hibernate.type.Type;

import wbs.framework.hibernate.EnumUserType;

import wbs.platform.queue.model.QueueItemClaimStatus;

public
class QueueItemClaimStatusType
	extends EnumUserType<String,QueueItemClaimStatus> {

	{

		sqlType (Types.VARCHAR);
		enumClass (QueueItemClaimStatus.class);

		add ("c", QueueItemClaimStatus.claimed);
		add ("u", QueueItemClaimStatus.unclaimed);
		add ("f", QueueItemClaimStatus.forcedUnclaim);
		add ("p", QueueItemClaimStatus.processed);
		add ("x", QueueItemClaimStatus.cancelled);

	}

	public final static
	Type INSTANCE =
		new CustomType (
			new QueueItemClaimStatusType ());

}
