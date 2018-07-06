/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package v2manifestgenerator;

/**
 * *************************************************************
 *
 * M A N I F E S T G E N E R A T O R
 *
 * This class generates a manifest from a collection of VEOs.
 *
 * Andrew Waugh (andrew.waugh@dvc.vic.gov.au) Copyright 2005 PROV
 *
 * A manifest is an XML document that lists each VEO in a Set to be transferred
 * to PROV. In addition, for Sets transferred on individual media (e.g. CDs) it
 * lists the pieces of media. For each VEO, the Manifest lists the (computer)
 * filename, the record identifier (if present) and the file identifier. These
 * are used by the digital archive to manage the transfer. The Manifest lists a
 * number of other elements abstracted from the VEO. These are used by the
 * transfer archivist to analyse the Set. The manifest is defined by an XML
 * schema (manifest.xsd).
 *
 * By default the program scans the current directory for files ending in '.veo'
 * or '.VEO'. These files are assumed to be part of the transfer. It parses each
 * file using SAX for certain key element values. At the end of the parse it
 * writes a precis of the file in the manifest. The manifest is generated upon
 * standard out.
 *
 * Options are: 1) To specify another directory to scan for VEOs 2) To specify a
 * different extension to identify VEOs. 3) Generate a media manifest. With this
 * option, the base directory does not contain VEOs. Instead it contains sub
 * directories that represent the pieces of media. These sub directories contain
 * the VEOs that are to be put on the media. The sub directory name is the name
 * of the media. 4) Use the VERS DTD referenced by the VEOs, not the standard
 * DTD on the web site.
 *
 * The program is configured by the command line. The following arguments must
 * be present: -va <number> The VA number of the agency exporting the VEOs -vprs
 * <number> The VPRS number of the series -p <number> The permanent consignment
 * number -tr <id> The transfer identifier of this consignment The following
 * arguments are optional: -d <directory> The base directory in which VEOs are
 * to be found. If this is not present, the current direction is used. -v
 * <extension> The file extension indicating VEOs (e.g. '.veo') The extension is
 * case insensitive. If not present the extension '.veo' is used. -m
 * <media type> Generate a manifest for a media export. The media type is either
 * 'CD', 'DVD', 'DDS', or 'LTO'. If not present, an electronic transfer is
 * generated. -n Use the DTD referenced by the VEO. If this is not present the
 * standard VERS DTD from the PROV web site is used. -proxy <host>:<port> It is
 * necessary to use a proxy to get to the PROV website
 *
 * Version History 20090506	Added an entity resolver to allow use of a local
 * vers.dtd file as something seems to block remote access to
 * www.prov.vic.gov.au/vers/std
 * ************************************************************
 */
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.TimeZone;
import java.util.Locale;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Stack;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;

public class V2ManifestGenerator extends DefaultHandler2 {
    // XMLReader xr;		// XML reader to perform parser

    SAXParser sax;		// XML parser to perform parse
    String proxyHost;	// Domain name of proxy server (null if none)
    String proxyPort;	// Port that proxy server is listening

    // global variables storing information about this export (as a whole)
    File sourceDirectory;	// directory in which VEOs are found
    String fileExtension;	// file extension denoting VEOs
    boolean internetExport;	// true if exporting over the internet
    String mediaType;	// if a media export, the type of media
    int vaNumber;		// VA number of exporting agency
    int vprsNumber;		// series number (VPRS number) of exported VEOs
    String consignmentType;	// type of consignment
    int consignmentNumber;	// number of consignment within series
    String transferId;	// transfer job number
    boolean useStdDtd;	// true if using the DTD from the VERS website

    // global variables storing information extracted from this VEO
    boolean record;		// true if this VEO contains a record VEO
    String fileIdentifier;	// vers:FileIdentifier
    String recIdentifier;	// vers:RecordIdentifier
    String titleWords;	// vers:TitleWords
    String function;	// vers:FunctionDescriptor (et al)
    String subject;		// vers:Subject (et al)
    String accessStatus;	// naa:AccessStatus
    String sentence;	// naa:Sentence
    String dateRegistered;	// naa:DateTimeRegistered
    String dateClosed;	// vers:DateTimeClosed

