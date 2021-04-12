// Check if field is blank

function fieldBlankCheck(fieldDiv, errDiv) {
  var field = document.getElementById(fieldDiv),
      err = document.getElementById(errDiv);

  if (field.value.trim() == '') {
    err.innerHTML = 'This field is required to request access.';
  } else {
    err.innerHTML = '';
  }
}

// Check password requirements