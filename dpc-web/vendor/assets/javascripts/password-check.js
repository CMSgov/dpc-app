function passwordCheck(checkType) {
  var passwordInput = document.getElementById("user_password"),
      passwordConfirm = document.getElementById("user_password_confirmation"),
      inputValue = passwordInput.value,
      confirmInput = passwordConfirm.value;

  if (checkType == 'user_password') {
    complexCheck(inputValue);
    confirmMatch(inputValue, confirmInput);
  } else if (checkType == 'user_password_confirmation') {
    confirmMatch(inputValue, confirmInput);
  } else {
    console.error();
  }
}

function complexCheck(inputValue) {
  var lowerCase = /[a-z]/g,
      upperCase = /[A-Z]/g,
      number = /[0-9]/g,
      specialChar = /[!@#$&*]/g;

  // Validates character count
  if (inputValue.length >= 15) {
    validCheck('password-char-set');
  } else {
    invalidCheck('password-char-set');
  }

  // Validates lowercase letters
  if (inputValue.match(lowerCase)) {
    validCheck('password-lower-set');
  } else {
    invalidCheck('password-lower-set');
  }

  // Validates uppercase letters
  if (inputValue.match(upperCase)) {
    validCheck('password-upper-set');
  } else {
    invalidCheck('password-upper-set');
  }

  // Validates numbers
  if (inputValue.match(number)) {
    validCheck('password-num-set');
  } else {
    invalidCheck('password-num-set');
  }

  // Validates special characters
  if (inputValue.match(specialChar)) {
    validCheck('password-special-set');
  } else {
    invalidCheck('password-special-set');
  }
}

function confirmMatch(inputValue, confirmInput) {
  if (inputValue == confirmInput && confirmInput.length > 0) {
    validCheck('password-confirm-set');
  } else {
    invalidCheck('password-confirm-set');
  }
}

function validCheck(id) {
  var id = document.getElementById(id);

  id.classList.remove("invalid");
  id.classList.add("valid");
}

function invalidCheck(id) {
  var id = document.getElementById(id);

  id.classList.remove("valid");
  id.classList.add("invalid");
}