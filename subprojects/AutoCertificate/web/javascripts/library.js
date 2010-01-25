/**
 * Library functions.
 */
function smallPopUp (href) {

  var height = 350;
  var width = 500;

  var newwindow = window.open (href,'unused','height=' + height +',width=' + width);
  if (window.focus) { 
    newwindow.focus(); 
  }

  return false;
}

/**
 * Closes any window.
 */
function closeThisWindow() {
  window.close();
  return true;
}

/**
 * Verify two password fields match
 */
function verifyPasswords() {
  var pw1 = document.getElementById('password1').value;
  var pw2 = document.getElementById('password2').value;

  if (pw1 != pw2) {
    alert('Passwords don\'t match. Please enter matching passwords.');
    return false;
  }

  if (pw1 == "") {
    alert('Passwords cannot be empty. Please enter matching passwords.');
    return false;
  }

  return true;
}

/**
 *
 */
function verifyExpireGroup() {
  var groupToExpire = document.getElementById('group_to_expire').value;

  if (groupToExpire == 'none') {
    alert('Please select a group to expire');
    return false;
  } else {
    return confirm("Are you sure you want to expire "+groupToExpire+"?");
  }
}

/**
 *
 */
function verifyValidateGroup() {
  var groupToValidate = document.getElementById('group_to_validate').value;

  if (groupToValidate == 'none') {
    alert('Please select a group to validate');
    return false;
  } else {
    return confirm("Are you sure you want to validate "+groupToValidate+"?");
  }
}