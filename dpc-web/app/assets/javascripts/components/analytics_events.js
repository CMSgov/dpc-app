const siteLogo = document.querySelector('a.site-logo');
siteLogo.addEventListener('click', function(e) {
  utag.link({
    'event_name': 'header_click',
    'link_type': 'link_other',
    'text': siteLogo.text,
    'link_url': siteLogo.href,
  });
});
