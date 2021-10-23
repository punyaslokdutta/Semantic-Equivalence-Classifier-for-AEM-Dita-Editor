<%--
  ADOBE CONFIDENTIAL
  __________________
  Copyright 2016 Adobe Systems Incorporated
  All Rights Reserved.
  NOTICE:  All information contained herein is, and remains
  the property of Adobe Systems Incorporated and its suppliers,
  if any.  The intellectual and technical concepts contained
  herein are proprietary to Adobe Systems Incorporated and its
  suppliers and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden unless prior written permission is obtained
  from Adobe Systems Incorporated.
____________________
--%>



<%@page session="false"%>
<%@include file="/libs/foundation/global.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://starlingbeta.com/taglib" prefix="f" %>
<%@page import="com.day.cq.i18n.I18n"%>
<%
      I18n i18n = new I18n(slingRequest);
%>
<%
    StringBuffer cls = new StringBuffer();
    for (String c: componentContext.getCssClassNames()) {
        cls.append(c).append(" ");
    }
%>
<c:if test="${f:isTopic(pageContext.request)}">
<style type="text/css">
  .asset-detail-view  {
    background: #f5f5f5;
  }
  #asset-mainimage {
    display: none;
  }
</style>

    <div class="column-left-30-preview" data-rhwidget="Basic">
        <% /*
        <span class="coral-Select" id="keywordsSelect">
            <button type="button" class="coral-Select-button coral-MinimalButton">
                <span class="coral-Select-button-text">One</span>
            </button>
            <select class="coral-Select-select">
                <option data-i-value="item" data-repeat=".k.keywords" data-itext="Granite.I18n.get(item)"></option> 
            </select>
        </span> 
        */
        %>

	<button class="coral-Button coral-Button--secondary" id="submitkeywords">SUBMIT KEYWORDS</button>
    </div>
    <div class="previewbody column-left-70-preview <%= cls %>" style="width: 70%;display: none">

<cq:includeClientLib categories="cq.jquery"/>
<cq:include path="clientcontext" resourceType="cq/personalization/components/clientcontext"/>
    <iframe src="" class="preview-frame"></iframe>
    <cq:include script="topicfooter.jsp"/>
</div>
<cq:includeClientLib categories="apps.fmdita.ditapreview"/>
<cq:includeClientLib categories="apps.fmdita.timeline"/>
<cq:includeClientLib categories="com.adobe.fmdita.keyspace"/>
<script type="text/javascript">
    fmxml.init()
    $('#submitkeywords').click( function () {
        var tagList = new CUI.Select({ element:'#keywordsSelect' });
        var selectList=tagList.getValue();
        var submittedkeywordsstring="";
                for (var i=0;i<selectList.length;i++)
                {
					submittedkeywordsstring+=selectList[i]+",";
                    }
        var keyphraselist="";
        for(var i=0;i<a.length;i++)
        {
           keyphraselist+=a[i]+","; 
        }
        $.ajax({
           url: '/bin/publishlistener',
            type: 'GET',
            data:{
                b : keyphraselist ,
           operation: "submittedkeywords",
                pathfinal: window.location.href,
                submittedkeywordsstring: submittedkeywordsstring
            }, 

            success: function(data) 
            {
    	alert('ok');
    	 },

    	error:function(result)
    	 {
    	alert('error');
           }


        });
    });
</script>

  <div id="error-alert" class="coral-Alert coral-Alert--error coral-Alert--large">
        <button type="button" class="coral-MinimalButton coral-Alert-closeButton" title='<%= i18n.get("Close") %>' data-dismiss="alert">
          <i class="coral-Icon coral-Icon--sizeXS coral-Icon--close coral-MinimalButton-icon"></i>
        </button>
        <i class="coral-Alert-typeIcon coral-Icon coral-Icon--sizeS coral-Icon--alert"></i>
        <strong class='coral-Alert-title'></strong>
        <div class='coral-Alert-message'></div>
      </div>
</c:if>
<c:if test="${!f:isTopic(pageContext.request)}">
<cq:include path="." resourceType="dam/gui/coral/components/admin/assetdetails/view" />
</c:if>