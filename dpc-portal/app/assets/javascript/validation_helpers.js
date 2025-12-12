function verifyNotBlank(formField, errorElementId) {
  let errorElement = document.getElementById(errorElementId);
  if (formField.value.trim()) {
    formField.classList.remove("usa-input--error");
    if (errorElement) {
      errorElement.remove();
    }
    return true;
  } else {
    renderError(formField, errorElementId, "Can't be blank");
    return false;
  }
}

function renderError(formField, errorElementId, errorMsg) {
  let errorElement = document.getElementById(errorElementId);
  if (!errorElement) {
    errorElement = document.createElement("p");
    errorElement.classList.add("usa-error-message");
    errorElement.id = errorElementId;
    formField.insertAdjacentElement("beforebegin", errorElement);
  }
  errorElement.textContent = errorMsg;
  formField.classList.add("usa-input--error");
}

const emailRegex = /^[^\s@]{1,255}@[^\s@]{1,255}\.[^\s@]{1,255}$/;
function verifyEmailFormat(formField, errorElementId) {
  let valid = emailRegex.test(email.value);
  if (!valid) {
    renderError(formField, errorElementId, "Invalid email format");
  }
  return valid;
}
