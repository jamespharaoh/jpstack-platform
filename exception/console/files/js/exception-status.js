$(function () {

	$("#exceptionsRow").hover (
		function () { $(this).addClass ("hover") },
		function () { $(this).removeClass ("hover") });

	$("#exceptionsRow")
		.css ("cursor", "pointer")
		.click (function () {
			top.frames.main.location = "/exceptionLogs";
		});

	wbsStatus.handlerRegister ("exceptions",
	function (data) {

		if (data.fatalExceptions) {

			$("#exceptionsCell").text ([
				String (data.exceptions),
				" exceptions (",
				String (data.fatalExceptions),
				" fatal)",
			].join (""));

			$("#exceptionsRow").addClass ("alert");

			$("#exceptionsRow").show ();

		} else if (data.exceptions) {

			$("#exceptionsCell").text ([
				String (data.exceptions),
				" exceptions",
			].join (""));

			$("#exceptionsRow").removeClass ("alert");

			$("#exceptionsRow").show ();

		} else {

			$("#exceptionsRow").hide ();

		}

	});

});

// ex: noet ts=4 filetype=javascript