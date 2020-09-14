function passwordCheck(checkType) {
  var passwordInput = document.getElementById("user_password"),
      passwordConfirm = document.getElementById("user_password_confirmation"),
      inputValue = passwordInput.value,
      confirmInputValue = passwordConfirm.value;

  if (checkType == "user_password") {
    complexCheck(inputValue);
    confirmMatch(inputValue, confirmInputValue);
  } else if (checkType == "user_password_confirmation") {
    confirmMatch(inputValue, confirmInputValue);
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
    validCheck('password-special-set')
  } else {
    invalidCheck('password-special-set')
  }
}

function confirmMatch(input, confirm) {
  if (input == confirm && confirm.length > 0) {
    validCheck('password-confirm-set');
  } else {
    invalidCheck('password-confirm-set');
  }
}

function validCheck(id) {
  var element = document.getElementById(id);

  element.classList.remove('invalid');
  element.classList.add('valid');
}

function invalidCheck(id) {
  var element = document.getElementById(id);

  element.classList.remove('valid');
  element.classList.add('invalid');
}