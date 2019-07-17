<!DOCTYPE html>
<html>
<head>
<title>Game</title>
<link href="/webjars/bootstrap/css/bootstrap.min.css" rel="stylesheet">
<link href="/main.css" rel="stylesheet">
<script src="/webjars/jquery/jquery.min.js"></script>
<script src="/webjars/sockjs-client/sockjs.min.js"></script>
<script src="/webjars/stomp-websocket/stomp.min.js"></script>
<script src="/app.js"></script>
</head>
<body>
    <input type="hidden" id="gameId" value="${gameId}">
    <input type="hidden" id="teamId" value="${teamId}">
	<noscript>
		<h2 style="color: #ff0000">Seems your browser doesn't support
			JavaScript! Websockets rely on JavaScript being enabled. Please
			enable JavaScript and try again</h2>
	</noscript>
	<br>
	<br>
	<div class="container-fluid">
		<div class="row">
			<div class="col-lg-1 col-sm-2"></div>
			<div class="col-lg-10 col-sm-8">
				<canvas id="canvas" style="border: 1px solid lightgrey;"
					width="3500">
    Your browser does not support the HTML5 canvas tag. Try with Firefox or Chrome.
</canvas>
			</div>
			<div class="col-lg-1 col-sm-2"></div>
		</div>
	</div>
</body>
</html>