package wbs.platform.user.console;

import java.util.Map;
import java.util.Set;

import com.google.common.base.Optional;

import wbs.console.misc.ConsoleUserHelper;

import wbs.framework.database.Transaction;

import wbs.platform.scaffold.model.SliceRec;
import wbs.platform.user.model.UserRec;

public
interface UserConsoleLogic
	extends ConsoleUserHelper {

	Optional <UserRec> user (
			Transaction parentTransaction);

	UserRec userRequired (
			Transaction parentTransaction);

	Optional <SliceRec> slice (
			Transaction parentTransaction);

	SliceRec sliceRequired (
			Transaction parentTransaction);

	Optional <Long> userId ();
	Long userIdRequired ();

	Optional <Long> sliceId (
			Transaction parentTransaction);

	Long sliceIdRequired (
			Transaction parentTransaction);

	Set <Long> getSupervisorSearchIds (
			Transaction parentTransaction,
			Map <String, Set <String>> conditions);

	Set <Long> getSupervisorFilterIds (
			Transaction parentTransaction);

}
