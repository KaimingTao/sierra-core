/*

    Copyright (C) 2019 Stanford HIVDB team

    Sierra is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Sierra is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package edu.stanford.hivdb.mutations;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Chars;

import edu.stanford.hivdb.comments.BoundComment;
import edu.stanford.hivdb.drugs.DrugClass;
import edu.stanford.hivdb.viruses.Gene;
import edu.stanford.hivdb.viruses.Strain;
import edu.stanford.hivdb.viruses.Virus;

public class AAMutation<VirusT extends Virus<VirusT>> implements Mutation<VirusT> {
	
	private transient AminoAcidPercents<VirusT> mainAAPcnts;

	protected static final int DEFAULT_MAX_DISPLAY_AAS = 6;

	protected final Gene<VirusT> gene;
	protected final int position;
	protected final Set<Character> aaChars;
	protected final int maxDisplayAAs;
	protected transient Character ref;
	protected transient List<MutationType<VirusT>> types;
	protected transient Boolean isAtDrugResistancePosition;
	protected transient Boolean isDRM;
	protected transient Boolean isSDRM;
	protected transient Boolean isTSM;
	protected transient Boolean isApobecMutation;
	protected transient Boolean isApobecDRM;
	protected transient Boolean isUnusual;
	protected transient Double highestMutPrevalence;
	protected transient DrugClass<VirusT> drmDrugClass;
	protected transient DrugClass<VirusT> sdrmDrugClass;
	protected transient DrugClass<VirusT> tsmDrugClass;
	
	public static Set<Character> normalizeAAChars(Set<Character> aaChars) {
		if (aaChars == null) { return null; }
		aaChars = new TreeSet<>(aaChars);
		if (aaChars.contains('#') || aaChars.contains('i')) {
			aaChars.remove('#');
			aaChars.remove('i');
			aaChars.add('_');
		}
		if (aaChars.contains('~') || aaChars.contains('d')) {
			aaChars.remove('~');
			aaChars.remove('d');
			aaChars.add('-');
		}
		if (aaChars.contains('Z') || aaChars.contains('.')) {
			aaChars.remove('Z');
			aaChars.remove('.');
			aaChars.add('*');
		}
		return Collections.unmodifiableSet(aaChars);
	}

	public AAMutation(Gene<VirusT> gene, int position, char aa) {
		this(gene, position, new char[] {aa}, DEFAULT_MAX_DISPLAY_AAS);
	}

	public AAMutation(Gene<VirusT> gene, int position, char[] aaCharArray) {
		this(gene, position, new TreeSet<>(Chars.asList(aaCharArray)), DEFAULT_MAX_DISPLAY_AAS);
	}

	public AAMutation(Gene<VirusT> gene, int position, char[] aaCharArray, int maxDisplayAAs) {
		this(gene, position, new TreeSet<>(Chars.asList(aaCharArray)), maxDisplayAAs);
	}

	public AAMutation(Gene<VirusT> gene, int position, Set<Character> aaChars) {
		this(gene, position, aaChars, DEFAULT_MAX_DISPLAY_AAS);
	}

	public AAMutation(Gene<VirusT> gene, int position, Set<Character> aaChars, int maxDisplayAAs) {
		if (position > gene.getAASize()) {
			throw new IllegalArgumentException("Length is out of bounds for this gene.");
		}
		this.gene = gene;
		this.aaChars = normalizeAAChars(aaChars);
		this.position = position;
		this.maxDisplayAAs = maxDisplayAAs;
	}

	protected int getMaxDisplayAAs() { return maxDisplayAAs; }
	
	protected AminoAcidPercents<VirusT> getMainAAPcnts() {
		if (mainAAPcnts == null) {
			mainAAPcnts = (
				gene.getVirusInstance()
				.getAminoAcidPercents(gene.getStrain(), "all", "all")
			);
		}
		return mainAAPcnts;
	}

	@Override
	@Deprecated
	public Mutation<VirusT> mergesWith(Mutation<VirusT> another) {
		if (another == null ||
			gene != another.getGene() ||
			position != another.getPosition()) {
			throw new IllegalArgumentException(String.format(
				"The other mutation must be at this position: %d (%s)",
				position, gene.getName()));
		}
		return mergesWith(another.getAAChars());
	}

	@Override
	public Mutation<VirusT> mergesWith(Collection<Character> otherAAChars) {
		Set<Character> newAAChars = getAAChars();
		newAAChars.addAll(otherAAChars);
		return new AAMutation<>(gene, position, newAAChars, maxDisplayAAs);
	}

	@Override
	@Deprecated
	public Mutation<VirusT> subtractsBy(Mutation<VirusT> another) {
		if (another == null ||
			gene != another.getGene() ||
			position != another.getPosition()
		) {
			throw new IllegalArgumentException(String.format(
				"The other mutation must be at this position: %d (%s)",
				position, gene.getName()));
		}
		return subtractsBy(another.getAAChars());
	}

	@Override
	public Mutation<VirusT> subtractsBy(Collection<Character> otherAAChars) {
		Set<Character> newAAChars = getAAChars();
		newAAChars.removeAll(otherAAChars);
		if (newAAChars.size() == 0) {
			return null;
		}
		return new AAMutation<>(gene, position, newAAChars, maxDisplayAAs);
	}

	@Override
	@Deprecated
	public Mutation<VirusT> intersectsWith(Mutation<VirusT> another) {
		if (another == null ||
			gene != another.getGene() ||
			position != another.getPosition()
		) {
			throw new IllegalArgumentException(String.format(
				"The other mutation must be at this position: %d (%s)",
				position, gene.getName()));
		}
		return intersectsWith(another.getAAChars());
	}

	@Override
	public Mutation<VirusT> intersectsWith(Collection<Character> otherAAChars) {
		Set<Character> newAAChars = getAAChars();
		newAAChars.retainAll(otherAAChars);
		if (newAAChars.size() == 0) {
			return null;
		}
		return new AAMutation<>(gene, position, newAAChars, maxDisplayAAs);
	}

	@Override
	public boolean isUnsequenced() { return false; }
	
	@Override
	public final Strain<VirusT> getStrain() { return gene.getStrain(); }

	@Override
	public final Gene<VirusT> getGene() { return gene; }
	
	@Override
	public final String getAbstractGene() { return gene.getAbstractGene(); }

	@Override
	public final String getReference() {
		return String.valueOf(getRefChar());
	}

	protected final Character getRefChar() {
		if (ref == null) {
			ref = gene.getRefChar(position);
		}
		return ref;
	}

	@Override
	public final int getPosition() { return position; }

	@Override
	public String getDisplayAAs() {
		if (aaChars.size() > maxDisplayAAs) {
			return "X";
		}
		return StringUtils.join(aaChars.toArray());
	}

	@Override
	public final Set<Character> getDisplayAAChars() {
		Set<Character> myAAChars = new TreeSet<>(aaChars);
		if (myAAChars.size() > maxDisplayAAs) {
			myAAChars.clear();
			myAAChars.add('X');
		}
		return myAAChars;
	}

	@Override
	public String getAAs() {
		return StringUtils.join(aaChars.toArray());
	}

	@Override
	public final Set<Character> getAAChars() {
		return new TreeSet<>(aaChars);
	}

	@Override
	public final Set<Mutation<VirusT>> split() {
		Set<Mutation<VirusT>> r = new TreeSet<>();
		for (char aa : getAAChars()) {
			if (aa == getRefChar()) {
				// ignore reference
				continue;
			}
			r.add(new AAMutation<>(gene, position, Sets.newHashSet(aa), DEFAULT_MAX_DISPLAY_AAS));
		}
		return r;
	}

	@Override
	public String getTriplet() { return ""; }

	@Override
	public String getInsertedNAs() { return ""; }

	@Override
	public final boolean isInsertion() { return getAAChars().contains('_'); }

	@Override
	public final boolean isDeletion() { return getAAChars().contains('-'); }

	@Override
	public final boolean isIndel() {
		Set<Character> myAAChars = getAAChars();
		return myAAChars.contains('_') || myAAChars.contains('-');
	}

	@Override
	public final boolean isMixture() {
		Set<Character> myAAChars = getAAChars();
		return myAAChars.size() > 1 || myAAChars.contains('X');
	}

	@Override
	public final boolean hasReference () { return getAAChars().contains(getRefChar()); }

	@Override
	public final boolean hasStop() { return getAAChars().contains('*'); }

	@Override
	public boolean hasBDHVN() {
		// no way to tell in BasicMutation
		return false;
	}

	@Override
	public boolean isAmbiguous() {
		Set<Character> myAAChars = getAAChars();
		return hasBDHVN() || myAAChars.size() > maxDisplayAAs || myAAChars.contains('X');
	}

	@Override
	public String getAAsWithRefFirst() {
		Set<Character> myAAChars = getDisplayAAChars();
		StringBuilder resultAAs = new StringBuilder();
		if (myAAChars.remove(getRefChar())) {
			resultAAs.append(getRefChar());
		}
		resultAAs.append(StringUtils.join(myAAChars.toArray()));
		return resultAAs.toString();
	}

	@Override
	public final MutationType<VirusT> getPrimaryType() {
		return getTypes().get(0);
	}

	@Override
	public String getAAsWithoutReference () {
		Set<Character> myAAChars = getDisplayAAChars();
		myAAChars.remove(getRefChar());
		return StringUtils.join(myAAChars.toArray());
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) { return true; }
		if (o == null) { return false; }
		if (!(o instanceof AAMutation<?>)) { return false; }
		AAMutation<?> m = (AAMutation<?>) o;

		// isDeletion and isInsertion is related to AAs
		return new EqualsBuilder()
			.append(gene, m.gene)
			.append(position, m.position)
			.append(getAAChars(), m.getAAChars())
			.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(4541, 83345463)
			.append(gene)
			.append(position)
			.append(getAAChars())
			.toHashCode();
	}

	@Override
	public final String toString() {
		return getHumanFormat();
	}

	@Override
	public final String getShortText() {
		return getShortHumanFormat();
	}

	@Override
	public int compareTo(Mutation<VirusT> mut) {
		int cmp = gene.compareTo(mut.getGene());
		if (cmp == 0) {
			cmp = new Integer(position).compareTo(mut.getPosition());
		}
		if (cmp == 0) {
			cmp = getAAs().compareTo(mut.getAAs());
		}
		return cmp;
	}

	@Override
	public final boolean containsSharedAA(Mutation<VirusT> queryMut) {
		if (this.gene.equals(queryMut.getGene()) &&
			this.position == queryMut.getPosition()
		) {
			return containsSharedAA(queryMut.getAAChars(), true);
		}
		return false;
	}

	@Override
	public boolean containsSharedAA(
		Set<Character> queryAAChars, boolean ignoreRefOrStops
	) {
		Set<Character> myAAChars = getAAChars();
		myAAChars.retainAll(queryAAChars);
		if (ignoreRefOrStops) {
			// Remove reference and stop codons so
			// that they are not responsible for a match
			myAAChars.remove(getRefChar());
			myAAChars.remove('*');
		}
		return !myAAChars.isEmpty();
	}

	@Override
	public boolean isAtDrugResistancePosition() {
		if (isAtDrugResistancePosition == null) {
			isAtDrugResistancePosition = getGenePosition().isDrugResistancePosition();
		}
		return isAtDrugResistancePosition;
	}
	
	private final boolean existsInMutationSets(Collection<MutationSet<VirusT>> mutSets) {
		return (
			mutSets.stream()
			.anyMatch(muts -> (
				muts.hasSharedAAMutation(this, /* ignoreRefOrStops = */false)
			))
		);
	}
	
	private final DrugClass<VirusT> lookupDrugClass(Map<DrugClass<VirusT>, MutationSet<VirusT>> lookup) {
		for (
			Map.Entry<DrugClass<VirusT>, MutationSet<VirusT>> entry : lookup.entrySet()
		) {
			if (entry.getValue().hasSharedAAMutation(this, /* ignoreRefOrStops=*/false)) {
				return entry.getKey();
			}
		}
		return null;
	}

	@Override
	public final boolean isDRM() {
		if (isDRM == null) {
			isDRM = existsInMutationSets(
				gene.getVirusInstance()
				.getDrugResistMutations()
				.values());
		}
		return isDRM;
	}

	@Override
	public final DrugClass<VirusT> getDRMDrugClass() {
		if (drmDrugClass == null) {
			if (!isDRM()) {
				return null;
			}
			drmDrugClass = lookupDrugClass(gene.getVirusInstance().getDrugResistMutations());
		}
		return drmDrugClass;
	}
	
	@Override
	public final boolean isTSM() {
		if (isTSM == null) {
			isTSM = existsInMutationSets(
				gene.getVirusInstance()
				.getRxSelectedMutations()
				.values());
		}
		return isTSM;
	}
	
	@Override
	public final DrugClass<VirusT> getTSMDrugClass() {
		if (tsmDrugClass == null) {
			if (!isTSM()) {
				return null;
			}
			tsmDrugClass = lookupDrugClass(gene.getVirusInstance().getRxSelectedMutations());
		}
		return tsmDrugClass;
	}

	@Override
	public GenePosition<VirusT> getGenePosition() {
		return new GenePosition<>(gene, position);
	}

	@Override
	public boolean isUnusual() {
		if (isUnusual == null) {
			Set<Character> myAAChars = getAAChars();
			if (myAAChars.contains('X')) {
				isUnusual = true;
			}
			else {
				isUnusual = (
					getMainAAPcnts()
					.containsUnusualAA(
						gene, position,
						StringUtils.join(myAAChars.toArray())
					)
				);
			}
		}
		return isUnusual;
	}

	@Override
	public boolean isSDRM() {
		if (isSDRM == null) {
			isSDRM = existsInMutationSets(
				gene.getVirusInstance()
				.getSurveilDrugResistMutations()
				.values());
		}
		return isSDRM;
	}

	@Override
	public final DrugClass<VirusT> getSDRMDrugClass() {
		if (sdrmDrugClass == null) {
			if (!isSDRM()) {
				return null;
			}
			sdrmDrugClass = lookupDrugClass(gene.getVirusInstance().getSurveilDrugResistMutations());
		}
		return sdrmDrugClass;
	}

	@Override
	public boolean isApobecMutation() {
		if (isApobecMutation == null) {
			isApobecMutation = (
				gene.getVirusInstance()
				.getApobecMutations()
				.hasSharedAAMutation(this, /* ignoreRefOrStops = */false)
			);
		}
		return isApobecMutation;
	}

	@Override
	public boolean isApobecDRM() {
		if (isApobecDRM == null) {
			isApobecDRM = (
				gene.getVirusInstance()
				.getApobecDRMs()
				.hasSharedAAMutation(this, /* ignoreRefOrStops = */false)
			);
		}
		return isApobecDRM;
	}

	@Override
	public double getHighestMutPrevalence() {
		if (highestMutPrevalence == null) {
			Set<Character> myAAChars = getAAChars();
			myAAChars.remove(getRefChar());
			myAAChars.remove('X');
			if (myAAChars.isEmpty()) {
				highestMutPrevalence = .0;
			}
			else {
				highestMutPrevalence = getMainAAPcnts().getHighestAAPercentValue(
					gene, position,
					StringUtils.join(myAAChars.toArray())) * 100;
			}
		}
		return highestMutPrevalence;
	}
	
	@Override
	public List<MutationPrevalence<VirusT>> getPrevalences() {
		return gene.getVirusInstance().getMutationPrevalence(getGenePosition());
	}

	@Override
	public List<MutationType<VirusT>> getTypes() {
		if (types == null) {
			types = (
				gene.getVirusInstance()
				.getMutationTypePairs()
				.stream()
				.filter(mtp -> mtp.isMutationMatched(this))
				.map(mtp -> mtp.getMutationType())
				.collect(Collectors.toList())
			);
			if (types.size() == 0) {
				types = Lists.newArrayList(
					gene.getVirusInstance().getOtherMutationType()
				);
			}
		}
		return types;
	}

	@Override
	public final String getASIFormat() {
		String fmtAAs = StringUtils.join(getAAChars().toArray());
		fmtAAs = (
			fmtAAs
			.replace('_', 'i')
			.replace('-', 'd')
			.replaceAll("[X*]", "Z")
		);
		return String.format("%s%d%s", getRefChar(), position, fmtAAs);
	}

	@Override
	public final String getHIVDBFormat() {
		String fmtAAs = StringUtils.join(getAAChars().toArray());
		fmtAAs = (
			fmtAAs
			.replace('_', '#')
			.replace('-', '~')
		);
		return String.format("%d%s", position, fmtAAs);
	}

	@Override
	public final String getHumanFormat() {
		String fmtAAs = getAAsWithRefFirst();
		fmtAAs = (
			fmtAAs
			.replaceAll("^_$", "Insertion")
			.replaceAll("^-$", "Deletion")
		);
		return String.format("%s%d%s", getRefChar(), position, fmtAAs);
	}

	@Override
	public final String getShortHumanFormat() {
		String fmtAAs = getAAsWithRefFirst();
		fmtAAs = (
			fmtAAs
			.replaceAll("^_$", "i")
			.replaceAll("^-$", "d")
		);
		return String.format("%s%d%s", getRefChar(), position, fmtAAs);
	}

	@Override
	public final String getHumanFormatWithoutLeadingRef() {
		return (getHumanFormat().substring(1));
	}

	@Override
	public final String getHumanFormatWithGene() {
		return String.format("%s_%s", gene.getName(), getHumanFormat());
	}

	@Override
	public Collection<BoundComment<VirusT>> getComments() {
		return gene.getVirusInstance().getConditionalComments().getComments(this);
	}

}