package org.matsim.episim.munich;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class XMLCombiner {

	public static void main(String[] args) {
		try {
			// Specify the input and output files
			String file1Path = "C:\\models\\mito7\\muc\\scenOutput\\tengos_25pct_matsim_sunday\\no_pt\\output_events-1.0.xml";
			String file2Path = "C:\\models\\mito7\\muc\\scenOutput\\tengos_25pct_matsim_sunday\\pt\\output_events-1.0.xml";
			String outputFilePath = "C:\\models\\mito7\\muc\\scenOutput\\tengos_25pct_matsim_sunday\\output_events-1.0_combined.xml";

			// Load and parse the first XML file
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc1 = dBuilder.parse(new File(file1Path));

			// Load and parse the second XML file
			Document doc2 = dBuilder.parse(new File(file2Path));

			// Normalize XML structure
			doc1.getDocumentElement().normalize();
			doc2.getDocumentElement().normalize();

			// Get the root element of the first document
			Element root1 = doc1.getDocumentElement();

			// Get all event nodes from the first and second documents
			NodeList events1 = doc1.getElementsByTagName("event");
			NodeList events2 = doc2.getElementsByTagName("event");

			// Collect all event nodes in a list
			List<Element> allEvents = new ArrayList<>();

			// Add all event nodes from the first document
			for (int i = 0; i < events1.getLength(); i++) {
				allEvents.add((Element) events1.item(i));
			}

			// Add all event nodes from the second document
			for (int i = 0; i < events2.getLength(); i++) {
				Node event = events2.item(i);
				Node importedNode = doc1.importNode(event, true);
				allEvents.add((Element) importedNode);
			}

			// Sort the event nodes by the 'time' attribute
			allEvents.sort(Comparator.comparing(e -> Double.parseDouble(e.getAttribute("time"))));

			// Remove all current event nodes from the first document's root
			while (root1.hasChildNodes()) {
				root1.removeChild(root1.getFirstChild());
			}

			// Append the sorted event nodes back to the root element
			for (Element event : allEvents) {
				root1.appendChild(event);
			}

			// Write the combined and sorted document to the output file with indentation
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();

			// Set the output properties for minimal spacing
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "1"); // Minimal indentation

			DOMSource source = new DOMSource(doc1);
			StreamResult result = new StreamResult(new File(outputFilePath));
			transformer.transform(source, result);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

