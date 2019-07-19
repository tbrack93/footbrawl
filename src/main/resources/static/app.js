
var canvas;
// below used to determine canvas absolute pixel locations
var canvasLeft;
var canvasTop;
var players;
var timeSinceClick;
var debounceInterval = 500;
var game;
var team;
var activePlayer;
var yourTurn;
var route;

window.onload = init;
document.addEventListener("keydown", escCheck);

function init() {
	yourTurn = true; // just for testing
	players = new Array();
	canvas = document.getElementById('canvas');
	context = canvas.getContext('2d');
	canvas.height = canvas.width * (15 / 26);
	canvasLeft = canvas.offsetLeft;
	canvasTop = canvas.offsetTop;
	drawBoard();
	timeSinceClick = new Date();
	var player = {id: 1, team: 1, name:"John", location: [0, 1]};
	var player2 = {id: 2, team: 2, name:"Bobby", location: [7,5]};
	var player3 = {id: 3, team: 2, name:"Sam", location: [7,7]};
	var player4 = {id: 4, team: 1, name:"Sarah", location: [5,3]};
	players.push(player);
	players.push(player2);
	players.push(player3);
	players.push(player4);
	drawPlayers();
	    
	    canvas.addEventListener('click', (e) => {
	    	var time = new Date();
	    	if(time - timeSinceClick > debounceInterval){
	    		timeSinceClick = new Date();
	    	    actOnClick(e);
	    	}});
	    
	    game = document.getElementById("gameId").value;
		team = document.getElementById("teamId").value;
		var socket = new SockJS('/messages');
		stompClient = Stomp.over(socket);
		stompClient.connect({}, function (frame) {
		    stompClient.subscribe('/topic/game/'+ game, function (message) {
		    	console.log("Message received");
		    	decodeMessage(JSON.parse(message.body));
	
		    });
		    stompClient.subscribe('/queue/game/'+ game + "/" + team, function (message) {
		    	console.log("Message received");
		    	decodeMessage(JSON.parse(message.body));
		    });
		    stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
		    JSON.stringify({"type": "INFO", action: "TEAMS"}));
		});
		
	}

function drawBoard() {
	context.save();
	// size of canvas
	var cw = canvas.width;
	var ch = canvas.height;

	var squareH = ch / 15;
	context.globalAlpha = 1;
	context.lineWidth = 1.1;

	for (var x = 0; x <= cw; x += squareH) {
		context.moveTo(0.5 + x, 0);
		context.lineTo(0.5 + x, ch);
	}

	for (var x = 0; x <= ch; x += squareH) {
		context.moveTo(0, 0.5 + x);
		context.lineTo(cw, 0.5 + x);
	}
	
	context.strokeStyle = "white";
	console.log(context.globalAlpha);
	console.log(context.lineWidth);
	context.stroke();
	context.restore();
}

function drawPlayers(){
	players.forEach(player => {
		drawPlayer(player);
	});
}

function drawPlayer(player) {
	context.save();
	context.globalAlpha = 1;
	var column = player.location[0];
	var row = 14 -player.location[1];
	var img = new Image();
	var squareH = canvas.height / 15;
	img.src = "/images/human_blitzer.png";
	img.onload = function(){
		context.drawImage(img, column * squareH, row * squareH, squareH,
				squareH);	
		context.strokeStyle = "white";
		var line = 4;
		if(player == activePlayer){
			line = 6;
		}
		context.lineWidth = line;
		context.strokeRect(column * squareH, row * squareH, squareH,
				squareH);
	}
	context.restore();
}

function drawSquare(tile){
	    context.save();
	    context.globalAlpha = 0.3;
	    var squareH = canvas.height/15;
        var colour = "blue";
        if(tile.tackleZones != null){
        	colour = "red";
        	// need to add logic for number of tacklezones
        }
	    context.fillStyle = colour;
	    var column = tile.position[0];
	    var row = 14 - tile.position[1];
	    context.fillRect(column*squareH+3, row*squareH+3 , squareH-5, squareH-5);
	    if(tile.tackleZones !=null){
	    	context.globalAlpha = 1;
		    context.fillStyle = "white";
		    context.font = "30px Arial";
		    context.fillText(tile.tackleZones, column*squareH +20, row*squareH +30);
	    }
	    if(tile.goingForItRoll != null){
	    	context.globalAlpha = 1;
		    context.fillStyle = "white";
		    context.font = "30px Arial";
		    context.fillText("GFI",column*squareH + squareH/3, row*squareH + squareH/2+10);
	    }
	    if(tile.dodgeRoll != null){
	    	context.globalAlpha = 1;
	    	context.textAlign = "center"; 
		    context.fillStyle = "white";
		    context.font = "bold 30px Arial";
		    context.fillText("Dodge", column*squareH+ squareH/2, row*squareH + squareH/2+10);
		    context.fillText(tile.dodgeRoll + "+", column*squareH+ squareH/2, row*squareH + squareH/1.25)
	    }
	    context.restore(); 
	}

function decodeMessage(message){
	console.log("Decoding message");
	if(message.type == "INFO"){
		console.log("in info");
		if(message.action == "MOVEMENT"){
			showMovement(message);
		} else if(message.action == "TEAMS"){
			updateTeamDetails(message);
		} else if(message.action == "ROUTE"){
			showRoute(message);
		}
	}
}

function showMovement(message){
	console.log("in show movement");
	context.clearRect(0, 0, canvas.width, canvas.height);
	message.squares.forEach(tile => {
		drawSquare(tile);
	});
	drawBoard();
	drawPlayers();
	activePlayer.movement = message.squares;
}

function updateTeamDetails(message){
	console.log("in team details");
	document.getElementById("team1Name").innerHTML = message.team1Name;
	document.getElementById("team2Name").innerHTML = message.team2Name;
}

function actOnClick(click){
	var square = determineSquare(click);
	players.forEach(player => {
		 // console.log("checking");
		 if(player.location[0] == square[0] && player.location[1] == square[1]) {
			 if(player != activePlayer){
			   activePlayer = player;
			   // console.log(player);
			   stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
			         JSON.stringify({"type": "INFO", action: "MOVEMENT", "player": player.id,
			                         "location": player.location}));
			   return;
			 }
		 } // else will be for blitz/ block/ throw actions
	 });
	if(activePlayer != null && activePlayer.team == team && yourTurn == true){ 
		 console.log(click);
		 stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
		         JSON.stringify({"type": "INFO", action: "ROUTE", "player": activePlayer.id,
		                         "target": square}));
	}
	 
}

function determineSquare(click){
	var rect = canvas.getBoundingClientRect()
	var squareSize = canvas.offsetWidth/26;
	var clickX = click.clientX - rect.left;
	var clickY = click.clientY - rect.top;
	var position = [parseInt(clickX/squareSize, 10), 14 - parseInt(clickY/squareSize, 10)];
	return position;
}

function showRoute(message){
	context.clearRect(0, 0, canvas.width, canvas.height);
	message.route.forEach(tile => {
		drawSquare(tile);
	});
	drawBoard();
	drawPlayers();
}

function escCheck (e) {
	console.log("keyPressTime");
	console.log(e);
    if(e.keyCode === "Escape" || e.keyCode === "Esc" || e.keyCode === 27) {
    	console.log("escape");
    	if(activePlayer != null && activePlayer.movement != null){
    		context.clearRect(0, 0, canvas.width, canvas.height);
    		activePlayer.movement.forEach(tile => {
    			drawSquare(tile);
    		});
    		drawBoard();
    		drawPlayers();
        }
    }
}