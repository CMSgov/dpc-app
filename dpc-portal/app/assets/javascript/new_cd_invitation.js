function verifyEmail() {
  const errorFieldId = "invited_email_error_msg";
  let valid = verifyNotBlank(email, errorFieldId);
  if (valid) {
    valid = verifyEmailFormat(email, errorFieldId);
  }
  return valid;
}

function verifyEmailConfirmation() {
  const errorFieldId = "invited_email_confirmation_error_msg";
  let valid = verifyNotBlank(emailConfirmation, errorFieldId);
  if (valid && emailConfirmation.value != email.value) {
    valid = false;
    renderError(emailConfirmation, errorFieldId, "Email doesn't match");
  } else if (valid) {
    valid = verifyEmailFormat(emailConfirmation, errorFieldId);
  }
  return valid;
}

const givenName  = document.getElementById("invited_given_name");
givenName.addEventListener("blur", () => {
  verifyNotBlank(givenName, "invited_given_name_error_msg");
});

const familyName  = document.getElementById("invited_family_name");
familyName.addEventListener("blur", () => {
  verifyNotBlank(familyName, "invited_family_name_error_msg");
});

const email  = document.getElementById("invited_email");
email.addEventListener("blur", () => {
  verifyEmail();
});

const emailConfirmation  = document.getElementById("invited_email_confirmation");
emailConfirmation.addEventListener("blur", () => {
  verifyEmailConfirmation();
});

document.getElementById("cd-form").addEventListener("submit", (event) => {
  if (event.submitter == document.getElementById("modal-submitter")) {
    return true;
  }

  event.preventDefault();

  let valid = verifyNotBlank(givenName, "invited_given_name_error_msg");
  if (!verifyNotBlank(familyName, "invited_family_name_error_msg")) {
    valid = false;
  }
  if (!verifyEmail()) {
    valid = false;
  }
  if (!verifyEmailConfirmation()) {
    valid = false;
  }

  if (valid) {
    document.getElementById("modal-opener").click();
  }
});
