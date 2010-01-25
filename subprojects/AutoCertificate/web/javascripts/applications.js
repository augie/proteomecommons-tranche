/**
 * Select all todos.
 */
function selectAll() {
  setStatusAll(true);
  return false;
}

/**
 * Deselect all todos.
 */
function deselectAll() {
  setStatusAll(false);
  return false;
}

/**
 * Helper method.
 */
function setStatusAll(setSelect) {

  // Select every checkbox in every row
  var requestsTable = document.getElementById('applications_table');

  // For each row except header, which is first row
  for (var i = 1; i < requestsTable.rows.length; i++) {
    // Only select if row is visible.
    var row = requestsTable.rows[i];
    if (row.style.display != 'none' && row.style.display != 'hidden') {
      var checkbox = requestsTable.rows[i].getElementsByTagName('input')[0];
      checkbox.checked = setSelect;
    }
  }
}

/**
 * Helper method. Returns true if there are any selected applications, false otherwise.
 */
function anySelections() {
  var requestsTable = document.getElementById('applications_table');

  // For each row except header, which is first row
  for (var i = 1; i < requestsTable.rows.length; i++) {
    var row = requestsTable.rows[i];
    if (row.style.display != 'none' && row.style.display != 'hidden') {
      var checkbox = requestsTable.rows[i].getElementsByTagName('input')[0];
      if (checkbox.checked) {
        return true;
      }
    }
  }
  return false;
}

function verifyApprove() {
  if (!anySelections()) {
    alert('There aren\'t any selected applications!');
    return false;
  }
  return confirm("Approve selected applications?");
}

function verifyRequest() {
  if (!anySelections()) {
    alert('There aren\'t any selected applications!');
    return false;
  }
  return confirm("Request more info from selected applicants?");
}

function verifyRemove() {
  if (!anySelections()) {
    alert('There aren\'t any selected applications!');
    return false;
  }
  return confirm("Deleting is forever. Are you sure you want to remove selected applications?");
}