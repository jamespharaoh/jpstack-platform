package wbs.platform.scaffold.logic;

import com.google.common.base.Optional;

import org.joda.time.Instant;

import wbs.framework.database.Transaction;

import wbs.platform.scaffold.model.SliceRec;

public
interface SliceLogic {

	void updateSliceInactivityTimestamp (
			Transaction parentTransaction,
			SliceRec slice,
			Optional <Instant> timestamp);

}
