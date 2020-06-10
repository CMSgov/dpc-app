function copyText(text) {
  /* Get the text field */
  const copyText = document.getElementById(text).textContent;

  /* Create text field & select text */
  var textArea = document.createElement("textarea");
    textArea.value = copyText;
    document.body.appendChild(textArea);
    textArea.select();

  let confirm = 'confirm-' + text

  const confirmMsg = document.getElementById(confirm);

  /* Copy text inside of textfield */
  try {
    var successful = document.execCommand('copy');
    if(successful)
    {
      document.body.removeChild(textArea);
    }
    var msg = successful ? 'successful' : 'unsuccessful';

    confirmMsg.innerHTML = 'Command copy ' + msg;
  } 
  catch (err) 
  {
    document.body.removeChild(textArea);
  }
}