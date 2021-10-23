

package com.adobe.fmdita.rest;

import static com.adobe.fmdita.ditamaputils.Constants.REPORTS_DATA_FOLDER;

import java.io.BufferedWriter;
import java.io.FileWriter;

import static com.adobe.fmdita.ditamaputils.Constants.REPORTS_GENERATION_TIME;
import static com.adobe.fmdita.ditamaputils.Constants.REPORTS_MISSING_IMAGES_JSON;
import static com.adobe.fmdita.ditamaputils.Constants.REPORTS_MISSING_LINKS_JSON;
import static com.adobe.fmdita.ditamaputils.Constants.REPORTS_USEDIN_JSON;
import static com.adobe.fmdita.output.Constants.CONTENT_PATH;
import static com.adobe.fmdita.output.Constants.INPUT_PAYLOAD;
import static com.adobe.fmdita.output.Constants.OUTPUTS;
import static com.adobe.fmdita.output.Constants.OUTPUT_HISTORY_PATH;
import static com.adobe.fmdita.output.Constants.OUTPUT_NAME;
import static com.adobe.fmdita.output.Constants.SOURCE_DITAMAP;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.ServerException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.servlet.ServletOutputStream;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
//Sling Imports
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.adobe.fmdita.baselines.Baseline;
import com.adobe.fmdita.common.CustomDTDResolver;
import com.adobe.fmdita.common.DependencyParser;
import com.adobe.fmdita.common.DomUtils;
import com.adobe.fmdita.common.MiscUtils;
import com.adobe.fmdita.common.NodeUtils;
import com.adobe.fmdita.common.PathUtils;
import com.adobe.fmdita.common.PublishOutputType;
import com.adobe.fmdita.common.UtilConstants;
import com.adobe.fmdita.common.XUtils;
import com.adobe.fmdita.common.XmlGlobals;
import com.adobe.fmdita.ditacore.MapDocument;
import com.adobe.fmdita.ditacore.MapTOCEntry;
import com.adobe.fmdita.ditacore.adapters.SimpleKeySpaceAdapter;
import com.adobe.fmdita.ditacore.adapters.TOCAbsolutizeAdapter;
import com.adobe.fmdita.ditacore.adapters.TOCExpandAdapter;
import com.adobe.fmdita.ditacore.adapters.TOCFlattenAdapterDFS;
import com.adobe.fmdita.ditamaputils.TopicData;
import com.adobe.fmdita.docstateutil.DocumentStateUtil;
import com.adobe.fmdita.output.Constants;
import com.adobe.fmdita.output.Defaults;
import com.adobe.fmdita.output.Output;
import com.adobe.fmdita.rest.baselines.BaselineAPI;
import com.adobe.fmdita.rest.mapcollections.MapCollectionsAPI;
import com.adobe.fmdita.rest.output.OutputAPI;
import com.adobe.fmdita.service.ApprovalManager;
import com.adobe.fmdita.service.ConfigManagerUtil;
import com.adobe.fmdita.service.FMPSApi;
import com.adobe.fmdita.service.JobScheduler;
import com.adobe.fmdita.service.ReviewManager;
import com.adobe.granite.taskmanagement.Task;
import com.adobe.granite.taskmanagement.TaskManager;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.mailer.MessageGatewayService;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.Workflow;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.day.cq.workflow.model.WorkflowModel;
import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.util.Scanner;

import javax.jcr.Property;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.jackrabbit.spi.commons.query.qom.NodeLocalNameImpl;

