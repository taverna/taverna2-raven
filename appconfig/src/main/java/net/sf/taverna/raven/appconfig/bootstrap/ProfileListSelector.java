/*******************************************************************************
 * Copyright (C) 2007 The University of Manchester   
 * 
 *  Modifications to the initial code base are copyright of their
 *  respective authors, or their employers as appropriate.
 * 
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *    
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *    
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 ******************************************************************************/
package net.sf.taverna.raven.appconfig.bootstrap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * When provided with a URL to the XML that defines a Profile list, the XML is
 * parsed to produce a list of Profile Definitions.
 * <p>
 * Its possible to store the first profile in the list to a local File, which is
 * used when Taverna is first run and no local profile is defined.
 * 
 * @author Stuart Owen
 * 
 */
public class ProfileListSelector {
	private List<ProfileDef> profiles = new ArrayList<>();
	private URL listUrl;
	
	public ProfileListSelector(URL listURL) throws Exception {	
		this.listUrl = listURL;
		processList(listURL);
	}
	
	private void processList(URL listURL) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		try (InputStream inputStream = listURL.openStream()) {
			NodeList list = builder.parse(inputStream).getElementsByTagName(
					"profile");
			for (int i = 0; i < list.getLength(); i++)
				processNode(listURL, list.item(i));
		}
	}
	
	/**
	 * Provides a list of the Profile definitions listed, ordered by version ascending
	 * @return
	 */
	public List<ProfileDef> getProfileList() {
		return profiles;
	}
	
	private void processNode(URL listURL,Node profileNode) {
		ProfileDef def = new ProfileDef();
		def.version = getChildNodeValue(profileNode, "version");
		def.location = getChildNodeValue(profileNode, "location");
		try {
			def.location = new URL(listURL, def.location).toExternalForm();
			profiles.add(def);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			System.out.println("Theres an error with the profile url:"+def.location);
		}
	}
	
	private String getChildNodeValue(Node node, String elementName) {
		for (Node child = node.getFirstChild(); child != null; child = child
				.getNextSibling())
			if (child.getNodeType() == Node.ELEMENT_NODE
					&& child.getNodeName().equals(elementName))
				return child.getFirstChild().getNodeValue();
		return null;
	}
	
	/**
	 * Stores the first profile version in the list to the destination
	 * @param destinationFile
	 */
	public void storeFirst(File destinationFile) throws Exception {
		if (profiles.isEmpty())
			throw new Exception("No profiles found in list:" + listUrl);
		store(destinationFile, profiles.get(0));
	}

	private void store(File destinationFile, ProfileDef profile)
			throws IOException {
		URL profileURL = new URL(profile.location);
		byte[] buffer = new byte[1024];
		int len;
		try (InputStream in = profileURL.openStream()) {
			if (!destinationFile.exists())
				destinationFile.createNewFile();
			try (OutputStream out = new FileOutputStream(destinationFile)) {
				while ((len = in.read(buffer)) != -1) 
					out.write(buffer, 0, len);
			}
		}
	}

	class ProfileDef {
		String location;
		String version;
	}
}