    // stack of elements encountered in the parse
    Stack<String> elementsFound; // stack of the elements recognised in the parse
    int elementFound;	// which element did we find?
    boolean recording;	// true if recognised an element name and are
    // now capturing element value
    StringBuffer elementValue; // the value of the element found

    /**
     * Default constructor
     *
     * @param args command line arguments
     */
    public V2ManifestGenerator(String args[]) {
        super();

        SAXParserFactory spf;

        // set up SAX parser
        try {
            spf = SAXParserFactory.newInstance();
            spf.setValidating(false);
            sax = spf.newSAXParser();
            XMLReader xmlReader = sax.getXMLReader();

            xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", this);
        } catch (SAXNotRecognizedException | SAXNotSupportedException | ParserConfigurationException e) {
            System.err.println("Failure initiating SAX Parser: " + e.toString());
            System.exit(-1);
        } catch (SAXException e) {
            System.err.println("Failure initiating SAX Parser: " + e.toString());
            System.exit(-1);
        }

        // set up default global variables
        proxyHost = null;
        proxyPort = null;
        sourceDirectory = new File(".");
        fileExtension = ".veo";
        internetExport = true;
        mediaType = null;
        vaNumber = -1;
        vprsNumber = -1;
        consignmentType = null;
        consignmentNumber = -1;
        transferId = null;
        useStdDtd = true;

        elementValue = new StringBuffer();

        // process command line arguments
        configure(args);

        // use proxy
        if (proxyHost != null) {
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("http.proxyPort", proxyPort);
        }
    }

