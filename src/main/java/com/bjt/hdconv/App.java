package com.bjt.hdconv;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        try {
            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            final XPathFactory xPathFactory = XPathFactory.newInstance();
            final XPath xPath = xPathFactory.newXPath();
            final XPathExpression xpContourLine = xPath.compile("DataSet/member/ContourLine");
            final XPathExpression xpHeight = xPath.compile("propertyValue[@uom='m']");
            final XPathExpression xpPosList = xPath.compile("geometry/LineString/posList");

            final File curDir = new File(".");
            final Collection<File> files = FileUtils.listFiles(curDir, new String[]{"gml"}, true);

            for (File file : files) {
                final File outputFile = new File(file.getAbsolutePath() + ".dat");
                if(outputFile.exists()) {
                    outputFile.delete();
                }
                
                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {
                    inputStream = new FileInputStream(file);
                    outputStream = new FileOutputStream(outputFile);

                    final Document document = documentBuilder.parse(inputStream);
                    final NodeList contourNodes = (NodeList) xpContourLine.evaluate(document, XPathConstants.NODESET);
                    final int contourCount = contourNodes.getLength();
                    writeInt(outputStream, contourCount);
                    for(int i = 0; i < contourCount; i++) {
                        final Node contourNode = contourNodes.item(i);
                        final String heightString = (String) xpHeight.evaluate(contourNode, XPathConstants.STRING);
                        final String posListString = (String)xpPosList.evaluate(contourNode, XPathConstants.STRING);

                        System.out.println(String.format("%s %s", heightString, posListString));

                        final int height = Integer.valueOf(heightString);
                        final String[] posStrings = posListString.split(" ");
                        final List<Float> positions = new ArrayList<Float>();
                        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;
                        for(int pos = 0; pos < posStrings.length; pos += 2) {
                            final float posX = Float.valueOf(posStrings[pos]);
                            final float posY = Float.valueOf(posStrings[pos+1]);
                            if(posX < minX) minX = posX;
                            if(posX > maxX) maxX = posX;
                            if(posY < minY) minY = posY;
                            if(posY > maxY) maxY = posY;
                            positions.add(posX);
                            positions.add(posY);
                        }
                        writeInt(outputStream, height);
                        writeFloat(outputStream, minX);
                        writeFloat(outputStream, minY);
                        writeFloat(outputStream, maxX);
                        writeFloat(outputStream, maxY);
                        writeInt(outputStream, positions.size() / 2);
                        for(Float pos : positions) {
                            writeFloat(outputStream, pos);
                        }
                    }

                    System.out.println();

                }finally {
                    if(inputStream != null) {
                        try {
                            inputStream.close();
                        } catch(IOException e) {}
                    }
                    if(outputStream != null) {
                        try {
                            outputStream.close();
                        } catch(IOException e) {}
                    }
                }

            }

            System.out.println();

        } catch (Exception e) {

            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void writeFloat(final OutputStream o, float f) throws IOException {
        writeInt(o, (int)(f * 10) /*(Float.floatToIntBits(f)*/);
    }

    final static void writeInt(final OutputStream o, final int i) throws IOException {
        byte[] bytes = new byte[]{
                (byte) (i & 0xff),
                (byte) ((i >> 8) & 0xffff),
                (byte)((i >> 16) & 0xffffff),
                (byte)(i >> 24)

        };
        o.write(bytes);
    }
}
