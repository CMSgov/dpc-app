function copyText(text) {
  /* Get the text field */

  var copyText = document.getElementById(text).textContent;

  copyText = copyText.trim();

  /* Create text field & select text */
  var textArea = document.createElement("textarea");
    textArea.value = copyText;
    document.body.appendChild(textArea);
    textArea.select();
    textArea.setSelectionRange(0, 99999); /*For mobile devices*/

  var confirm = 'confirm-' + text

  var confirmMsg = document.getElementById(confirm);

  /* Copy text inside of textfield */
  try {
    var successful = document.execCommand('copy');
    if(successful)
    {
      document.body.removeChild(textArea);
      confirmMsg.classList.add("confirm-success");
    }
    var msg = successful ? 'successful' : 'unsuccessful';

    confirmMsg.innerHTML = 'Command copy ' + msg;
  } 
  catch (err) 
  {
    document.body.removeChild(textArea);
  }
}