    /**
     * Configure
     *
     * This method gets the options for this run of the manifest generator from
     * the command line. See the comment at the start of this file for the
     * command line arguments.
     *
     * @param args[] the command line arguments
     */
    public final void configure(String args[]) {
        int i, j;
        String usage = "manifestGenerator -va <number> -vprs <number> -p|r|t|u <number> -tr <id> [-m <media type>] [-d <directory>] [-v extension] [-n] [-proxy host:port]";
        String s;

        // process command line arguments
        i = 0;
        try {
            while (i < args.length) {
                // get agency number
                if (args[i].equals("-va")) {
                    i++;
                    vaNumber = parseInt("VA", args[i]);
                    System.err.println("VA Number: '" + vaNumber + "'");
                    i++;
                    continue;
                }

                // get series number
                if (args[i].equals("-vprs")) {
                    i++;
                    vprsNumber = parseInt("VPRS", args[i]);
                    System.err.println("VPRS Number: '" + vprsNumber + "'");
                    i++;
                    continue;
                }

                // get consignment type and number
                if (args[i].equals("-p")
                        || // permanent
                        args[i].equals("-r")
                        || // review
                        args[i].equals("-t")
                        || // temporary
                        args[i].equals("-u")) { // unsentenced
                    switch (args[i]) {
                        case "-p":
                            consignmentType = "P";
                            break;
                        case "-r":
                            consignmentType = "R";
                            break;
                        case "-t":
                            consignmentType = "T";
                            break;
                        default:
                            consignmentType = "U";
                            break;
                    }
                    i++;
                    consignmentNumber = parseInt("Consignment", args[i]);
                    if (consignmentNumber < 0 || consignmentNumber > 9999) {
                        System.err.println("Consignment number (" + consignmentNumber + ") must be between 1 and 9999");
                        System.exit(-1);
                    }
                    System.err.println("Consignment Type: '" + consignmentType + "', Number: '" + consignmentNumber + "'");
                    i++;
                    continue;
                }

                // if '-tr' remember transfer number
                if (args[i].equals("-tr")) {
                    i++;
                    transferId = args[i];
                    System.err.println("Transfer Job Number: '" + transferId + "'");
                    i++;
                    continue;
                }

                // '-d' specifies base directory
                if (args[i].equals("-d")) {
                    i++;
                    s = "";
                    sourceDirectory = new File(args[i]);
                    try {
                        s = sourceDirectory.getCanonicalPath();
                    } catch (IOException ioe) {
                        /* ignore */ }
                    System.err.println("Source directory: '" + s + "'");
                    i++;
                    continue;
                }

                // '-n' use DTD from VEOs, not from VERS web site
                if (args[i].equals("-n")) {
                    useStdDtd = false;
                    System.err.println("Using 'vers.dtd' from current directory nstead of from VERS website");
                    i++;
                    continue;
                }

                // get proxy host and port...
                if (args[i].equals("-proxy")) {
                    i++;
                    j = args[i].indexOf(':');
                    if (j != -1) {
                        proxyHost = args[i].substring(0, j);
                        proxyPort = args[i].substring(j + 1, args[i].length());
                    } else {
                        proxyHost = args[i];
                        proxyPort = "80";
                    }
                    System.err.println("Using proxy server: " + proxyHost + ":" + proxyPort);
                    i++;
                    continue;
                }

                // get VEO file extension
                if (args[i].equals("-v")) {
                    i++;
                    fileExtension = args[i].toLowerCase();
                    System.err.println("File extension: '" + fileExtension + "'");
                    i++;
                    continue;
                }

                // if '-m' will be a media output & get media type
                if (args[i].equals("-m")) {
                    internetExport = false;
                    i++;
                    mediaType = args[i];
                    switch (mediaType.toLowerCase()) {
                        case "dds":
                            mediaType = "DDS TAPE";
                            break;
                        case "lto":
                            mediaType = "LTO TAPE";
                            break;
                        case "cd":
                            mediaType = "CD";
                            break;
                        case "dvd":
                            mediaType = "DVD";
                            break;
                        default:
                            System.err.println("Media Type: '" + mediaType + "' should be DDS, LTO, CD, or LTO");
                            System.exit(-1);
                    }
                    System.err.println("Media Type: '" + mediaType + "'");
                    i++;
                    continue;
                }

                // if unrecognised arguement, print help string and exit
                System.err.println("Unrecognised argument '" + args[i] + "'");
                System.err.println(usage);
                System.exit(-1);
            }
        } catch (ArrayIndexOutOfBoundsException ae) {
            System.err.println("Missing argument. Usage: ");
            System.err.println(usage);
            System.exit(-1);
        }

        // check to see that user specified a VA, VPRS, and consignment number
        if (vaNumber == -1) {
            System.err.println("You must specify a VA number");
            System.err.println(usage);
            System.exit(-1);
        }
        if (vprsNumber == -1) {
            System.err.println("You must specify a VPRS number");
            System.err.println(usage);
            System.exit(-1);
        }
        if (consignmentNumber == -1) {
            System.err.println("You must specify a consignment number");
            System.err.println(usage);
            System.exit(-1);
        }
    }

    /**
     * parseInt
     *
     * Utility function that converts a string to a positive integer. Program
     * execution ends if string is not a positive intenger.
     */
    private int parseInt(String purpose, String s) {
        int i;

        i = 0;
        try {
            i = Integer.parseInt(s);
        } catch (NumberFormatException ne) {
            System.err.println(purpose + " Number: '" + s + "' should be a positive integer");
            System.exit(-1);
        }
        if (i < 1) {
            System.err.println(purpose + " Number: " + i + " should be a positive integer");
            System.exit(-1);
        }
        return i;
    }

