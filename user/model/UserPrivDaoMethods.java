package wbs.platform.user.model;

import java.util.List;

import wbs.framework.database.Transaction;

import wbs.platform.priv.model.PrivRec;

public
interface UserPrivDaoMethods {

	UserPrivRec find (
			Transaction parentTransaction,
			UserRec user,
			PrivRec priv);

	List <UserPrivRec> find (
			Transaction parentTransaction,
			PrivRec priv);

}