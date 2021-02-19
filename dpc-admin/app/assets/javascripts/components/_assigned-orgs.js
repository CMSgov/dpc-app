var assignedOrgSelect = document.querySelector('select[data-org-toggle="org-toggle"]'),
    requestedOrgData = document.getElementById('requested_org_hint');



if(typeof(assignedOrgSelect) != 'undefined' && assignedOrgSelect != null){
  if(typeof(assignedOrgSelect) != 'undefined'){
    function showRequestedOrg() {
      requestedOrgData.removeAttribute("hidden");
    }

    function hideRequestedOrg() {
      requestedOrgData.setAttribute("hidden", "");
    }

    function handleRequestedOrgVisibility() {
      if (assignedOrgSelect.value !== '') hideRequestedOrg();
      else showRequestedOrg();
    }

    if (assignedOrgSelect.value !== '') {
      handleRequestedOrgVisibility();
    }

    assignedOrgSelect.onchange = function(){
      handleRequestedOrgVisibility();
    }
  }
}

