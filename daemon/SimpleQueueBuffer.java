package wbs.platform.daemon;

import java.util.Set;

public
class SimpleQueueBuffer <Key> {

	QueueBuffer <Key, Key> queueBuffer;

	public
	SimpleQueueBuffer (
			long fullSize) {

		queueBuffer =
			new QueueBuffer <> (
				fullSize);

	}

	public
	long getFullSize () {
		return queueBuffer.getFullSize ();
	}

	public
	void add (
			Key key) {

		queueBuffer.add (
			key,
			key);

	}

	public
	Key next ()
		throws InterruptedException {

		return queueBuffer.next ();

	}

	public
	void remove (
			Key key) {

		queueBuffer.remove (
			key);

	}

	public
	void waitNotFull ()
		throws InterruptedException {

		queueBuffer.waitNotFull ();

	}

	public
	Set<Key> getKeys () {

		return queueBuffer.getKeys ();

	}

}