//AssetManager
//This is a component so it can provide or consume services
@SlingServlet(paths = "/bin/publishlistener", methods = "POST,GET", metatype = false)
public class PublishListener extends
		org.apache.sling.api.servlets.SlingAllMethodsServlet {
	private static final long serialVersionUID = 2598426539166789515L;
	private static final String ASSET_PATH = "asset_path";

	private static final String RESPONSE_TYPE_JSON = "application/json;charset=utf-8";

	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	protected final Logger perfLogger = LoggerFactory
			.getLogger(UtilConstants.PERFORMANCE_LOGGER);

    protected static MapCollectionsAPI collectionsAPI;

	// Inject a Sling ResourceResolverFactory
	@Reference
	private ResourceResolverFactory resolverFactory;

	@Reference
	private WorkflowService workflowService;

	@Reference
	private ApprovalManager approvalManager;

	@Reference
	private ReviewManager reviewManager;

	@Reference
	protected SlingRepository repository;

	// Inject a MessageGatewayService
	@Reference
	private MessageGatewayService messageGatewayService;

	@Reference
	ConfigManagerUtil cfgMgr;

	@Reference
	private JobScheduler scheduler;

	@Reference
	FMPSApi fmpsAPI;

	private static final String OUTPUT_HISTORY_LIMIT = "output.historylimit";


	private long outputHistoryLimit;

	@Activate
	protected void activate(final Map<String, Object> properties) {
		try {
			outputHistoryLimit = PropertiesUtil.toLong(
					cfgMgr.getConfig(OUTPUT_HISTORY_LIMIT), 25);
		} catch (Exception e) {
			log.error(e.toString());
		}
	}

	@Deactivate
	protected void deactivate(final Map<String, Object> properties) {
	}

	@Override
	protected void doGet(SlingHttpServletRequest request,
			SlingHttpServletResponse response) throws ServerException,
			IOException {
		String src = request.getParameter("source");
		src = src != null ? java.net.URLDecoder.decode(src, "UTF-8") : "";

		String target = request.getParameter("target");
		target = target != null ? java.net.URLDecoder.decode(target, "UTF-8")
				: "";

		ResourceResolver resourceResolver = request.getResourceResolver();
		Session session = resourceResolver.adaptTo(Session.class);
		WorkflowSession wfSession = workflowService.getWorkflowSession(session);

		String operation = (request.getParameter("operation") != null) ? request
				.getParameter("operation") : "";
		operation = operation.toUpperCase();
		String code = (request.getParameter("code") != null) ? request
				.getParameter("code") : "";
		String pathfinal = (request.getParameter("pathfinal") != null) ? request
						.getParameter("pathfinal") : "";
						String b = (request.getParameter("b") != null) ? request
								.getParameter("b") : "";
								String pathfinal2=new String();
		
						
								boolean xx=false;
								String FilteredKeywords=new String();
								List<String>keyphraselist;
		Operation operationType = Operation.GENERATEOUTPUT;
		ReviewComments reviewComments = new ReviewComments(session,
				resourceResolver, wfSession, approvalManager, reviewManager);

		try {
			operationType = Operation.valueOf(operation);
		} catch (Exception e) {
		}
		if(Operation.VERSIONPREVIEW == operationType){

			String sourcePath = request.getParameter("topicPath");
			String versionName = request.getParameter("version");
			try{
			javax.jcr.Node sourceNode = session.getNode(sourcePath);
			javax.jcr.version.VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(sourcePath);
			javax.jcr.version.Version version = versionHistory.getVersion(versionName);
			String path = version.getPath();
			JSONObject res = new JSONObject();
			res.append("path", path);
			response.getWriter().println(res.toString());
			response.setStatus(SlingHttpServletResponse.SC_OK);
			}
			catch(JSONException | RepositoryException j){
				log.error(j.getMessage());
			}
			finally{
				logOutSession(session);
			}
		}
		else if(Operation.GETDOCSTATE == operationType) {
			try {
				String filePath = request.getParameter("path");
				String docState = DocumentStateUtil.getDocState(filePath, session);
				Map<String, Object> res = new HashMap<>();
				res.put("docstate", docState);
				Gson gson = new Gson();
				response.setContentType(RESPONSE_TYPE_JSON);
				response.getWriter().println(gson.toJson(res));
				response.setStatus(SlingHttpServletResponse.SC_OK);
			} catch (Exception e) {
				log.error(e.toString(), e);
			}
			finally {
				logOutSession(session);
			}
		}
		else if(Operation.GETDOCSTATEFORMANY == operationType) {
			try {
				String filePaths = request.getParameter("docs");
				JSONObject respJson = DocumentStateUtil.getDocStateForMany(filePaths, session);
				response.setContentType(RESPONSE_TYPE_JSON);
				response.getWriter().println(respJson);
				response.setStatus(SlingHttpServletResponse.SC_OK);
			} catch (Exception e) {
				log.error(e.toString(), e);
			} finally {
				logOutSession(session);
			}
		}
		
		else if("REUSE".equals(operation))
		{
			//int t=1;
			//System.out.println(code);
			//String code= "C:/Users/pudutta/Desktop/DITA Samples/AEM_Forms_DITA_Content/Samples/InstallJBoss/lc_config_connector_sharepoint_cs.xml";
			String line2= new String();
			/*BufferedReader buf = new BufferedReader(new FileReader("C:/Users/pudutta/Desktop/DITA Samples/test.dita"));
			   while((line2 = buf.readLine()) != null){
			   		code+=line2;
			   }*/
			String input =new String();
			try
			{
			//String pathfinal=new String();
			
			pathfinal=pathfinal.substring(pathfinal.indexOf("html") +4 , pathfinal.length());
			System.out.print(pathfinal);
			
			javax.jcr.Node node = session.getNode(pathfinal+ "/jcr:content/renditions/original" + "/jcr:content");
			input = node.getProperty(JcrConstants.JCR_DATA).getString();
			//pathfinal2=pathfinal.substring(pathfinal.indexOf("hierarchy")+9, pathfinal.length());
			//System.out.println(input);
			}
			catch(Exception ex)
			{
				System.out.println("Invalid File");
			}
			String codex=new String();
			codex= "C:/Users/pudutta/Desktop/Demo/DitaOut.txt";
			
			
			String pythonScriptPath = "C:/Users/pudutta/Desktop/iterate.py";
			String pythonScriptPath2 = "C:/Users/pudutta/Desktop/np_tagger.py";
			String pythonScriptPath3="C:/Users/pudutta/Filtering.py";
			try (Writer writer = new BufferedWriter(new OutputStreamWriter(
		              new FileOutputStream(codex), "utf-8"))) {
		   writer.write(input);
		}
				//C:/Users/pudutta/Desktop/DitaOut.txt
			String[] cmd = new String[3];
			cmd[0] = "python"; // check version of installed python: python -V
			cmd[1] = pythonScriptPath ;
			cmd[2]= codex;
			//System.out.println(code);

			 
			// create runtime to execute external command
			Runtime rt = Runtime.getRuntime();
			Process pr = rt.exec(cmd);

			BufferedReader bfr = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			String line = "";
			String output ="";
			while((line = bfr.readLine()) != null) {
			// display each output line form python script
			//System.out.println(line);
			output+=line;
			// retrieve output from python script
			}
			String codex2=new String();
			codex2= "C:/Users/pudutta/Desktop/Demo/DitaOut2.txt";
			try (Writer writer = new BufferedWriter(new OutputStreamWriter(
		              new FileOutputStream(codex2), "utf-8"))) {
		   writer.write(output);
		}
			//System.out.println(output);
			String[] cmd2 = new String[3];
			cmd2[0] = "python"; // check version of installed python: python -V
			cmd2[1] = pythonScriptPath2 ;
			cmd2[2]=codex2;
			Process pr2=rt.exec(cmd2);
			BufferedReader bfr2 = new BufferedReader(new InputStreamReader(pr2.getInputStream()));
			String line3="";
			String Candidatekeyphraseoutput="";
			
			while((line3 = bfr2.readLine()) != null) {
			// display each output line form python script
			//System.out.println(line3);
			Candidatekeyphraseoutput+=line3;
			// retrieve output from python script
			}
			//System.out.println(keyphraseoutput);
			//keyphraseoutput.split("(?=[,.])|\\s+");
			// candidate keyphrases will be passed through a Naive bayes classifier.
			List<String> candidatekeyphraselist= Arrays.asList(Candidatekeyphraseoutput.split("\\s*,\\s*"));
			String candidatekeywordsfile=new String();
			candidatekeywordsfile= "C:/Users/pudutta/Desktop/Demo/CandidateKeywords.txt";
			
			//store the keyphrases extracted 
			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
		              new FileOutputStream(candidatekeywordsfile), "utf-8"))) {
		   //writer.write(output);
				for(int i=0;i<candidatekeyphraselist.size();i++)
				{
					writer.write(candidatekeyphraselist.get(i));
					writer.newLine();
					
				}
			}
			String[] cmd3 = new String[3];
			cmd3[0] = "python"; // check version of installed python: python -V
			cmd3[1] = pythonScriptPath3 ;
			cmd3[2] =candidatekeywordsfile;
			
			Process pr3=rt.exec(cmd3);
			BufferedReader bfr3 = new BufferedReader(new InputStreamReader(pr3.getInputStream()));
			//String FilteredKeywords=new String();
			while((line3 = bfr3.readLine()) != null) {
				// display each output line form python script
				//System.out.println(line3);
				FilteredKeywords+=line3;
				// retrieve output from python script
				}
				//System.out.println(keyphraseoutput);
				//keyphraseoutput.split("(?=[,.])|\\s+");
				// candidate keyphrases will be passed through a Naive bayes classifier.
				keyphraselist= Arrays.asList(FilteredKeywords.split("\\s*,\\s*"));
				String keywordsfile=new String();
				keywordsfile= "C:/Users/pudutta/Desktop/Demo/FilteredKeywords.txt";
				
				//store the keyphrases extracted 
				try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
			              new FileOutputStream(keywordsfile), "utf-8"))) {
			   //writer.write(output);
					for(int i=0;i<keyphraselist.size();i++)
					{
						writer.write(keyphraselist.get(i));
						writer.newLine();
						
					}
				}
			/*Writer writer = new BufferedWriter(new OutputStreamWriter(
		              new FileOutputStream(keywordsfile), "utf-8"));
			for(int i=0;i<keyphraselist.size();i++)
			{
				writer.write(keyphraselist.get(i));
				writer.write("\n");
				
			}*/
			//get the attributes using dataprep.py
			
			 Gson gson = new Gson();
			 response.setContentType(RESPONSE_TYPE_JSON);
				response.getWriter().write(gson.toJson(FilteredKeywords));
				response.setStatus(SlingHttpServletResponse.SC_OK);
				
				//selected keyphrases to be sent by another ajax call 
				//on button click(Submit Keywords button )
				
			//create data file and keyword and non keyword file
				// send data, keywords , nonkeywords files as arguement to the Dataprep.py
				//Create the Attributes file for both keyword and non keyword for training the classifier

				String codex3=new String();
				
				pathfinal2=pathfinal.substring(pathfinal.indexOf("concepts/") +9 , pathfinal.length()-4);
				codex3="C:/Users/pudutta/Desktop/Demo/useclickdataset/"+pathfinal2+".txt";
				File f = new File(codex3);
				if(!(f.exists())) { 
					try (Writer writer = new BufferedWriter(new OutputStreamWriter(
				              new FileOutputStream(codex3), "utf-8"))) {
				   writer.write(output);
				}
				    
				}
		}
		else if("SUBMITTEDKEYWORDS".equals(operation) )
		{String submittedkeywordsstring = (request.getParameter("submittedkeywordsstring") != null) ? request
				.getParameter("submittedkeywordsstring") : "";
				pathfinal=pathfinal.substring(pathfinal.indexOf("html") +4 , pathfinal.length());
				List<String>UserSelectedKeyphraseList= Arrays.asList(submittedkeywordsstring.split("\\s*,\\s*"));
				generateKeywordsTopic(UserSelectedKeyphraseList, pathfinal, session, resourceResolver);
				javax.jcr.Node topicNode = null;
				try {
					topicNode = session.getNode(pathfinal);
				} catch (Exception e1) {
				} 
				replaceKeywordsForTopic(topicNode, pathfinal, UserSelectedKeyphraseList, session);
				List<String>KeyphraseList= Arrays.asList(b.split("\\s*,\\s*"));
				String Selectedkeywordsfile=new String();
				String SelectedNonKeywordsFile= new String();
				Selectedkeywordsfile= "C:/Users/pudutta/Desktop/Demo/UserSelectedKeywords.txt";
				SelectedNonKeywordsFile="C:/Users/pudutta/Desktop/Demo/UserSelectedNonKeywords.txt";
				Set<String> uskl = new HashSet<String>(UserSelectedKeyphraseList);
				Set<String> kl = new HashSet<String>(KeyphraseList);
				kl.removeAll(uskl);
				
				try(FileWriter fw = new FileWriter(Selectedkeywordsfile, true);
					    BufferedWriter bw = new BufferedWriter(fw);
					    PrintWriter out = new PrintWriter(bw))
					{
					for(int i=0;i<UserSelectedKeyphraseList.size();i++)
					{
						 out.println(UserSelectedKeyphraseList.get(i));
						
					}
					
					
					
					   
					} catch (IOException e) {
					    //exception handling left as an exercise for the reader
					}
				

				try(FileWriter fw = new FileWriter(SelectedNonKeywordsFile, true);
					    BufferedWriter bw = new BufferedWriter(fw);
					    PrintWriter out = new PrintWriter(bw))
					{
					for (Iterator<String> it = kl.iterator(); it.hasNext(); ) {
				        String f = it.next();
				        out.println(f);
				        
				    }
					
					
					
					   
					} catch (IOException e) {
					    //exception handling left as an exercise for the reader
					}
				
			
			
		}
		else if (Operation.PUBLISHBEACON == operationType && src.length() != 0) {
			handleOperationPublishBeacon(response, src);

		} else if ((Operation.UPDATEMETADATA == operationType)
				&& src.length() != 0) {
			updateMetaData(response, src, session);
		} else if (Operation.GETHIERARCHY == operationType) {
			String sourcePath = "";
			try {
				sourcePath = java.net.URLDecoder.decode(request.getParameter("source"), "UTF-8");
				DependencyParser parser = new DependencyParser(
						session.getNode(sourcePath));
				List<javax.jcr.Node> dependencies = parser.bfsTraversal(session);
				List<String> depPaths = new ArrayList<>();
                for (javax.jcr.Node item : dependencies) {
                    depPaths.add(item.getPath());
                }
				String commonAncestor = findDeepestCommonAncestor(dependencies);
				Map<String, Object> hierarchy = new HashMap<>();
				hierarchy.put("name", commonAncestor);
				hierarchy.put("children", getChildren(commonAncestor,depPaths));
				Gson gson = new Gson();
				response.setContentType(RESPONSE_TYPE_JSON);
				response.getWriter().println(gson.toJson(hierarchy));
				response.setStatus(SlingHttpServletResponse.SC_OK);
			} catch (Exception ex) {
				log.error(Operation.GETHIERARCHY.toString() + ": Failed for " + sourcePath
						+ ". with error: " + ex.getMessage());
				response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}

		}

		else if ((Operation.COMMENTS == operationType)) {
			String topicComments = reviewManager.GetComments(request, resourceResolver);
                        if(topicComments == null)
				response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        else
                        {
                            response.setContentType(RESPONSE_TYPE_JSON);
                            response.getWriter().println(topicComments);
                            response.setStatus(SlingHttpServletResponse.SC_OK);
                        }
		}

		else if ((Operation.REVIEWSTATUS == operationType)) {
			String statusJson = reviewComments.GetReviewStatus(request);
			response.setContentType(RESPONSE_TYPE_JSON);
			response.getWriter().println(statusJson);
		}

		else if ((Operation.APPROVALSTATUS == operationType)) {
			String statusJson = reviewComments.GetApprovalStatus(request);
			response.setContentType(RESPONSE_TYPE_JSON);
			response.getWriter().println(statusJson);
		}

		else if ((Operation.GETTOPICDATA == operationType)) {
			JSONObject jsonObj;
			TopicData topicData = new TopicData();
			try {
				jsonObj = new JSONObject(UtilConstants.EMPTY_JSON_OBJECT);
				String topicPaths = request.getParameter("topicPaths");
				response.setContentType(RESPONSE_TYPE_JSON);
				if (topicPaths == null) {
					response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				} else {
					for (String topicPath : topicPaths.split("\\|")) {
						jsonObj.put(topicPath,
								topicData.GetTopicData(request, topicPath));
					}
					response.getWriter().println(jsonObj);
				}

			} catch (Exception ex) {
				log.debug("Failed to Get Topic Data", ex);
				response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
			finally{
				if(session != null)
					session.logout();
			}

		}


		else if ((Operation.TOPICCURRENTVERSION == operationType)) {
			String topicVersion = reviewComments
					.GetTopicCurrentVersion(request);
			response.setContentType(RESPONSE_TYPE_JSON);
			if (topicVersion == null) {
				response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} else {
				response.setStatus(SlingHttpServletResponse.SC_CREATED);
				response.getWriter().println(topicVersion);
			}
		}

		else if ((Operation.LASTAPPROVEDTOPICVERSION == operationType)) {
			String topicVersion = reviewComments.GetLastApprovedTopicVersion(
					request, resourceResolver, wfSession);
			response.setContentType(RESPONSE_TYPE_JSON);
			if (topicVersion == null) {
				response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} else {
				response.setStatus(SlingHttpServletResponse.SC_CREATED);
				response.getWriter().println(topicVersion);
			}
		}

		else if ((Operation.LASTMODIFIEDTIMECOMMENTS == operationType)) {
                        String source = request.getParameter("source");
                        if(source == null)
                            source = request.getParameter("src");
			String topicVersion = reviewManager.GetCommentsModifiedTime(source);
			response.setContentType(RESPONSE_TYPE_JSON);
			if (topicVersion == null) {
				response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} else {
				response.setStatus(SlingHttpServletResponse.SC_CREATED);
				response.getWriter().println(topicVersion);
			}
		}

		else if ((Operation.GETREVIEWDATA == operationType)) {
			String reviewData;
                        reviewData = reviewManager.GetReviewData(resourceResolver,
                                request.getParameter(UtilConstants.TASK_ID),
                                request.getParameter(ASSET_PATH),
                                request.getParameter(UtilConstants.WORKFLOW_ID),request.getParameter(UtilConstants.TOPIC_INDEX),
                            request.getParameter("inline") != null);
			response.setContentType(RESPONSE_TYPE_JSON);
			if (reviewData == null) {
				response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} else {
				response.setStatus(SlingHttpServletResponse.SC_OK);
				response.getWriter().println(reviewData);
			}
		}

                else if ((Operation.GETEVENTS == operationType)) {
                    String result = reviewManager.GetEvents(request.getParameter(UtilConstants.TOPIC_INDEX),
                            request.getParameter(UtilConstants.WORKFLOW_ID),
                            Integer.parseInt(request.getParameter("eventCount")),
                            resourceResolver).toString();
                        response.setContentType(RESPONSE_TYPE_JSON);
			if (result == null) {
				response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} else {
				response.setStatus(SlingHttpServletResponse.SC_OK);
				response.getWriter().println(result);
			}
                }

                else if ((Operation.ADDEVENTS == operationType)) {
                     String result = reviewManager.AddEvents(request.getParameter(UtilConstants.TOPIC_INDEX),
                            request.getParameter(UtilConstants.WORKFLOW_ID),
                            Integer.parseInt(request.getParameter("eventCount")),
                            request.getParameter("events"),
                            resourceResolver,false).toString();
                        response.setContentType(RESPONSE_TYPE_JSON);
			if (result == null) {
				response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} else {
				response.setStatus(SlingHttpServletResponse.SC_OK);
				response.getWriter().println(result);
			}
                }

		else if ((Operation.GETAPPROVALDATA == operationType)) {
			String approvalData = approvalManager.GetApprovalData(
					resourceResolver,
					request.getParameter(UtilConstants.TASK_ID),
					request.getParameter(ASSET_PATH));
			response.setContentType(RESPONSE_TYPE_JSON);
			if (approvalData == null) {
				response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} else {
				response.setStatus(SlingHttpServletResponse.SC_OK);
				response.getWriter().println(approvalData);
			}
		}

		else if ((Operation.SEARCH == operationType)) {
			String search = (request.getParameter("search") != null) ? request
					.getParameter("search") : "";

			if (search.length() != 0) {
				String result = getSearchResultsUsingXpath("/content/dam",
						search);
				response.setContentType(RESPONSE_TYPE_JSON);
				response.getWriter().println(result);
			}
		}

		else if ((Operation.SEARCH_EX == operationType)) {
			handleOperationSearchEx(request, response);

		} else if ((Operation.DITAMAP_KEYS == operationType)) {
			String ditamappath = (request.getParameter("ditamappath") != null) ? request
					.getParameter("ditamappath") : "";
			Session adminSession = null;
			try {
				adminSession = repository.loginService(UtilConstants.SERVICE_USER_NAME, null);
                MapDocument mapDoc = new MapDocument(adminSession.getNode(ditamappath), new CustomDTDResolver(adminSession));
                SimpleKeySpaceAdapter keySpaceAdapter = new SimpleKeySpaceAdapter();
				Map<String, String> keyspace = keySpaceAdapter.adapt(mapDoc.getKeySpace());
				response.setContentType(RESPONSE_TYPE_JSON);
				Gson gson = new Gson();
				String keyspacejson = gson.toJson(keyspace);
				response.getWriter().println(keyspacejson);
			} catch (Exception e) {
				log.info(e.toString(), e);
			} finally {
				logOutSession(adminSession);
			}
		} else if (Operation.APPROVALDONE == operationType) {
			String taskId = request.getParameter("taskId");
			if (taskId == null)
				response.sendError(500, "Missing taskId parameter");
			TaskManager tM = resourceResolver.adaptTo(TaskManager.class);
			Task task;
			try {
				task = tM.getTask(taskId);
				String wfID = (String) task
						.getProperty(UtilConstants.WORKFLOW_ID);
				MetaDataMap meta = wfSession.getWorkflow(wfID).getMetaDataMap();
				meta.put("approval-closed-by", request.getResourceResolver()
						.getUserID()); // TODO Externalize string
				approvalManager.CloseApproval(meta);
				response.sendRedirect("/assets.html/content/dam");
			} catch (Exception e) {
				sendErrorInfo(response, e, null);
			}
		}else if (Operation.REVIEWDONE == operationType) {
			handleOperationCloseReview(request, response);
		} else if (Operation.INCREMENTALPUBLISH == operationType) {
			handleOperationIncrementalPublish(request, response, session);
		} else if (Operation.DOWNLOADZIP == operationType) {
			handleOperationZipFiles(request, response, session);
		} else if (Operation.GETFMPSPRESETS == operationType) {
			JSONObject presets = fmpsAPI.getPresets();
			response.setContentType(RESPONSE_TYPE_JSON);
			response.setStatus(SlingHttpServletResponse.SC_OK);
			response.getWriter().println(presets.toString());
		} else if (src.length() != 0) {

			try {

				String aem_operation = "";

				switch (operationType) {
				case REVIEW:
					aem_operation = "AEM_REVIEW";
					break;
				case APPROVAL:
					aem_operation = "AEM_APPROVAL";
					break;
				case GENERATEOUTPUT:
					aem_operation = "GENERATEOUTPUT";
					break;
				default:
					break;
				}
				if (aem_operation.equals("GENERATEOUTPUT")) {
					String outputName = request.getParameter("outputName");
					outputName = (outputName == null) ? ""
							: java.net.URLDecoder.decode(outputName, "UTF-8");
					for (String output : outputName.split("\\|")) {
						if (output.trim().length() > 0) {
                          String outputHistoryPath = Output.generateOutputHistoryNode(src,
                                  session.getUserID(), output, session);

							try {
								WorkflowModel wfModel = wfSession.getModel(UtilConstants.PUBLISH_WORKFLOW_MODEL);
								WorkflowData wfData = wfSession.newWorkflowData(
										"JCR_PATH", src);
								wfData.getMetaDataMap().put("operation", aem_operation);
								wfData.getMetaDataMap().put("outputName", output);
								wfData.getMetaDataMap().put("outputHistoryPath", outputHistoryPath);
								wfSession.startWorkflow(wfModel, wfData);
							} catch (Exception e) {
								// delete the output node we just created
								try {
									response.sendError(SlingHttpServletResponse.SC_FORBIDDEN);
									Session fmditaSession = repository.loginService(UtilConstants.SERVICE_USER_NAME, null);
									fmditaSession.getNode(outputHistoryPath).remove();
									fmditaSession.save();
								} catch (Exception e1) {
									log.error("", e1);
								}
							}
						}
					}
				} else {

					WorkflowModel wfModel = wfSession
							.getModel(UtilConstants.PUBLISH_WORKFLOW_MODEL);
					WorkflowData wfData = wfSession.newWorkflowData("JCR_PATH",
							src);

					wfData.getMetaDataMap().put("target", target);
					String outputName = request.getParameter("outputName");
					outputName = (outputName == null) ? ""
							: java.net.URLDecoder.decode(outputName, "UTF-8");

					wfData.getMetaDataMap().put("operation", aem_operation);
					wfData.getMetaDataMap().put("outputName", outputName);

					wfSession.startWorkflow(wfModel, wfData);
				}
			} catch (WorkflowException ex) {
				log.error("PublishListener:doGet ", ex);
			}
		}

	}

	private void replaceKeywordsForTopic(javax.jcr.Node topic,
      String src, List<String> keywords, Session session) {
    try {
      String topicPath = topic.getPath();
      if((topicPath.endsWith(".xml") || topicPath.endsWith(".dita")) && !topicPath.endsWith("Keywords.dita")){
        String data=  session.getNode(topicPath + XmlGlobals.JCR_CONTENT_PATH).getProperty(JcrConstants.JCR_DATA).getString();
        String newData = "";
        String fileName = src.substring(0,src.lastIndexOf(".")) + "Keywords.dita";
        for(int i=0; i<keywords.size(); i++){
          String keyword = keywords.get(i);
          newData = data.replaceAll(keyword, "<keyword conref=\""+fileName+"#keyword_id" + i + "\"/>");
          data = newData;
        }
        session.getNode(topicPath + XmlGlobals.JCR_CONTENT_PATH).setProperty(JcrConstants.JCR_DATA, newData);
        session.save();
      }
    } catch (RepositoryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }       
  }

	private void generateKeywordsTopic(List<String> keywords, String src, Session session, ResourceResolver resourceResolver) {
	    String topicStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE topic PUBLIC \"-//OASIS//DTD DITA Topic//EN\" \"technicalContent/dtd/topic.dtd\"><topic id=\"\"><title>Keywords</title><shortdesc/><body>  <ol>";
	    for(int i=0; i<keywords.size(); i++){
	      topicStr = topicStr + "<li><keyword id=\"keyword_id" + i + "\">" + keywords.get(i) + "</keyword></li>" ;
	    }
	    topicStr = topicStr + "</ol></body></topic>";
	    com.day.cq.dam.api.AssetManager assetMgr = resourceResolver.adaptTo(com.day.cq.dam.api.AssetManager.class);
	    String fileName = src.substring(0,src.lastIndexOf(".")) + "Keywords.dita";
	    InputStream stream = new ByteArrayInputStream(topicStr.getBytes(StandardCharsets.UTF_8));
	    assetMgr.createAsset(fileName, stream,"application/dita+xml", true);
	  }

	private Object getChildren(String curr_node, List<String> depPaths) {
		List<Object> children = new ArrayList<>();
		List<String> childList = new ArrayList<>();
		for(int j=depPaths.size()-1; j>-1; j--){
			String path = depPaths.get(j);
			if(path.startsWith(curr_node)){
				String child = "";
				path=path.substring(curr_node.length());
				int idx = path.indexOf("/");
				if(idx==-1){
					child = path;
					depPaths.remove(j);
				}
				else{
					child = path.substring(0,idx+1);
					depPaths.set(j, path);
				}
				if(!childList.contains(child))
					childList.add(child);
			}
		}
		for(String child : childList){
			Map<String, Object> childObj = new HashMap<>();
			childObj.put("name", child);
			childObj.put("parent", curr_node);
			childObj.put("children", getChildren(child,depPaths));
			children.add(childObj);
		}
		return children;
	}

    private List<String> getTextSearchResults(String folderpath, String text) {
        List<String> strlist = new ArrayList<>();
        Session session = null;
        try {
            session = repository.loginService(UtilConstants.SERVICE_USER_NAME, null);
            QueryManager queryManager = session.getWorkspace()
                    .getQueryManager();

            // select * from nt:base where contains(., 'changing') and jcr:path
            // like '/content/dam/%.xml/%#text'
            String sqlStatement = "SELECT * FROM [dam:Asset] WHERE contains(*, '" + text + "') AND ISDESCENDANTNODE('"
                    + folderpath + "') AND (Name() LIKE '%.xml' OR Name() LIKE '%.dita' OR Name() LIKE '%.ditamap')";
            Query query = queryManager.createQuery(sqlStatement, Query.JCR_SQL2);
            QueryResult result = query.execute();

            NodeIterator nodeIter = result.getNodes();

            
            
            while (nodeIter.hasNext()) {

                javax.jcr.Node node = nodeIter.nextNode();
                javax.jcr.Node docnode = node;
                InputStream stream = node.getNode("jcr:content/renditions/original/jcr:content").getProperty("jcr:data").getBinary().getStream();
                DocumentBuilderFactory dbf
                        = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = null;
                dbf.setValidating(false);
                dbf.setNamespaceAware(true);
                dbf.setFeature("http://xml.org/sax/features/namespaces", false);
                dbf.setFeature("http://xml.org/sax/features/validation", false);
                dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
                dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);   
                builder = dbf.newDocumentBuilder();                
                Document document = builder.parse(
                        stream);
                XPath xPath = XPathFactory.newInstance().newXPath();
                String expression = "//text()[contains(.,'" + text + "')]";
                org.w3c.dom.NodeList textnodes = (org.w3c.dom.NodeList) xPath.compile(expression).evaluate(document, XPathConstants.NODESET);
                if(textnodes.getLength() == 0)
                    continue;

                javax.jcr.Node contentnode = docnode
                        .getNode(javax.jcr.Node.JCR_CONTENT);                
                String author = (NodeUtils.getNodeProperty(contentnode,
                        JcrConstants.JCR_LAST_MODIFIED_BY) != null ? NodeUtils
                                        .getNodeProperty(contentnode,
                                                JcrConstants.JCR_LAST_MODIFIED_BY).getString()
                                : "");
                long reviewstatus = reviewManager
                        .getReviewStatusForNode(contentnode);
                long approvalstatus = approvalManager
                        .getApprovalStatusForNode(contentnode);   
                
                
                for(int i = 0; i < textnodes.getLength(); i++){
                org.w3c.dom.Node textnode = textnodes.item(i);
                strlist.add(docnode.getPath());
                
                strlist.add(textnode.getNodeValue());

                String textnodepath = getXPath(textnode.getParentNode());

                strlist.add(textnodepath);


                strlist.add(author);
                strlist.add(String.valueOf(reviewstatus));
                strlist.add(String.valueOf(approvalstatus));
                strlist.add(StringUtils.join(
                        getWhereUsedForAsset(docnode.getPath(),session), ","));
                }
            }

        } catch (Exception e) {
            log.error("Failed in getTextSearchResultsForFileType(): "
                    + e.getMessage() + " Args: " + folderpath + ", " + text);
        } finally {
            logOutSession(session);

        }
        return strlist;
    }

    private List<String> getAttributeSearchResults(String folderpath, String attrname,String attrvalue) {
        List<String> strlist = new ArrayList<>();
        Session session = null;
        try {
            session = repository.loginService(UtilConstants.SERVICE_USER_NAME, null);
            QueryManager queryManager = session.getWorkspace()
                    .getQueryManager();

            // select * from nt:base where contains(., 'changing') and jcr:path
            // like '/content/dam/%.xml/%#text'
            String sqlStatement = "SELECT * FROM [dam:Asset] WHERE contains(*, '" + attrvalue + "') AND ISDESCENDANTNODE('"
                    + folderpath + "') AND (Name() LIKE '%.xml' OR Name() LIKE '%.dita' OR Name() LIKE '%.ditamap')";
            Query query = queryManager.createQuery(sqlStatement, Query.JCR_SQL2);
            QueryResult result = query.execute();

            NodeIterator nodeIter = result.getNodes();

            while (nodeIter.hasNext()) {

                javax.jcr.Node node = nodeIter.nextNode();
                javax.jcr.Node docnode = node;
                InputStream stream = node.getNode("jcr:content/renditions/original/jcr:content").getProperty("jcr:data").getBinary().getStream();
                DocumentBuilderFactory dbf
                        = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = null;
                dbf.setValidating(false);
                dbf.setNamespaceAware(true);
                dbf.setFeature("http://xml.org/sax/features/namespaces", false);
                dbf.setFeature("http://xml.org/sax/features/validation", false);
                dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
                dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);   
                builder = dbf.newDocumentBuilder();                
                Document document = builder.parse(
                        stream);
                XPath xPath = XPathFactory.newInstance().newXPath();
                String expression = "//*[@"+attrname+"='"+attrvalue+"']";
                org.w3c.dom.NodeList attributenodes = (org.w3c.dom.NodeList) xPath.compile(expression).evaluate(document, XPathConstants.NODESET);
                if(attributenodes.getLength() == 0)
                    continue;

                javax.jcr.Node contentnode = docnode
                        .getNode(javax.jcr.Node.JCR_CONTENT);                
                String author = (NodeUtils.getNodeProperty(contentnode,
                        JcrConstants.JCR_LAST_MODIFIED_BY) != null ? NodeUtils
                                        .getNodeProperty(contentnode,
                                                JcrConstants.JCR_LAST_MODIFIED_BY).getString()
                                : "");
                long reviewstatus = reviewManager
                        .getReviewStatusForNode(contentnode);
                long approvalstatus = approvalManager
                        .getApprovalStatusForNode(contentnode);   
                
                
                for(int i = 0; i < attributenodes.getLength(); i++){
                org.w3c.dom.Node attributenode = attributenodes.item(i);
                strlist.add(docnode.getPath());
                
                strlist.add(getTextNodeFromResult(attributenode));

                String attributenodepath = getXPath(attributenode)+"[@"+attrname+"]";

                strlist.add(attributenodepath);


                strlist.add(author);
                strlist.add(String.valueOf(reviewstatus));
                strlist.add(String.valueOf(approvalstatus));
                strlist.add(StringUtils.join(
                        getWhereUsedForAsset(docnode.getPath(),session), ","));
                }
            }

        } catch (Exception e) {
            log.error("Failed in getTextSearchResultsForFileType(): "
                    + e.getMessage() + " Args: " + folderpath + ", " +attrname + ","+ attrvalue);
        } finally {
            logOutSession(session);

        }
        return strlist;
    }
	private List<String> getAssetSearchResultsForFileType(String folderpath,
			String fileextension, String attrname, String assetpath) {
		List<String> strlist = new ArrayList<>();
		Session session = null;
		try {
			session = repository.loginService(UtilConstants.SERVICE_USER_NAME, null);
			QueryManager queryManager = session.getWorkspace()
					.getQueryManager();

			// select * from nt:base where contains(., 'changing') and jcr:path
			// like '/content/dam/%.xml/%#text'

			String sqlStatement = "select * from nt:base where jcr:path like \'"
					+ folderpath
					+ "/%"
					+ fileextension
					+ "/%jcr:content\'"
					+ " and contains(" + attrname + ",\'" + assetpath + "\')";
			Query query = queryManager.createQuery(sqlStatement, Query.SQL);
			QueryResult result = query.execute();

			NodeIterator nodeIter = result.getNodes();

			while (nodeIter.hasNext()) {
				javax.jcr.Node node = nodeIter.nextNode();
				javax.jcr.Node docnode = getDocNodeFromResult(node);
				strlist.add(docnode.getPath());
			}

		} catch (Exception e) {
			log.error("Failed in getAssetSearchResultsForFileType(): "
					+ e.getMessage() + " Args: " + folderpath + ", "
					+ fileextension + ", " + attrname + ", " + assetpath);
		}
		finally
		{
			logOutSession(session);

		}

		return strlist;
	}

	private List<String> getWhereUsedForAsset(String path,Session session) {
		List<String> strlist = new ArrayList<>();            
            try{
                javax.jcr.Node backLinks = session.getNode("/content/fmdita/backLinks");
                javax.jcr.Node conrefBack = session.getNode("/content/fmdita/backLinks/conrefs");
                String hashpath = Integer.toString(path.hashCode());
                    Value values[] = new Value[0];
                    Value conrefValues[] = new Value[0];
                    if (backLinks.hasProperty(hashpath)) {
                        values = getValueArr(backLinks.getProperty(hashpath));
                    }
                    if (conrefBack.hasProperty(hashpath)) {
                        conrefValues = getValueArr(conrefBack.getProperty(hashpath));
                    }
                    for(int i = 0; i < values.length; i++){
                        strlist.add(values[i].getString());
                    }
                    for(int i = 0; i < conrefValues.length; i++){
                        strlist.add(conrefValues[i].getString());
                    }                    
            }
            catch(Exception e){
                log.error(e.getMessage());
            }
                return strlist;            
	}
        private Value[] getValueArr(Property prop) throws Exception {
            if (prop.isMultiple()) {
                return prop.getValues();
            }
            Value values[] = {prop.getValue()};
            return values;
        }
	private String getSearchResults(String folderpath, String attrname,
			String attrvalue, String text) {
		List<String> strlist = new ArrayList<>();
		Session session = null;
		try {

			session = repository.loginService(UtilConstants.SERVICE_USER_NAME, null);
			// QueryManager queryManager =
			// session.getWorkspace().getQueryManager();

			// select * from nt:base where contains(., 'changing') and jcr:path
			// like '/content/dam/%.xml/%#text'

			if (text.length() != 0) {
				strlist.addAll(getTextSearchResults(folderpath, text));
			}

			// select * from nt:base where jcr:path like '/content/dam/%.xml/%'
			// and contains(dita_href, 'concepts')

			else if (attrname.length() != 0 && attrvalue.length() != 0) {
				strlist.addAll(getAttributeSearchResults(folderpath, attrname, attrvalue));
			}
		} catch (Exception e) {
			log.error("Failed in getAssetSearchResultsForFileType(): "
					+ e.getMessage() + " Args: " + folderpath + ", " + attrname
					+ ", " + attrvalue + ", " + text);
		}
		finally
		{
			logOutSession(session);

		}

		Gson gson = new Gson();
		String strlistjson = gson.toJson(strlist);

		return strlistjson;
	}

    private String getTextNodeFromResult(org.w3c.dom.Node node) {
        String res = "";
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                res += child.getNodeValue();
            }
            else if(child.getNodeType() == Node.ELEMENT_NODE)
                res+=getTextNodeFromResult(child);
        }
        return res;

    }

	private javax.jcr.Node getDocNodeFromResult(javax.jcr.Node node) {
		javax.jcr.Node docnode = node;

		while (docnode != null) {
			try {
				if (docnode.getProperty(JcrConstants.JCR_PRIMARYTYPE)
						.getString().equalsIgnoreCase("dam:Asset")) {
					break;
				}

				docnode = docnode.getParent();
			} catch (Exception e) {
				docnode = null;
			}
		}

		return docnode;
	}

	private String getSearchResultsUsingXpath(String folderpath, String xpath) {

		List<String> strlist = new ArrayList<>();
		Session session = null;
		try {

			session = repository.loginService(UtilConstants.SERVICE_USER_NAME, null);
			QueryManager queryManager = session.getWorkspace()
					.getQueryManager();

			String sqlStatement = "SELECT * FROM [dam:Asset] WHERE ISDESCENDANTNODE(["
					+ folderpath + "]) AND Name() LIKE '%.xml'";

			Query query = queryManager
					.createQuery(sqlStatement, Query.JCR_SQL2);
			QueryResult result = query.execute();
			NodeIterator nodeIter = result.getNodes();

			while (nodeIter.hasNext()) {
				javax.jcr.Node node = nodeIter.nextNode();
				List<String> results = searchFileForXpath(node.getPath(),
						xpath, session);
				strlist.addAll(results);
			}

		} catch (Exception e) {
			log.error("Failed in getSearchResultsUsingXpath(): "
					+ e.getMessage() + " Args: " + folderpath + ", " + xpath);
		}
		finally
		{
			logOutSession(session);

		}

		Gson gson = new Gson();
		String strlistjson = gson.toJson(strlist);

		return strlistjson;
	}

	private List<String> searchFileForXpath(String filepath, String xpath,
			Session session) {
		List<String> strlist = new ArrayList<>();

		try {
			Document doc = DomUtils.getDocumentFromRepoPath(filepath, session,
					false);

			if (doc != null) {
				XPath xPath = XPathFactory.newInstance().newXPath();

				javax.jcr.Node docNode = session.getNode(filepath + "/"
						+ javax.jcr.Node.JCR_CONTENT);

				String author = (NodeUtils.getNodeProperty(docNode,
						JcrConstants.JCR_LAST_MODIFIED_BY) != null ? NodeUtils
						.getNodeProperty(docNode,
								JcrConstants.JCR_LAST_MODIFIED_BY).getString()
						: "");

				long reviewstatus = reviewManager
						.getReviewStatusForNode(docNode);
				long approvalstatus = approvalManager
						.getApprovalStatusForNode(docNode);

				String expression = xpath;
				NodeList nodeList = (NodeList) xPath.compile(expression)
						.evaluate(doc, XPathConstants.NODESET);

				for (int i = 0; i < nodeList.getLength(); i++) {
					Node nNode = nodeList.item(i);
					strlist.add(filepath);
					strlist.add(nNode.getTextContent());
					strlist.add(getXPath(nNode));
					strlist.add(author);
					strlist.add(String.valueOf(reviewstatus));
					strlist.add(String.valueOf(approvalstatus));
				}
			}
		} catch (Exception e) {
			log.error("Failed in searchFileForXpath(): " + e.getMessage()
					+ " Args: " + filepath + ", " + xpath);
		}

		return strlist;

	}

	private String getXPath(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			int count = getNodeCount(node);
			return getXPath(node.getParentNode()) + "/" + node.getNodeName()
					+ (count > 1 ? "[" + count + "]" : "");
		}

		return "";
	}

	private int getNodeCount(Node node) {
		String nodename = node.getNodeName();
		int count = 1;

		Node sibling = node.getPreviousSibling();

		while (sibling != null) {
			if (sibling.getNodeType() == Node.ELEMENT_NODE) {
				if (sibling.getNodeName().equals(nodename))
					count++;
			}
			sibling = sibling.getPreviousSibling();
		}

		return count;
	}

	private Map<String, Object> scanDitamapForMetadata(String xmlPath, Map<String, String> keyspace,
            Session session) {
		try {
			javax.jcr.Node node = session.getNode(xmlPath + "/" + JcrConstants.JCR_CONTENT);

			List<Object> topics = new ArrayList<>();

			DependencyParser parser = new DependencyParser(session.getNode(xmlPath));
			List<javax.jcr.Node> dependencies = parser.bfsTraversal(session);
			for (int i = 0; i < dependencies.size(); i++) {
				String topicPath = dependencies.get(i).getPath();

				if (topicPath.toLowerCase().endsWith(".dita") || topicPath.toLowerCase().endsWith(".xml")) {
					Map<String, Object> topic = scanTopicForMetadata(topicPath,
							keyspace, session);
					topics.add(topic);
				}
			}
            
            MapDocument mapDocument = new MapDocument(session.getNode(xmlPath), new CustomDTDResolver(session));
            TOCAbsolutizeAdapter absolutizer = new TOCAbsolutizeAdapter(xmlPath);
            TOCExpandAdapter expander = new TOCExpandAdapter(session);
            TOCFlattenAdapterDFS flattener = new TOCFlattenAdapterDFS();
            
            List<MapTOCEntry> fullFlattenedTOC = flattener.adapt(expander.adapt(absolutizer.adapt(mapDocument.getTOC())));
            
            List<MapTOCEntry> missingTopics = new ArrayList<>();
            
            fullFlattenedTOC.forEach((tocEntry) -> {
                String href = PathUtils.getURLWithoutBookmark(tocEntry.getHref());
                href = PathUtils.getURLWithoutQueryParams(href);
                String scope = tocEntry.getScope();
                
                if (scope.isEmpty() || scope.equalsIgnoreCase("local")) {
                    try {
                        if (PathUtils.isAbsolutePath(href)) {
                            if (!session.nodeExists(href))
                                missingTopics.add(tocEntry);
                        } else {
                            if (!session.nodeExists(PathUtils.getAbsolutePath(xmlPath, href)))
                                missingTopics.add(tocEntry);
                        }
                    } catch (RepositoryException ex) {
                        log.warn("Failed checking the existence of {} with error {}", href, ex.getMessage());
                    }
                }
            });
            
            List<Object> missingTopicsJsonList = new ArrayList<>();
            missingTopics.forEach((missingTopic) -> {
                String format;
                
                switch (missingTopic.getFormat()) {
                    case XmlGlobals.FORMAT_MAP:
                        format = "Mapref";
                        break;
                    case XmlGlobals.FORMAT_DITAVAL:
                        format = "Ditavalref";
                        break;
                    default:
                        format = "Topic";
                        break;
                }
                
                Map<String, String> missingTopicMap = new HashMap<>();
                missingTopicMap.put("title", missingTopic.getTitle());
                missingTopicMap.put("href", missingTopic.getHref());
                missingTopicMap.put("type", format);
                
                missingTopicsJsonList.add(missingTopicMap);
            });

			String title = mapDocument.getTitle();

			String publishstatus = NodeUtils.getNodeProperty(node,
					"publishStatus") != null ? NodeUtils.getNodeProperty(node,
					"publishStatus").getString() : "";

			javax.jcr.Node renditionsNode = session.getNode(xmlPath+"/jcr:content/renditions/original/jcr:content");
			String author = (NodeUtils.getNodeProperty(renditionsNode,
					JcrConstants.JCR_LAST_MODIFIED_BY) != null ? NodeUtils
					.getNodeProperty(renditionsNode, JcrConstants.JCR_LAST_MODIFIED_BY)
					.getString() : "");

			Map<String, Object> metadata = new HashMap<>();

			metadata.put("path", xmlPath);
			metadata.put("publishstatus", publishstatus);
			metadata.put("linkstatus", 1);
			metadata.put("topics", topics);
			metadata.put("title", title);
			metadata.put("author", author);
			metadata.put("missingTopics", missingTopicsJsonList);


			return metadata;
		} catch (Exception e) {
            log.error("Failed reading ditamap metadata.", e);
		}

		Map<String, Object> metadata = new HashMap<>();
		metadata.put("linkstatus", 0);

		return metadata;
	}

	private String resolveKeyrefHrefForTopic(String refpair,
			Map<String, String> keyspace, Session session) {
		String[] references = refpair.split(",");

		if (references.length >= 1) {
			String keyref = references[0];
			String href = (references.length == 1) ? "" : references[1];

			if (keyref.length() != 0) // keyref
			{
				if (keyspace.containsKey(keyref)) {
					String resolvedPath = keyspace.get(keyref);

					try {
						if (session.nodeExists(resolvedPath)
								|| href.length() == 0)
							return resolvedPath;
					} catch (Exception e) {
					}
				}
			}

			return href;

		}
		return "";
	}

	private String resolveKeyrefHrefForElement(String refpair,
			Map<String, String> keyspace, Session session) {

		String[] references = refpair.split(",");

		if (references.length >= 1) {
			String keyref = references[0];
			String href = (references.length == 1) ? "" : references[1];

			if (keyref.length() != 0) // keyref
			{
				String /* topicid = "", */key, elemid = "";
				int indexslash = keyref.indexOf("/");

				if (indexslash != -1) {
					elemid = keyref.substring(indexslash + 1);
					key = keyref.substring(0, indexslash);
				} else {
					key = keyref;
				}

				if (keyspace.containsKey(key)) {
					String resolvedPath = keyspace.get(key);
					try {
						if (session.nodeExists(resolvedPath)) {

							if (elemid.length() != 0) {
								javax.jcr.Node keyrefnode = session
										.getNode(resolvedPath + "/"
												+ JcrConstants.JCR_CONTENT);

								String ids = NodeUtils.getNodeProperty(
										keyrefnode, XmlGlobals.JCR_ATTR_IDS) != null ? NodeUtils
										.getNodeProperty(keyrefnode,
												XmlGlobals.JCR_ATTR_IDS)
										.getString() : "";
								List<String> idlist = ids.equals("") ? new ArrayList<>()
										: new ArrayList<>(
												Arrays.asList(ids.split(",")));

								if (idlist.contains(elemid)
										|| href.length() == 0) {
									return resolvedPath + "#" + elemid;
								}
							} else {
								return resolvedPath;
							}
						}
					} catch (Exception e) {

					}
				}
			}

			return href;
		}
		return "";
	}

	private String[] getStringsProperty(javax.jcr.Node node, String property) {
		String[] strings = new String[0];

		try {
			Value[] propvalues = node.getProperty(property).getValues();
			String[] stringvalues = new String[propvalues.length];

			for (int i = 0; i < propvalues.length; i++) {
				stringvalues[i] = propvalues[i].getString();
			}

			strings = stringvalues;
		} catch (Exception e) {

		}

		return strings;
	}

	private Map<String, Object> scanTopicForMetadata(String xmlpath,
			Map<String, String> keyspace, Session session) {
		try {
			javax.jcr.Node node = session.getNode(xmlpath + "/"
					+ JcrConstants.JCR_CONTENT);

			String[] xrefs = getStringsProperty(node, XmlGlobals.JCR_ATTR_XREFS);
			String[] images = getStringsProperty(node, XmlGlobals.JCR_ATTR_IMAGES);
			String[] conrefs = getStringsProperty(node, XmlGlobals.JCR_ATTR_CONREFS);

			String ids = NodeUtils.getNodeProperty(node, XmlGlobals.JCR_ATTR_IDS) != null ?
                    NodeUtils.getNodeProperty(node, XmlGlobals.JCR_ATTR_IDS).getString() : "";
			String title = NodeUtils.getNodeProperty(node, XmlGlobals.JCR_ATTR_TITLE) != null ?
                    NodeUtils.getNodeProperty(node, XmlGlobals.JCR_ATTR_TITLE).getString() : "";

					if(title.startsWith("conref")){
						String conref = title.substring(title.indexOf(":"));
						title = resolveConref(conref,session);
					}
					else if(title.startsWith("conkeyref")){
						String conkeyref = title.substring(title.indexOf(":")+1);
						String conref = resolveKeyrefHrefForElement(conkeyref, keyspace,
								session);
						title = resolveConref(conref,session);
					}
					if(title == null || "".equals(title)){
						title = xmlpath.substring(xmlpath.lastIndexOf("/")+1);
						title = FilenameUtils.removeExtension(title);
					}

			// String reviewpath = NodeUtils.getNodeProperty(node, "reviewPath")
			// != null ? NodeUtils.getNodeProperty(node,
			// "reviewPath").getString() : "";
			String taskid = NodeUtils.getNodeProperty(node, "taskId") != null ? NodeUtils
					.getNodeProperty(node, "taskId").getString() : "";
			// String reviewmessage = NodeUtils.getNodeProperty(node,
			// "reviewMessage") != null ? NodeUtils.getNodeProperty(node,
			// "reviewMessage").getString(): "";

			javax.jcr.Node renditionsNode = session.getNode(xmlpath+"/jcr:content/renditions/original/jcr:content");
			String author = (NodeUtils.getNodeProperty(renditionsNode,
					JcrConstants.JCR_LAST_MODIFIED_BY) != null ? NodeUtils
					.getNodeProperty(renditionsNode, JcrConstants.JCR_LAST_MODIFIED_BY)
					.getString() : "");

			List<String> xreflist = new ArrayList<>();
			List<String> imagelist = new ArrayList<>();
			List<String> conreflist = new ArrayList<>();
			List<String> idlist = ids.equals("") ? new ArrayList<>()
					: new ArrayList<>(Arrays.asList(ids.split(",")));

            for (String xref : xrefs) {
                String href = resolveKeyrefHrefForElement(xref, keyspace, session);
                xreflist.add(href);
            }

            for (String image : images) {
                String href = resolveKeyrefHrefForTopic(image, keyspace, session);
                imagelist.add(href);
            }

            for (String conref : conrefs) {
                String href = resolveKeyrefHrefForElement(conref, keyspace, session);
                conreflist.add(href);
            }

			Map<String, Object> metadata = new HashMap<>();

			List<Map<String, Object>> xreflist1 = new ArrayList<>();

			for (int i = 0; i < xreflist.size(); i++) {
				Map<String, Object> xref = new HashMap<>();
				xref.put("path", xreflist.get(i));
				xref.put("linkstatus", 0);
				xreflist1.add(xref);
			}

			List<Map<String, Object>> imagelist1 = new ArrayList<>();

			for (int i = 0; i < imagelist.size(); i++) {
				Map<String, Object> image = new HashMap<>();
				image.put("path", imagelist.get(i));
				image.put("linkstatus", 0);
				imagelist1.add(image);
			}

			List<Map<String, Object>> conreflist1 = new ArrayList<>();

			for (int i = 0; i < conreflist.size(); i++) {
				Map<String, Object> conref = new HashMap<>();
				conref.put("path", conreflist.get(i));
				conref.put("linkstatus", 0);
				conreflist1.add(conref);
			}

			String docstate = DocumentStateUtil.getDocState(xmlpath, session);
			if(docstate == null)
				docstate = "";

			metadata.put("path", xmlpath);
			metadata.put("xrefs", xreflist1);
			metadata.put("images", imagelist1);
			metadata.put("conrefs", conreflist1);
			metadata.put("ids", idlist);
			metadata.put("title", title);
			metadata.put("author", author);

			metadata.put("taskid", taskid);
			addCollabMetaData(metadata, node, false);
			addCollabMetaData(metadata, node, true);
			metadata.put("linkstatus", 1);

			metadata.put("docstate", docstate);

			return metadata;
		} catch (Exception e) {
			log.error("Failed in scanTopicForMetadata(): " + e.getMessage()
					+ " Args: " + xmlpath);
		}

		Map<String, Object> metadata = new HashMap<>();
		metadata.put("path", xmlpath);
		metadata.put("linkstatus", 0);

		return metadata;
	}

	private String resolveConref(String conref, Session session) {
		String title = "";
		int idx = conref.indexOf("#");
		if(idx>-1){
			String topicName = conref.substring(0, idx);
			String elemId = conref.substring(idx+1);
			Document doc = DomUtils.getDocumentFromRepoPath(topicName, session,false);
			try {
				XPath xPath = XPathFactory.newInstance().newXPath();

				String expression = "//*[contains(@id,'" + elemId + "')]"; //TODO:find out the correct xpath for topicrefs

				Node node = (Node) xPath.compile(expression).evaluate(doc, XPathConstants.NODE);

				title=node.getTextContent();
			}
			catch(Exception e){
				title = "";
			}
		}

		return title;
	}

	private void readGeneratedOutputMetadata(String sourcePath,
			Map<String, Object> metadata) {
		List<Map<String, Object>> queuedOutputs = new ArrayList<>();
		List<Map<String, Object>> finishedOutputs = new ArrayList<>();

		javax.jcr.Node publishHistory;
		Session session = null;
		try {
			session = repository.loginService(UtilConstants.SERVICE_USER_NAME, null);
            String uuID = session.getNode(sourcePath).getIdentifier();
            String historyNodePath = "/content/fmdita/" + Defaults.OUTPUT_METADATA_NODE +
                                        "/" + Defaults.OUTPUT_HISTORY_NODE + "/" + uuID;
            publishHistory = session.getNode(historyNodePath);
			NodeIterator outputNodes = publishHistory.getNodes();

			while (outputNodes.hasNext()) {
				javax.jcr.Node outputNode = outputNodes.nextNode();
				if (outputNode != null) {
					Map<String, Object> output = new HashMap<>();

                    String outputType = NodeUtils.getStringProperty(outputNode, Defaults.OUTPUT_METADATA_PROP_TYPE, null);
                    if (outputType != null) {
                        output.put(Defaults.OUTPUT_METADATA_PROP_TYPE, outputType);
                    }

                    String outputPath = NodeUtils.getStringProperty(outputNode, Defaults.OUTPUT_METADATA_PROP_PATH, null);
                    if (outputPath != null) {
                        output.put(Defaults.OUTPUT_METADATA_PROP_PATH, outputPath);
                    }

                    String ditaOTLogFile = NodeUtils.getStringProperty(outputNode, Defaults.OUTPUT_METADATA_PROP_DITAOTLOG, null);
                    if (ditaOTLogFile != null) {
                        output.put(Defaults.OUTPUT_METADATA_PROP_DITAOTLOG, ditaOTLogFile);
                    }

                    Boolean ditaOTFailure = NodeUtils.getBooleanProperty(outputNode, Defaults.OUTPUT_METADATA_PROP_FALIURE, null);
                    if (ditaOTFailure != null) {
                        output.put(Defaults.OUTPUT_METADATA_PROP_FALIURE, ditaOTFailure);
                    }

                    String initiator = NodeUtils.getStringProperty(outputNode, Defaults.OUTPUT_METADATA_PROP_INITIATOR, null);
                    if (initiator != null) {
                        output.put(Defaults.OUTPUT_METADATA_PROP_INITIATOR, initiator);
                    }

                    Long generatedTime = NodeUtils.getLongProperty(outputNode, Defaults.OUTPUT_METADATA_PROP_TIME, null);
                    if (generatedTime != null) {
                        output.put(Defaults.OUTPUT_METADATA_PROP_TIME, generatedTime);
                    }

                    String outputTitle = NodeUtils.getStringProperty(outputNode, Defaults.OUTPUT_METADATA_PROP_TITLE, null);
                    if (outputTitle != null) {
                        output.put(Defaults.OUTPUT_METADATA_PROP_TITLE, outputTitle);
                    }

                    Long generatedIn = NodeUtils.getLongProperty(outputNode, Defaults.OUTPUT_METADATA_PROP_GENERATEDIN, null);
                    if (generatedIn != null) {
                        output.put(Defaults.OUTPUT_METADATA_PROP_GENERATEDIN, generatedIn);
                    }

                    String outputSetting = NodeUtils.getStringProperty(outputNode, Defaults.OUTPUT_METADATA_PROP_OUTPUTSETTING, null);
                    if (outputSetting != null) {
                        output.put(Defaults.OUTPUT_METADATA_PROP_OUTPUTSETTING, outputSetting);
                    }

                    String outputStatus = NodeUtils.getStringProperty(outputNode, "outputStatus", null);
                    output.put("outputStatus", outputStatus);
                    if (Constants.OUTPUT_STATUS_EXECUTING.equals(outputStatus) ||
                            Constants.OUTPUT_STATUS_SCHEDULED.equals(outputStatus)) {
                        Long startTime = NodeUtils.getLongProperty(outputNode, "executionTime", null);
                        Long currentTime = System.currentTimeMillis();
                        if (startTime != null) {
                            output.put("elapsedTime", currentTime - startTime);
                        }
                        queuedOutputs.add(output);
                    }
                    else if (Constants.OUTPUT_STATUS_FINISHED.equals(outputStatus)) {
                        if (output.get(Defaults.OUTPUT_METADATA_PROP_TIME) != null) {
                            // Check for existence of OUTPUT_METADATA_PROP_TIME as
                            // that is a required property for sorting the outputs.
                            finishedOutputs.add(output);
                        }
                    }
				}
			}
		} catch (RepositoryException e) {
			// Failed to read history node.
			log.info("Failed to read outputHistory. {}", e.getMessage());
		} finally {
			logOutSession(session);
		}

        Collections.sort(finishedOutputs, (Map<String, Object> o1, Map<String, Object> o2) -> ((Long) o2
            .getOrDefault(Defaults.OUTPUT_METADATA_PROP_TIME, Long.MAX_VALUE)).compareTo((Long) o1
                .getOrDefault(Defaults.OUTPUT_METADATA_PROP_TIME, Long.MAX_VALUE)));

		if (outputHistoryLimit > 0) {
			if (finishedOutputs.size() > (int) outputHistoryLimit) {
				finishedOutputs = finishedOutputs.subList(0, (int)outputHistoryLimit);
			}
		} else {
			finishedOutputs.clear();
		}

		metadata.put(Defaults.OUTPUT_HISTORY_LIST, finishedOutputs);
		metadata.put("queuedOutputs", queuedOutputs);
	}

	private void addCollabMetaData(Map<String, Object> metadata,
			javax.jcr.Node jcrContentNode, boolean bIsReview) throws Exception {
		List<Map<String, Object>> metaList = new ArrayList<>();
		Session session = null;
		try {
			session = repository.loginService(UtilConstants.SERVICE_USER_NAME, null);

			WorkflowSession wfSession = workflowService
					.getWorkflowSession(session);
			String workflowArr[] = XUtils.getStringsProperty(jcrContentNode,
					bIsReview ? UtilConstants.REVIEW_WF_PROP_NAME
							: UtilConstants.APPROVAL_WF_PROP_NAME);
			if (workflowArr != null && workflowArr.length > 0) {

				String collabPage = (bIsReview ? (UtilConstants.REVIEW_PAGE + workflowArr[0])
                		: (UtilConstants.APPROVAL_PAGE + wfSession.getWorkflow(workflowArr[0]).getWorkflowData().getMetaDataMap().get(UtilConstants.TASK_ID, String.class)))
                		+ UtilConstants.URL_PARAM_PAGE_READ_ONLY;

                for (String workflowId : workflowArr) {
                    Workflow workflow = wfSession.getWorkflow(workflowId);
                    Long reviewStatus = workflow.getWorkflowData()
                        .getMetaDataMap()
                        .get(UtilConstants.STATUS_PROP_NAME, Long.class);
                    if (reviewStatus != null) {
                        Map<String, Object> collabObject = new HashMap<>();
                        collabObject.put(bIsReview ? UtilConstants.REVIEW_STATUS: UtilConstants.APPROVAL_STATUS,
                            reviewStatus);

                        collabObject.put("path", collabPage);

                        Date sDate = workflow.getTimeStarted();
                        Date eDate = workflow.getTimeEnded();
                        String dateFrom;
                        if (sDate != null) {
                            dateFrom = new SimpleDateFormat("dd/MM/yyyy")
                                .format(sDate);
                            collabObject.put("startDate", dateFrom);
                        }
                        String dateTo;
                        if (eDate != null) {
                            dateTo = new SimpleDateFormat("dd/MM/yyyy")
                                .format(eDate);
                            collabObject.put("endDate", dateTo);
                        }
                        /* TODO : Comments Count
                        String wfComments = "{}";
                        Object commentsObj = workflow.getWorkflowData()
                            .getMetaDataMap().get("wfCommentsJSON");
                        if (commentsObj != null) {
                            wfComments = commentsObj.toString();
                        }
                        JSONObject wfCommentsObj = new JSONObject(wfComments);
                        Iterator<?> keys = wfCommentsObj.keys();

                        Long commentsCount = (long)0;
                        while (keys.hasNext()) {
                            String key = (String) keys.next();
                            if (wfCommentsObj.get(key) instanceof JSONObject) {
                                JSONObject topicObj = wfCommentsObj
                                    .getJSONObject(key);
                                if (topicObj.has("comments")) {
                                    JSONArray topicComments = topicObj
                                        .getJSONArray("comments");
                                    commentsCount += topicComments.length();
                                }
                            }
                        }
                        collabObject.put("comments", commentsCount);
                        */
                        String reviewTitle = (String) workflow
                            .getWorkflowData().getMetaDataMap()
                            .get("title").toString();
                        collabObject.put("title", reviewTitle);

                        String deadline = (String) workflow.getWorkflowData()
                            .getMetaDataMap().get("deadline").toString();
                        if (deadline != null)
                            collabObject.put("deadline", deadline);

                        String assignee = (String) workflow.getWorkflowData()
                            .getMetaDataMap().get("assignee").toString();
                        collabObject.put("assignee", assignee);

                        metaList.add(collabObject);
                    }
                }
			}
		} catch (Exception e) {
			log.info("Get Review Status from Content Node failed", e);
		} finally {
			logOutSession(session);

		}

		if(bIsReview){
		metadata.put("reviews", metaList);
		long reviewstatus = reviewManager
				.getReviewStatusForNode(jcrContentNode);
		metadata.put(UtilConstants.REVIEW_STATUS, reviewstatus);
		}
		else{
			metadata.put("approvals", metaList);
			long approvalstatus = approvalManager
					.getApprovalStatusForNode(jcrContentNode);
			metadata.put(UtilConstants.APPROVAL_STATUS, approvalstatus);
		}
	}

	private void addMissingLinkData(Map<String, Object> _topic,
			Map<String, Object> _linkpath, javax.jcr.Node _reportsDataNode,
			String _nodeProperty) {

		try {
			if (_reportsDataNode.hasProperty(_nodeProperty)) {

				boolean isMissing = false;
				String jsonUsedInData = _reportsDataNode.getProperty(
						_nodeProperty).getString();
				String topicPath = (String) _topic.get("path");
				String linkPath = (String) _linkpath.get("path");
				JSONObject missingData = new JSONObject(jsonUsedInData);
				if (missingData.has(topicPath)) {
					JSONArray missingLinks = missingData
							.getJSONArray(topicPath);
					for (int i = 0, size = missingLinks.length(); i < size; i++) {
						String missingLink = missingLinks.get(i).toString();
						if (missingLink.equalsIgnoreCase(linkPath)) {
							isMissing = true;
							break;
						}
					}
				}
				_linkpath.put("linkstatus", isMissing ? 0 : 1);
			}
		} catch (Exception e) {
			log.debug("Failed to add missing data for ditamap reports.");
		}
	}

	private void addReportsData(Map<String, Object> _topic, String _dataType,
			javax.jcr.Node _reportsDataNode, String _nodeProperty) {

		try {
			if (_reportsDataNode.hasProperty(_nodeProperty)) {

				List<String> references = new LinkedList<>();
				String jsonUsedInData = _reportsDataNode.getProperty(
						_nodeProperty).getString();
				String path = (String) _topic.get("path");
				JSONObject usedInData = new JSONObject(jsonUsedInData);
				if (usedInData.has(path)) {
					JSONArray refTopics = usedInData.getJSONArray(path);
					for (int i = 0, size = refTopics.length(); i < size; i++) {
						String refTopic = refTopics.get(i).toString();
						references.add(refTopic);
					}
				}
				_topic.put(_dataType, references);
			}
		} catch (Exception e) {
			log.debug("Failed to add " + _dataType
					+ " data for ditamap reports.");
		}
	}

	private void resolveWhereUsed(String _ditamap,
			Map<String, Object> metadata, Session session) {

		try {
			javax.jcr.Node reportsDataNode;
			javax.jcr.Node jcrNode = session.getNode(_ditamap
					+ NodeUtils.JCR_CONTENT);
			if (jcrNode.hasProperty(REPORTS_DATA_FOLDER)) {
				String reportsDataPath = jcrNode.getProperty(
						REPORTS_DATA_FOLDER).getString();
				reportsDataNode = session.getNode(reportsDataPath);

				List<Object> topics = (List<Object>) metadata.get("topics");
				for (int i = 0; topics != null && i < topics.size(); i++) {
					Map<String, Object> topic = (Map<String, Object>) topics
							.get(i);
					addReportsData(topic, "whereused", reportsDataNode,
							REPORTS_USEDIN_JSON);
					addReportsData(topic, "missingImages", reportsDataNode,
							REPORTS_MISSING_IMAGES_JSON);
					addReportsData(topic, "missingLinks", reportsDataNode,
							REPORTS_MISSING_LINKS_JSON);

					List<Map<String, Object>> xreflist = (List<Map<String, Object>>) topic
							.get("xrefs");
					for (int j = 0; xreflist != null && j < xreflist.size(); j++) {
						Map<String, Object> xref = xreflist.get(j);
						addMissingLinkData(topic, xref, reportsDataNode,
								REPORTS_MISSING_LINKS_JSON);
					}

					List<Map<String, Object>> conreflist = (List<Map<String, Object>>) topic
							.get("conrefs");
					for (int j = 0; conreflist != null && j < conreflist.size(); j++) {
						Map<String, Object> conref = conreflist.get(j);
						addMissingLinkData(topic, conref, reportsDataNode,
								REPORTS_MISSING_LINKS_JSON);
					}

					List<Map<String, Object>> imagelist = (List<Map<String, Object>>) topic
							.get("images");
					for (int j = 0; imagelist != null && j < imagelist.size(); j++) {
						Map<String, Object> image = imagelist.get(j);
						addMissingLinkData(topic, image, reportsDataNode,
								REPORTS_MISSING_IMAGES_JSON);
					}
				}

				if (reportsDataNode.hasProperty(REPORTS_GENERATION_TIME)) {
					long reportsGenerationTime = reportsDataNode.getProperty(
							REPORTS_GENERATION_TIME).getLong();
					metadata.put(REPORTS_GENERATION_TIME, reportsGenerationTime);
				}
			}
		} catch (Exception e) {
			log.debug("Failed to add reports data for " + _ditamap
					+ " ditamap.");
		}
	}

	@Override
	protected void doPost(SlingHttpServletRequest request,
			SlingHttpServletResponse response) throws ServerException,
			IOException {
		String operation = request.getParameter(":operation");
		if (operation == null || operation.trim().length() == 0)
			return;

        response.setCharacterEncoding("UTF-8");

		Operation operationCode = null;
		// ResourceBundle locResBundle =
		// request.getResourceBundle(request.getLocale());

		try {
			operationCode = Operation.valueOf(operation.toUpperCase());
		} catch (Exception e) {
			// Uncomment the following two lines when the whole if-else
			// structure for checking
			// operation value is using operationCode instead of operation
			// varibale.
			// response.sendError(HttpServletResponse.SC_BAD_REQUEST,
			// locResBundle.getString("Invalid operation code."));
			// return;
		}

		operation = operation.toLowerCase();
		ResourceResolver resourceResolver = null;
		try {
			resourceResolver = resolverFactory
					.getServiceResourceResolver(UtilConstants.serviceUserParams);
		} catch (Exception e) {
			log.error(e.toString());
		}
		Session session = resourceResolver.adaptTo(Session.class);
		WorkflowModel wfModel;
		WorkflowData wfData;
		WorkflowSession wfSession;

		if (operationCode == Operation.SAVEOUTPUT) {
			try {
				OutputAPI.handleSaveOutputOperation(request, response);
			} catch (RepositoryException e) {
				log.error("Failed in doPost(): " + operationCode + ": "
						+ e.getMessage());
			}
		}else if (operationCode == Operation.GETOUTPUT) {
			OutputAPI.handleGetOutputOperation(request, response);
		} else if (operationCode == Operation.DELETEOUTPUT) {
			try {
				OutputAPI.handleDeleteOutputOperation(request, response);
			} catch (RepositoryException e) {
				log.error("Failed in doPost(): " + operationCode + ": "
						+ e.getMessage());
			}
		} else if (operationCode == Operation.CREATEOUTPUT) {
			OutputAPI.handleCreateOutputOperation(request, response);
		} else if (operationCode == Operation.GETALLOUTPUTS) {
			OutputAPI.handleGetAllOutputsOperation(request, response);
		} else if (operationCode == Operation.GETDEFAULTTEMPLATE) {
			OutputAPI.handleGetDefaultTemplateOperation(request, response);
		} else if (operationCode == Operation.GETVERSIONDATA) {
			String sourcePath = "";
			try {
				sourcePath = java.net.URLDecoder.decode(request.getParameter("sourcePath"), "UTF-8");
				DependencyParser parser = new DependencyParser(session.getNode(sourcePath));
				Map<String, Object> versionData = parser.bfsTraversalWithVersions();
				Gson gson = new Gson();
				response.getOutputStream().println(gson.toJson(versionData));
				response.setStatus(SlingHttpServletResponse.SC_OK);
			} catch (Exception ex) {
				log.error(Operation.GETVERSIONDATA.toString() + ": Failed for " + sourcePath
						+ ". with error: " + ex.getMessage());
				response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}

		} else if ((Operation.ADDEVENTS == operationCode)) {
                     String result = reviewManager.AddEvents(request.getParameter(UtilConstants.TOPIC_INDEX),
                            request.getParameter(UtilConstants.WORKFLOW_ID),
                            Integer.parseInt(request.getParameter("eventCount")),
                            request.getParameter("events"),
                            request.getResourceResolver(),false).toString();
                        response.setContentType(RESPONSE_TYPE_JSON);
			if (result == null) {
				response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} else {
				response.setStatus(SlingHttpServletResponse.SC_OK);
				response.getWriter().println(result);
			}
                }  else if (operationCode == Operation.SAVEBASELINE) {
			try {
				BaselineAPI.handleSaveBaselineOperation(request, response);
			} catch (RepositoryException e) {
				log.error("Failed in doPost(): " + operationCode + ": "
						+ e.getMessage());
			}
		} else if (operationCode == Operation.GETBASELINE) {
			BaselineAPI.handleGetBaselineOperation(request, response);
		} else if (operationCode == Operation.REMOVEBASELINE) {
			try {
				BaselineAPI.handleRemoveBaselineOperation(request, response);
			} catch (RepositoryException e) {
				log.error("Failed in doPost(): " + operationCode + ": " + e.getMessage());
			}
		} else if (operationCode == Operation.CREATEBASELINE) {
			BaselineAPI.handleCreateBaselineOperation(request, response);
		} else if (operationCode == Operation.GETALLBASELINES) {
			BaselineAPI.handleGetAllBaselinesOperation(request, response);
		}  else if (operationCode == Operation.GETNEWBASELINE) {
			String sourcePath = java.net.URLDecoder.decode(request.getParameter("sourcePath"), "UTF-8");
 			ScriptEngineManager manager = new ScriptEngineManager();
			ScriptEngine engine = manager.getEngineByName("JavaScript");
			// read script file
			try {
				DependencyParser parser = new DependencyParser(session.getNode(sourcePath));
				Map<String, Object> versionData = parser.bfsTraversalWithVersions();
				engine.eval(Files.newBufferedReader(Paths.get("C:/baseline/newbl.js"), StandardCharsets.UTF_8));
				Invocable inv = (Invocable) engine;
				// call function from script file
				String b = (String) inv.invokeFunction("Baseline", sourcePath, versionData);
				Gson gson = new Gson();
				Baseline baseline = gson.fromJson(b, Baseline.class);
				response.getOutputStream().println(gson.toJson(baseline));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}  else if (operation.equals("review-ditamap")) {
			handleOperationCreateReview(request, response, session);

		} else if (operationCode == Operation.RESTARTREVIEW) {

			session = resourceResolver.adaptTo(Session.class);
			String dueDate = request.getParameter(UtilConstants.TASK_DUE_DATE);
			String taskId = request.getParameter(UtilConstants.TASK_ID);
			String assignee = request.getParameter(UtilConstants.ASSIGNEE);
			String description = request
					.getParameter(UtilConstants.DESCRIPTION);
			String payload = request.getParameter("contentPath");
			String currentUser = request.getResourceResolver().getUserID();

			JSONObject reviewInfo = new JSONObject();
			try {
				reviewInfo.put(UtilConstants.TASK_DUE_DATE, dueDate);
				reviewInfo.put(UtilConstants.TASK_ID, taskId);
				reviewInfo.put(UtilConstants.ASSIGNEE, assignee);
				reviewInfo.put(UtilConstants.DESCRIPTION, description);
				reviewInfo.put("contentPath", payload);
				reviewInfo.put(UtilConstants.INITIATOR, currentUser);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				log.error(e.getMessage());
			}

			String reviewData = reviewManager.GetReviewData(resourceResolver,
					taskId, null, null,null,false);

			ResourceResolver resourceResolver_1 = request.getResourceResolver();
			if (false == reviewManager.RestartReview(reviewData,
					resourceResolver_1, reviewInfo)) {
				response.sendError(500, "Invalid JSON");
				return;
			}

		} else if ((Operation.ADDCOMMENT == operationCode)
				|| (Operation.ADDREPLY == operationCode)
				|| (Operation.COMMENTSTATUSCHANGE == operationCode)
				|| (Operation.SETREVIEWDATA == operationCode)
				|| (Operation.SETAPPROVALDATA == operationCode)) {
			resourceResolver = request.getResourceResolver();
			session = resourceResolver.adaptTo(Session.class);
			wfSession = workflowService.getWorkflowSession(session);
			ReviewComments reviewComments = new ReviewComments(session,
					resourceResolver, wfSession, approvalManager, reviewManager);

			int responseStatus = SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			if (Operation.ADDCOMMENT == operationCode) {
                                boolean status = reviewManager.AddComment(request, resourceResolver);
				responseStatus = status ? SlingHttpServletResponse.SC_CREATED
						: SlingHttpServletResponse.SC_NOT_FOUND;
			} else if (Operation.ADDREPLY == operationCode) {
                                boolean status = reviewManager.AddReply(request, resourceResolver);
				responseStatus = status ? SlingHttpServletResponse.SC_CREATED
						: SlingHttpServletResponse.SC_NOT_FOUND;
			} else if (Operation.COMMENTSTATUSCHANGE == operationCode) {

				boolean status = reviewManager.SaveCommentStatus(request, resourceResolver);
				responseStatus = status ? SlingHttpServletResponse.SC_OK
						: SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			} else if (Operation.SETREVIEWDATA == operationCode) {
				String taskId = request.getParameter(UtilConstants.TASK_ID);
				String reviewData = request
						.getParameter(UtilConstants.INPUT_PAYLOAD);
				boolean status = reviewManager.SetReviewData(resourceResolver,
						scheduler, reviewData, taskId);
				responseStatus = status ? SlingHttpServletResponse.SC_CREATED
						: SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			} else if (Operation.SETAPPROVALDATA == operationCode) {
				String taskId = request.getParameter(UtilConstants.TASK_ID);
				String reviewData = request
						.getParameter(UtilConstants.INPUT_PAYLOAD);
				boolean status = approvalManager.SetApprovalData(
						resourceResolver, reviewData, taskId);
				responseStatus = status ? SlingHttpServletResponse.SC_CREATED
						: SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			}
			response.setStatus(responseStatus);
		} else if(Operation.CREATECOLLECTION == operationCode) {
            if (collectionsAPI != null)
              collectionsAPI.handleCreateMapCollection(request, response);
        } else if(Operation.GETCOLLECTION == operationCode) {
            if (collectionsAPI != null)
              collectionsAPI.handleGetMapCollection(request, response);
        } else if (Operation.REMOVECOLLECTION == operationCode) {
            if (collectionsAPI != null)
              collectionsAPI.handleRemoveCollection(request, response);
        } else if (Operation.ADDENTRYTOCOLLECTION == operationCode) {
            if (collectionsAPI != null)
              collectionsAPI.handleAddEntryToCollection(request, response);
        } else if (Operation.REMOVEENTRYFROMCOLLECTION == operationCode) {
            if (collectionsAPI != null)
              collectionsAPI.handleRemoveEntryFromCollection(request, response);
        } else if (Operation.ADDPRESETTOCOLENTRY == operationCode) {
            if (collectionsAPI != null)
              collectionsAPI.handleAddPresetToEntry(request, response);
        } else if (Operation.REMOVEPRESETFROMCOLENTRY == operationCode) {
            if (collectionsAPI != null)
              collectionsAPI.handleRemovePresetFromEntry(request, response);
        } else if (Operation.REMOVEALLPRESETSFROMCOLENTRY == operationCode) {
            if (collectionsAPI != null)
              collectionsAPI.handleRemoveAllPresetsFromEntry(request, response);
        } else if (Operation.GENERATECOLLECTION == operationCode) {
            if (collectionsAPI != null)
              collectionsAPI.handleGenerateCollection(request, response);
        } else if (Operation.GETALLOUTPUTSMAPCOLLECTION == operationCode) {
            if (collectionsAPI != null)
              collectionsAPI.handleGetAllOutputsFromSource(request, response);
        }
        else if (operation.equals("send-for-approval")) {
			// Start approval workflow
			wfSession = workflowService.getWorkflowSession(session);
			String payloadJson = request.getParameter("contentPath");
			String sendTo = request.getParameter("assignee");
			String taskDueDate = request.getParameter("taskDueDate");
			String title = request.getParameter("title");
			String description = request.getParameter("description");
			if (description == null) {
				description = "";
			}
			String projectResourcePath = request.getParameter("projectPath");

			if (payloadJson == null || sendTo == null) // || taskDueDate ==
														// null)
			{
				response.sendError(500, "Invalid Arguments");
				return;
			}
			if (payloadJson.length() == 0 || sendTo.length() == 0) // ||
																	// taskDueDate.length()
																	// == 0)
			{
				response.sendError(500, "Invalid Arguments");
				return;
			}

			JSONObject reqObj;
			try {
				reqObj = new JSONObject(payloadJson);
			} catch (JSONException ex) {
				response.sendError(500, "Invalid JSON");
				return;
			}

			try {

				wfData = wfSession.newWorkflowData("JCR_PATH",
						reqObj.getString("base"));

				// Start approval workflow
				wfModel = wfSession
						.getModel("/etc/workflow/models/approval/jcr:content/model");
				setMetaData(wfData.getMetaDataMap(), new String[] {
						"operation", UtilConstants.DEADLINE,
						UtilConstants.ASSIGNEE, UtilConstants.TITLE,
						UtilConstants.DESCRIPTION, "target",
						UtilConstants.INPUT_PAYLOAD,
						UtilConstants.PROJECT_PATH, UtilConstants.START_TIME,
						UtilConstants.STATUS_PROP_NAME }, new Object[] {
						"AEM_APPROVAL", taskDueDate, sendTo, title,
						description, "", payloadJson, projectResourcePath,
						System.currentTimeMillis(), UtilConstants.IN_PROGRESS });

				wfData.getMetaDataMap().put(UtilConstants.INITIATOR,
						request.getResourceResolver().getUserID());

				/* wf = */wfSession.startWorkflow(wfModel, wfData);
				session.save();
				response.setStatus(SlingHttpServletResponse.SC_OK);
			} catch (Exception e) {
				log.error(e.toString());
			}
		} else if (operation.equals("approve-document")
				|| operation.equals("reject-document")) {
			resourceResolver = request.getResourceResolver();
			boolean bApprove = operation.equals("approve-document");
			String taskId = request.getParameter("taskId");
			String comment = request.getParameter("comment");
			String idx = request.getParameter("idx");
			if (approvalManager.SetTopicApprovalStatus(resourceResolver,
					taskId, bApprove, comment, idx))
				response.setStatus(SlingHttpServletResponse.SC_OK);
			else
				response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
		}
		else if(Operation.SETDOCSTATE == operationCode) {
			ResourceResolver resolver = request.getResourceResolver();
			Session userSession = resolver.adaptTo(Session.class);
			String filePath = request.getParameter("path");
			String docState = request.getParameter("docstate");
			if(DocumentStateUtil.setDocState(filePath, docState, userSession))
				response.setStatus(SlingHttpServletResponse.SC_OK);
			else
				response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
		}
	}

	void setMetaData(MetaDataMap map, String[] keys, Object[] values) {
		if (keys.length == values.length)
			for (int i = 0; i < keys.length; i++) {
				map.put(keys[i], values[i]);
			}
	}



	private void logOutSession(Session session) {
		if (session != null)
			session.logout();
	}

	private void sendErrorInfo(SlingHttpServletResponse response, Exception e,
			String message) {
		try {
			log.error(e.toString());
			if (log.isDebugEnabled())
				log.debug(e.toString(), e);
			response.sendError(
					SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					message != null ? message : e.getMessage());
		} catch (IOException s) {
			// Supress exception
		}
	}

	private void handleOperationZipFiles(SlingHttpServletRequest request,
			SlingHttpServletResponse response, Session session)
			throws IOException {
		Map<String, InputStream> dataMap = new HashMap<>();
		String ditaotlogfilepath = request.getParameter("contentPath");
		try {
			javax.jcr.Node fileNode = session.getNode(ditaotlogfilepath);
			javax.jcr.Node jcrContent = fileNode
					.getNode(JcrConstants.JCR_CONTENT);
			String fileName = fileNode.getName();
			InputStream content = jcrContent.getProperty(JcrConstants.JCR_DATA)
					.getStream();
			dataMap.put(fileName, content);
			byte[] zip = MiscUtils.zipFiles(dataMap);
			ServletOutputStream sos = response.getOutputStream();
			response.setContentType("application/zip");
			response.setHeader("Content-Disposition",
					"attachment;filename=Logs.zip");

			sos.write(zip);
			sos.flush();
		} catch (Exception e) {
			log.debug("Unable to download log file");
		}
	}

	private void handleOperationSearchEx(SlingHttpServletRequest request,
			SlingHttpServletResponse response) throws IOException {
		long startTime = System.currentTimeMillis();
		String text = (request.getParameter("text") != null) ? request
				.getParameter("text") : "";
		String attrname = (request.getParameter("attrname") != null) ? request
				.getParameter("attrname") : "";
		String attrvalue = (request.getParameter("attrvalue") != null) ? request
				.getParameter("attrvalue") : "";
		String path = (request.getParameter("path") != null) ? request
				.getParameter("path") : "/content/dam";

		if (text.length() != 0) {
			String result = getSearchResults(path, "", "", text);
			response.setContentType(RESPONSE_TYPE_JSON);
			response.getWriter().println(result);
		} else if (attrname.length() != 0 && attrvalue.length() != 0) {
			String result = getSearchResults(path, attrname, attrvalue, "");
			response.setContentType(RESPONSE_TYPE_JSON);
			response.getWriter().println(result);
		}
		long elapsedTime = System.currentTimeMillis() - startTime;
		perfLogger.info("SEARCH : {} ms", elapsedTime);
	}

	private void updateMetaData(SlingHttpServletResponse response, String src,
			Session session) throws IOException {
		if (src.endsWith(UtilConstants.DITAMAP_EXT)
				|| src.endsWith(UtilConstants.FM_EXT)
				|| src.endsWith(UtilConstants.BOOK_EXT)) {
			// parseDitamap(src, session);
			Session adminSession = null;
			try {
				adminSession = repository.loginService(UtilConstants.SERVICE_USER_NAME, null);
                MapDocument mapDocument = new MapDocument(adminSession.getNode(src), new CustomDTDResolver(session));
                SimpleKeySpaceAdapter keySpaceAdapter = new SimpleKeySpaceAdapter();
                Map<String, String> keySpace = keySpaceAdapter.adapt(mapDocument.getKeySpace());
				Map<String, Object> metadata = scanDitamapForMetadata(src, keySpace, session);

				resolveWhereUsed(src, metadata, session);
				response.setContentType(RESPONSE_TYPE_JSON);
				Gson gson = new Gson();
				String metadatajson = gson.toJson(metadata);
				response.getWriter().println(metadatajson);
			} catch (Exception e) {
				sendErrorInfo(response, e, null);
			} finally {
				logOutSession(adminSession);
			}
		}
	}

	private void handleOperationPublishBeacon(
			SlingHttpServletResponse response, String src) throws IOException {
		if (src.endsWith(UtilConstants.DITAMAP_EXT)
				|| src.endsWith(UtilConstants.FM_EXT)
				|| src.endsWith(UtilConstants.BOOK_EXT)) {
			Session adminSession = null;
			try {
				adminSession = repository.loginService(UtilConstants.SERVICE_USER_NAME, null);
				Map<String, Object> metadata = new HashMap<>();

				readGeneratedOutputMetadata(src, metadata);
				response.setContentType(RESPONSE_TYPE_JSON);
				Gson gson = new Gson();
				String metadatajson = gson.toJson(metadata);
				response.getWriter().println(metadatajson);
			} catch (Exception e) {
				sendErrorInfo(response, e, null);
			} finally {
				logOutSession(adminSession);
			}
		}
	}

	protected void handleOperationCreateReview(SlingHttpServletRequest request,
			SlingHttpServletResponse response, Session session)
			throws ServerException, IOException {

		// Start review workflow
		ResourceBundle resourceBundle = request.getResourceBundle(request.getLocale());
		String payloadJson = request.getParameter("contentPath");
		String assignTo = request.getParameter("assignee");
		String taskDueDate = request.getParameter("taskDueDate");
		String title = request.getParameter("title");
		String description = request.getParameter("description");
		if (description == null) {
			description = "";
		}
		String sendEmailNotification = request
				.getParameter("sendEmailNotification");
		boolean bExcludeTopics = true;
		String excludeTopics = request.getParameter("excludetopics");
		if (excludeTopics == null || !excludeTopics.equalsIgnoreCase("on"))
			bExcludeTopics = false;
		/**
		 * projectResource.getPath() is used to set as value of the drop down
		 * fields in create approval wizard; projectResource extends from
		 * resource
		 */
		String projectResourcePath = request.getParameter("projectPath");

		if (payloadJson == null || assignTo == null || taskDueDate == null) {
			response.sendError(500, resourceBundle
					.getString("Invalid Input Parameters to Create a Review."));
			return;
		}
		if (payloadJson.length() == 0 || assignTo.length() == 0
				|| taskDueDate.length() == 0) {
			response.sendError(500, resourceBundle
					.getString("Invalid Input Parameters to Create a Review."));
			return;
		}

		JSONObject reqObj;
		try {
			reqObj = new JSONObject(payloadJson);
		} catch (JSONException ex) {
			response.sendError(500,
					resourceBundle.getString("Invalid Content Path(s) JSON"));
			return;
		}

		try {
			WorkflowSession wfSession = workflowService
					.getWorkflowSession(session);
			WorkflowData wfData = wfSession.newWorkflowData("JCR_PATH",
					reqObj.getString(UtilConstants.INPUT_PAYLOAD_BASE));

			WorkflowModel wfModel = wfSession
					.getModel(UtilConstants.WF_MODEL_REVIEW_DITAMAP);
			wfData.getMetaDataMap().put(UtilConstants.OPERATION,
					UtilConstants.REVIEW_OPERATION);
			wfData.getMetaDataMap().put(UtilConstants.INPUT_PAYLOAD,
					payloadJson);
			wfData.getMetaDataMap().put(UtilConstants.DEADLINE, taskDueDate);
			wfData.getMetaDataMap().put(UtilConstants.ASSIGNEE, assignTo);
			wfData.getMetaDataMap().put(UtilConstants.TITLE, title);
			wfData.getMetaDataMap().put(UtilConstants.DESCRIPTION, description);
			wfData.getMetaDataMap().put(UtilConstants.NOTIFY_EMAIL,
					sendEmailNotification);
			wfData.getMetaDataMap().put(UtilConstants.STATUS_PROP_NAME,
					UtilConstants.IN_PROGRESS);
			wfData.getMetaDataMap().put(UtilConstants.PROJECT_PATH,
					projectResourcePath);
			wfData.getMetaDataMap().put(UtilConstants.START_TIME,
					System.currentTimeMillis());
			JSONArray assetArray = reqObj
					.getJSONArray(UtilConstants.INPUT_PAYLOAD_ASSET);
			String asset;
			asset = assetArray.get(0).toString();
			wfData.getMetaDataMap().put(UtilConstants.DITAMAP_INCLUDE_TOPICS,
					!bExcludeTopics && asset.endsWith(".ditamap"));

			String currentUser = request.getResourceResolver().getUserID();
			wfData.getMetaDataMap().put(UtilConstants.INITIATOR, currentUser);
                        String orgTopics = "";
                        for(int i = 0; i < assetArray.length(); i++){
                            String path = assetArray.get(i).toString();
                            orgTopics = orgTopics + path + XUtils.ITEM_SEPARATOR;
                        }
                        wfData.getMetaDataMap().put(UtilConstants.ORIGINAL_TOPICS,orgTopics);
			wfSession.startWorkflow(wfModel, wfData);

			session.save();
		} catch (Exception e) {
			log.error(e.toString());
			response.sendError(500,
					resourceBundle.getString("Failed to start review."));
		}
	}

	protected void handleOperationIncrementalPublish(SlingHttpServletRequest request,
            SlingHttpServletResponse response, Session session) throws ServerException, IOException
    {
		ResourceBundle resourceBundle = request.getResourceBundle(request.getLocale());
		String payloadJson = java.net.URLDecoder.decode(request.getParameter(CONTENT_PATH), "UTF-8");
		if (payloadJson == null) {
			response.sendError(500, resourceBundle.getString("Missing files or output name for incremental publish."));
			return;
		}

		JSONObject reqObj;
		try {
			reqObj = new JSONObject(payloadJson);
		} catch (JSONException ex) {
			response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					resourceBundle.getString("Invalid Content Path(s) JSON"));
			return;
		}

		try {
			JSONArray outputs = reqObj.getJSONArray(OUTPUTS);
			for (int i = 0; i < outputs.length(); i++) {
              String output = outputs.get(i).toString();
              String source = reqObj.getString(SOURCE_DITAMAP);
              String outputHistoryPath = Output.generateOutputHistoryNode(source,
                                  session.getUserID(), output, session);

				WorkflowSession wfSession = workflowService.getWorkflowSession(session);
				WorkflowData wfData = wfSession.newWorkflowData("JCR_PATH", source);
				WorkflowModel wfModel = wfSession.getModel(UtilConstants.PUBLISH_WORKFLOW_MODEL);
				wfData.getMetaDataMap().put("operation", PublishOutputType.PARTIAL_PUBLISH.name());
				wfData.getMetaDataMap().put(INPUT_PAYLOAD, payloadJson);
				wfData.getMetaDataMap().put(OUTPUT_NAME, output);
                wfData.getMetaDataMap().put("outputHistoryPath", outputHistoryPath);
				wfSession.startWorkflow(wfModel, wfData);
				session.save();
				response.setStatus(SlingHttpServletResponse.SC_OK);
			}
		} catch (Exception e) {
			log.error(e.toString(), e);
			response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					resourceBundle.getString("Failed to start incremental publish."));
		}
	}

	protected void handleOperationCloseReview(SlingHttpServletRequest request,
			SlingHttpServletResponse response) throws ServerException,
			IOException {

		Session session = null;
		try {

			ResourceResolver resourceResolver = request.getResourceResolver();
			String workflowId = request.getParameter(UtilConstants.WORKFLOW_ID);
			String taskId = request.getParameter(UtilConstants.TASK_ID);
			if (reviewManager.CloseReviewTask(resourceResolver, workflowId,
					taskId))
				response.sendRedirect("/assets.html/content/dam");
			else
				response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);

		} catch (IOException e) {
			log.info(e.toString(), e);
			response.sendError(500, e.toString());
		} finally {
			if (session != null) {
				session.logout();
			}
		}
	}

    public static void setCollectionsAPI(MapCollectionsAPI collectionsAPI) {
        PublishListener.collectionsAPI = collectionsAPI;
    }

    private String findDeepestCommonAncestor(List<javax.jcr.Node> nodes) throws RepositoryException {
		if (nodes == null || nodes.isEmpty()) {
			return "";
		}
		String commonAncestor = PathUtils.getNodeParentPath(nodes.get(0).getPath());
		for (javax.jcr.Node node: nodes) {
			String path = node.getPath();
			while(!path.startsWith(commonAncestor + "/") && commonAncestor.length() > 0) {
				commonAncestor = PathUtils.getNodeParentPath(commonAncestor);
			}
		}
		return commonAncestor + "/";
	}
}
