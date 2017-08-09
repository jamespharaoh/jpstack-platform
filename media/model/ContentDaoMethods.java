package wbs.platform.media.model;

import java.util.List;

import wbs.framework.database.Transaction;

public
interface ContentDaoMethods {

	List <ContentRec> findByShortHash (
			Transaction parentTransaction,
			Long shortHash);

}