    /**
     * writePreamble
     *
     * This method writes the start of the manifest to standard out. This
     * includes the <?xml...> attribute, and the contextual elements
     */
    public void writePreamble() {
        System.out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        System.out.println("<dam:set_manifest");
        System.out.println("\txmlns:dam=\"http://www.prov.vic.gov.au/digitalarchive/\"");
        System.out.println("\txmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        System.out.println("\txsi:schemaLocation=\"http://www.prov.vic.gov.au/digitalarchive/");
        System.out.println("\thttp://www.prov.vic.gov.au/digitalarchive/setManifest_1_0_0.xsd\">");
        if (internetExport) {
            System.out.println(" <dam:electronic_transfer>");
        } else {
            System.out.println(" <dam:media_transfer>");
        }
        System.out.print("  <dam:created_timestamp>");
        System.out.print(versDateTime(0));
        System.out.println("</dam:created_timestamp>");
        System.out.print("  <dam:agency_id>");
        System.out.print(vaNumber);
        System.out.println("</dam:agency_id>");
        System.out.println("  <dam:series_type>VPRS</dam:series_type>");
        System.out.print("  <dam:series_number>");
        System.out.print(vprsNumber);
        System.out.println("</dam:series_number>");
        System.out.print("  <dam:job_id>");
        System.out.print("TR " + transferId);
        System.out.println("</dam:job_id>");
        System.out.print("  <dam:consignment_type>");
        System.out.print(consignmentType.toUpperCase());
        System.out.println("</dam:consignment_type>");
        System.out.print("  <dam:consignment_number>");
        if (consignmentNumber < 9) {
            System.out.print("000" + consignmentNumber);
        } else if (consignmentNumber < 99) {
            System.out.print("00" + consignmentNumber);
        } else if (consignmentNumber < 999) {
            System.out.print("0" + consignmentNumber);
        } else if (consignmentNumber < 9999) {
            System.out.print(consignmentNumber);
        }
        System.out.println("</dam:consignment_number>");
    }

    /**
     * writePostamble
     *
     * This method writes the end of the manifest to standard out.
     */
    public void writePostamble() {
        if (internetExport) {
            System.out.println(" </dam:electronic_transfer>");
        } else {
            System.out.println(" </dam:media_transfer>");
        }
        System.out.println("</dam:set_manifest>");
    }

    /**
     * isInternetExport
     *
     * This method returns true if the export is to be transfered to PROV over
     * the internet
     *
     * @return true if it is an internet export
     */
    public boolean isInternetExport() {
        return internetExport;
    }

    /**
     * processMediaExport
     *
     * This method processes a media export. It is assumed that the source
     * directory (given by the -d argument, or the current directory if there
     * was no -d argument given) contains a series of directories representing
     * the media to be written.
     */
    class FilterDirectories implements FileFilter {

        @Override
        public boolean accept(File f) {
            return f.isDirectory();
        }
    }

    @SuppressWarnings("empty-statement")
    public void processMediaExport() {
        File[] directories;
        FilterDirectories filterDirectories;
        int i;
        String s;

        // is this source directory actually a directory?
        if (!sourceDirectory.isDirectory()) {
            s = "";
            try {
                s = sourceDirectory.getCanonicalPath();
            } catch (IOException ioe) {/* ignore */
            }
            System.err.println("Source directory '" + s + "' is not a directory");
            System.exit(-1);
        }

        // get list of directories (i.e. media) in this directory
        filterDirectories = new FilterDirectories();
        directories = sourceDirectory.listFiles(filterDirectories);

        // go through list of directories, processing VEOs
        System.out.println("  <dam:manifest_object_list>");
        for (i = 0; i < directories.length; i++) {
            s = "";
            try {
                s = directories[i].getCanonicalPath();
            } catch (IOException ioe) {/* ignore */
            }
            System.err.println("Processing media '" + s + "'");
            processVEOs(directories[i]);
        }
        System.out.println("  </dam:manifest_object_list>");

        // write dam:media_list element listing the media
        System.out.println("  <dam:media_list>");
        for (i = 0; i < directories.length; i++) {
            System.out.println("   <dam:media_item>");
            System.out.print("    <dam:media_written>");
            System.out.print(versDateTime(directories[i].lastModified()));
            System.out.println("</dam:media_written>");
            System.out.print("    <dam:media_item>");
            System.out.print(i + 1);
            System.out.println("</dam:media_item>");
            System.out.print("    <dam:media_item_total>");
            System.out.print(directories.length);
            System.out.println("</dam:media_item_total>");
            System.out.print("    <dam:media_type>");
            System.out.print(mediaType);
            System.out.println("</dam:media_type>");
            System.out.println("   </dam:media_item>");
        }
        System.out.println("  </dam:media_list>");
    }

