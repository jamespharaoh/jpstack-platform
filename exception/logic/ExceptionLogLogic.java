package wbs.platform.exception.logic;

import com.google.common.base.Optional;

import wbs.framework.database.Transaction;
import wbs.framework.exception.GenericExceptionResolution;

import wbs.platform.exception.model.ExceptionLogRec;

public
interface ExceptionLogLogic {

	ExceptionLogRec logException (
			Transaction parentTransaction,
			CharSequence typeCode,
			CharSequence source,
			CharSequence summary,
			CharSequence dump,
			Optional <Long> userId,
			GenericExceptionResolution resolution);

}