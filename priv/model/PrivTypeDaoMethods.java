package wbs.platform.priv.model;

import java.util.List;

import wbs.framework.database.Transaction;

public
interface PrivTypeDaoMethods {

	PrivTypeRec findRequired (
			Transaction parentTransaction,
			Long id);

	List <PrivTypeRec> findAll (
			Transaction parentTransaction);

}