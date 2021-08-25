function characterCount(string, countMax, counter) {
  var input = document.getElementById(string),
      inputValue = input.value,
      inputLength = inputValue.length,
      increment = document.getElementById(counter);

  increment.innerHTML = "(" + inputLength + "/" + countMax + ")";

  if (inputLength > countMax) {
    increment.classList.remove('ds-u-color--gray');
    increment.classList.remove('ds-u-font-weight--normal');
    increment.classList.add('ds-u-color--error-dark');
    increment.classList.add('ds-u-font-weight--bold');
  } else {
    increment.classList.remove('ds-u-color--error-dark');
    increment.classList.remove('ds-u-font-weight--bold');
    increment.classList.add('ds-u-color--gray');
    increment.classList.add('ds-u-font-weight--normal');
  }
}