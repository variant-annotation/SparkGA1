/*
 * Original author: ddecap; Modified by Hamid Mushtaq
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tudelft.utils;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import scala.Tuple2;

public class SAMRecordIterator 
{
    protected ChromosomeRange r;
    protected SAMRecord sam = null;
    protected int reads = 0;
    protected int currentStart = -1, currentEnd = -1, currentChr = -1;
    protected String chrString = "";
    protected boolean requireFixQuality = false;
    protected SAMFileHeader header;
    protected static final int INTERVAL_OVERLAP = 51;
	protected Tuple2<Integer, SAMRecord>[] samRecords;
	protected int index;
	protected int startIndex;
	protected int endIndex;

    public SAMRecordIterator(Tuple2<Integer, SAMRecord>[] samRecords, SAMFileHeader header) throws QualityException 
	{
        this.samRecords = samRecords;
		index = 0;
        r = new ChromosomeRange();
        this.header = header;
		this.startIndex = 0;
		this.endIndex = samRecords.length;
        getFirstRecord();
    }
    
    public SAMRecordIterator(Tuple2<Integer, SAMRecord>[] samRecords, SAMFileHeader header, ChromosomeRange r, 
		int startIndex, int endIndex) throws QualityException 
	{
        this.samRecords = samRecords;
        this.r = r;
        this.header = header;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		index = startIndex;
        getFirstRecord();
    }
    
	public boolean hasNext()
	{
		return (index < endIndex)? true : false;
	}
	
	public SAMRecord getNext()
	{
		return samRecords[index++]._2;
	}
	
    private void getFirstRecord() throws QualityException {
        sam = null;
        if(hasNext()) 
		{
            sam = getNext();
            sam.setHeader(header);
            //requireFixQuality = (QualityEncoding.guessEncoding(sam) == QualityEncoding.QENCODING.ILLUMINA);
            if (requireFixQuality) sam = QualityEncoding.fixMisencodedQuals(sam);
            reads++;
            currentStart = sam.getAlignmentStart();
            currentEnd = sam.getAlignmentEnd();
            currentChr = sam.getReferenceIndex();
            chrString = sam.getReferenceName();
        }
    }

    public SAMRecord next() 
	{
        SAMRecord tmp = sam;
        if (hasNext()) 
		{
            try {
                sam = getNext();
                sam.setHeader(header);
                if (requireFixQuality) sam = QualityEncoding.fixMisencodedQuals(sam);
                reads++;
                if(sam.getReferenceIndex() == currentChr && sam.getAlignmentStart() <= currentEnd + INTERVAL_OVERLAP)
				{
                    if (sam.getAlignmentEnd() > currentEnd) 
                        currentEnd = sam.getAlignmentEnd();
                } 
				else 
				{
                    // new region to start here, add current!
                    r.addRange(chrString, currentStart, currentEnd);
                    currentStart = sam.getAlignmentStart();
                    currentEnd = sam.getAlignmentEnd();
                    currentChr = sam.getReferenceIndex();
                    chrString = sam.getReferenceName();
                }
            } 
			catch (QualityException ex) 
			{
                ex.printStackTrace();
                sam = null;
            }
        } else {
            sam = null;
        }
        return tmp;
    }

	public void addLastChrRange()
	{
		if (reads > 0)
			r.addRange(chrString, currentStart, currentEnd);
	}
	
    public int getCount() 
	{
        return reads;
    }
    
    public ChromosomeRange getChromosomeRange() 
	{
        return r;
    }
}
