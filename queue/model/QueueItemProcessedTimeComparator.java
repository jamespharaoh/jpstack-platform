package wbs.platform.queue.model;

import java.util.Comparator;

public
class QueueItemProcessedTimeComparator
	implements Comparator<QueueItemRec> {

	@Override
	public
	int compare (
			QueueItemRec queueItemLeft,
			QueueItemRec queueItemRight) {

		return queueItemLeft.getProcessedTime ().compareTo (
			queueItemRight.getProcessedTime ());

	}

	public final static
	QueueItemProcessedTimeComparator instance =
		new QueueItemProcessedTimeComparator ();

}
