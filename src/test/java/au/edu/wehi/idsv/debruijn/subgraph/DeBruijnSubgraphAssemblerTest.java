package au.edu.wehi.idsv.debruijn.subgraph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import htsjdk.samtools.util.SequenceUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import au.edu.wehi.idsv.AssemblyEvidence;
import au.edu.wehi.idsv.AssemblyParameters;
import au.edu.wehi.idsv.BreakpointSummary;
import au.edu.wehi.idsv.ProcessingContext;
import au.edu.wehi.idsv.SmallIndelSAMRecordAssemblyEvidence;
import au.edu.wehi.idsv.TestHelper;
import au.edu.wehi.idsv.picard.InMemoryReferenceSequenceFile;

import com.google.common.collect.Lists;


public class DeBruijnSubgraphAssemblerTest extends TestHelper {
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	private DeBruijnSubgraphAssembler DSA(int k) {
		ProcessingContext pc = getContext();
		AssemblyParameters p = pc.getAssemblyParameters();
		p.k = k;
		p.debruijnGraphVisualisationDirectory = new File(testFolder.getRoot(), "visualisation");
		p.visualiseAll = true;
		p.trackAlgorithmProgress = true;
		return new DeBruijnSubgraphAssembler(pc, AES());
	}
	@Test
	public void should_assemble_all_contigs() {
		DeBruijnSubgraphAssembler ass = DSA(3);
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence("TAAAGTC", Read(0, 1, "4M3S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence("AAAGTCT", Read(0, 2, "3M4S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence("TAAAGTC", Read(1, 1, "4M3S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence("AAAGTCT", Read(1, 2, "3M4S"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertEquals(2, results.size());
	}
	@Test
	public void should_assemble_RP_with_SC_anchor() {
		DeBruijnSubgraphAssembler ass = DSA(3);
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence("TAAT", Read(0, 15, "3M1S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(NRRP(withSequence(SequenceUtil.reverseComplement("TAAAGTC"), OEA(0, 1, "7M", true))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertEquals(1, results.size());
		assertEquals(3, results.get(0).getAssemblyAnchorLength());
	}
	@Test
	public void should_export_debruijn_graph() {
		DeBruijnSubgraphAssembler ass = DSA(3);
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence("TAAAGTC", Read(0, 1, "4M3S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence("AAAGTCT", Read(0, 2, "3M4S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence("TAAAGTC", Read(1, 1, "4M3S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence("AAAGTCT", Read(1, 2, "3M4S"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertTrue(new File(new File(testFolder.getRoot(), "visualisation"), "debruijn.kmers.polyA.gexf").exists());
		assertTrue(new File(new File(testFolder.getRoot(), "visualisation"), "debruijn.kmers.polyACGT.gexf").exists());
	}
	@Test
	public void should_track_progress() throws IOException {
		DeBruijnSubgraphAssembler ass = DSA(5);
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(NRRP(withSequence("GTCTTA", DP(0, 1, "6M", true, 0, 500, "6M", false))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence("CTTAGA", Read(0, 100, "1M5S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence("CTTAGA", Read(0, 101, "1M5S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence("TAAAGTC", Read(0, 1, "4M3S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence("AAAGTCT", Read(0, 2, "3M4S"))))));
		//results.addAll(Lists.newArrayList(ass.addEvidence(SCE(BWD, withSequence("TAAAGTCATGTATT", Read(0, 1, "5S9M"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		File file = new File(new File(testFolder.getRoot(), "visualisation"), "debruijn.assembly.metrics.polyA.bed");
		assertTrue(file.exists());
		String contents = new String(Files.readAllBytes(file.toPath())); 
		assertTrue(contents.contains("Times"));
		assertTrue(contents.contains("-"));
		assertTrue(contents.contains("polyA"));
		assertTrue(contents.contains("Kmers"));
		assertTrue(contents.contains("PathNodes \"1 (1 0 0)\"; "));
	}
	@Test
	public void should_assemble_both_directions() {
		DeBruijnSubgraphAssembler ass = DSA(3);
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence("TAAAGTC", Read(0, 1, "4M3S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence("AAAGTCT", Read(0, 2, "3M4S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(BWD, withSequence("TATG", Read(0, 10, "1S3M"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(BWD, withSequence("TTATG", Read(0, 10, "2S3M"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertEquals(2, results.size());
	}
	@Test
	public void should_anchor_at_reference_kmer() {
		DeBruijnSubgraphAssembler ass = DSA(3);
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence("TAAAGTC", Read(0, 1, "4M3S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence("AAAGTCT", Read(0, 2, "3M4S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(BWD, withSequence("CCGACAT", Read(0, 10, "3S4M"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(BWD, withSequence("GCCGACA", Read(0, 10, "4S3M"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertEquals(2, results.size());
		assertEquals(4, results.get(0).getBreakendSequence().length);
		assertEquals(4, results.get(1).getBreakendSequence().length);
	}
	@Test
	public void should_assemble_() {
		DeBruijnSubgraphAssembler ass = DSA(3);
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence("TAAAGTC", Read(0, 1, "4M3S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence("AAAGTCT", Read(0, 2, "3M4S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(BWD, withSequence("CCGACAT", Read(0, 10, "3S4M"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(BWD, withSequence("GCCGACA", Read(0, 10, "4S3M"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertEquals(2, results.size());
		assertEquals(4, results.get(0).getBreakendSequence().length);
	}
	@Test
	public void should_anchor_at_reference_kmer_large_kmer() {
		DeBruijnSubgraphAssembler ass = DSA(32);
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence(S(RANDOM).substring(0, 200), Read(0, 1, "100M100S"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertEquals(100, results.get(0).getBreakendSequence().length);
		assertEquals(200, results.get(0).getAssemblySequence().length);
	}
	@Test
	public void soft_clip_assembly_should_anchor_at_reference_kmer() {
		DeBruijnSubgraphAssembler ass = DSA(4);
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(BWD, withSequence("TTGCTCAAAA", Read(0, 1, "6S4M"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(NRRP(withSequence("TGCTG", OEA(0, 4, "5M", false))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(NRRP(withSequence("TGCTG", OEA(0, 5, "5M", false))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(NRRP(withSequence("TGCTG", OEA(0, 6, "5M", false))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertEquals(1, results.size());
		assertEquals("TTGCTCAAAA", S(results.get(0).getAssemblySequence()));
		assertEquals(1, results.get(0).getBreakendSummary().start);
		assertEquals(4, results.get(0).getAssemblySequence().length - results.get(0).getBreakendSequence().length);
	}
	@Test
	@Ignore("TODO: NYI: Not Yet Implemented")
	public void should_assemble_anchor_shorter_than_kmer() {
		DeBruijnSubgraphAssembler ass = DSA(5);
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence("ATTAGA", Read(0, 1, "1M5S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence("ATTAGA", Read(0, 1, "1M5S"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertEquals(1, results.size());
	}
	@Test
	@Ignore("TODO: NYI: Not Yet Implemented")
	public void should_assemble_anchor_shorter_than_kmer_with_indel_rp_support() {
		DeBruijnSubgraphAssembler ass = DSA(5);
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(NRRP(withSequence("GTCTTA", DP(0, 1, "8M", true, 0, 500, "8M", false))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence("CTTAGA", Read(0, 100, "1M5S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence("CTTAGA", Read(0, 100, "1M5S"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertEquals(1, results.size());
		assertEquals(3, results.get(0).getBreakendSummary().start);
	}
	@Test
	public void should_assemble_deletion() {
		//          1         2         3         4
		// 1234567890123456789012345678901234567890123456789
		// CATTAATCGCAAGAGCGGGTTGTATTCGACGCCAAGTCAGCTGAAGCACCATTACCCGATCAAA
		// ***************         ***************
		//        12345678         12345678
		// start anchor position = 8 -> 8 + k-1 = 15
		// end anchor position = 25
		String seq = S(RANDOM).substring(0, 0+15) + S(RANDOM).substring(24, 24+15);
		DeBruijnSubgraphAssembler ass = DSA(8);
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence(seq, Read(2, 1, "15M15S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(BWD, withSequence(seq, Read(2, 25, "15S15M"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertEquals(1, results.size());
		SmallIndelSAMRecordAssemblyEvidence e = (SmallIndelSAMRecordAssemblyEvidence)results.get(0);
		assertEquals(seq, S(e.getAssemblySequence()));
		assertEquals("15M9D15M", e.getBackingRecord().getCigarString());
		assertEquals(1, e.getBackingRecord().getAlignmentStart());
		assertEquals(seq, S(e.getBackingRecord().getReadBases()));
		assertEquals(new BreakpointSummary(2, FWD, 15, 15, 2, BWD, 25, 25), e.getBreakendSummary());
	}
	@Test
	public void should_assemble_deletion_microhomology() {
		String contig = "CATTAATCGCAATAAAATGTTCAAAACGACGCCAAGTCAGCTGAAGCACCATTACCCGATCAAA";
		ProcessingContext context = getContext(new InMemoryReferenceSequenceFile(new String[] { "Contig" }, new byte[][] { B(contig) }));
		context.getAssemblyParameters().k = 8;
		MockSAMEvidenceSource ses = SES(context);
		//          1         2         3         4
		// 1234567890123456789012345678901234567890123456789
		// CATTAATCGCAATAAAATGTTCAAAACGACGCCAAGTCAGCTGAAGCACCATTACCCGATCAAA
		// ***************         ***************
		//        12345678         12345678
		// start anchor position = 8 -> 8 + k-1 = 15
		// end anchor position = 25
		String seq = contig.substring(0, 0+15) + contig.substring(24, 24+15);
		DeBruijnSubgraphAssembler ass = new DeBruijnSubgraphAssembler(context, AES(context));
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence(seq, Read(0, 1, "15M15S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(BWD, ses, withSequence(seq, Read(0, 25, "15S15M"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertEquals(1, results.size());
		SmallIndelSAMRecordAssemblyEvidence e = (SmallIndelSAMRecordAssemblyEvidence)results.get(0);
		assertEquals(seq, S(e.getAssemblySequence()));
		assertEquals("15M9D15M", e.getBackingRecord().getCigarString());
		assertEquals(1, e.getBackingRecord().getAlignmentStart());
		assertEquals(seq, S(e.getBackingRecord().getReadBases()));
		assertEquals(1, e.getSAMRecord().getAlignmentStart());
		assertEquals(25, e.getRemoteSAMRecord().getAlignmentStart());
		assertEquals("CATTAATCGCAATAAAACGACGCCAAGTCA", S(e.getSAMRecord().getReadBases()));
		assertEquals("AACGACGCCAAGTCA", S(e.getRemoteSAMRecord().getReadBases()));
		assertEquals(new BreakpointSummary(0, FWD, 13, 17, 0, BWD, 23, 27), e.getBreakendSummary());
	}
	@Test
	public void breakpoint_microhomology_should_be_centre_aligned() {
		String contig = "CATTAATCGCAATAAAATGTTCAAAACGACGCCAAGTCAGCTGAAGCACCATTACCCGATCAAA";
		ProcessingContext context = getContext(new InMemoryReferenceSequenceFile(new String[] { "Contig" }, new byte[][] { B(contig) }));
		context.getAssemblyParameters().k = 8;
		MockSAMEvidenceSource ses = SES(context);
		//          1         2         3         4
		// 1234567890123456789012345678901234567890123456789
		// CATTAATCGCAATAAAATGTTCAAAACGACGCCAAGTCAGCTGAAGCACCATTACCCGATCAAA
		// *****************     *****************
		//        12345678         12345678
		// start anchor position = 8 -> 8 + k-1 = 15
		// end anchor position = 25
		String seq = contig.substring(0, 0+15) + contig.substring(24, 24+15);
		DeBruijnSubgraphAssembler ass = new DeBruijnSubgraphAssembler(context, AES(context));
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence(seq, Read(0, 1, "17M13S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(BWD, ses, withSequence(seq, Read(0, 23, "13S17M"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertEquals(1, results.size());
		SmallIndelSAMRecordAssemblyEvidence e = (SmallIndelSAMRecordAssemblyEvidence)results.get(0);
		assertEquals(seq, S(e.getAssemblySequence()));
		assertEquals("15M9D15M", e.getBackingRecord().getCigarString());
		assertEquals(1, e.getBackingRecord().getAlignmentStart());
		assertEquals(seq, S(e.getBackingRecord().getReadBases()));
		assertEquals(1, e.getSAMRecord().getAlignmentStart());
		assertEquals(25, e.getRemoteSAMRecord().getAlignmentStart());
		assertEquals("CATTAATCGCAATAAAACGACGCCAAGTCA", S(e.getSAMRecord().getReadBases()));
		assertEquals("AACGACGCCAAGTCA", S(e.getRemoteSAMRecord().getReadBases()));
		assertEquals(new BreakpointSummary(0, FWD, 13, 17, 0, BWD, 23, 27), e.getBreakendSummary());
	}
	@Test
	@Ignore() //  TODO: special case these?
	public void should_not_call_reference_bubble() {
		String seq = S(RANDOM).substring(0, 10);
		DeBruijnSubgraphAssembler ass = DSA(5);
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, withSequence(seq, Read(0, 1, "5M5S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(BWD, withSequence(seq, Read(0, 6, "5S5M"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		// no actual variant support - artifact of tracking reference/non-reference at kmer resolution, not base pair resolution  
		assertEquals(0, results.size());
	}
}
