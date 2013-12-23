package hu.vidyavana.ui.model;

import java.util.ArrayList;
import javax.swing.text.*;

// TODO insert method, ElementBuffer and their references to accept List<ElementSpec>
public class VDocument extends DefaultStyledDocument
{
	private static final char[] EOL_ARRAY = {'\n'};

	private ArrayList<ElementSpec> batch = null;


	public void startBatchInsert()
	{
		batch = new ArrayList<>();
	}


	public void append(char[] chars, AttributeSet a)
	{
		a = a.copyAttributes();
		batch.add(new ElementSpec(a, ElementSpec.ContentType, chars, 0, chars.length));
	}


	public void appendPara(AttributeSet a)
	{
		batch.add(new ElementSpec(a, ElementSpec.ContentType, EOL_ARRAY, 0, 1));
		Element para = getParagraphElement(0);
		AttributeSet paraAttr = para.getAttributes();
		batch.add(new ElementSpec(null, ElementSpec.EndTagType));
		batch.add(new ElementSpec(paraAttr, ElementSpec.StartTagType));
	}


	public void endBatchInsert(int offs) throws BadLocationException
	{
		ElementSpec[] inserts = new ElementSpec[batch.size()];
		batch.toArray(inserts);
		super.insert(offs, inserts);
	}
	
	
	@Override
	public String getText(int offset, int length) throws BadLocationException
	{
		System.out.println("Offset: "+offset+", length: "+length);
		return super.getText(offset, length);
	}
	
	
	@Override
	public void getText(int offset, int length, Segment txt) throws BadLocationException
	{
		System.out.println("Offset: "+offset+", length: "+length+", has segment");
		super.getText(offset, length, txt);
	}
}