    /**
     * processInternetExport
     *
     * This method processes an internet export.
     */
    public void processInternetExport() {
        String s;

        // is this source directory actually a directory?
        if (!sourceDirectory.isDirectory()) {
            s = "";
            try {
                s = sourceDirectory.getCanonicalPath();
            } catch (IOException ioe) {/* ignore */
            }
            System.err.println("Source directory '" + s + "' is not a directory");
            System.exit(-1);
        }

        // go through list of directories, processing VEOs
        System.out.println("  <dam:manifest_object_list>");
        processVEOs(sourceDirectory);
        System.out.println("  </dam:manifest_object_list>");
    }

    /**
     * Process VEOs
     *
     * This method goes through a directory and processes each VEO found in it.
     * A VEO is identified by the file extension given in the -v command line
     * argument (or '.veo' if no -v argument was specified).
     */
    class FilterVEOs implements FileFilter {

        @Override
        public boolean accept(File f) {
            String name;
            int i;

            // cannot be a VEO if it is a directory
            if (f.isDirectory()) {
                return false;
            }

            // does the name end with the specified extension (ignore case)
            name = f.getName();
            i = name.lastIndexOf('.');
            if (i == -1) {
                return false;
            }
            return name.substring(i).toLowerCase().equals(fileExtension);
        }
    }

    public void processVEOs(File directory) {
        File[] veos;
        int i;
        String s;

        // is this source directory actually a directory?
        if (!directory.isDirectory()) {
            s = "";
            try {
                s = directory.getCanonicalPath();
            } catch (IOException ioe) {/* ignore */
            }
            System.err.println("File '" + s + "' must be a directory to contain VEOs");
            System.exit(-1);
        }

        // get list of VEOs in this directory
        veos = directory.listFiles(new FilterVEOs());

        // go through list of directories, processing VEOs
        System.err.println("Start " + (new Date()).getTime() / 1000);
        for (i = 0; i < veos.length; i++) {
            try {
                if (i % 10 == 0) {
                    System.err.println(veos[i].getName());
                } else {
                    System.err.print('.');
                }
                System.out.print(processVEO(veos[i]));
            } catch (IOException ioe) {
                System.err.println(ioe.getMessage());
            }
        }
        System.err.println("End " + (new Date()).getTime() / 1000);
    }

