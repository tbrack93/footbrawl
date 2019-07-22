
var canvas;
var background;
var squares;
// below used to determine canvas absolute pixel locations
var canvasLeft;
var canvasTop;
var players;
var lastSquareClicked;
var timeSinceClick;
var debounceInterval = 200;
var game;
var team;
var activePlayer;
var yourTurn;
var route;
var inRoute;
var waypoints;
var xIncrement;
var yIncrement;

var requestAnimationFrame = window.requestAnimationFrame || 
window.mozRequestAnimationFrame || 
window.webkitRequestAnimationFrame || 
window.msRequestAnimationFrame;

window.onload = init;
document.addEventListener("keydown", escCheck);

function init() {
	yourTurn = true; // just for testing
	players = new Array();
	waypoints = new Array();
	route = new Array();
	canvas = document.getElementById("canvas");
	context = canvas.getContext("2d");
	canvas.height = canvas.width * (15 / 26);
	canvasLeft = canvas.offsetLeft;
	background = document.getElementById("backgroundCanvas");
	background.height = background.width * (15/26);
	squares = document.getElementById("squaresCanvas");
	squares.height = squares.width * (15/26);
	canvasTop = canvas.offsetTop;
	drawBoard();
	timeSinceClick = new Date();
// var player = {id: 1, team: 1, name:"John", location: [0, 1]};
// var player2 = {id: 2, team: 2, name:"Bobby", location: [7,5]};
// var player3 = {id: 3, team: 2, name:"Sam", location: [7,7]};
// var player4 = {id: 4, team: 1, name:"Sarah", location: [5,3]};
// players.push(player);
// players.push(player2);
// players.push(player3);
// players.push(player4);
// drawPlayers();
	    
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
	var backgroundCtx = background.getContext("2d");
	// size of canvas
	var cw = background.width;
	var ch = background.height;

	var squareH = ch / 15;
	backgroundCtx.globalAlpha = 1;
	backgroundCtx.lineWidth = 0.6;

	for (var x = 0; x <= cw; x += squareH) {
		backgroundCtx.moveTo(0.5 + x, 0);
		backgroundCtx.lineTo(0.5 + x, ch);
	}

	for (var x = 0; x <= ch; x += squareH) {
		backgroundCtx.moveTo(0, 0.5 + x);
		backgroundCtx.lineTo(cw, 0.5 + x);
	}
	
	backgroundCtx.strokeStyle = "white";
	backgroundCtx.stroke();
}

function drawPlayers(){
	context.clearRect(0, 0, canvas.width, canvas.height);
	players.forEach(player => {
		drawPlayer(player);
	});
}

function drawPlayer(player) {
	  var img = new Image();
	  img.src = player.imgUrl;
	  img.onload = function() {
		  context.globalAlpha = 1;
			var column = player.location[0];
			var row = 14 -player.location[1];
			var squareH = canvas.height / 15;
			context.clearRect(column * squareH-5, row * squareH-5, squareH+10,squareH+10);
				context.drawImage(img, column * squareH, row * squareH, squareH,
						squareH);	
				context.strokeStyle = "white";
				var line = 3;
				if(player == activePlayer){
					line = 8;
				}
				context.lineWidth = line;
				context.strokeRect(column * squareH, row * squareH, squareH,
						squareH);
	  }
}


function drawMovementSquare(tile){
	    squareCtx = squares.getContext("2d");
	    squareCtx.save();
	    squareCtx.globalAlpha = 0.4;
	    var squareH = squares.height/15;
        var colour = "blue";
        if(tile.tackleZones != null){
        	colour = "red";
        	// need to add logic for number of tacklezones
        }
        squareCtx.fillStyle = colour;
	    var column = tile.position[0];
	    var row = 14 - tile.position[1];
	    squareCtx.fillRect(column*squareH+3, row*squareH+3 , squareH-5, squareH-5);
	    if(tile.tackleZones !=null){
	    	squareCtx.globalAlpha = 1;
	    	squareCtx.fillStyle = "white";
	    	squareCtx.font = "30px Arial";
	    	squareCtx.fillText(tile.tackleZones, column*squareH +20, row*squareH +30);
	    }
	    if(tile.goingForItRoll != null){
	    	squareCtx.globalAlpha = 1;
	    	squareCtx.fillStyle = "white";
	    	squareCtx.font = "30px Arial";
	    	squareCtx.fillText("GFI",column*squareH + squareH/3, row*squareH + squareH/2+10);
	    }
	    if(tile.dodgeRoll != null){
	    	squareCtx.globalAlpha = 1;
	    	squareCtx.textAlign = "center"; 
	    	squareCtx.fillStyle = "white";
	    	squareCtx.font = "bold 30px Arial";
	    	squareCtx.fillText("Dodge", column*squareH+ squareH/2, row*squareH + squareH/2+10);
	    	squareCtx.fillText(tile.dodgeRoll + "+", column*squareH+ squareH/2, row*squareH + squareH/1.25)
	    }
	    squareCtx.restore(); 
	}

