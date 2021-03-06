/*

    Copyright (C) 2019-2020 Stanford HIVDB team

    This file is part of Sierra.

    Sierra is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Sierra is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Sierra.  If not, see <https://www.gnu.org/licenses/>.
*/
package edu.stanford.hivdb.drugresistance.algorithm;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.fstrf.stanfordAsiInterpreter.resistance.ASIParsingException;
import org.fstrf.stanfordAsiInterpreter.resistance.xml.XmlAsiTransformer;

import edu.stanford.hivdb.viruses.Gene;
import edu.stanford.hivdb.viruses.Virus;

public class DrugResistanceAlgorithm<VirusT extends Virus<VirusT>> implements Comparable<DrugResistanceAlgorithm<VirusT>>, Serializable {
	
	private static final long serialVersionUID = -6177916093946091256L;
	final private String name;
	final private String family;
	final private String version;
	final private String publishDate;
	final private transient String xmlText;
	final private transient String originalLevelText;
	final private transient String originalLevelSIR;
	final private transient Map<String, org.fstrf.stanfordAsiInterpreter.resistance.definition.Gene> geneMap;

	private static <VirusT extends Virus<VirusT>> Map<String, org.fstrf.stanfordAsiInterpreter.resistance.definition.Gene> initGeneMap(
		String xmlText, VirusT virus
	) {
		InputStream resource = new ByteArrayInputStream(xmlText.getBytes());
		Map<?, ?> geneMap;
		XmlAsiTransformer transformer = new XmlAsiTransformer(true);
		try {
			geneMap = transformer.transform(resource);
		} catch (Exception e) {
			throw new ExceptionInInitializerError(e);
		}
		Set<String> absGenes = new HashSet<>(virus.getAbstractGenes());
		return Collections.unmodifiableMap(geneMap
			.entrySet()
			.stream()
			// removes genes supported by algorithm but not by Virus implementation
			.filter(e -> absGenes.contains(e.getKey()))
			.collect(Collectors.toMap(
				e -> ((String) e.getKey()),
				e -> (org.fstrf.stanfordAsiInterpreter.resistance.definition.Gene) e.getValue()
			))
		);
	}
	
	private static Map<?, ?> getAlgorithmInfo(String xmlText) {
		InputStream resource = new ByteArrayInputStream(xmlText.getBytes());
		XmlAsiTransformer transformer = new XmlAsiTransformer(true);
		try {
			Map<?, ?> algInfo = (Map<?, ?>) transformer.getAlgorithmInfo(resource);
			return algInfo;
		} catch (ASIParsingException e) {
			throw new ExceptionInInitializerError(e);
		}
	}
	
	public DrugResistanceAlgorithm(VirusT virus, String xmlText) {
		this(null, null, null, null, virus, xmlText);
	}
	
	public DrugResistanceAlgorithm(String name, VirusT virus, String xmlText) {
		this(name, null, null, null, virus, xmlText);
	}
	
	public DrugResistanceAlgorithm(
		String name, String family, String version,
		String publishDate, VirusT virus, String xmlText
	) {
		Map<?, ?> algInfo = getAlgorithmInfo(xmlText);
		Map<?, ?> algNVD = (Map<?, ?>) algInfo.get("ALGNAME_ALGVERSION_ALGDATE");
		Map<?, ?> originalLevel = (Map<?, ?>) algInfo.get("ORDER1_ORIGINAL_SIR");
		this.name = name == null ? String.format("%s_%s", algNVD.get("ALGNAME"), algNVD.get("ALGVERSION")) : name;
		this.family = family == null ? (String) algNVD.get("ALGNAME") : family;
		this.version = version == null ? (String) algNVD.get("ALGVERSION") : version;
		this.publishDate = publishDate == null ? (String) algNVD.get("ALGDATE") : publishDate;
		this.originalLevelText = (String) originalLevel.get("ORIGINAL");
		this.originalLevelSIR = (String) originalLevel.get("SIR");
		this.xmlText = xmlText;
		this.geneMap = initGeneMap(xmlText, virus);
	}
	
	public String getName() {
		return name;
	}
	
	public String getDisplay() {
		return String.format("%s %s", family, version);
	}
	
	public String name() {
		return name;
	}
	
	public String getFamily() {
		return family;
	}
	
	public String getVersion() {
		return version;
	}
	
	public String getPublishDate() {
		return publishDate;
	}
	
	public String getOriginalLevelText() {
		return originalLevelText;
	}
	
	public SIREnum getOriginalLevelSIR() {
		return SIREnum.valueOf(originalLevelSIR);
	}
	
	public org.fstrf.stanfordAsiInterpreter.resistance.definition.Gene getASIGene(Gene<VirusT> gene) {
		return geneMap.get(gene.getAbstractGene());
	}
	
	public String getXMLText() {
		return xmlText;
	}
	
	public String getEnumCompatName() {
		String name = getName().replaceAll("[^_0-9A-Za-z-]", "_");
		name = name.replace("-stanford", "stanford").replace('-', 'p');
		if (name.matches("^\\d")) {
			name = "_" + name;
		}
		return name;
	}
	
	@Override
	public String toString() {
		return getName();
	}

	@Override
	public int compareTo(DrugResistanceAlgorithm<VirusT> other) {
		return new CompareToBuilder()
			.append(family, other.family)
			.append(version, other.version)
			.append(publishDate, other.publishDate)
			.toComparison();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) { return true; }
		// require singleton
		return false;
    }

}
