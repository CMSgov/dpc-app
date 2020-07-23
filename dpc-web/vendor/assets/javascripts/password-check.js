var passwordInput = document.getElementById("user_password");
var passwordConfirm = document.getElementById("user_password_confirmation");
var passCharCount = document.getElementById("password-char-set");
var passLowerCount = document.getElementById("password-lower-set");
var passUpperCount = document.getElementById("password-upper-set");
var passNumCount = document.getElementById("password-num-set");
var passSpecialCount = document.getElementById("password-special-set");
var passConfirmCheck = document.getElementById("password-confirm-set");

var inputValue = passwordInput.value;
var confirmInput = passwordConfirm.value;

passwordInput.onkeyup = function () {
  inputValue = passwordInput.value;
  confirmInput = passwordConfirm.value;

  // Validates character count
  if (inputValue.length >= 15) {
    passCharCount.classList.remove("invalid");
    passCharCount.classList.add("valid");
  } else {
    passCharCount.classList.remove("valid");
    passCharCount.classList.add("invalid");
  }
  
  // Validates lowercase letters
  var lowerCase = /[a-z]/g;

  if (inputValue.match(lowerCase)) {
    passLowerCount.classList.remove("invalid");
    passLowerCount.classList.add("valid");
  } else {
    passLowerCount.classList.remove("valid");
    passLowerCount.classList.add("invalid");
  }

  // Validates uppercase letters
  var upperCase = /[A-Z]/g;

  if (inputValue.match(upperCase)) {
    passUpperCount.classList.remove("invalid");
    passUpperCount.classList.add("valid");
  } else {
    passUpperCount.classList.remove("valid");
    passUpperCount.classList.add("invalid");
  }

  // Validates numbers
  var number = /[0-9]/g;

  if (inputValue.match(number)) {
    passNumCount.classList.remove("invalid");
    passNumCount.classList.add("valid");
  } else {
    passNumCount.classList.remove("valid");
    passNumCount.classList.add("invalid");
  }

  // Validates special characters
  var specialChar = /[!@#$&*]/g;

  if (inputValue.match(specialChar)) {
    passSpecialCount.classList.remove("invalid");
    passSpecialCount.classList.add("valid");
  } else {
    passSpecialCount.classList.remove("valid");
    passSpecialCount.classList.add("invalid");
  }

  // Validate match
  confirmMatch(inputValue, confirmInput);
}

passwordConfirm.onkeyup = function () {
  inputValue = passwordInput.value;
  confirmInput = passwordConfirm.value;

  confirmMatch(inputValue, confirmInput);
}

function confirmMatch(inputValue, confirmInput) {
  if (inputValue == confirmInput && confirmInput.length > 0) {
    passConfirmCheck.classList.remove("invalid");
    passConfirmCheck.classList.add("valid");
  } else {
    passConfirmCheck.classList.remove("valid");
    passConfirmCheck.classList.add("invalid");
  }
}