function drawRouteSquare(tile){
    squareCtx = squares.getContext("2d");
    squareCtx.save();
    squareCtx.globalAlpha = 0.5;
    var squareH = canvas.height/15;
    var colour = "white";
    if(tile.dodgeRoll != null || tile.goingForItRoll != null){
    	colour = "orange";
    }
    squareCtx.fillStyle = colour;
    var column = tile.position[0];
    var row = 14 - tile.position[1];
    squareCtx.fillRect(column*squareH+3, row*squareH+3 , squareH-5, squareH-5);
    if(tile.goingForItRoll != null){
    	squareCtx.globalAlpha = 1;
    	squareCtx.fillStyle = "white";
    	squareCtx.textAlign = "center"; 
    	squareCtx.font = "bold 30px Arial";
    	squareCtx.fillText("GFI: " + tile.goingForItRoll + "+",column*squareH + squareH/2, row*squareH + squareH/4);
    }
    if(tile.dodgeRoll != null){
    	squareCtx.globalAlpha = 1;
    	squareCtx.textAlign = "center"; 
    	squareCtx.fillStyle = "white";
    	squareCtx.font = "bold 30px Arial";
    	squareCtx.fillText("Dodge:", column*squareH+ squareH/2, row*squareH + squareH/2+10);
    	squareCtx.fillText(tile.dodgeRoll + "+", column*squareH+ squareH/2, row*squareH + squareH/1.25)
    }
    squareCtx.restore();
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
	} else if(message.type == "ACTION"){
	    if(message.action == "ROUTE"){
		  showMoved(message);
	  }
	}
}

function showMovement(message){
	squares.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
	message.squares.forEach(tile => {
		drawMovementSquare(tile);
	});
	if(inRoute == true){
	  route.forEach(tile => {
	    drawRouteSquare(tile);
	  });
	} else{
	activePlayer.movement = message.squares;
	}
}

function updateTeamDetails(message){
	console.log("in team details");
	document.getElementById("team1Name").innerHTML = message.team1Name;
	document.getElementById("team2Name").innerHTML = message.team2Name;
	message.team1.forEach(player =>{
		players.push(player);
	});
	message.team2.forEach(player =>{
		players.push(player);
	});
	drawPlayers();
}

function actOnClick(click){
	var square = determineSquare(click);
	var done = false;
	players.forEach(player => {
		 // console.log("checking");
		 if(player.location[0] == square[0] && player.location[1] == square[1]) {
			 if(player == activePlayer){
				 resetMovement();
				 done = true; // needed as return just escapes forEach block
				 return;
			 } else{	
			   var pTemp = activePlayer;
			   activePlayer = player;
			   if(pTemp != null){
			     drawPlayer(pTemp);
			   }
			   drawPlayer(activePlayer);
			   inRoute = false;
			   waypoints.length = 0;
			   route.length = 0;
			   
			   // console.log(player);
			   stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
			                 JSON.stringify({"type": "INFO", "action": "MOVEMENT", "player": player.id,
			                 "location": player.location, "routeMACost": 0}));
			   done = true;
			   return;
			 }
		 } // will be more options for blitz/ block/ throw actions
	 });
	if(done == false && activePlayer != null && activePlayer.team == team && yourTurn == true){ 
		if(lastSquareClicked != null && square[0] == lastSquareClicked[0] && square[1] == lastSquareClicked[1]){
			var messageRoute = new Array();
			route.forEach(tile => {
				var t = tile.position;
				messageRoute.push(t);
			});
			  stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
	                    JSON.stringify({"type": "ACTION", "action": "ROUTE", "player": activePlayer.id, 
	        	        "location": activePlayer.location, "route": messageRoute}));
		}
		else if(playerCanReach(square) && route.length <= activePlayer.remainingMA +2){
		   stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
		                    JSON.stringify({"type": "INFO", "action": "ROUTE", "player": activePlayer.id, 
		        	        "location": activePlayer.location, "target": square, "waypoints": waypoints}));
		   lastSquareClicked = square;
		} else{
			console.log("player can't reach that square");
		}
		
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
	route = message.route;
	if(route.length > 0){
	  if(inRoute == false){
	  	  waypoints.length = 0;
		  inRoute = true;
	  }
	  squares.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
	  var location = route[route.length -1].position;
	  
	  waypoints.push(location);
	  stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
	         JSON.stringify({"type": "INFO", "action": "MOVEMENT", "player": activePlayer.id,
	                         "location": location, "routeMACost": message.routeMACost}));
	} // else say it's empty
}

