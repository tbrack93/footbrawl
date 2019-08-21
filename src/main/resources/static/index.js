var team;

function selectTeam(choice) {
	team = choice;
	var humans = document.getElementById("humans");
	var orcs = document.getElementById("orcs");
	var either = document.getElementById("either");
	if(choice == "humans"){
		orcs.style.opacity = 0.3;
		either.style.opacity = 0.3;
		humans.style.opacity = 1;
	} else if(choice == "orcs"){
		orcs.style.opacity = 1;
		either.style.opacity = 0.3;
		humans.style.opacity = 0.3;
	} else {
		orcs.style.opacity = 0.3;
		either.style.opacity = 1;
		humans.style.opacity = 0.3;
	}
}

function join(){
	if(team == null){
		alert("Please choose a team");
		return;
	}
	window.location = "/game/join?team=" + team + "&invite=" + document.getElementById("invite").checked;
}

function showModal(){
	document.getElementById("modal").style.display = "block";
}

function closeModal(){
	document.getElementById("modal").style.display = "none";
	document.getElementById("humans").style.opacity = 1;
	document.getElementById("orcs").style.opacity = 1;
	document.getElementById("either").style.opacity = 1;
	team = null;
}