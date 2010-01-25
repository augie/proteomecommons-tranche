/**
 * This will allow user to add new group name if selected in
 * drop-down box
 */
function checkForNewGroup() {
  var groupCheckBox = document.getElementById('group');
  var groupNewNameDiv = document.getElementById('new_group_box');
  if (groupCheckBox.value == 'CREATE') {
    groupNewNameDiv.style.display = 'block';
  } else {
    groupNewNameDiv.style.display = 'none';
  }
}

/**
 * Verifies contents of form before submitting
 */
function verifyNewUsersForm() {

  // Drop-down menu option for group to use or create new group
  var selectedGroupOption = document.getElementById('group').value;

  // Grab the value. Only used if there is supposed to be a new group.
  var newGroup = document.getElementById('new_group').value;
  var newGroupIsEmpty = (newGroup == '');

  // If CREATE selected but no group added, mistake
  if (newGroupIsEmpty && selectedGroupOption == 'CREATE') {
    alert("You selected to create a group but didn't specify a name.\nPlease add a new group name, select an existing name or select to not use a group.");
    return false;
  }

  // Flag for whether any new users
  var isUser = null;

  // Each user is in a div
  var possibleUsers = document.getElementsByTagName('div');

  // Verify new users (need to find them first)
  for (var x = 0; x < possibleUsers.length; x++) {

    // Make sure right class
    if (possibleUsers[x].className == "user") {

      // Flag that there is a user
      isUser = new Boolean(true);

      // Get the ids
      var id = possibleUsers[x].id;
      var name_id = 'name_'+id;
      var pw1_id = 'passphrase_'+id;
      var pw2_id = 'passphrase_confirm_'+id;

      // Use ids to get values
      var name = document.getElementById(name_id).value;
      var pw1 = document.getElementById(pw1_id).value;
      var pw2 = document.getElementById(pw2_id).value;

      // Make sure name isn't blank
      if (!name) {
        alert("One of the users doesn't have a name.\nPlease remove or add a name.");
        return false;
      }

      // Make sure passphrase isn't blank
      if (!pw1) {
        alert(name+"'s passphrase is blank.\nPassphrases are required for new users.");
        return false;
      }

      // Make sure passphrases match
      if (pw1 != pw2) {
        alert(name+"'s passphrases don't match.");
        return false;
      }
    }
  } // Verifying new users

  // For readability
  var isNewGroup = !newGroupIsEmpty && selectedGroupOption == 'CREATE';

  if (isNewGroup && isUser) {
    return confirm('Add new users and new group?');
  } else if (isNewGroup && !isUser) {
    return confirm('Add new group? (There are no new users specified)');
  } else if (!isNewGroup && isUser) {
    return confirm('Add new users? (No new group)');
  } else if (!isNewGroup && !isUser) {
    alert('There are no new users or new group. Nothing to do.');
    return false;
  }

  // Won't get here
  alert('Should not get here');
  return false;
}

/**
 * Adds user fields to DOM
 */
function addAnotherUser() {
  // Need a unique id, will monotomically increase
  var id = getNextUserId();

  // Create and add the user to DOM
  createNewUser(id);

  // Make sure message about no users is hidden
  document.getElementById('no-added-users').style.display = 'none';

  // Always return false to disable the default action
  return false;
}

/**
 * Returns number of userss already in form.
 */
function getUsersCount() {

  var count = 0;

  // Each user is in a div
  var possibleUsers = document.getElementsByTagName('div');

  for (var x = 0; x < possibleUsers.length; x++) {
    // Make sure right class
    if (possibleUsers[x].className == "user") {
      count++;
    }
  }
  return count;
}

/**
 *
 */
function getNextUserId() {

  var next = 0;
  var highest = 0;

  // Each user is in a div
  var possibleUsers = document.getElementsByTagName('div');

  for (var x = 0; x < possibleUsers.length; x++) {
    // Make sure right class
    if (possibleUsers[x].className == "user") {
      next = parseInt(possibleUsers[x].id);
      if (next > highest) {
        highest = next;
      }
    }
  }
  return highest+1;
}

