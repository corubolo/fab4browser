
package uk.ac.liverpool.shaman.thumbnailserviceclient;

import java.util.List;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.3-b02-
 * Generated source version: 2.1
 * 
 */
@WebService(name = "ThumbnailService", targetNamespace = "http://shaman.liv.ac.uk/")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface ThumbnailService {


    /**
     * 
     * @param arg0
     * @return
     *     returns java.lang.String
     * @throws IOException_Exception
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "resolve", targetNamespace = "http://shaman.liv.ac.uk/", className = "uk.ac.liverpool.shaman.thumbnailserviceclient.Resolve")
    @ResponseWrapper(localName = "resolveResponse", targetNamespace = "http://shaman.liv.ac.uk/", className = "uk.ac.liverpool.shaman.thumbnailserviceclient.ResolveResponse")
    public String resolve(
        @WebParam(name = "arg0", targetNamespace = "")
        String arg0)
        throws IOException_Exception
    ;

    /**
     * 
     * @param arg5
     * @param arg4
     * @param arg3
     * @param arg2
     * @param arg1
     * @param arg0
     * @return
     *     returns byte[]
     * @throws MalformedURLException_Exception
     * @throws IOException_Exception
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "generateThumbnailFromData", targetNamespace = "http://shaman.liv.ac.uk/", className = "uk.ac.liverpool.shaman.thumbnailserviceclient.GenerateThumbnailFromData")
    @ResponseWrapper(localName = "generateThumbnailFromDataResponse", targetNamespace = "http://shaman.liv.ac.uk/", className = "uk.ac.liverpool.shaman.thumbnailserviceclient.GenerateThumbnailFromDataResponse")
    public byte[] generateThumbnailFromData(
        @WebParam(name = "arg0", targetNamespace = "")
        byte[] arg0,
        @WebParam(name = "arg1", targetNamespace = "")
        int arg1,
        @WebParam(name = "arg2", targetNamespace = "")
        int arg2,
        @WebParam(name = "arg3", targetNamespace = "")
        String arg3,
        @WebParam(name = "arg4", targetNamespace = "")
        String arg4,
        @WebParam(name = "arg5", targetNamespace = "")
        int arg5)
        throws IOException_Exception, MalformedURLException_Exception
    ;

    /**
     * 
     * @param arg3
     * @param arg2
     * @param arg1
     * @param arg0
     * @return
     *     returns java.lang.String
     * @throws MalformedURLException_Exception
     * @throws IOException_Exception
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "generateSVGThumbnailFromData", targetNamespace = "http://shaman.liv.ac.uk/", className = "uk.ac.liverpool.shaman.thumbnailserviceclient.GenerateSVGThumbnailFromData")
    @ResponseWrapper(localName = "generateSVGThumbnailFromDataResponse", targetNamespace = "http://shaman.liv.ac.uk/", className = "uk.ac.liverpool.shaman.thumbnailserviceclient.GenerateSVGThumbnailFromDataResponse")
    public String generateSVGThumbnailFromData(
        @WebParam(name = "arg0", targetNamespace = "")
        byte[] arg0,
        @WebParam(name = "arg1", targetNamespace = "")
        int arg1,
        @WebParam(name = "arg2", targetNamespace = "")
        int arg2,
        @WebParam(name = "arg3", targetNamespace = "")
        int arg3)
        throws IOException_Exception, MalformedURLException_Exception
    ;

    /**
     * 
     * @param arg0
     * @return
     *     returns java.util.List<uk.ac.liverpool.shaman.thumbnailserviceclient.FontInformation>
     * @throws MalformedURLException_Exception
     * @throws IOException_Exception
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "extractFontInformationFromData", targetNamespace = "http://shaman.liv.ac.uk/", className = "uk.ac.liverpool.shaman.thumbnailserviceclient.ExtractFontInformationFromData")
    @ResponseWrapper(localName = "extractFontInformationFromDataResponse", targetNamespace = "http://shaman.liv.ac.uk/", className = "uk.ac.liverpool.shaman.thumbnailserviceclient.ExtractFontInformationFromDataResponse")
    public List<FontInformation> extractFontInformationFromData(
        @WebParam(name = "arg0", targetNamespace = "")
        byte[] arg0)
        throws IOException_Exception, MalformedURLException_Exception
    ;

    /**
     * 
     * @param arg0
     * @return
     *     returns java.lang.String
     * @throws MalformedURLException_Exception
     * @throws IOException_Exception
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "extractXmlTextFromData", targetNamespace = "http://shaman.liv.ac.uk/", className = "uk.ac.liverpool.shaman.thumbnailserviceclient.ExtractXmlTextFromData")
    @ResponseWrapper(localName = "extractXmlTextFromDataResponse", targetNamespace = "http://shaman.liv.ac.uk/", className = "uk.ac.liverpool.shaman.thumbnailserviceclient.ExtractXmlTextFromDataResponse")
    public String extractXmlTextFromData(
        @WebParam(name = "arg0", targetNamespace = "")
        byte[] arg0)
        throws IOException_Exception, MalformedURLException_Exception
    ;

    /**
     * 
     * @param arg5
     * @param arg4
     * @param arg3
     * @param arg2
     * @param arg1
     * @param arg0
     * @return
     *     returns byte[]
     * @throws MalformedURLException_Exception
     * @throws IOException_Exception
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "generateThumbnail", targetNamespace = "http://shaman.liv.ac.uk/", className = "uk.ac.liverpool.shaman.thumbnailserviceclient.GenerateThumbnail")
    @ResponseWrapper(localName = "generateThumbnailResponse", targetNamespace = "http://shaman.liv.ac.uk/", className = "uk.ac.liverpool.shaman.thumbnailserviceclient.GenerateThumbnailResponse")
    public byte[] generateThumbnail(
        @WebParam(name = "arg0", targetNamespace = "")
        String arg0,
        @WebParam(name = "arg1", targetNamespace = "")
        int arg1,
        @WebParam(name = "arg2", targetNamespace = "")
        int arg2,
        @WebParam(name = "arg3", targetNamespace = "")
        String arg3,
        @WebParam(name = "arg4", targetNamespace = "")
        String arg4,
        @WebParam(name = "arg5", targetNamespace = "")
        int arg5)
        throws IOException_Exception, MalformedURLException_Exception
    ;

    /**
     * 
     * @param arg3
     * @param arg2
     * @param arg1
     * @param arg0
     * @return
     *     returns java.lang.String
     * @throws MalformedURLException_Exception
     * @throws IOException_Exception
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "generateSVGThumbnail", targetNamespace = "http://shaman.liv.ac.uk/", className = "uk.ac.liverpool.shaman.thumbnailserviceclient.GenerateSVGThumbnail")
    @ResponseWrapper(localName = "generateSVGThumbnailResponse", targetNamespace = "http://shaman.liv.ac.uk/", className = "uk.ac.liverpool.shaman.thumbnailserviceclient.GenerateSVGThumbnailResponse")
    public String generateSVGThumbnail(
        @WebParam(name = "arg0", targetNamespace = "")
        String arg0,
        @WebParam(name = "arg1", targetNamespace = "")
        int arg1,
        @WebParam(name = "arg2", targetNamespace = "")
        int arg2,
        @WebParam(name = "arg3", targetNamespace = "")
        int arg3)
        throws IOException_Exception, MalformedURLException_Exception
    ;

    /**
     * 
     * @param arg0
     * @return
     *     returns java.util.List<uk.ac.liverpool.shaman.thumbnailserviceclient.FontInformation>
     * @throws MalformedURLException_Exception
     * @throws IOException_Exception
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "extractFontInformation", targetNamespace = "http://shaman.liv.ac.uk/", className = "uk.ac.liverpool.shaman.thumbnailserviceclient.ExtractFontInformation")
    @ResponseWrapper(localName = "extractFontInformationResponse", targetNamespace = "http://shaman.liv.ac.uk/", className = "uk.ac.liverpool.shaman.thumbnailserviceclient.ExtractFontInformationResponse")
    public List<FontInformation> extractFontInformation(
        @WebParam(name = "arg0", targetNamespace = "")
        String arg0)
        throws IOException_Exception, MalformedURLException_Exception
    ;

    /**
     * 
     * @param arg0
     * @return
     *     returns java.lang.String
     * @throws MalformedURLException_Exception
     * @throws IOException_Exception
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "extractXmlText", targetNamespace = "http://shaman.liv.ac.uk/", className = "uk.ac.liverpool.shaman.thumbnailserviceclient.ExtractXmlText")
    @ResponseWrapper(localName = "extractXmlTextResponse", targetNamespace = "http://shaman.liv.ac.uk/", className = "uk.ac.liverpool.shaman.thumbnailserviceclient.ExtractXmlTextResponse")
    public String extractXmlText(
        @WebParam(name = "arg0", targetNamespace = "")
        String arg0)
        throws IOException_Exception, MalformedURLException_Exception
    ;

    /**
     * 
     * @return
     *     returns java.util.List<java.lang.String>
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "getSupportedMimeTypes", targetNamespace = "http://shaman.liv.ac.uk/", className = "uk.ac.liverpool.shaman.thumbnailserviceclient.GetSupportedMimeTypes")
    @ResponseWrapper(localName = "getSupportedMimeTypesResponse", targetNamespace = "http://shaman.liv.ac.uk/", className = "uk.ac.liverpool.shaman.thumbnailserviceclient.GetSupportedMimeTypesResponse")
    public List<String> getSupportedMimeTypes();

    /**
     * 
     * @return
     *     returns java.util.List<java.lang.String>
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "getSupportedOutputType", targetNamespace = "http://shaman.liv.ac.uk/", className = "uk.ac.liverpool.shaman.thumbnailserviceclient.GetSupportedOutputType")
    @ResponseWrapper(localName = "getSupportedOutputTypeResponse", targetNamespace = "http://shaman.liv.ac.uk/", className = "uk.ac.liverpool.shaman.thumbnailserviceclient.GetSupportedOutputTypeResponse")
    public List<String> getSupportedOutputType();

}
