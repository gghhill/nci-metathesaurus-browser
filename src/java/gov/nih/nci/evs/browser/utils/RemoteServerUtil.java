package gov.nih.nci.evs.browser.utils;

//v5.0
import gov.nih.nci.system.applicationservice.ApplicationException;
import gov.nih.nci.system.client.ApplicationServiceProvider;
import org.LexGrid.LexBIG.DataModel.Collections.CodingSchemeRenderingList;
import org.LexGrid.LexBIG.DataModel.InterfaceElements.CodingSchemeRendering;
import org.LexGrid.LexBIG.Exceptions.LBInvocationException;
import org.LexGrid.LexBIG.caCore.interfaces.LexEVSApplicationService;
import org.LexGrid.LexBIG.caCore.interfaces.LexEVSDataService;
import org.LexGrid.LexBIG.caCore.interfaces.LexEVSDistributed;
import org.LexGrid.LexBIG.caCore.interfaces.LexEVSService;
import org.LexGrid.codingSchemes.CodingScheme;

import gov.nih.nci.evs.browser.properties.NCImBrowserProperties;
import java.util.Hashtable;
import java.util.Properties;

import org.LexGrid.LexBIG.LexBIGService.LexBIGService;
import org.LexGrid.LexBIG.Impl.LexBIGServiceImpl;

//KLO 100709
import gov.nih.nci.evs.security.SecurityToken;
import gov.nih.nci.evs.browser.bean.SecurityTokenHolder;
import java.util.List;

/**
  * <!-- LICENSE_TEXT_START -->
* Copyright 2008,2009 NGIT. This software was developed in conjunction with the National Cancer Institute,
* and so to the extent government employees are co-authors, any rights in such works shall be subject to Title 17 of the United States Code, section 105.
* Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
* 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the disclaimer of Article 3, below. Redistributions
* in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other
* materials provided with the distribution.
* 2. The end-user documentation included with the redistribution, if any, must include the following acknowledgment:
* "This product includes software developed by NGIT and the National Cancer Institute."
* If no such end-user documentation is to be included, this acknowledgment shall appear in the software itself,
* wherever such third-party acknowledgments normally appear.
* 3. The names "The National Cancer Institute", "NCI" and "NGIT" must not be used to endorse or promote products derived from this software.
* 4. This license does not authorize the incorporation of this software into any third party proprietary programs. This license does not authorize
* the recipient to use any trademarks owned by either NCI or NGIT
* 5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
* MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
* NGIT, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
* PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
* WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  * <!-- LICENSE_TEXT_END -->
  */

/**
  * @author EVS Team
  * @version 1.0
  *
  * Modification history
  *     Initial implementation kim.ong@ngc.com
  *
 */

public class RemoteServerUtil {
    private static boolean debug = false;
    private static String _serviceInfo = "EvsServiceInfo";
    //private Properties systemProperties = null;
    private static String serviceURL = null;

    public RemoteServerUtil() {
        // Do nothing
    }

    public static LexBIGService createLexBIGService()
    {
        String url = "http://ncias-d177-v.nci.nih.gov:19480/lexevsapi51";

        NCImBrowserProperties properties = null;
        try {
            properties = NCImBrowserProperties.getInstance();
            url = properties.getProperty(NCImBrowserProperties.EVS_SERVICE_URL);
            return createLexBIGService(url);
        } catch (Exception ex) {
            // Do nothing
            //System.out.println("WARNING: NCImBrowserProperties loading error...");
            //System.out.println("\t-- trying to connect to " + url + " instead.");
            ex.printStackTrace();
        }
        return null;//createLexBIGService(url);
    }


    public static LexBIGService createLexBIGService(String serviceUrl)
    {
        try {
            NCImBrowserProperties properties = null;
            properties = NCImBrowserProperties.getInstance();

            if (serviceUrl == null || serviceUrl.compareTo("") == 0)
            {
                String lg_config_file = properties.getProperty(NCImBrowserProperties.LG_CONFIG_FILE);
                System.setProperty(NCImBrowserProperties.LG_CONFIG_FILE,lg_config_file);
                LexBIGService lbSvc = new LexBIGServiceImpl();
                return lbSvc;
            }
            if (debug) {
                System.out.println(Utils.SEPARATOR);
                System.out.println("LexBIGService(remote): " + serviceUrl);
            }
            LexEVSApplicationService lexevsService = (LexEVSApplicationService)ApplicationServiceProvider.getApplicationServiceFromUrl(serviceUrl, "EvsServiceInfo");
            lexevsService = registerAllSecurityTokens(lexevsService);
            return (LexBIGService) lexevsService;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    //KLO 100709
    public static LexEVSApplicationService registerAllSecurityTokens(LexEVSApplicationService lexevsService) {
		List list = NCImBrowserProperties.getSecurityTokenList();
		if (list == null || list.size() == 0) return lexevsService;
		for (int i=0; i<list.size(); i++) {
            SecurityTokenHolder holder = (SecurityTokenHolder) list.get(i);
            lexevsService = registerSecurityToken(lexevsService, holder.getName(), holder.getValue());
		}
		return lexevsService;
	}

    //KLO 100709
    public static LexEVSApplicationService registerSecurityToken(LexEVSApplicationService lexevsService, String codingScheme, String token) {
		SecurityToken securityToken = new SecurityToken();
		securityToken.setAccessToken(token);
		Boolean retval = null;
		try {
			retval = lexevsService.registerSecurityToken(codingScheme, securityToken);
			if(retval != null && retval.equals(Boolean.TRUE))	{
				//System.out.println("Registration of SecurityToken was successful.");
			}
			else {
				System.out.println("WARNING: Registration of SecurityToken failed.");
			}
		} catch (Exception e) {
			System.out.println("WARNING: Registration of SecurityToken failed.");
		}
		return lexevsService;
	}


    //KLO 100709
    public static LexBIGService createLexBIGService(String serviceUrl, String codingScheme, String token) {
		SecurityToken securityToken = new SecurityToken();
		securityToken.setAccessToken(token);
		return createLexBIGService(serviceUrl, codingScheme, securityToken);
	}

    //KLO 100709
    public static LexBIGService createLexBIGService(String serviceUrl, String codingScheme, SecurityToken securityToken)
    {
        try {
            if (serviceUrl == null || serviceUrl.compareTo("") == 0)
            {
                LexBIGService lbSvc = new LexBIGServiceImpl();
                return lbSvc;
            }

            LexEVSApplicationService lexevsService = (LexEVSApplicationService)ApplicationServiceProvider.getApplicationServiceFromUrl(serviceUrl, "EvsServiceInfo");

			Boolean retval = false;
			retval = lexevsService.registerSecurityToken(codingScheme, securityToken);

			if(retval.equals(Boolean.TRUE))	{
				//System.out.println("Registration of SecurityToken was successful.");
			}
			else {
				System.out.println("WARNING: Registration of SecurityToken failed.");
			}

            System.out.println("Connected to " + serviceUrl);
            return (LexBIGService) lexevsService;
        }
        catch (Exception e)
        {
			System.out.println("Unable to connected to " + serviceUrl);
            e.printStackTrace();
        }
        return null;
    }



    public static String getServiceURL() {
        return serviceURL;
    }
}