/**
 * Creates and returns new user w/ given id
 */
function createNewUser(id) {

  //var usersForm = document.getElementById('create_users_form');
  var divForUsers = document.getElementById('space_for_users');
 
  // Create the container
  var newUserDiv = document.createElement('div');
  // Yeay, MSIE only understands className.
  newUserDiv.setAttribute('class','user');
  newUserDiv.setAttribute('className','user');
  newUserDiv.setAttribute('id',id);

  // Add a link to remove if desired
  var removeLink = document.createElement('a');
  removeLink.setAttribute('href','none');
  var clickableFunction = new Function("e","return removeUser("+id+");");
  
  removeLink.onclick = clickableFunction;
  removeLink.innerHTML = '[remove this user]';

  // Put link in paragraph
  var removeParagraph = document.createElement('p');
  removeParagraph.appendChild(removeLink);
  newUserDiv.appendChild(removeParagraph);

  // Create hidden input to make submitted form easier to handle
  var hiddenInput = document.createElement('input');
  hiddenInput.setAttribute('type','hidden');
  hiddenInput.setAttribute('name',id);
  hiddenInput.setAttribute('id',id);
  hiddenInput.setAttribute('class','display_none');
  hiddenInput.setAttribute('className','display_none');
  hiddenInput.setAttribute('value','Used for quick way to gather user information by JSP.');
  newUserDiv.appendChild(hiddenInput);

  // Create name
  var nameLabel = document.createElement('label');
  nameLabel.setAttribute('for','name_'+id);
  nameLabel.innerHTML = 'Name';
  newUserDiv.appendChild(nameLabel);

  var nameInput = document.createElement('input');
  nameInput.setAttribute('type','text');
  nameInput.setAttribute('id','name_'+id);
  nameInput.setAttribute('name','name_'+id);
  newUserDiv.appendChild(nameInput);

  // Create passphrase
  var passphraseLabel = document.createElement('label');
  passphraseLabel.setAttribute('for','passphrase_'+id);
  passphraseLabel.innerHTML = 'Passphrase';
  newUserDiv.appendChild(passphraseLabel);

  var passphraseInput = document.createElement('input');
  passphraseInput.setAttribute('type','password');
  passphraseInput.setAttribute('id','passphrase_'+id);
  passphraseInput.setAttribute('name','passphrase_'+id);
  newUserDiv.appendChild(passphraseInput);

  // Confirm passphrase
  var passphraseConfirmLabel = document.createElement('label');
  passphraseConfirmLabel.setAttribute('for','passphrase_confirm_'+id);
  passphraseConfirmLabel.innerHTML = 'Confirm Passphrase';
  newUserDiv.appendChild(passphraseConfirmLabel);

  var passphraseConfirmInput = document.createElement('input');
  passphraseConfirmInput.setAttribute('type','password');
  passphraseConfirmInput.setAttribute('id','passphrase_confirm_'+id);
  passphraseConfirmInput.setAttribute('name','passphrase_confirm_'+id);
  newUserDiv.appendChild(passphraseConfirmInput);

  // Create email
  var emailLabel = document.createElement('label');
  emailLabel.setAttribute('for','email_'+id);
  emailLabel.innerHTML = 'Email (optional)';
  newUserDiv.appendChild(emailLabel);

  var emailInput = document.createElement('input');
  emailInput.setAttribute('type','text');
  emailInput.setAttribute('id','email_'+id);
  emailInput.setAttribute('name','email_'+id);
  newUserDiv.appendChild(emailInput);

  divForUsers.appendChild(newUserDiv);
}

/**
 * Removes a user based on id.
 */
function removeUser(id) {

  var usersSpace = document.getElementById('space_for_users');
  var userToRemove = document.getElementById(id);

  usersSpace.removeChild(userToRemove);

  var count = getUsersCount();

  // Show message if no users
  if (count == 0) {
    document.getElementById('no-added-users').style.display = 'block';
  }

  return false;
}