    /**
     * Process VEO
     *
     * This method opens a VEO and calls SAX to parse it looking for information
     * to include in the manifest. The parse ends as soon as we have completed
     * processing the vers:FileMetadata or vers:RecordMetadata element
     *
     * @param veo	the VEO to parse
     * @return
     * @throws java.io.IOException if something failed
     */
    private String processVEO(File veo) throws IOException {
        FileInputStream fis;
        BufferedInputStream bis;
        InputStreamReader fir;
        InputSource is;
        InputStreamReader isr;
        StringBuffer sb;

        // print diagnostic...
        // System.err.println((new Date()).getTime()/1000+" Processing '"+veo.getName()+"'");
        // check parameters
        if (veo == null) {
            throw new IOException("veo must not be null");
        }

        record = false;
        fileIdentifier = null;
        recIdentifier = null;
        titleWords = null;
        function = null;
        subject = null;
        accessStatus = null;
        sentence = null;
        dateRegistered = null;
        dateClosed = null;
        elementsFound = new Stack<>();

        // Open the VEO for reading
        try {
            fis = new FileInputStream(veo);
        } catch (FileNotFoundException e) {
            throw new IOException("XML file '" + veo.toString() + "' does not exist");
        }
        bis = new BufferedInputStream(fis);
        // this is necessary because SAX cannot auto-detect the encoding of the XML files
        // it will break if the encoding is not UTF-8
        try {
            fir = new InputStreamReader(bis, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            try {
                fis.close();
            } catch (IOException ioe) {
                /* ignore */ }
            throw new IOException("XMLParser.parse(): Error when setting encoding of input file: " + uee.getMessage());
        }
        is = new InputSource(fir);

        // do it...
        try {
            sax.parse(is, this);
        } catch (IOException ioe) {
            throw new IOException("XMLParser.parse(): SAX parse of " + veo.toString() + " due to: " + ioe.getMessage());
        } catch (SAXException e) {
            if (!e.getMessage().equals("Finished Parse")) {
                throw new IOException("XMLParser.parse(): SAX parse of " + veo.toString() + " due to: " + e.getMessage());
            }
        } finally {
            elementsFound = null;
            try {
                fir.close();
            } catch (IOException ioe) {
                /* ignore */ }
            try {
                bis.close();
            } catch (IOException ioe) {
                /* ignore */ }
            try {
                fis.close();
            } catch (IOException ioe) {
                /* ignore */ }
        }

        // generate dam:manifest_object_item
        sb = new StringBuffer();
        sb.append("   <dam:manifest_object_item>\n");

        // dam:computer_filename from File.getAbsoluteFile()
        sb.append("    <dam:computer_filename>");
        sb.append(trunc(veo.getName(), 256));
        sb.append("</dam:computer_filename>\n");

        // dam:file_identifier from fileIdentifier
        sb.append("    <dam:file_identifier>");
        sb.append(trunc(fileIdentifier, 32));
        sb.append("</dam:file_identifier>\n");

        // dam:record_identifier from recIdentifier if not null else empty elem
        if (recIdentifier != null) {
            sb.append("    <dam:vers_record_identifier>");
            sb.append(trunc(recIdentifier, 32));
            sb.append("</dam:vers_record_identifier>\n");
        } else {
            sb.append("    <dam:vers_record_identifier xsi:nil=\"true\"/>\n");
        }

        // dam:veo_title from titlewords
        sb.append("    <dam:veo_title>");
        sb.append(trunc(titleWords, 1024));
        sb.append("</dam:veo_title>\n");

        // dam:classification from function, subject, or default value
        if (function != null) {
            sb.append("    <dam:veo_classification>");
            sb.append(trunc(function, 1024));
            sb.append("</dam:veo_classification>\n");
        } else if (subject != null) {
            sb.append("    <dam:veo_classification>");
            sb.append(trunc(subject, 1024));
            sb.append("</dam:veo_classification>\n");
        } else {
            sb.append("    <dam:veo_classification>");
            sb.append("No classification");
            sb.append("</dam:veo_classification>\n");
        }

        // dam:veo_access_category from accessStatus or default value
        if (accessStatus != null) {
            sb.append("    <dam:veo_access_category>");
            sb.append(trunc(accessStatus, 1024));
            sb.append("</dam:veo_access_category>\n");
        } else {
            sb.append("    <dam:veo_access_category>");
            sb.append("None");
            sb.append("</dam:veo_access_category>\n");
        }

        // dam:veo_disposal_authority from sentence
        sb.append("    <dam:veo_disposal_authority>");
        sb.append(trunc(sentence, 1024));
        sb.append("</dam:veo_disposal_authority>\n");

        // start a date range
        sb.append("    <dam:veo_date_range>\n");

        // dam:veo_start_date is vers:DateTimeRegistered
        sb.append("     <dam:veo_start_date>");
        sb.append(dateRegistered.trim());
        sb.append("</dam:veo_start_date>\n");

        // dam:veo_end_date is empty (if record) or dateClosed (if a file & present)
        if (record) {
            sb.append("     <dam:veo_end_date xsi:nil=\"true\"/>\n");
        } else if (dateClosed != null) {
            sb.append("     <dam:veo_end_date>");
            sb.append(dateClosed.trim());
            sb.append("</dam:veo_end_date>\n");
        } else {
            sb.append("     <dam:veo_end_date>");
            sb.append("Not closed");
            sb.append("</dam:veo_end_date>\n");
        }
        sb.append("    </dam:veo_date_range>\n");

        // dam:veo_size_kb from File.getLength()
        sb.append("    <dam:size_kb>");
        sb.append(Math.round(veo.length() / 1000));
        sb.append("</dam:size_kb>\n");
        sb.append("   </dam:manifest_object_item>\n");

        // return string containing dam:manifest_object_item
        return sb.toString();
    }

    /**
     * Truncate an element
     *
     * This function trims the leading and trailing whitespace from a string, It
     * then truncates the result to a fixed length. If data is lost, a '*' is
     * appended
     */
    private String trunc(String value, int maxLen) {
        String s;

        s = value.trim();
        if (maxLen < 1) {
            maxLen = 1;
        }
        if (s.length() > maxLen) {
            s = s.substring(0, maxLen - 1) + "*";
        }
        return s;
    }

    /**
     * This forces the parser to ignore the reference to the external DTD (if
     * any is present)
     */
    private static final ByteArrayInputStream BAIS = new ByteArrayInputStream("".getBytes());

    @Override
    public InputSource resolveEntity(String name, String publicId, String baseURI, String systemId) throws SAXException {
        return new InputSource(BAIS);
    }

    /**
     * Entity Resolver
     *
     * If -n command line is set, try to use a local copy of the vers.dtd in the
     * directory in which the program is running
     */
    /*
    @Override
    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException {
        InputSource is;
        FileReader fr;
        File f;

        if (systemId.contains("vers.dtd") && !useStdDtd) {
            f = new File("vers.dtd");
            try {
                fr = new FileReader(f);
            } catch (FileNotFoundException fnfe) {
                System.err.println("There is no 'vers.dtd' in the local directory when using '-n' command line argument");
                return null;
            }
            return new InputSource(fr);
        }
        return null;
    }
     */
    /**
     * SAX Events captured
     */
    /**
     * Start of element
     *
     * This event is called when the parser finds a new element. The element
     * name is pushed onto the stack. The stack keeps the element path from the
     * root of the parse tree to the current element. We then check this path to
     * see if the current element matches one of the elements we are interested
     * in (the parent and grandparent of this element may be checked on the
     * stack to check the context). If they match, we start recording the
     * element value.
     */
    String[][] captureElements = {
        {"vers:Text", "vers:FileIdentifier", "vers:VEOIdentifier"},
        {"vers:Text", "vers:VERSRecordIdentifier", "vers:VEOIdentifier"},
        {"naa:TitleWords", "naa:Title", ""},
        {"naa:FunctionDescriptor", "naa:Function", ""},
        {"naa:ActivityDescriptor", "naa:Function", ""},
        {"naa:ThirdLevelDescriptor", "naa:Function", ""},
        {"vers:Keyword", "vers:Subject", ""},
        {"vers:KeywordLevel", "vers:Subject", ""},
        {"naa:AccessStatus", "naa:RightsManagement", ""},
        {"naa:Sentence", "", ""},
        {"naa:DateTimeRegistered", "", ""},
        {"vers:DateTimeClosed", "", ""}};

    @Override
    public void startElement(String uri, String localName,
            String qName, Attributes attributes)
            throws SAXException {
        boolean match;
        int i, j;

        // push element on stack
        elementsFound.push(qName);

        // check for vers:RecordMetadata (i.e. record VEO)
        if (qName.equals("vers:RecordMetadata")) {
            record = true;
        }

        // match against path for interesting elements
        match = false;
        elementFound = 0;
        for (i = 0; i < captureElements.length; i++) {
            for (j = 0; j < captureElements[i].length; j++) {
                if (!(captureElements[i][j].equals(elementsFound.elementAt(elementsFound.size() - j - 1))
                        || (captureElements[i][j].equals("")))) {
                    break;
                }
            }
            if (j == captureElements[i].length) {
                elementFound = i;
                match = true;
                break;
            }
        }

        // if match start recording element contents
        recording = match;
        elementValue.setLength(0);
    }

    /**
     * Processing the content of an element
     *
     * Remember the element content if we are interested in the element
     *
     * @throws org.xml.sax.SAXException
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        int i;

        if (!recording) {
            return;
        }
        for (i = start; i < start + length; i++) {
            switch (ch[i]) {
                case '&':
                    elementValue.append("&amp;");
                    break;
                case '<':
                    elementValue.append("&lt;");
                    break;
                case '>':
                    elementValue.append("&gt;");
                    break;
                default:
                    elementValue.append(ch[i]);
                    break;
            }
        }
    }

    /**
     * End of an element
     *
     * Found the end of an element. Pop the element from the top of the stack.
     * If recording, stop, and store the recorded element. If a
     * vers:FileMetadata or vers:RecordMetadata element has been finished, throw
     * a SAX Parse Error to force the parse to terminate.
     *
     * @throws org.xml.sax.SAXException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        // if recording store element value in appropriate global variable
        if (recording) {
            switch (elementFound) {
                case (0): // vers:fileIdentifier
                    fileIdentifier = elementValue.toString();
                    break;
                case (1): // vers:recordIdentifier
                    recIdentifier = elementValue.toString();
                    break;
                case (2): // naa:titleWords
                    titleWords = elementValue.toString();
                    break;
                case (3): // naa:functionDescriptor
                    if (function == null) {
                        function = elementValue.toString();
                    } else {
                        function = function + " " + elementValue.toString();
                    }
                    break;
                case (4): // naa:activityDescriptor
                    function = function + " " + elementValue.toString();
                    break;
                case (5): // naa:ThirdLevelDescriptor
                    function = function + " " + elementValue.toString();
                    break;
                case (6): // vers:Keyword
                    if (subject == null) {
                        subject = elementValue.toString();
                    } else {
                        subject = subject + " " + elementValue.toString();
                    }
                    break;
                case (7): // vers:KeywordLevel
                    if (subject == null) {
                        subject = elementValue.toString();
                    } else {
                        subject = subject + " " + elementValue.toString();
                    }
                    break;
                case (8): // naa:AccessStatus
                    accessStatus = elementValue.toString();
                    break;
                case (9): // naa:Sentence
                    sentence = elementValue.toString();
                    break;
                case (10): // naa:DateTimeRegistered
                    dateRegistered = elementValue.toString();
                    break;
                case (11): // vers:DateTimeClosed
                    dateClosed = elementValue.toString();
                    break;
            }
        }

        // stop recording
        recording = false;
        elementsFound.pop();

        // if finished a file or record metadata element stop parsing
        if (qName.equals("vers:RecordMetadata")
                || qName.equals("vers:FileMetadata")) {
            throw new SAXException("Finished Parse");
        }
    }

    /**
     * versDateTime
     *
     * Returns a date and time in the standard VERS format (see PROS 99/007
     * (Version 2), Specification 2, p146
     *
     * @param ms	milliseconds since the epoch (if zero, return current
     * date/time)
     */
    private String versDateTime(long ms) {
        Date d;
        SimpleDateFormat sdf;
        TimeZone tz;
        Locale l;
        String s;

        tz = TimeZone.getDefault();
        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        sdf.setTimeZone(tz);
        if (ms == 0) {
            d = new Date();
        } else {
            d = new Date(ms);
        }
        s = sdf.format(d);
        return s.substring(0, 22) + ":" + s.substring(22, 24);
    }

    /**
     * Main program
     *
     * @param args command line args
     */
    public static void main(String args[]) {
        V2ManifestGenerator mg;

        // create manifest generator instance
        mg = new V2ManifestGenerator(args);

        // write start of XML document
        mg.writePreamble();

        // if media export, assume top level directories represent media
        if (!mg.isInternetExport()) {
            mg.processMediaExport();
        } else {
            mg.processInternetExport();
        }

        // finish XML document
        mg.writePostamble();
    }
}
