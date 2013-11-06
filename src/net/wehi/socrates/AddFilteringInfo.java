package net.wehi.socrates;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.StringTokenizer;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;
import net.sf.samtools.SAMFileReader.ValidationStringency;

import net.wehi.socrates.util.SAMFileInfo;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;




public class AddFilteringInfo {

	private static final Options options = new Options();
	static {
		Option help = new Option( "h", "help", false, "print this message" );
		Option verbose = new Option( "v", "verbose", false, "be verbose of progress" );
		options.addOption(verbose);
		options.addOption(help);
	}
	
	private static boolean is5primeSoftClip(SAMRecord s){
		int sc_start = s.getAlignmentStart() - s.getUnclippedStart();
		if (sc_start > 0)
			return true;
		return false;
	}
	private static int softclipLength5prime(SAMRecord s){
		int sc_start = s.getAlignmentStart() - s.getUnclippedStart();
		return sc_start;
	}
	private static int softclipLength3prime(SAMRecord s){
		int sc_end = s.getUnclippedEnd() - s.getAlignmentEnd();
		return sc_end;
	}
	private static boolean is3primeSoftClip(SAMRecord s){
		int sc_end = s.getUnclippedEnd() - s.getAlignmentEnd();
		if (sc_end > 0)
			return true;
		return false;
	}
	private static boolean isSoftClip(SAMRecord s){
		if (s.getUnclippedEnd() != s.getAlignmentEnd() || s.getUnclippedStart() != s.getAlignmentStart())
			return true;
		return false;
	}
	private static boolean isInteresingSoftclip(SAMRecord s, int breakpoint){
		if(s.getAlignmentStart()-2 <= breakpoint && s.getAlignmentEnd()+2>=breakpoint && (softclipLength3prime(s) > 4 || softclipLength5prime(s) > 4))
			return true;
//		if(s.getAlignmentStart() breakpoint  && )
//			return true;
		return false;
	}
	
	private static boolean isAnomalousPair(SAMRecord s, int lowerBound, int upperBound, int breakpoint){
		
		if (!s.getProperPairFlag())
			return true;
		
		if(!s.getReferenceName().equals(s.getMateReferenceName()) )
			return true;
		
		int iSize = Math.abs(s.getInferredInsertSize());
		if(iSize < lowerBound || iSize > upperBound){
			if(s.getAlignmentStart() < breakpoint && s.getInferredInsertSize() > 0)
				return true;
			if(s.getAlignmentEnd() > breakpoint && s.getInferredInsertSize() < 0)
				return true;
		}
		
		return false;
	}
	
    private static boolean mateMatch(SAMRecord s, String chr, int start, String dir, int upper, int readToBreakpointDist)
    {
      int end;

		if(dir.equals("-"))
		{
                        end=start+1+(upper-readToBreakpointDist);
		        start=start+1;
                }
                else
                {			
		        end=start-1;
                        start=start-1-(upper-readToBreakpointDist);
                }
		if(s.getMateReferenceName().equals(chr) &&
                        s.getMateAlignmentStart()>=start &&
                        (s.getMateAlignmentStart()+100)<=end)
                {
                        return true;
                }
		return false;
	}
	
	private static boolean isConcordantPair(SAMRecord s, int lowerBound, int upperBound, int breakpoint){
		if (!s.getProperPairFlag())
			return false;
		if(!s.getReferenceName().equals(s.getMateReferenceName()) )
			return false;
		int iSize = Math.abs(s.getInferredInsertSize());
		if(iSize >= lowerBound && iSize <= upperBound){
			if(s.getAlignmentStart() < breakpoint && s.getInferredInsertSize() > 0)
				return true;
			if(s.getAlignmentEnd() > breakpoint && s.getInferredInsertSize() < 0)
				return true;
		}
		return false;
	}
	
	private static boolean isAlignedAcrossBreakpoint(SAMRecord s, int breakpointPosition){
		if(s.getAlignmentStart() < breakpointPosition-4 && s.getAlignmentEnd() > breakpointPosition+4)
			return true;
		return false;
	}
	
   private static int[] getAnchorInsertStats(
			String chr, int start, String dir, SAMFileReader bamFile, int lowerInsertBound, int upperInsertBound, int readlen) {
      //get reads which  overlap the breakpoint

		SAMRecordIterator iter = bamFile.queryContained(chr, start-readlen+1, start+readlen-1);
      //for those which are softclipped check if mate aligns correct
      //side of BP
         //if so store the insert size
      //if no insert sizes (short sc) return 0,0
      return new int[] {0,0};
      //compute the mean insert size
      //compute the std insert size
   }


