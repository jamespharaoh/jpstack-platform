$(function () {

	var cookieName = "txt2_console";
	var cookieDuration = 365;

	var consoleSelected =
		Cookies.get (cookieName) || "current";

	function main () {

		$(".core-system-switch-mode input").each (function () {
			if ($(this).val () == consoleSelected) {
				$(this).prop ("checked", true);
			}
		});

		$(".core-system-switch-mode").click (consoleUpdate);

	}

	function consoleUpdate () {

		var radio =
			$(this).find ("input");

		radio.prop ("checked", true);

		consoleSelected =
			radio.val ();

		Cookies.set (
			cookieName,
			consoleSelected,
			{ expires: cookieDuration });

	}

	main ();

});

// ex: noet ts=4 filetype=javascript