function showMoved(message){
	if(message.route.length >1){
		route = message.route;
		console.log(route.length)
	    var player = getPlayerById(message.player);
	    var playerImg = new Image();
		playerImg.src = player.imgUrl;
	    var squareH = canvas.height / 15;
	    squares.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
	    var startingX = route[0].position[0] * squareH;
		var startingY = (14 - route[0].position[1]) * squareH;
		var targetX = route[1].position[0] * squareH;
		var targetY = (14 - route[1].position[1]) * squareH;
		xIncrement = (targetX - startingX) / 10;
	    yIncrement = (targetY - startingY) / 10;
		context.clearRect(startingX-5, startingY-5, squareH+5, squareH+5)
		animateMovement(message.route, 0, playerImg, startingX, startingY, targetX, targetY, squareH, xIncrement, yIncrement); 
	    player.location = route[route.length-1].position;
	    waypoints.length = 0;
	    inRoute = false;
	    stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
		         JSON.stringify({"type": "INFO", "action": "MOVEMENT", "player": player.id,
		                         "location": player.location, "routeMACost": 0}));
	}
}

function animateMovement(route, counter, playerImg, startingX, startingY, targetX, targetY, squareH){
	context.clearRect(startingX, startingY, squareH, squareH);
	var newX = startingX + xIncrement;
	var newY = startingY + yIncrement;
	context.drawImage(playerImg, newX, newY, squareH, squareH);
	if(Math.round(newX) == Math.round(targetX) && Math.round(newY) == Math.round(targetY)){
		console.log(route.length);
		console.log(counter);
		if(counter == route.length-1){
			route.length = 0;
			return;
		}
		console.log("there");
		counter++;
	    targetX = route[counter].position[0] * squareH;
	    targetY = (14 - route[counter].position[1]) * squareH;
		xIncrement = (targetX - newX) / 10;
	    yIncrement = (targetY - newY) / 10;
	}
	requestAnimationFrame(function() { animateMovement(route, counter, playerImg, newX, newY, targetX, targetY, squareH); });	

}

function escCheck (e) {
	console.log("keyPress Time");
    if(e.keyCode === "Escape" || e.keyCode === "Esc" || e.keyCode === 27) {
    	console.log("escape");
    	if(activePlayer != null && activePlayer.movement != null){
    		resetMovement();
        }
    }
}

function resetMovement(){
	inRoute = false;
	waypoints.length = 0;
	route.length = 0;
	squares.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
	if(activePlayer.movement.length>0){
	  activePlayer.movement.forEach(tile => {
		drawMovementSquare(tile);
	  });
	}
}

function playerCanReach(square){
	var result = false;
	activePlayer.movement.forEach(tile => {
		if(tile.position[0] == square[0] && tile.position[1] == square[1]){
			result = true;
		}
	});
	return result;
}

function getPlayerById(id){
	for(i = 0; i< players.length ; i++){
		if(players[i].id == id){
			return players[i]
		}
	}
}

function sleep(milliseconds) {
	  var start = new Date().getTime();
	  for (var i = 0; i < 1e7; i++) {
	    if ((new Date().getTime() - start) > milliseconds){
	      break;
	    }
	  }
	}