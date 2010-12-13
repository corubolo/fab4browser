
package uk.ac.liverpool.shaman.thumbnailserviceclient;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the uk.ac.liverpool.shaman.thumbnailserviceclient package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _ExtractXmlTextFromData_QNAME = new QName("http://shaman.liv.ac.uk/", "extractXmlTextFromData");
    private final static QName _GetSupportedMimeTypes_QNAME = new QName("http://shaman.liv.ac.uk/", "getSupportedMimeTypes");
    private final static QName _GetSupportedOutputType_QNAME = new QName("http://shaman.liv.ac.uk/", "getSupportedOutputType");
    private final static QName _ExtractXmlTextResponse_QNAME = new QName("http://shaman.liv.ac.uk/", "extractXmlTextResponse");
    private final static QName _GenerateSVGThumbnailFromDataResponse_QNAME = new QName("http://shaman.liv.ac.uk/", "generateSVGThumbnailFromDataResponse");
    private final static QName _GenerateSVGThumbnail_QNAME = new QName("http://shaman.liv.ac.uk/", "generateSVGThumbnail");
    private final static QName _ExtractFontInformationFromData_QNAME = new QName("http://shaman.liv.ac.uk/", "extractFontInformationFromData");
    private final static QName _GenerateSVGThumbnailFromData_QNAME = new QName("http://shaman.liv.ac.uk/", "generateSVGThumbnailFromData");
    private final static QName _ExtractFontInformation_QNAME = new QName("http://shaman.liv.ac.uk/", "extractFontInformation");
    private final static QName _ExtractXmlText_QNAME = new QName("http://shaman.liv.ac.uk/", "extractXmlText");
    private final static QName _ExtractFontInformationResponse_QNAME = new QName("http://shaman.liv.ac.uk/", "extractFontInformationResponse");
    private final static QName _ExtractXmlTextFromDataResponse_QNAME = new QName("http://shaman.liv.ac.uk/", "extractXmlTextFromDataResponse");
    private final static QName _ExtractFontInformationFromDataResponse_QNAME = new QName("http://shaman.liv.ac.uk/", "extractFontInformationFromDataResponse");
    private final static QName _GenerateThumbnailFromDataResponse_QNAME = new QName("http://shaman.liv.ac.uk/", "generateThumbnailFromDataResponse");
    private final static QName _GetSupportedOutputTypeResponse_QNAME = new QName("http://shaman.liv.ac.uk/", "getSupportedOutputTypeResponse");
    private final static QName _MalformedURLException_QNAME = new QName("http://shaman.liv.ac.uk/", "MalformedURLException");
    private final static QName _ResolveResponse_QNAME = new QName("http://shaman.liv.ac.uk/", "resolveResponse");
    private final static QName _Resolve_QNAME = new QName("http://shaman.liv.ac.uk/", "resolve");
    private final static QName _GenerateThumbnail_QNAME = new QName("http://shaman.liv.ac.uk/", "generateThumbnail");
    private final static QName _IOException_QNAME = new QName("http://shaman.liv.ac.uk/", "IOException");
    private final static QName _GenerateThumbnailResponse_QNAME = new QName("http://shaman.liv.ac.uk/", "generateThumbnailResponse");
    private final static QName _GenerateThumbnailFromData_QNAME = new QName("http://shaman.liv.ac.uk/", "generateThumbnailFromData");
    private final static QName _GetSupportedMimeTypesResponse_QNAME = new QName("http://shaman.liv.ac.uk/", "getSupportedMimeTypesResponse");
    private final static QName _GenerateSVGThumbnailResponse_QNAME = new QName("http://shaman.liv.ac.uk/", "generateSVGThumbnailResponse");
    private final static QName _GenerateSVGThumbnailFromDataArg0_QNAME = new QName("", "arg0");
    private final static QName _GenerateThumbnailFromDataResponseReturn_QNAME = new QName("", "return");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: uk.ac.liverpool.shaman.thumbnailserviceclient
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ExtractXmlTextFromDataResponse }
     * 
     */
    public ExtractXmlTextFromDataResponse createExtractXmlTextFromDataResponse() {
        return new ExtractXmlTextFromDataResponse();
    }

    /**
     * Create an instance of {@link GetSupportedOutputTypeResponse }
     * 
     */
    public GetSupportedOutputTypeResponse createGetSupportedOutputTypeResponse() {
        return new GetSupportedOutputTypeResponse();
    }

    /**
     * Create an instance of {@link GenerateThumbnail }
     * 
     */
    public GenerateThumbnail createGenerateThumbnail() {
        return new GenerateThumbnail();
    }

    /**
     * Create an instance of {@link ExtractXmlText }
     * 
     */
    public ExtractXmlText createExtractXmlText() {
        return new ExtractXmlText();
    }

    /**
     * Create an instance of {@link ExtractFontInformationFromDataResponse }
     * 
     */
    public ExtractFontInformationFromDataResponse createExtractFontInformationFromDataResponse() {
        return new ExtractFontInformationFromDataResponse();
    }

    /**
     * Create an instance of {@link GenerateThumbnailFromData }
     * 
     */
    public GenerateThumbnailFromData createGenerateThumbnailFromData() {
        return new GenerateThumbnailFromData();
    }

    /**
     * Create an instance of {@link GenerateThumbnailResponse }
     * 
     */
    public GenerateThumbnailResponse createGenerateThumbnailResponse() {
        return new GenerateThumbnailResponse();
    }

    /**
     * Create an instance of {@link ExtractFontInformationFromData }
     * 
     */
    public ExtractFontInformationFromData createExtractFontInformationFromData() {
        return new ExtractFontInformationFromData();
    }

    /**
     * Create an instance of {@link GenerateSVGThumbnail }
     * 
     */
    public GenerateSVGThumbnail createGenerateSVGThumbnail() {
        return new GenerateSVGThumbnail();
    }

    /**
     * Create an instance of {@link FontInformation }
     * 
     */
    public FontInformation createFontInformation() {
        return new FontInformation();
    }

    /**
     * Create an instance of {@link GenerateSVGThumbnailFromDataResponse }
     * 
     */
    public GenerateSVGThumbnailFromDataResponse createGenerateSVGThumbnailFromDataResponse() {
        return new GenerateSVGThumbnailFromDataResponse();
    }

    /**
     * Create an instance of {@link ExtractFontInformation }
     * 
     */
    public ExtractFontInformation createExtractFontInformation() {
        return new ExtractFontInformation();
    }

    /**
     * Create an instance of {@link GetSupportedMimeTypes }
     * 
     */
    public GetSupportedMimeTypes createGetSupportedMimeTypes() {
        return new GetSupportedMimeTypes();
    }

    /**
     * Create an instance of {@link Resolve }
     * 
     */
    public Resolve createResolve() {
        return new Resolve();
    }

    /**
     * Create an instance of {@link MalformedURLException }
     * 
     */
    public MalformedURLException createMalformedURLException() {
        return new MalformedURLException();
    }

    /**
     * Create an instance of {@link GetSupportedOutputType }
     * 
     */
    public GetSupportedOutputType createGetSupportedOutputType() {
        return new GetSupportedOutputType();
    }

    /**
     * Create an instance of {@link IOException }
     * 
     */
    public IOException createIOException() {
        return new IOException();
    }

    /**
     * Create an instance of {@link GenerateSVGThumbnailResponse }
     * 
     */
    public GenerateSVGThumbnailResponse createGenerateSVGThumbnailResponse() {
        return new GenerateSVGThumbnailResponse();
    }

    /**
     * Create an instance of {@link GenerateSVGThumbnailFromData }
     * 
     */
    public GenerateSVGThumbnailFromData createGenerateSVGThumbnailFromData() {
        return new GenerateSVGThumbnailFromData();
    }

    /**
     * Create an instance of {@link ExtractXmlTextFromData }
     * 
     */
    public ExtractXmlTextFromData createExtractXmlTextFromData() {
        return new ExtractXmlTextFromData();
    }

    /**
     * Create an instance of {@link GenerateThumbnailFromDataResponse }
     * 
     */
    public GenerateThumbnailFromDataResponse createGenerateThumbnailFromDataResponse() {
        return new GenerateThumbnailFromDataResponse();
    }

    /**
     * Create an instance of {@link GetSupportedMimeTypesResponse }
     * 
     */
    public GetSupportedMimeTypesResponse createGetSupportedMimeTypesResponse() {
        return new GetSupportedMimeTypesResponse();
    }

    /**
     * Create an instance of {@link ExtractXmlTextResponse }
     * 
     */
    public ExtractXmlTextResponse createExtractXmlTextResponse() {
        return new ExtractXmlTextResponse();
    }

    /**
     * Create an instance of {@link ExtractFontInformationResponse }
     * 
     */
    public ExtractFontInformationResponse createExtractFontInformationResponse() {
        return new ExtractFontInformationResponse();
    }

    /**
     * Create an instance of {@link ResolveResponse }
     * 
     */
    public ResolveResponse createResolveResponse() {
        return new ResolveResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ExtractXmlTextFromData }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://shaman.liv.ac.uk/", name = "extractXmlTextFromData")
    public JAXBElement<ExtractXmlTextFromData> createExtractXmlTextFromData(ExtractXmlTextFromData value) {
        return new JAXBElement<ExtractXmlTextFromData>(_ExtractXmlTextFromData_QNAME, ExtractXmlTextFromData.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetSupportedMimeTypes }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://shaman.liv.ac.uk/", name = "getSupportedMimeTypes")
    public JAXBElement<GetSupportedMimeTypes> createGetSupportedMimeTypes(GetSupportedMimeTypes value) {
        return new JAXBElement<GetSupportedMimeTypes>(_GetSupportedMimeTypes_QNAME, GetSupportedMimeTypes.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetSupportedOutputType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://shaman.liv.ac.uk/", name = "getSupportedOutputType")
    public JAXBElement<GetSupportedOutputType> createGetSupportedOutputType(GetSupportedOutputType value) {
        return new JAXBElement<GetSupportedOutputType>(_GetSupportedOutputType_QNAME, GetSupportedOutputType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ExtractXmlTextResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://shaman.liv.ac.uk/", name = "extractXmlTextResponse")
    public JAXBElement<ExtractXmlTextResponse> createExtractXmlTextResponse(ExtractXmlTextResponse value) {
        return new JAXBElement<ExtractXmlTextResponse>(_ExtractXmlTextResponse_QNAME, ExtractXmlTextResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GenerateSVGThumbnailFromDataResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://shaman.liv.ac.uk/", name = "generateSVGThumbnailFromDataResponse")
    public JAXBElement<GenerateSVGThumbnailFromDataResponse> createGenerateSVGThumbnailFromDataResponse(GenerateSVGThumbnailFromDataResponse value) {
        return new JAXBElement<GenerateSVGThumbnailFromDataResponse>(_GenerateSVGThumbnailFromDataResponse_QNAME, GenerateSVGThumbnailFromDataResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GenerateSVGThumbnail }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://shaman.liv.ac.uk/", name = "generateSVGThumbnail")
    public JAXBElement<GenerateSVGThumbnail> createGenerateSVGThumbnail(GenerateSVGThumbnail value) {
        return new JAXBElement<GenerateSVGThumbnail>(_GenerateSVGThumbnail_QNAME, GenerateSVGThumbnail.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ExtractFontInformationFromData }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://shaman.liv.ac.uk/", name = "extractFontInformationFromData")
    public JAXBElement<ExtractFontInformationFromData> createExtractFontInformationFromData(ExtractFontInformationFromData value) {
        return new JAXBElement<ExtractFontInformationFromData>(_ExtractFontInformationFromData_QNAME, ExtractFontInformationFromData.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GenerateSVGThumbnailFromData }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://shaman.liv.ac.uk/", name = "generateSVGThumbnailFromData")
    public JAXBElement<GenerateSVGThumbnailFromData> createGenerateSVGThumbnailFromData(GenerateSVGThumbnailFromData value) {
        return new JAXBElement<GenerateSVGThumbnailFromData>(_GenerateSVGThumbnailFromData_QNAME, GenerateSVGThumbnailFromData.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ExtractFontInformation }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://shaman.liv.ac.uk/", name = "extractFontInformation")
    public JAXBElement<ExtractFontInformation> createExtractFontInformation(ExtractFontInformation value) {
        return new JAXBElement<ExtractFontInformation>(_ExtractFontInformation_QNAME, ExtractFontInformation.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ExtractXmlText }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://shaman.liv.ac.uk/", name = "extractXmlText")
    public JAXBElement<ExtractXmlText> createExtractXmlText(ExtractXmlText value) {
        return new JAXBElement<ExtractXmlText>(_ExtractXmlText_QNAME, ExtractXmlText.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ExtractFontInformationResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://shaman.liv.ac.uk/", name = "extractFontInformationResponse")
    public JAXBElement<ExtractFontInformationResponse> createExtractFontInformationResponse(ExtractFontInformationResponse value) {
        return new JAXBElement<ExtractFontInformationResponse>(_ExtractFontInformationResponse_QNAME, ExtractFontInformationResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ExtractXmlTextFromDataResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://shaman.liv.ac.uk/", name = "extractXmlTextFromDataResponse")
    public JAXBElement<ExtractXmlTextFromDataResponse> createExtractXmlTextFromDataResponse(ExtractXmlTextFromDataResponse value) {
        return new JAXBElement<ExtractXmlTextFromDataResponse>(_ExtractXmlTextFromDataResponse_QNAME, ExtractXmlTextFromDataResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ExtractFontInformationFromDataResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://shaman.liv.ac.uk/", name = "extractFontInformationFromDataResponse")
    public JAXBElement<ExtractFontInformationFromDataResponse> createExtractFontInformationFromDataResponse(ExtractFontInformationFromDataResponse value) {
        return new JAXBElement<ExtractFontInformationFromDataResponse>(_ExtractFontInformationFromDataResponse_QNAME, ExtractFontInformationFromDataResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GenerateThumbnailFromDataResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://shaman.liv.ac.uk/", name = "generateThumbnailFromDataResponse")
    public JAXBElement<GenerateThumbnailFromDataResponse> createGenerateThumbnailFromDataResponse(GenerateThumbnailFromDataResponse value) {
        return new JAXBElement<GenerateThumbnailFromDataResponse>(_GenerateThumbnailFromDataResponse_QNAME, GenerateThumbnailFromDataResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetSupportedOutputTypeResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://shaman.liv.ac.uk/", name = "getSupportedOutputTypeResponse")
    public JAXBElement<GetSupportedOutputTypeResponse> createGetSupportedOutputTypeResponse(GetSupportedOutputTypeResponse value) {
        return new JAXBElement<GetSupportedOutputTypeResponse>(_GetSupportedOutputTypeResponse_QNAME, GetSupportedOutputTypeResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link MalformedURLException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://shaman.liv.ac.uk/", name = "MalformedURLException")
    public JAXBElement<MalformedURLException> createMalformedURLException(MalformedURLException value) {
        return new JAXBElement<MalformedURLException>(_MalformedURLException_QNAME, MalformedURLException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ResolveResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://shaman.liv.ac.uk/", name = "resolveResponse")
    public JAXBElement<ResolveResponse> createResolveResponse(ResolveResponse value) {
        return new JAXBElement<ResolveResponse>(_ResolveResponse_QNAME, ResolveResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Resolve }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://shaman.liv.ac.uk/", name = "resolve")
    public JAXBElement<Resolve> createResolve(Resolve value) {
        return new JAXBElement<Resolve>(_Resolve_QNAME, Resolve.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GenerateThumbnail }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://shaman.liv.ac.uk/", name = "generateThumbnail")
    public JAXBElement<GenerateThumbnail> createGenerateThumbnail(GenerateThumbnail value) {
        return new JAXBElement<GenerateThumbnail>(_GenerateThumbnail_QNAME, GenerateThumbnail.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IOException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://shaman.liv.ac.uk/", name = "IOException")
    public JAXBElement<IOException> createIOException(IOException value) {
        return new JAXBElement<IOException>(_IOException_QNAME, IOException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GenerateThumbnailResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://shaman.liv.ac.uk/", name = "generateThumbnailResponse")
    public JAXBElement<GenerateThumbnailResponse> createGenerateThumbnailResponse(GenerateThumbnailResponse value) {
        return new JAXBElement<GenerateThumbnailResponse>(_GenerateThumbnailResponse_QNAME, GenerateThumbnailResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GenerateThumbnailFromData }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://shaman.liv.ac.uk/", name = "generateThumbnailFromData")
    public JAXBElement<GenerateThumbnailFromData> createGenerateThumbnailFromData(GenerateThumbnailFromData value) {
        return new JAXBElement<GenerateThumbnailFromData>(_GenerateThumbnailFromData_QNAME, GenerateThumbnailFromData.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetSupportedMimeTypesResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://shaman.liv.ac.uk/", name = "getSupportedMimeTypesResponse")
    public JAXBElement<GetSupportedMimeTypesResponse> createGetSupportedMimeTypesResponse(GetSupportedMimeTypesResponse value) {
        return new JAXBElement<GetSupportedMimeTypesResponse>(_GetSupportedMimeTypesResponse_QNAME, GetSupportedMimeTypesResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GenerateSVGThumbnailResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://shaman.liv.ac.uk/", name = "generateSVGThumbnailResponse")
    public JAXBElement<GenerateSVGThumbnailResponse> createGenerateSVGThumbnailResponse(GenerateSVGThumbnailResponse value) {
        return new JAXBElement<GenerateSVGThumbnailResponse>(_GenerateSVGThumbnailResponse_QNAME, GenerateSVGThumbnailResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link byte[]}{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "arg0", scope = GenerateSVGThumbnailFromData.class)
    public JAXBElement<byte[]> createGenerateSVGThumbnailFromDataArg0(byte[] value) {
        return new JAXBElement<byte[]>(_GenerateSVGThumbnailFromDataArg0_QNAME, byte[].class, GenerateSVGThumbnailFromData.class, ((byte[]) value));
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link byte[]}{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "arg0", scope = ExtractXmlTextFromData.class)
    public JAXBElement<byte[]> createExtractXmlTextFromDataArg0(byte[] value) {
        return new JAXBElement<byte[]>(_GenerateSVGThumbnailFromDataArg0_QNAME, byte[].class, ExtractXmlTextFromData.class, ((byte[]) value));
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link byte[]}{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "return", scope = GenerateThumbnailFromDataResponse.class)
    public JAXBElement<byte[]> createGenerateThumbnailFromDataResponseReturn(byte[] value) {
        return new JAXBElement<byte[]>(_GenerateThumbnailFromDataResponseReturn_QNAME, byte[].class, GenerateThumbnailFromDataResponse.class, ((byte[]) value));
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link byte[]}{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "arg0", scope = GenerateThumbnailFromData.class)
    public JAXBElement<byte[]> createGenerateThumbnailFromDataArg0(byte[] value) {
        return new JAXBElement<byte[]>(_GenerateSVGThumbnailFromDataArg0_QNAME, byte[].class, GenerateThumbnailFromData.class, ((byte[]) value));
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link byte[]}{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "return", scope = GenerateThumbnailResponse.class)
    public JAXBElement<byte[]> createGenerateThumbnailResponseReturn(byte[] value) {
        return new JAXBElement<byte[]>(_GenerateThumbnailFromDataResponseReturn_QNAME, byte[].class, GenerateThumbnailResponse.class, ((byte[]) value));
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link byte[]}{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "arg0", scope = ExtractFontInformationFromData.class)
    public JAXBElement<byte[]> createExtractFontInformationFromDataArg0(byte[] value) {
        return new JAXBElement<byte[]>(_GenerateSVGThumbnailFromDataArg0_QNAME, byte[].class, ExtractFontInformationFromData.class, ((byte[]) value));
    }

}
