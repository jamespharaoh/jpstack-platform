// ensure we are not in a frame

if (window.parent != window) {
	window.parent.location = window.location;
}

// enable form and focus

$(function () {

	$("#login-form input").prop ("disabled", false);
	$(".login-buttons .login-button").prop ("disabled", false);

	$("#login-form .username-input").focus ();

})

// login buttons (dev mode)

$(function () {

	$(".login-buttons .login-button").each (function () {

		var sliceCode =
			$(this).data ("slice-code");

		var userCode =
			$(this).data ("user-code");

		$(this).click (function () {

			$("#login-form .slice-input").val (sliceCode);
			$("#login-form .username-input").val (userCode);
			$("#login-form .password-input").val ("**********");

			$(".login-buttons .login-button").prop ("disabled", true);

			$("#login-form").submit ();

		});

		$(this).removeAttr ("disabled");

	});

});

// ex: noet ts=4 filetype=javascript