function verify() {
  var passes = true;

  var name = document.getElementById('name').value;
  var password1 = document.getElementById('password1').value;
  var password2 = document.getElementById('password2').value;
  var firstName = document.getElementById('first').value;
  var lastName = document.getElementById('last').value;
  var affiliation = document.getElementById('affiliation').value;
  var email = document.getElementById('email').value;
  
  var errorStr = '';

  // Make sure there is at least a name and password
  if (name == '' || password1 == '') {
    errorStr+='* A name and password are required.\n';
    passes = false;
  }

  // Make sure passwords match
  if (password1 != password2) {
    errorStr+='* Passwords don\'t match, please provide matching passwords.\n';
    passes = false;
  }
  
  // Make sure user specified first name
  if (firstName == '') {
    errorStr+='* You must provide a first name.\n';
    passes = false;
  }
  
  // Make sure user specified last name
  if (lastName == '') {
    errorStr+='* You must provide a last name.\n';
    passes = false;
  }
  
  // Make sure user specified an affiliation
  if (affiliation == '') {
    errorStr+='* You must provide an affiliation.\n';
    passes = false;
  }
  
  // Make sure email address
  if (email == '') {
    errorStr+='* You must provide a valid email address or we cannot contact you.\n';
    passes = false;
  }
  
  if (!passes) {
    alert('There were problems with your application:\n\n'+errorStr+'\n\nPlease correct these errors and resubmit.');
  }
  
  return passes;
}

function whatIsAffiliation() {
    alert('Affiliation could be a lab, department and university, employeer, project, or a very brief description of involvement in the field.\n\nYou may specify multiple affiliations, separated by commas.');
    return false;
}

function whatIsUserName() {
    alert('This is the name you will use to log in to certain ProteomeCommons.org services, such as Tranche.');
    return false;
}

function refresh() {
  window.location.reload(true);
  return false;
}
