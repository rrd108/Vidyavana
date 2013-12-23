package hu.vidyavana.ui.model;

import hu.vidyavana.ui.view.VZoneView;
import javax.swing.text.*;

public class VEditorKit extends StyledEditorKit
{
	ViewFactory vf;


	@Override
	public ViewFactory getViewFactory()
	{
		if(vf == null)
			vf = new ZoneViewFactory();
		return vf;
	}


	public class ZoneViewFactory implements ViewFactory
	{
		@Override
		public View create(Element elem)
		{
			String kind = elem.getName();
			if(kind != null)
			{
				if(kind.equals(AbstractDocument.ContentElementName))
					return new LabelView(elem);

				else if(kind.equals(AbstractDocument.ParagraphElementName))
					return new ParagraphView(elem);

				else if(kind.equals(AbstractDocument.SectionElementName))
					return new VZoneView(elem, View.Y_AXIS);

				else if(kind.equals(StyleConstants.ComponentElementName))
					return new ComponentView(elem);

				else if(kind.equals(StyleConstants.IconElementName))
					return new IconView(elem);
			}

			// default to text display
			return new LabelView(elem);
		}
	}
}
