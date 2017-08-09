package wbs.platform.user.console;

import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalMapRequired;
import static wbs.utils.string.StringUtils.stringToUtf8;

import java.io.Serializable;

import com.google.common.base.Optional;

import lombok.NonNull;

import org.apache.commons.lang3.SerializationUtils;

import wbs.console.request.ConsoleRequestContext;
import wbs.console.session.UserSessionVerifyLogic;

import wbs.framework.database.Transaction;

import wbs.platform.user.model.UserRec;
import wbs.platform.user.model.UserSessionRec;

import wbs.utils.string.StringUtils;

public
interface UserSessionLogic
	extends UserSessionVerifyLogic {

	UserSessionRec userLogon (
			Transaction parentTransaction,
			ConsoleRequestContext requestContext,
			UserRec user,
			Optional <String> userAgent,
			Optional <String> consoleDeploymentCode);

	Optional <UserSessionRec> userLogonTry (
			Transaction parentTransaction,
			ConsoleRequestContext requestContext,
			String sliceCode,
			String username,
			String password,
			Optional <String> userAgent,
			Optional <String> consoleDeploymentCode);

	void userLogoff (
			Transaction parentTransaction,
			UserRec user);

	boolean userSessionVerify (
			Transaction parentTransaction,
			ConsoleRequestContext requestContext);

	Optional <byte[]> userData (
			Transaction parentTransaction,
			UserRec user,
			String code);

	default
	byte[] userDataRequired (
			@NonNull Transaction parentTransaction,
			@NonNull UserRec user,
			@NonNull String code) {

		return optionalGetRequired (
			userData (
				parentTransaction,
				user,
				code));

	}

	void userDataStore (
			Transaction parentTransaction,
			UserRec user,
			String code,
			byte[] value);

	void userDataRemove (
			Transaction parentTransaction,
			UserRec user,
			String code);

	Optional <Serializable> userDataObject (
			Transaction parentTransaction,
			UserRec user,
			String code);

	default
	Serializable userDataObjectRequired (
			@NonNull Transaction parentTransaction,
			@NonNull UserRec user,
			@NonNull String code) {

		return optionalGetRequired (
			userDataObject (
				parentTransaction,
				user,
				code));

	}

	default
	void userDataObjectStore (
			@NonNull Transaction parentTransaction,
			@NonNull UserRec user,
			@NonNull String code,
			@NonNull Serializable value) {

		userDataStore (
			parentTransaction,
			user,
			code,
			SerializationUtils.serialize (
				value));

	}

	default
	Optional <String> userDataString (
			@NonNull Transaction parentTransaction,
			@NonNull UserRec user,
			@NonNull String code) {

		return optionalMapRequired (
			userData (
				parentTransaction,
				user,
				code),
			StringUtils::utf8ToString);

	}

	default
	String userDataStringRequired (
			@NonNull Transaction parentTransaction,
			@NonNull UserRec user,
			@NonNull String code) {

		return optionalGetRequired (
			userDataString (
				parentTransaction,
				user,
				code));

	}

	default
	void userDataStringStore (
			@NonNull Transaction parentTransaction,
			@NonNull UserRec user,
			@NonNull String code,
			@NonNull String value) {

		userDataStore (
			parentTransaction,
			user,
			code,
			stringToUtf8 (
				value));

	}

}
