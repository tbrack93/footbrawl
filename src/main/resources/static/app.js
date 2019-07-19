
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

window.onload = init;

function init() {
	players = new Array();
	canvas = document.getElementById('canvas');
	context = canvas.getContext('2d');
	canvas.height = canvas.width * (15 / 26);
	canvasLeft = canvas.offsetLeft;
	canvasTop = canvas.offsetTop;
	drawBoard();
	timeSinceClick = new Date();
	var player = {id: 1, name:"John", location: [0, 1]};
	var player2 = {id: 2, name:"Bobby", location: [7,5]};
	var player3 = {id: 3, name:"Sam", location: [7,7]};
	var player4 = {id: 4, name:"Sarah", location: [5,3]};
	players.push(player);
	players.push(player2);
	players.push(player3);
	players.push(player4);
	drawPlayers();
	    
	    canvas.addEventListener('click', (e) => {
	    	var time = new Date();
	    	if(time - timeSinceClick > debounceInterval){
	    		timeSinceClick = new Date();
	    	    // console.log(e.clientX - canvasLeft);
	    	    // console.log(e.clientY - canvasTop);
	    	  
	    	    players.forEach(player => {
	    		 // console.log("checking");
	    		 if(isIntersect(e, player)) {
	    			 if(player != activePlayer){
	    			   activePlayer = player;
	    			   console.log(player);
	    			   stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
					         JSON.stringify({"type": "INFO", action: "MOVEMENT", "player": player.id,
					                         "location": player.location}));
	    			 }
	    		 }
	    	  });
	    	}});
	    
	    game = document.getElementById("gameId").value;
		team = document.getElementById("teamId").value;
		var socket = new SockJS('/messages');
		stompClient = Stomp.over(socket);
		stompClient.connect({}, function (frame) {
		    stompClient.subscribe('/topic/game/'+ game, function (message) {
		    	console.log("Message received");
		    	decodeMessage(JSON.parse(message.body));
		    	// players.push(JSON.parse(info.body));
		       // draw(JSON.parse(info.body).content.currentColumn,
				// JSON.parse(info.body).content.currentRow);
		        // showGreeting(JSON.parse(info.body).content.name);
		    });
		    stompClient.subscribe('/queue/game/'+ game + "/" + team, function (message) {
		    	console.log("Message received");
		    	decodeMessage(JSON.parse(message.body));
		    	// players.push(JSON.parse(info.body));
		       // draw(JSON.parse(info.body).content.currentColumn,
				// JSON.parse(info.body).content.currentRow);
		        // showGreeting(JSON.parse(info.body).content.name);
		    });
		    stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
		    JSON.stringify({"type": "INFO", action: "TEAMS"}));
		});
		
	}

function drawBoard() {

	// size of canvas
	var cw = canvas.width;
	var ch = canvas.height;

	var squareH = ch / 15;

	for (var x = 0; x <= cw; x += squareH) {
		context.moveTo(0.5 + x, 0);
		context.lineTo(0.5 + x, ch);
	}

	for (var x = 0; x <= ch; x += squareH) {
		context.moveTo(0, 0.5 + x);
		context.lineTo(cw, 0.5 + x);
	}
	context.lineWidth = 0.5;
	// context.strokeStyle = "#696969";
	context.stroke();
}

function drawPlayers(){
	players.forEach(player => {
		drawPlayer(player);
	});
}

function drawPlayer(player) {
	var column = player.location[0];
	var row = 14 -player.location[1];
	var img = new Image();
	var squareH = canvas.height / 15;
	img.src = "/images/human_blitzer.png";
	img.onload = function(){
		context.drawImage(img, column * squareH, row * squareH, squareH,
				squareH);	
		context.strokeStyle = "white";
		var line =3;
		if(player == activePlayer){
			line = 6;
		}
		context.lineWidth = line;
		context.strokeRect(column * squareH, row * squareH, squareH,
				squareH);
	}
}

function drawSquare(tile){
	    context.globalAlpha = 0.3;
	    var squareH = canvas.height/15;
        var colour = "blue";
        if(tile.tackleZones != null){
        	colour = "red";
        	// need to add logic for number of tacklezones
        }
        // need to add logic for showing going for it
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
	}

function isIntersect(click, player){
	var rect = canvas.getBoundingClientRect()
	var squareSize = canvas.offsetWidth/26;
	var playerColumn = player.location[0];
	var playerRow = 14 - player.location[1];
	var clickX = click.clientX - rect.left;
	var clickY = click.clientY - rect.top;
	console.log(clickX);
	console.log(clickY);
	return clickX > (squareSize * playerColumn) &&
	       clickX < (squareSize * (playerColumn +1)) &&
	       clickY > (squareSize * playerRow) &&
	       clickY < (squareSize * (playerRow+1));
	
}

function decodeMessage(message){
	console.log("Decoding message");
	if(message.type == "INFO"){
		console.log("in info");
		if(message.action == "MOVEMENT"){
			showMovement(message);
		} else if(message.action == "TEAMS"){
			updateTeamDetails(message);
		}
	}
}

function showMovement(message){
	console.log("in show movement");
	context.clearRect(0, 0, canvas.width, canvas.height);
	message.squares.forEach(tile => {
		drawSquare(tile);
	});
	drawPlayers();
}

function updateTeamDetails(message){
	console.log("in team details");
	document.getElementById("team1Name").innerHTML = message.team1Name;
	document.getElementById("team2Name").innerHTML = message.team2Name;
}