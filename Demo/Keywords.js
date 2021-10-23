function addOption1(select, value, displayText, isDefault) {
    var option = document.createElement('option');
    if (isDefault) {
        option.setAttribute('selected', '');
    }
    option.setAttribute('value', value);
    option.appendChild(document.createTextNode(displayText));
    select.appendChild(option);
}

function addOptions1(select, versionList) {
    addOption1(select, -1, 'Select', true);
    versionList.map(function(v) {
        addOption1(select, v, v, false);
    });
}

function addVersionList1(container, id, versionList) {
    var div = document.createElement('div');
    div.setAttribute('id', id);
    div.setAttribute('class', 'coral-Select');

    var button = document.createElement('button');
    button.setAttribute('class', 'coral-Select-button coral-MinimalButton');
    var span = document.createElement('span');
    span.setAttribute('class', 'coral-Select-button-text');
    span.appendChild(document.createTextNode('Select'));
    button.appendChild(span);
    div.appendChild(button);

    var select = document.createElement('select');
    select.setAttribute('class', 'coral-Select-select');
    select.setAttribute('multiple', 'true');
    addOptions1(select, versionList);
    div.appendChild(select);

    //div.style.width = '112px';

    container.appendChild(div);
}


var b;
var a;

$(document).ready(function() {

    //var code = $('#DitaFile').val() // include ";" and stuff
    $.ajax({url: '/bin/publishlistener',
    	type: 'GET', 
    	data: {
               pathfinal: window.location.href,
    	operation:"reuse"},
    	success: function(data) {
            	 b=/([^,]+)/g;// FilteredKeyphrases
            	 a=data.match(b);
            //var select12 = $('#select12').get(0);
            /* var str = '';
            	for (var i = 0; i < a.length; i++) {
							str+=a[i];

  					}*/

            //$("textarea#SimilarKeywords").val(str);
			/*
            rh.model.publish(".k.keywords", a);
            var selectId = '#keywordsSelect';
            var tagList = new CUI.Select({ element: selectId });
        	var select = $(selectId).data('select');
            select.setValue(rh.model.get(".k.keywords"));
            */
            if(a!=null)
            {
            var container = document.createElement('div');
            addVersionList1(container, 'keywordsSelect', a);
            document.getElementsByClassName('column-left-30-preview')[0].appendChild(container);

            var tagList = new CUI.Select({ element:'#keywordsSelect' });

            tagList.on('selected', function(event) {
								submittedkeywords=event.selected;
                /*for (var i=0;i<submittedkeywords.length;i++)
                {
					submittedkeywordsstring+=submittedkeywords[i]+",";
                    }*/

                alert(event.selected);
            });

            }
    	alert('ok');
    	 },
    	error:function(result)
    	 {
    	alert('error');
           }
           })

    /*var targetURL = "http://localhost:4502/bin/publishlistener?operation=reuse&code="+code;
      var xhttp=new XMLHttpRequest();
  xhttp.onreadystatechange=function(){
    if(xhttp.readyState==4 && xhttp.status==200){
       mydata=JSON.parse(xhttp.responseText);
       $("textarea#SimilarKeywords").val(mydata);*/



                     //xhttp.open('GET',targetURL,true);




})

                   //xhttp.send(null);