	private static int getSpanningCount(
			String bp1chr, int bp1start, String bp1dir, String bp2chr, int bp2start, String bp2dir,SAMFileReader bamFile, int lowerInsertBound, int upperInsertBound, int readlen) {
		String chr;
                int start;
                int end;

		if(bp1dir.equals("-"))
		{
		        start=bp1start+1;
                        end=bp1start+1+(upperInsertBound-readlen);
                        chr=bp1chr;
                }
                else
                {			
		        end=bp1start-1;
                        start=bp1start-1-(upperInsertBound-readlen);
                        chr=bp1chr;
                }

		SAMRecordIterator iter = bamFile.queryContained(chr, start, end);
		
		
		int spanningReadCount=0;
		
                int readToBreakpointDist;
		for(SAMRecord s; iter.hasNext();){
			s = iter.next();
                        if(bp1dir.equals("-"))
                        {
                                readToBreakpointDist=s.getAlignmentStart()-bp1start;
                        }
                        else
                        {
                                readToBreakpointDist=bp1start-s.getAlignmentEnd();
                        }

                        if(mateMatch(s,bp2chr,bp2start,bp2dir,upperInsertBound,readToBreakpointDist))
                        {
                                spanningReadCount++;
                        }
		}
		iter.close();
		return spanningReadCount;
	}
	
	private static String resultsToString(int[] result){
		return result[0]+"\t"+result[1];
	}
	
	public static void main(String[] args) throws IOException {
	   CommandLineParser parser = new GnuParser();
      try{
      CommandLine cmd = parser.parse(options, args);
      String[] allArgs = cmd.getArgs();
   	
		
		
		
		String bamfilename = allArgs[0];
		File bamfile = new File(bamfilename);
      //SAMFileInfo fileInfo = new SAMFileInfo(bamfilename);
      //HashMap<String,Point2D.Float> insertSizes = SAMFileInfo.getInsertMeanStdev(bamfile+".metrics");
      //meanInsertSizes = new HashMap<String, Float>();
      //for (String rg : insertSizes.keySet()) 
      //{
       //    Point2D.Float i = insertSizes.get(rg);
        //   maxInsertSizes.put( rg, i.x + 3*i.y );
      //}
	float[] stats = SAMFileInfo.getInsertMeanStdReadlen(bamfile+".metrics");
	
		
      //double mean = 451;
	float mean = stats[0];
		//double std = 57;
	float std = stats[1];
		int upper = (int) Math.round(mean + 3*std);
		int lower = (int) Math.round(mean - 3*std);
      //int readlen = 100;
	int readlen = (int) stats[2];
		//System.out.println(bamfilename);	
		final SAMFileReader reader = new SAMFileReader(bamfile);
	        reader.setValidationStringency(ValidationStringency.SILENT);	
		
		BufferedReader vars = new BufferedReader(new FileReader(allArgs[1]));
		String line;
		
		while( (line = vars.readLine()) != null) {
                        //System.out.println("*"+line);
			String[] cols = line.split("\t");
			StringTokenizer bp1 = new StringTokenizer(cols[3],":");
			String bp1dir = cols[4];
			StringTokenizer bp2 = new StringTokenizer(cols[15],":");
			String bp2dir = cols[16];

			String bp1chr = bp1.nextToken();
			int bp1start = Integer.parseInt(bp1.nextToken());
			int bp1end = bp1start;
			String bp2chr = bp2.nextToken();
			int bp2start = Integer.parseInt(bp2.nextToken());
			int bp2end = bp1start;
			

         //get reads overlapping breakpoint1
         //count up spanning reads
         //determine mean and std of insert size for anchors

			//System.out.println(bp1chr+":"+bp1start+"\t"+bp1dir+"\t"+bp2chr+":"+bp2start+"\t"+bp2dir);
			
			int spanning = getSpanningCount(bp1chr, bp1start, bp1dir, bp2chr, bp2start, bp2dir, reader, lower, upper,readlen);
			int[] bp1InsertMeanStd = getAnchorInsertStats(bp1chr, bp1start, bp1dir, reader, lower, upper,readlen);
			int[] bp2InsertMeanStd = getAnchorInsertStats(bp2chr, bp2start, bp2dir, reader, lower, upper,readlen);
			System.out.println(line+"\t"+resultsToString(bp1InsertMeanStd)+"\t"+resultsToString(bp2InsertMeanStd)+"\t"+spanning);
		}
      }catch(ParseException exp)
      {
         System.err.println(exp.getMessage());
         System.exit(1);
      }
		
	}
	
}
